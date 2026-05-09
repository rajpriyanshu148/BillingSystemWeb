@echo off
setlocal

set MAVEN_CMD_LINE_ARGS=%*
set MVN_CMD=mvn

REM Check if mvn is available
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    mvn %MAVEN_CMD_LINE_ARGS%
) else (
    echo Maven not found. Please install Maven and add it to PATH.
    echo Download from: https://maven.apache.org/download.cgi
    echo Or run: choco install maven  (as Administrator)
    exit /b 1
)
