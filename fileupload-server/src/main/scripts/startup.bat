@echo off 
if "%1" == "h" goto begin 
mshta vbscript:createobject("wscript.shell").run("%~nx0 h",0)(window.close)&&exit 
:begin
@echo off
@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set ENV_PATH=.\
if "%OS%" == "Windows_NT" set ENV_PATH=%~dp0%

set allparam=

:param
set str=%1
if "%str%"=="" (
    goto end
)
set allparam=%allparam% %str%
shift /0
goto param

:end
if "%allparam%"=="" (
    goto eof
)

rem remove left right blank
:intercept_left
if "%allparam:~0,1%"==" " set "allparam=%allparam:~1%"&goto intercept_left

:intercept_right
if "%allparam:~-1%"==" " set "allparam=%allparam:~0,-1%"&goto intercept_right

:eof

set SERVICE_NAME=fileupload-server
::Adjust log dir if necessary
set LOG_DIR=C:/tmp/logs/fileupload-server
::Adjust server port if necessary
set SERVER_PORT=8080

set JAVA_OPTS= -Dserver.port=%SERVER_PORT% -Dlogging.file=%LOG_DIR%/%SERVICE_NAME%.log

java -jar %ENV_PATH%\..\%SERVICE_NAME%.jar %allparam% --spring.config.location=..\config\application-github.properties