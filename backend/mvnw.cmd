@REM Maven Wrapper startup script for Windows
@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_CMD=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\dists\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
    goto end
)

@REM Maven not found — download it
echo Downloading Maven 3.9.9...
set "DIST_DIR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\dists"
set "DIST_ZIP=%TEMP%\apache-maven-3.9.9-bin.zip"

powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%DIST_ZIP%'"
if ERRORLEVEL 1 (
    echo Error downloading Maven
    exit /b 1
)

echo Extracting Maven...
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
powershell -Command "Expand-Archive -Path '%DIST_ZIP%' -DestinationPath '%DIST_DIR%' -Force"
del "%DIST_ZIP%" 2>nul

"%MAVEN_CMD%" %*

:end
exit /b %ERRORLEVEL%
