
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('Verificar Java') {
            steps {
                bat 'java -version'
            }
        }

        stage('Compilar Spring Boot') {
            steps {
                bat 'call mvnw.cmd -B clean package'
            }
        }

        stage('Verificar WAR') {
            steps {
                powershell '''
                    $war = Join-Path $env:WORKSPACE "target\\osinergmin.war"

                    if (-not (Test-Path $war)) {
                        throw "No se encontro osinergmin.war"
                    }

                    Write-Host "WAR generado correctamente"
                    Get-Item $war | Select-Object Name, Length
                '''
            }
        }
    }
}
