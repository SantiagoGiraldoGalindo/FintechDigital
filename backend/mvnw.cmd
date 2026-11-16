@echo off
rem Wrapper sencillo de Maven (Windows).
rem Descarga Apache Maven la primera vez y luego lo reutiliza. No requiere tener Maven instalado.
setlocal

set "MAVEN_VERSION=3.9.11"
if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper\dists"
set "DIST_DIR=%WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%"

if exist "%DIST_DIR%\bin\mvn.cmd" goto run

echo Descargando Apache Maven %MAVEN_VERSION% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $v='%MAVEN_VERSION%'; $dest='%WRAPPER_HOME%'; $tmp=Join-Path $env:TEMP ('mvn-wrapper-'+$v); if(Test-Path $tmp){Remove-Item $tmp -Recurse -Force}; New-Item -ItemType Directory -Path $tmp | Out-Null; $zip=Join-Path $tmp 'maven.zip'; Invoke-WebRequest -UseBasicParsing -Uri ('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/'+$v+'/apache-maven-'+$v+'-bin.zip') -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $tmp -Force; New-Item -ItemType Directory -Path $dest -Force | Out-Null; Move-Item -Path (Join-Path $tmp ('apache-maven-'+$v)) -Destination $dest -Force; Remove-Item $tmp -Recurse -Force"
if errorlevel 1 (
  echo Error al descargar Maven. Revisa tu conexion a internet.
  exit /b 1
)

:run
call "%DIST_DIR%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
