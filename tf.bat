@echo off
setlocal

REM this script controls the fascinator using maven and jetty
REM only useful for development mode (i.e. source code checkout)

REM show usage if no parameters given
if "%1" == "" goto usage

REM get fascinator home dir
for %%F in ("%0") do set TF_HOME=%%~dpF

REM set environment
call "%TF_HOME%tf_env.bat"

REM create logging directory
IF EXIST "%FASCINATOR_HOME%\logs" GOTO LOGDIRDONE
mkdir "%FASCINATOR_HOME%\logs"
:LOGDIRDONE

if "%1" == "debug" goto debug
if "%1" == "status" goto status
if "%1" == "start" goto start
if "%1" == "stop" goto stop
if "%1" == "build" goto build
if "%1" == "rebuild" goto rebuild
if "%1" == "notest" goto notest

:status
set Cmd=tasklist /fi "WINDOWTITLE eq The Fascinator - mvn*" /fo csv /nh
for /f "tokens=1*" %%i in ('%Cmd% ^| findstr "cmd.exe"') do goto running
echo The Fascinator is STOPPED.
goto end

:debug
set MAVEN_OPTS=%MAVEN_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=localhost:8787
goto start

:start
start "The Fascinator" /d"%TF_HOME%portal" mvn -P dev jetty:run ^> "%FASCINATOR_HOME%\logs\stdout.out"
goto end

:stop
pushd "%TF_HOME%portal"
call mvn -P dev jetty:stop
popd
goto end

:usage
echo Usage: %0 start^|stop^|status^|build^|rebuild
goto end

:build
call mvn install
goto end

:rebuild
call mvn clean install
goto end

:notest
call mvn clean install -Dmaven.test.skip=true
goto end

:running
echo The Fascinator is RUNNING.
goto end

:end
endlocal
