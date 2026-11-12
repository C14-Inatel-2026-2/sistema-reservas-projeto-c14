param([string]$ReportPath)

$ErrorActionPreference = 'Stop'
$deployScript = Join-Path $PSScriptRoot 'deploy-render.ps1'
$commit = '1234567890123456789012345678901234567890'
$previousHook = $env:RENDER_DEPLOY_HOOK
$previousUrl = $env:RENDER_SERVICE_URL
$results = New-Object 'System.Collections.Generic.List[object]'

# Substitui somente as chamadas externas durante estes testes.
function Invoke-RestMethod {
    param([string]$Uri, [string]$Method, [int]$TimeoutSec)
    if ($Method -eq 'Post') {
        $state.PostCount++
        $state.LastDeployUrl = $Uri
        if ($state.Mode -eq 'hook-failure') { throw 'Erro simulado no hook.' }
        return @{}
    }
    if ($Uri.EndsWith('/actuator/health')) { return @{ status = 'UP' } }
    $state.InfoCount++
    $reportedCommit = $commit
    if ($state.Mode -eq 'old-only' -or ($state.Mode -eq 'old-then-current' -and $state.InfoCount -eq 1)) {
        $reportedCommit = '0000000000000000000000000000000000000000'
    }
    return @{ app = @{ commit = $reportedCommit } }
}

function Start-Sleep { param([int]$Seconds) }
function Assert-True($Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}
function Assert-Throws([scriptblock]$Action, [string]$MessagePart) {
    $caught = $null
    try { & $Action } catch { $caught = $_.Exception.Message }
    Assert-True ($caught -and $caught.Contains($MessagePart)) 'A falha esperada nao ocorreu.'
}

$cases = @(
    @{ Name = 'Publica o SHA testado e aguarda a nova versao'; Run = {
        $state.Mode = 'old-then-current'
        & $deployScript -Commit $commit -TimeoutSeconds 2 -RetryIntervalSeconds 0
        Assert-True ($state.PostCount -eq 1) 'O hook deve ser chamado uma vez.'
        Assert-True ($state.LastDeployUrl.EndsWith('&ref=' + $commit)) 'O SHA nao foi enviado ao Render.'
        Assert-True ($state.InfoCount -eq 2) 'O deploy anterior foi aceito como se fosse o novo.'
    } },
    @{ Name = 'Recusa versao antiga mesmo com health UP'; Run = {
        $state.Mode = 'old-only'
        Assert-Throws { & $deployScript -Commit $commit -TimeoutSeconds 1 -RetryIntervalSeconds 0 } 'nao confirmou'
    } },
    @{ Name = 'Falha do hook interrompe a publicacao'; Run = {
        $state.Mode = 'hook-failure'
        Assert-Throws { & $deployScript -Commit $commit } 'Falha ao acionar'
        Assert-True ($state.InfoCount -eq 0) 'Nao deve aguardar um deploy que nao iniciou.'
    } },
    @{ Name = 'Recusa SHA invalido antes de acessar a rede'; Run = {
        Assert-Throws { & $deployScript -Commit 'main' } 'Commit'
        Assert-True ($state.PostCount -eq 0) 'Um SHA invalido acionou o deploy.'
    } },
    @{ Name = 'Exige as credenciais do Jenkins'; Run = {
        $env:RENDER_DEPLOY_HOOK = ''
        Assert-Throws { & $deployScript -Commit $commit } 'Configure as credenciais'
        Assert-True ($state.PostCount -eq 0) 'O deploy foi acionado sem credenciais.'
    } },
    @{ Name = 'Exige HTTPS na URL publica'; Run = {
        $env:RENDER_SERVICE_URL = 'http://reservas.example'
        Assert-Throws { & $deployScript -Commit $commit } 'deve usar HTTPS'
        Assert-True ($state.PostCount -eq 0) 'Uma URL insegura acionou o deploy.'
    } }
)

try {
    foreach ($case in $cases) {
        $env:RENDER_DEPLOY_HOOK = 'https://api.render.com/deploy/srv-test?key=fake'
        $env:RENDER_SERVICE_URL = 'https://reservas.example'
        $state = @{ Mode = ''; PostCount = 0; InfoCount = 0; LastDeployUrl = '' }
        $failure = $null
        try { & $case.Run } catch { $failure = $_.Exception.Message }
        $results.Add([pscustomobject]@{ Name = $case.Name; Failure = $failure })
        if ($failure) { Write-Host "FALHOU: $($case.Name) - $failure" }
        else { Write-Host "PASSOU: $($case.Name)" }
    }
} finally {
    $env:RENDER_DEPLOY_HOOK = $previousHook
    $env:RENDER_SERVICE_URL = $previousUrl
}

$failures = @($results | Where-Object Failure).Count
if ($ReportPath) {
    $document = New-Object System.Xml.XmlDocument
    $suite = $document.CreateElement('testsuite')
    $suite.SetAttribute('name', 'DeployRender')
    $suite.SetAttribute('tests', [string]$results.Count)
    $suite.SetAttribute('failures', [string]$failures)
    $suite.SetAttribute('errors', '0')
    $suite.SetAttribute('skipped', '0')
    foreach ($result in $results) {
        $test = $document.CreateElement('testcase')
        $test.SetAttribute('classname', 'DeployRender')
        $test.SetAttribute('name', $result.Name)
        if ($result.Failure) {
            $failure = $document.CreateElement('failure')
            $failure.InnerText = $result.Failure
            [void]$test.AppendChild($failure)
        }
        [void]$suite.AppendChild($test)
    }
    [void]$document.AppendChild($suite)
    $absolutePath = [IO.Path]::GetFullPath($ReportPath)
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($absolutePath))
    $document.Save($absolutePath)
}
if ($failures -gt 0) { throw "$failures teste(s) de deploy falharam." }
Write-Host "$($results.Count) testes de deploy passaram."
