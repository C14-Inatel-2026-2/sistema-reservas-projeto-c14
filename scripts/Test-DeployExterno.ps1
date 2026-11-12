param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$Commit,
    [ValidateSet('Criar', 'Conferir')]
    [string]$Fase = 'Criar',
    [string]$StatePath = (Join-Path $PSScriptRoot '../backend/target/deploy-smoke.json')
)

$ErrorActionPreference = 'Stop'
$address = [uri]$BaseUrl
$local = $address.IsAbsoluteUri -and $address.IsLoopback -and $address.Scheme -eq 'http'
$render = $address.IsAbsoluteUri -and $address.Scheme -eq 'https' -and $address.Host.EndsWith('.onrender.com')
if ((-not $local -and -not $render) -or $address.UserInfo -or
    $address.Query -or $address.Fragment -or $address.AbsolutePath -ne '/') {
    throw 'Informe a URL HTTPS raiz do servico no Render ou uma URL HTTP local de teste.'
}
$BaseUrl = $address.GetLeftPart([System.UriPartial]::Authority)

function Request-Json {
    param([string]$Path, [string]$Method = 'Get', $Body = $null, [int]$ExpectedStatus = 200)
    $arguments = @{
        Uri = $BaseUrl + $Path; Method = $Method; UseBasicParsing = $true
        TimeoutSec = 30; MaximumRedirection = 0
    }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = $Body | ConvertTo-Json -Compress
    }
    try {
        $response = Invoke-WebRequest @arguments
    } catch {
        if ($ExpectedStatus -ge 400 -and $_.Exception.Response -and
            [int]$_.Exception.Response.StatusCode -eq $ExpectedStatus) {
            return
        }
        throw "Falha em $Method $Path. Confira o status da API."
    }
    if ([int]$response.StatusCode -ne $ExpectedStatus) {
        throw "Status inesperado em $Method $Path : $($response.StatusCode)."
    }
    if ($response.Content) {
        $content = $response.Content
        if ($content -is [byte[]]) { $content = [Text.Encoding]::UTF8.GetString($content) }
        return ($content | ConvertFrom-Json)
    }
}

$health = Request-Json '/actuator/health'
$info = Request-Json '/actuator/info'
if ($health.status -ne 'UP' -or $info.app.commit -ne $Commit) {
    throw 'A API precisa estar saudavel no commit informado. Aguarde a conclusao do deploy.'
}

if ($Fase -eq 'Criar') {
    if (Test-Path -LiteralPath $StatePath) {
        throw 'Ja existe um estado de teste. Use -Fase Conferir ou outro -StatePath.'
    }
    $email = 'deploy-' + [guid]::NewGuid().ToString('N') + '@example.test'
    $userBody = @{ nome = 'Teste de deploy'; email = $email; senha = 'senha-teste-123'; tipo = 'ALUNO' }
    $user = Request-Json '/usuarios' 'Post' $userBody 201
    if (-not $user.id -or ($user.PSObject.Properties.Name -contains 'senha') -or
        ($user.PSObject.Properties.Name -contains 'hash')) {
        throw 'Cadastro invalido ou senha exposta na resposta.'
    }
    Request-Json '/usuarios' 'Post' $userBody 409 | Out-Null
    $resource = Request-Json '/recursos/quadras' 'Post' @{
        nome = 'Quadra de teste de deploy'; localizacao = 'Demonstracao'; modalidade = 'Futsal'; coberta = $true
    } 201
    $start = [DateTime]::UtcNow.AddDays(2)
    $reservationBody = @{
        usuarioId = $user.id; recursoId = $resource.id
        dataHoraInicio = $start.ToString('yyyy-MM-ddTHH:mm:ss')
        dataHoraFim = $start.AddHours(1).ToString('yyyy-MM-ddTHH:mm:ss')
    }
    $reservation = Request-Json '/reservas' 'Post' $reservationBody 201
    Request-Json '/reservas' 'Post' $reservationBody 409 | Out-Null
    $confirmed = Request-Json "/reservas/$($reservation.id)/confirmar" 'Patch'
    if ($confirmed.status -ne 'CONFIRMADA') { throw 'A reserva nao foi confirmada.' }
    $state = @{
        baseUrl = $BaseUrl; originalCommit = $Commit; usuarioId = $user.id
        recursoId = $resource.id; reservaId = $reservation.id; email = $email
    }
    $fullPath = [IO.Path]::GetFullPath($StatePath)
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($fullPath))
    $state | ConvertTo-Json | Set-Content -LiteralPath $fullPath -Encoding UTF8
    Write-Host 'PASSOU: cadastro, e-mail unico, reserva, conflito de horario e confirmacao.'
    Write-Host 'Depois do redeploy, execute novamente com -Fase Conferir e o commit publicado.'
} else {
    $state = Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
    if ($state.baseUrl -ne $BaseUrl) { throw 'O estado pertence a outro servico.' }
    $user = Request-Json "/usuarios/$($state.usuarioId)"
    $reservation = Request-Json "/reservas/$($state.reservaId)"
    if ($user.email -ne $state.email -or $reservation.usuarioId -ne $state.usuarioId -or
        $reservation.recursoId -ne $state.recursoId -or $reservation.status -ne 'CONFIRMADA') {
        throw 'Os registros nao persistiram como esperado.'
    }
    Write-Host 'PASSOU: usuario e reserva confirmada persistiram apos o redeploy.'
}
