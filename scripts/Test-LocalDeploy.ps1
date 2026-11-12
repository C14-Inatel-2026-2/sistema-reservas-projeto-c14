param(
    [string]$BaseUrl = 'http://127.0.0.1:8081',
    [ValidatePattern('^[a-z0-9][a-z0-9_-]*$')]
    [string]$ProjectName = 'c14-reservas'
)

$ErrorActionPreference = 'Stop'
if (-not ([uri]$BaseUrl).IsLoopback) { throw 'Este teste so pode usar uma API local.' }
$BaseUrl = $BaseUrl.TrimEnd('/')
$repo = Split-Path -Parent $PSScriptRoot

function Wait-Healthy {
    $deadline = [DateTime]::UtcNow.AddMinutes(2)
    do {
        try {
            $health = Invoke-RestMethod -Uri ($BaseUrl + '/actuator/health') -TimeoutSec 5
            if ($health.status -eq 'UP') { return }
        } catch { }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)
    throw 'A aplicacao nao ficou saudavel dentro do prazo.'
}

function Assert-True($Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

Wait-Healthy
$info = Invoke-RestMethod -Uri ($BaseUrl + '/actuator/info')
Assert-True ([bool]$info.app.commit) 'A API nao informou o commit.'
$email = 'deploy-' + [guid]::NewGuid().ToString('N') + '@example.test'
$userBody = @{ nome = 'Teste de deploy'; email = $email; senha = 'senha-teste-123'; tipo = 'ALUNO' } | ConvertTo-Json
$user = Invoke-RestMethod -Uri ($BaseUrl + '/usuarios') -Method Post -ContentType 'application/json' -Body $userBody
Assert-True ($user.id -and -not ($user.PSObject.Properties.Name -contains 'senha')) 'Cadastro invalido ou senha exposta.'
$resourceBody = @{ nome = 'Quadra de teste'; localizacao = 'Teste local'; modalidade = 'Futsal'; coberta = $true } | ConvertTo-Json
$resource = Invoke-RestMethod -Uri ($BaseUrl + '/recursos/quadras') -Method Post -ContentType 'application/json' -Body $resourceBody
$start = [DateTime]::UtcNow.AddDays(1)
$reservationBody = @{
    usuarioId = $user.id; recursoId = $resource.id
    dataHoraInicio = $start.ToString('yyyy-MM-ddTHH:mm:ss')
    dataHoraFim = $start.AddHours(1).ToString('yyyy-MM-ddTHH:mm:ss')
} | ConvertTo-Json
$reservation = Invoke-RestMethod -Uri ($BaseUrl + '/reservas') -Method Post -ContentType 'application/json' -Body $reservationBody
Assert-True ($reservation.status -eq 'PENDENTE') 'A reserva nao foi criada.'
Write-Host 'PASSOU: cadastro de usuario, recurso e reserva com MySQL real.'

docker compose --project-directory $repo -p $ProjectName restart app
if ($LASTEXITCODE -ne 0) { throw 'Falha ao reiniciar a aplicacao.' }
Wait-Healthy
$savedUser = Invoke-RestMethod -Uri ($BaseUrl + '/usuarios/' + $user.id)
$savedReservation = Invoke-RestMethod -Uri ($BaseUrl + '/reservas/' + $reservation.id)
Assert-True ($savedUser.email -eq $email -and $savedReservation.id -eq $reservation.id) 'Os dados nao persistiram.'
Write-Host 'PASSOU: usuario e reserva persistiram apos reiniciar a aplicacao.'

try {
    docker compose --project-directory $repo -p $ProjectName stop db
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao interromper o banco de teste.' }
    $status = 0
    try {
        $response = Invoke-WebRequest -Uri ($BaseUrl + '/actuator/health') -TimeoutSec 45 -UseBasicParsing
        $status = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        else { throw }
    }
    Assert-True ($status -eq 503) 'O health check nao sinalizou a indisponibilidade do banco.'
    Write-Host 'PASSOU: health check retorna HTTP 503 quando o banco esta indisponivel.'
} finally {
    docker compose --project-directory $repo -p $ProjectName start db
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao religar o banco de teste.' }
}
Wait-Healthy
Write-Host 'PASSOU: API e banco disponiveis novamente.'
