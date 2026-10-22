
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
                    $war = Join-Path $env:WORKSPACE "target\\osinergmin-0.0.1-SNAPSHOT.war"

                    if (-not (Test-Path $war)) {
                        throw "No se encontro el archivo WAR."
                    }

                    Write-Host "WAR generado correctamente:"
                    Get-Item $war | Select-Object Name, Length, LastWriteTime
                '''
            }
        }
    }
}
