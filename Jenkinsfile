
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
                    $war = Join-Path $env:WORKSPACE 'target/osinergmin-0.0.1-SNAPSHOT.war'

                    if (-not (Test-Path $war)) {
                        throw "No se encontro el WAR generado"
                    }

                    Write-Host "WAR generado correctamente"
                    Get-Item $war | Select-Object Name, Length
                '''
            }
        }

        stage('Desplegar en Tomcat 11') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Stop'

                    $servicio = 'Tomcat11'
                    $origen = Join-Path $env:WORKSPACE 'target/osinergmin-0.0.1-SNAPSHOT.war'
                    $webapps = 'C:/Program Files/Apache Software Foundation/Tomcat 11.0/webapps'
                    $destino = Join-Path $webapps 'osinergmin.war'
                    $backups = 'C:/TomcatBackups/osi-back'
                    $respaldo = Join-Path $backups 'osinergmin-anterior.war'
                    $url = 'http://127.0.0.1:8092/osinergmin/api/aplicaciones'

                    if (-not (Test-Path $origen)) {
                        throw "No existe el WAR generado"
                    }

                    if (-not (Test-Path $webapps)) {
                        throw "No existe la carpeta webapps de Tomcat 11"
                    }

                    Get-Service -Name $servicio -ErrorAction Stop | Out-Null

                    New-Item -ItemType Directory -Force -Path $backups | Out-Null

                    $hayRespaldo = $false

                    if (Test-Path $destino) {
                        Write-Host "Respaldando version anterior"
                        Copy-Item $destino $respaldo -Force
                        $hayRespaldo = $true
                    }
                    else {
                        Write-Host "No existe WAR anterior. Se realizara instalacion nueva."
                    }

                    $despliegueIniciado = $false

                    try {
                        $despliegueIniciado = $true

                        Write-Host "Deteniendo Tomcat 11"

                        Stop-Service -Name $servicio -ErrorAction Stop

                        (Get-Service $servicio).WaitForStatus(
                            'Stopped',
                            [TimeSpan]::FromSeconds(60)
                        )

                        Write-Host "Copiando nuevo WAR"

                        Copy-Item $origen $destino -Force

                        Write-Host "Iniciando Tomcat 11"

                        Start-Service -Name $servicio -ErrorAction Stop

                        (Get-Service $servicio).WaitForStatus(
                            'Running',
                            [TimeSpan]::FromSeconds(60)
                        )

                        Write-Host "Verificando API"

                        $correcto = $false

                        for ($i = 1; $i -le 40; $i++) {
                            $codigo = 0

                            try {
                                $respuesta = Invoke-WebRequest `
                                    -Uri $url `
                                    -UseBasicParsing `
                                    -Proxy $null `
                                    -TimeoutSec 5

                                $codigo = [int]$respuesta.StatusCode
                            }
                            catch {
                                if ($_.Exception.Response) {
                                    $codigo = [int]$_.Exception.Response.StatusCode
                                }
                            }

                            if ($codigo -in @(200, 401, 403)) {
                                Write-Host "API responde HTTP $codigo"
                                $correcto = $true
                                break
                            }

                            Start-Sleep -Seconds 3
                        }

                        if (-not $correcto) {
                            throw "La API no respondio correctamente"
                        }

                        Write-Host "Despliegue terminado"
                    }
                    catch {
                        $errorOriginal = $_.Exception.Message

                        Write-Host "ERROR: $errorOriginal"

                        if ($despliegueIniciado) {
                            Write-Host "Intentando recuperar despliegue anterior"

                            try {
                                Stop-Service -Name $servicio -ErrorAction Stop

                                (Get-Service $servicio).WaitForStatus(
                                    'Stopped',
                                    [TimeSpan]::FromSeconds(60)
                                )

                                if ($hayRespaldo) {
                                    Write-Host "Restaurando WAR anterior"
                                    Copy-Item $respaldo $destino -Force
                                }
                                else {
                                    Write-Host "No existe respaldo anterior"

                                    if (Test-Path $destino) {
                                        Remove-Item $destino -Force
                                    }
                                }

                                Start-Service -Name $servicio -ErrorAction Stop

                                (Get-Service $servicio).WaitForStatus(
                                    'Running',
                                    [TimeSpan]::FromSeconds(60)
                                )

                                Write-Host "Recuperacion ejecutada. Verificar aplicacion."
                            }
                            catch {
                                Write-Host "Fallo la recuperacion: $($_.Exception.Message)"
                            }
                        }

                        throw "Fallo el despliegue: $errorOriginal"
                    }
                '''
            }
        }
    }
}
