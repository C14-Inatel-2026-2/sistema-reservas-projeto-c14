pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DEPLOY_DIR = 'C:\\deploy\\sistema-reservas'
        APP_PORT = '8081'
        // Credenciais do MySQL local de desenvolvimento
        DB_USERNAME = 'root'
        DB_PASSWORD = ''
        // Evita que o Jenkins mate a aplicacao ao terminar o build
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }

    stages {
        stage('Build') {
            steps {
                dir('backend') {
                    bat 'mvnw.cmd -B -ntp clean package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Testes') {
            steps {
                dir('backend') {
                    bat 'mvnw.cmd -B -ntp test'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Cobertura') {
            steps {
                recordCoverage(
                    tools: [[parser: 'JACOCO', pattern: 'backend/target/site/jacoco/jacoco.xml']],
                    sourceCodeRetention: 'EVERY_BUILD'
                )
                publishHTML(target: [
                    reportName: 'Cobertura JaCoCo',
                    reportDir: 'backend/target/site/jacoco',
                    reportFiles: 'index.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: false
                ])
            }
        }

        stage('Deploy') {
            steps {
                powershell '''
                    New-Item -ItemType Directory -Force -Path $env:DEPLOY_DIR | Out-Null
                    $pidFile = Join-Path $env:DEPLOY_DIR 'app.pid'

                    if (Test-Path $pidFile) {
                        Stop-Process -Id (Get-Content $pidFile) -Force -ErrorAction SilentlyContinue
                        Start-Sleep -Seconds 2
                    }

                    Copy-Item 'backend\\target\\sistema-reservas-*.jar' (Join-Path $env:DEPLOY_DIR 'app.jar') -Force

                    $log = Join-Path $env:DEPLOY_DIR 'app.log'
                    $erro = Join-Path $env:DEPLOY_DIR 'app-erro.log'
                    $processo = Start-Process -FilePath 'java' `
                        -ArgumentList '-jar', (Join-Path $env:DEPLOY_DIR 'app.jar'), "--server.port=$env:APP_PORT" `
                        -RedirectStandardOutput $log -RedirectStandardError $erro `
                        -WindowStyle Hidden -PassThru
                    $processo.Id | Set-Content $pidFile

                    $ok = $false
                    for ($i = 0; $i -lt 30; $i++) {
                        Start-Sleep -Seconds 2
                        try {
                            Invoke-WebRequest -Uri "http://localhost:$env:APP_PORT/usuarios" -UseBasicParsing | Out-Null
                            $ok = $true
                            break
                        } catch { }
                    }
                    if (-not $ok) {
                        Write-Error 'A aplicacao nao respondeu apos o deploy.'
                        exit 1
                    }
                    Write-Host "Aplicacao no ar em http://localhost:$env:APP_PORT"
                '''
            }
        }
    }

    post {
        success { echo 'Pipeline concluido com sucesso.' }
        failure { echo 'Pipeline falhou. Verifique o stage que ficou vermelho.' }
    }
}
