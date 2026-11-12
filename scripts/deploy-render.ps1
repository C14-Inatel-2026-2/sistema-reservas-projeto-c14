param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9a-fA-F]{40}$')]
    [string]$Commit,
    [ValidateRange(1, 3600)]
    [int]$TimeoutSeconds = 900,
    [ValidateRange(0, 60)]
    [int]$RetryIntervalSeconds = 5
)

$ErrorActionPreference = 'Stop'
if (-not $env:RENDER_DEPLOY_HOOK -or -not $env:RENDER_SERVICE_URL) {
    throw 'Configure as credenciais render-deploy-hook e render-service-url no Jenkins.'
}

$hook = [uri]$env:RENDER_DEPLOY_HOOK
$service = [uri]$env:RENDER_SERVICE_URL
if (-not $hook.IsAbsoluteUri -or $hook.Scheme -ne 'https' -or $hook.Host -ne 'api.render.com') {
    throw 'O deploy hook deve usar HTTPS no dominio api.render.com.'
}
if (-not $service.IsAbsoluteUri -or $service.Scheme -ne 'https') {
    throw 'A URL publica da aplicacao deve usar HTTPS.'
}

$separator = if ($hook.Query) { '&' } else { '?' }
$deployUrl = $hook.AbsoluteUri + $separator + 'ref=' + [uri]::EscapeDataString($Commit)
try {
    Invoke-RestMethod -Uri $deployUrl -Method Post -TimeoutSec 30 | Out-Null
} catch {
    throw 'Falha ao acionar o deploy no Render. Confira o hook e o acesso ao commit.'
}

Write-Host "Aguardando o commit $Commit no Render."
$baseUrl = $service.AbsoluteUri.TrimEnd('/')
$timer = [Diagnostics.Stopwatch]::StartNew()
do {
    try {
        $health = Invoke-RestMethod -Uri ($baseUrl + '/actuator/health') -TimeoutSec 10
        $info = Invoke-RestMethod -Uri ($baseUrl + '/actuator/info') -TimeoutSec 10
        if ($health.status -eq 'UP' -and $info.app.commit -eq $Commit) {
            Write-Host "Deploy confirmado: commit $Commit e banco disponivel."
            return
        }
    } catch {
        # Durante o build ou a inicializacao, o servico pode estar indisponivel.
    }
    if ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        Start-Sleep -Seconds $RetryIntervalSeconds
    }
} while ($timer.Elapsed.TotalSeconds -lt $TimeoutSeconds)

throw "O Render nao confirmou o commit $Commit dentro do prazo. Confira os logs do servico."
