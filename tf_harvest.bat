@echo off

REM this script starts a fascinator harvest using maven
REM only usable when installed in development mode

REM get fascinator home dir
for %%F in ("%0") do set TF_HOME=%%~dpF

REM set environment
call "%TF_HOME%tf_env.bat"

set HARVEST_DIR=%FASCINATOR_HOME%\harvest
if "%1" == "" goto usage
set JSON_FILE=%1

set Cmd=tasklist /fi "windowtitle eq The Fascinator - mvn*" /fo csv /nh
for /f "tokens=1*" %%i in ('%Cmd% ^| find "cmd.exe"') do goto harvest
echo Please start The Fascinator before harvesting.
goto end

:harvest
if exist "%JSON_FILE%" (set BASE_FILE=%JSON_FILE%) else (set BASE_FILE=%HARVEST_DIR%\%JSON_FILE%.json)
if not exist "%BASE_FILE%" goto notfound
REM escape slashes for exec.args
set BASE_FILE=%BASE_FILE:\=\\%
pushd "%TF_HOME%\core"
call mvn -P dev -Dexec.args="%BASE_FILE%" -Dexec.mainClass=au.edu.usq.fascinator.HarvestClient exec:java > "%FASCINATOR_HOME%/logs/harvest.out"
popd
goto end

:notfound
echo Configuration file not found:
echo '%BASE_FILE%'

:usage
echo Usage: %0 jsonFile
echo Where jsonFile is a JSON configuration file
echo If jsonFile is not an absolute path, the file is assumed to be in:
echo     %HARVEST_DIR%
echo Available files:
for /f "tokens=1,2* delims=." %%i in ('dir /b "%HARVEST_DIR%\*.json"') do @echo     %%~ni

:end
