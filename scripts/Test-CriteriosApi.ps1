param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$Commit,
    [string]$StatePath = (Join-Path $PSScriptRoot '../backend/target/aceite-api.json')
)

$ErrorActionPreference = 'Stop'

# Reutiliza a validacao da URL e do SHA e preserva uma reserva confirmada.
& (Join-Path $PSScriptRoot 'Test-DeployExterno.ps1') `
    -BaseUrl $BaseUrl -Commit $Commit -Fase Criar -StatePath $StatePath

$state = Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
$baseAddress = $state.baseUrl
$checks = [Collections.Generic.List[string]]::new()
$checks.Add('cadastro sem senha exposta, e-mail unico, conflito de horario e confirmacao')

function Request-Criterio {
    param([string]$Path, [string]$Method, $Body, [int]$Status)
    $httpOptions = @{
        Uri = $baseAddress + $Path; Method = $Method; UseBasicParsing = $true
        TimeoutSec = 30; MaximumRedirection = 0
    }
    if ($null -ne $Body) {
        $httpOptions.ContentType = 'application/json; charset=utf-8'
        $httpOptions.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
    }
    try {
        $response = Invoke-WebRequest @httpOptions
    } catch {
        if ($Status -ge 400 -and $_.Exception.Response -and
            [int]$_.Exception.Response.StatusCode -eq $Status) {
            return
        }
        throw "Falha em $Method $Path; esperado HTTP $Status."
    }
    if ([int]$response.StatusCode -ne $Status) {
        throw "Status incorreto em $Method $Path; esperado HTTP $Status."
    }
    if ($response.Content) {
        $content = $response.Content
        if ($content -is [byte[]]) { $content = [Text.Encoding]::UTF8.GetString($content) }
        return ($content | ConvertFrom-Json)
    }
}

$valid = @{
    nome = 'Validacao de aceite'; email = ('aceite-' + [guid]::NewGuid().ToString('N') + '@example.test')
    senha = 'senha-teste-123'; tipo = 'ALUNO'
}
foreach ($field in @('nome', 'email', 'senha', 'tipo')) {
    $invalid = $valid.Clone()
    switch ($field) {
        'nome' { $invalid.nome = '' }
        'email' { $invalid.email = 'invalido' }
        'senha' { $invalid.senha = '123' }
        'tipo' { $invalid.Remove('tipo') }
    }
    Request-Criterio '/usuarios' Post $invalid 400 | Out-Null
    $checks.Add("cadastro invalido: $field retorna HTTP 400")
}
$oversize = $valid.Clone()
$oversize.senha = ([string][char]0x00E1) * 37
Request-Criterio '/usuarios' Post $oversize 422 | Out-Null
$checks.Add('senha com 74 bytes UTF-8 retorna HTTP 422')

$sala = Request-Criterio '/recursos/salas' Post @{
    nome = 'Sala de aceite'; localizacao = 'Demonstracao'; capacidadePessoas = 6; possuiProjetor = $true
} 201
if ($sala.tipo -ne 'SALA_ESTUDO' -or $sala.capacidadePessoas -ne 6 -or -not $sala.possuiProjetor) {
    throw 'Mapeamento de sala incorreto.'
}
$patrimonio = 'ACEITE-' + [guid]::NewGuid().ToString('N')
$equipmentBody = @{
    nome = 'Equipamento de aceite'; localizacao = 'Demonstracao'
    laboratorio = 'Lab teste'; numeroPatrimonio = $patrimonio
}
$equipment = Request-Criterio '/recursos/equipamentos' Post $equipmentBody 201
if ($equipment.tipo -ne 'EQUIPAMENTO_LABORATORIO' -or
    $equipment.laboratorio -ne 'Lab teste' -or $equipment.numeroPatrimonio -ne $patrimonio) {
    throw 'Mapeamento de equipamento incorreto.'
}
Request-Criterio '/recursos/equipamentos' Post $equipmentBody 409 | Out-Null
$quadra = Request-Criterio "/recursos/$($state.recursoId)" Get $null 200
if ($quadra.tipo -ne 'QUADRA' -or $quadra.modalidade -ne 'Futsal' -or -not $quadra.coberta) {
    throw 'Mapeamento de quadra incorreto.'
}
$checks.Add('tres tipos de recurso preservam campos; patrimonio duplicado retorna HTTP 409')

$confirmed = Request-Criterio "/reservas/$($state.reservaId)" Get $null 200
$begin = [DateTime]$confirmed.dataHoraFim
$reservationBody = @{
    usuarioId = $state.usuarioId; recursoId = $state.recursoId
    dataHoraInicio = $begin.ToString('yyyy-MM-ddTHH:mm:ss')
    dataHoraFim = $begin.AddHours(1).ToString('yyyy-MM-ddTHH:mm:ss')
}
$adjacent = Request-Criterio '/reservas' Post $reservationBody 201
$checks.Add('reserva adjacente ao fim da anterior aceita com HTTP 201')
$cancel = Request-Criterio "/reservas/$($adjacent.id)/cancelar" Patch @{ motivo = 'Teste de aceite' } 200
if ($cancel.reserva.status -ne 'CANCELADA' -or $cancel.percentualMultaAplicada -ne 0) {
    throw 'Cancelamento sem regra incorreto.'
}
Request-Criterio "/reservas/$($adjacent.id)/cancelar" Patch $null 422 | Out-Null
$checks.Add('cancelamento sem regra aplica zero; segunda tentativa retorna HTTP 422')

$past = $reservationBody.Clone()
$past.dataHoraInicio = [DateTime]::UtcNow.AddDays(-2).ToString('yyyy-MM-ddTHH:mm:ss')
$past.dataHoraFim = [DateTime]::UtcNow.AddDays(-1).ToString('yyyy-MM-ddTHH:mm:ss')
Request-Criterio '/reservas' Post $past 422 | Out-Null
$empty = $reservationBody.Clone()
$empty.dataHoraFim = $empty.dataHoraInicio
Request-Criterio '/reservas' Post $empty 422 | Out-Null
$checks.Add('inicio no passado e periodo vazio retornam HTTP 422')

$rule = @{
    horasMinimasAntecedencia = 100000; percentualMulta = 25
    permiteCancelamento = $false; observacoes = 'Teste de aceite'
}
Request-Criterio "/recursos/$($sala.id)/regra-cancelamento" Put $rule 200 | Out-Null
$reservationBody.recursoId = $sala.id
$restricted = Request-Criterio '/reservas' Post $reservationBody 201
Request-Criterio "/reservas/$($restricted.id)/cancelar" Patch $null 422 | Out-Null
$unchanged = Request-Criterio "/reservas/$($restricted.id)" Get $null 200
if ($unchanged.status -ne 'PENDENTE') { throw 'Cancelamento bloqueado alterou a reserva.' }
$rule.permiteCancelamento = $true
Request-Criterio "/recursos/$($sala.id)/regra-cancelamento" Put $rule 200 | Out-Null
$fine = Request-Criterio "/reservas/$($restricted.id)/cancelar" Patch $null 200
if ($fine.reserva.status -ne 'CANCELADA' -or $fine.percentualMultaAplicada -ne 25) {
    throw 'Multa incorreta.'
}
$checks.Add('regra bloqueia cancelamento sem alterar status; regra permitida aplica 25%')

$preserved = Request-Criterio "/reservas/$($state.reservaId)" Get $null 200
if ($preserved.status -ne 'CONFIRMADA') { throw 'Reserva da prova de persistencia foi alterada.' }
$checks.Add('reserva confirmada preservada para conferir depois do redeploy')
$resultPath = [IO.Path]::ChangeExtension([IO.Path]::GetFullPath($StatePath), 'resultado.json')
@{
    executadoEmUtc = [DateTimeOffset]::UtcNow.ToString('o'); baseUrl = $baseAddress; commit = $Commit
    resultado = 'PASSOU'; verificacoes = $checks.ToArray(); estadoPersistencia = [IO.Path]::GetFullPath($StatePath)
} | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $resultPath -Encoding UTF8
Write-Host "PASSOU: $($checks.Count) grupos de verificacoes pela API HTTP."
Write-Host 'A prova de persistencia exige novo deploy e Test-DeployExterno.ps1 -Fase Conferir com o mesmo StatePath.'
