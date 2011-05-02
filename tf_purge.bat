@echo off
setlocal
REM this script is used for deleting fascinator data
REM data will be deleted:
REM 1. %FASCINATOR_HOME%/storage
REM 2. %FASCINATOR_HOME%/activemq-data
REM 3. %FASCINATOR_HOME%/logs
REM 4. %FASCINATOR_HOME%/cache
REM 5. %SOLR_BASE_DIR%/solr/indexes/anotar/index
REM 6. %SOLR_BASE_DIR%/solr/indexes/fascinator/index
REM 7. %SOLR_BASE_DIR%/solr/indexes/security/index

REM show usage if no parameters given
if "%1" == "" goto usage

REM get fascinator home dir
for %%F in ("%0") do set TF_HOME=%%~dpF

REM set environment
call "%TF_HOME%tf_env.bat"

set Cmd=tasklist /fi "WINDOWTITLE eq The Fascinator - mvn*" /fo csv /nh
for /f "tokens=1*" %%i in ('%Cmd% ^| findstr "cmd.exe"') do goto running

if "%1" == "all" goto all
if "%1" == "solr" goto solr

:all
REM deleting all data
echo Deleting: %FASCINATOR_HOME%\storage
rd /s/q %FASCINATOR_HOME%\storage
echo ----

echo Deleting: %FASCINATOR_HOME%\activemq-data
rd /s/q %FASCINATOR_HOME%\activemq-data
echo ----

echo echo Deleting: %FASCINATOR_HOME%\logs
rd /s/q %FASCINATOR_HOME%\logs
echo ----

echo Deleting: %FASCINATOR_HOME%\cache
rd /s/q %FASCINATOR_HOME%\cache
echo ----
goto solr

:solr
REM deleting solr indexer
echo Deleting: %FASCINATOR_HOME%\solr\indexes\anotar\index
rd /s/q %SOLR_BASE_DIR%\solr\indexes\anotar\index
echo ----

echo Deleting: %FASCINATOR_HOME%\solr\indexes\fascinator\index
rd /s/q %SOLR_BASE_DIR%\solr\indexes\fascinator\index
echo ----

echo Deleting: %FASCINATOR_HOME%\solr\indexes\security\index
rd /s/q %SOLR_BASE_DIR%\solr\indexes\security\index
echo ----
goto end

:running
echo The Fascinator is RUNNING.
echo Please stop Fascinator by running "tf.bat stop" before you delete any data
goto end

:usage 
echo Usage: %0 all^|solr
goto end

:end
endlocal