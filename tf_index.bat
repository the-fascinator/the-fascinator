echo off

REM This script runs a Re-Index

REM Make sure you check to make sure tf_env.bat reflects
REM your config.

call tf_env.bat

IF "%1"=="" goto USAGE

start /D%TF_HOME%\code\core mvn -Dhttp.nonProxyHosts=localhost -DXmx1024m -P %1 exec:java
goto :EOF

:USAGE
echo Usage: tf_index profile


