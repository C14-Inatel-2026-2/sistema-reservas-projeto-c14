pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Build') {
            steps {
                dir('backend') {
                    bat '.\\mvnw.cmd -B -ntp clean package -DskipTests'
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
                    bat '.\\mvnw.cmd -B -ntp test'
                }
                powershell '.\\scripts\\Test-DeployRender.ps1 -ReportPath backend/target/surefire-reports/TEST-DeployRender.xml'
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                recordCoverage(
                    tools: [[parser: 'JACOCO', pattern: 'backend/target/site/jacoco/jacoco.xml']],
                    sourceCodeRetention: 'EVERY_BUILD',
                    qualityGates: [[threshold: 40.0, metric: 'LINE', baseline: 'PROJECT', criticality: 'UNSTABLE']]
                )
            }
        }

        stage('Cobertura') {
            steps {
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
            when {
                expression {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    return !env.CHANGE_ID && (branch in ['main', 'origin/main', 'refs/heads/main']) && currentBuild.currentResult == 'SUCCESS'
                }
            }
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            steps {
                withCredentials([
                    string(credentialsId: 'render-deploy-hook', variable: 'RENDER_DEPLOY_HOOK'),
                    string(credentialsId: 'render-service-url', variable: 'RENDER_SERVICE_URL')
                ]) {
                    powershell '.\\scripts\\deploy-render.ps1 -Commit $env:GIT_COMMIT'
                }
            }
        }
    }

    post {
        success { echo 'Pipeline concluido com sucesso.' }
        failure { echo 'Pipeline falhou. Verifique o stage que ficou vermelho.' }
    }
}
