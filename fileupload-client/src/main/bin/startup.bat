@echo off
@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set ENV_PATH=.\
if "%OS%" == "Windows_NT" set ENV_PATH=%~dp0%

set conf=%ENV_PATH%\..\conf
set fileupload_conf=%conf%\fileupload.properties
set logback_configurationFile=%conf%\logback.xml
set classpath=%conf%\..\lib\*;%conf%

set JAVA_OPTS= -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dapplication.codeset=UTF-8 -Dfile.encoding=UTF-8 -Xms128m -Xmx512m -XX:PermSize=128m -XX:+HeapDumpOnOutOfMemoryError -DappName=fileupload -Dlogback.configurationFile="%logback_configurationFile%" -Dfileupload.conf="%fileupload_conf%"
set JAVA_DEBUG_OPT= -server -Xdebug -Xnoagent -Djava.compiler=NONE

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
set COMMAND= java %JAVA_OPTS% %JAVA_DEBUG_OPT% -classpath "%classpath%" com.github.xdering.fileupload.ClientMultipartFormPost %allparam%
echo %COMMAND%
java %JAVA_OPTS% %JAVA_DEBUG_OPT% -classpath "%classpath%" com.github.xdering.fileupload.ClientMultipartFormPost %allparam%