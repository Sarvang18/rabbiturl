@echo off
set MAVEN_DIR=.maven\apache-maven-3.9.6

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
    echo [Antigravity] Downloading Maven locally...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile 'maven.zip'; Expand-Archive -Path 'maven.zip' -DestinationPath '.maven' -Force; Remove-Item 'maven.zip'"
)

if not exist ".jdk" (
    echo [Antigravity] Downloading Java 21 JDK locally - this may take a minute...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://aka.ms/download-jdk/microsoft-jdk-21.0.3-windows-x64.zip' -OutFile 'jdk.zip'; Expand-Archive -Path 'jdk.zip' -DestinationPath '.jdk' -Force; Remove-Item 'jdk.zip'"
)

REM Find the exact name of the extracted JDK folder
for /d %%i in (".jdk\*") do set JAVA_HOME=%CD%\%%i

call "%MAVEN_DIR%\bin\mvn.cmd" %*
