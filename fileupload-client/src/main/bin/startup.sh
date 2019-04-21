#!/bin/bash

cygwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        bin_abs_path=`cd $(dirname $0); pwd`
        cygwin=true
        ;;
    Linux*)
        bin_abs_path=$(readlink -f $(dirname $0))
        linux=true
        ;;
    *)
        bin_abs_path=`cd $(dirname $0); pwd`
        ;;
esac

search_pid() {
    STR=$1
    PID=$2
    if $cygwin; then
        JAVA_CMD="$JAVA_HOME\bin\java"
        JAVA_CMD=`cygpath --path --unix $JAVA_CMD`
        JAVA_PID=`ps |grep $JAVA_CMD |awk '{print $1}'`
    else
        if $linux; then
            if [ ! -z "$PID" ]; then
                JAVA_PID=`ps -C java -f --width 1000|grep "$STR"|grep "$PID"|grep -v grep|awk '{print $2}'`
            else
                JAVA_PID=`ps -C java -f --width 1000|grep "$STR"|grep -v grep|awk '{print $2}'`
            fi
        else
            if [ ! -z "$PID" ]; then
                JAVA_PID=`ps aux |grep "$STR"|grep "$PID"|grep -v grep|awk '{print $2}'`
            else
                JAVA_PID=`ps aux |grep "$STR"|grep -v grep|awk '{print $2}'`
            fi
        fi
    fi
    echo $JAVA_PID;
}

current_path=`pwd`
base=${bin_abs_path}/..
fileupload_conf=$base/conf/fileupload.properties
logback_configurationFile=$base/conf/logback.xml
export LANG=en_US.UTF-8
export BASE=$base
pidfile=$base/bin/fileupload.pid

if [ -f $pidfile ] ; then
	pid=`cat $pidfile`
	gpid=`search_pid "appName=fileupload" "$pid"`
    if [ "$gpid" == "" ] ; then
    	`rm $pidfile`
    else
    	echo "found fileupload.pid , Please run stop.sh first ,then startup.sh" 2>&2
    	exit 1
    fi
fi

if [ ! -d $base/logs/fileupload ] ; then
	mkdir -p $base/logs/fileupload
fi

## set java path
if [ -z "$JAVA" ] ; then
  JAVA=$(which java)
fi

ALIBABA_JAVA="/usr/alibaba/java/bin/java"
TAOBAO_JAVA="/opt/taobao/java/bin/java"
if [ -z "$JAVA" ]; then
  if [ -f $ALIBABA_JAVA ] ; then
  	JAVA=$ALIBABA_JAVA
  elif [ -f $TAOBAO_JAVA ] ; then
  	JAVA=$TAOBAO_JAVA
  else
  	echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.5) in your PATH." 2>&2
    exit 1
  fi
fi


str=`file $JAVA_HOME/bin/java | grep 64-bit`
if [ -n "$str" ]; then
	JAVA_OPTS="-server -Xms2048m -Xmx3072m -Xmn1024m -XX:SurvivorRatio=2 -XX:PermSize=96m -XX:MaxPermSize=256m -Xss256k -XX:-UseAdaptiveSizePolicy -XX:MaxTenuringThreshold=15 -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:+HeapDumpOnOutOfMemoryError"
else
	JAVA_OPTS="-server -Xms1024m -Xmx1024m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:MaxPermSize=128m "
fi

JAVA_OPTS=" $JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
fileupload_OPTS="-DappName=fileupload -Dlogback.configurationFile=$logback_configurationFile -Dfileupload.conf=$fileupload_conf"

if [ -e $fileupload_conf -a -e $logback_configurationFile ]
then

	for i in $base/lib/*;
		do CLASSPATH=$i:"$CLASSPATH";
	done
 	CLASSPATH="$base/conf:$CLASSPATH";

 	echo "cd to $bin_abs_path for workaround relative path"
  	cd $bin_abs_path

	echo LOG CONFIGURATION : $logback_configurationFile
	echo fileupload conf : $fileupload_conf
	echo CLASSPATH :$CLASSPATH
	$JAVA $JAVA_OPTS $JAVA_DEBUG_OPT $fileupload_OPTS -classpath .:$CLASSPATH com.github.xdering.fileupload.ClientMultipartFormPost $*
else
	echo "fileupload conf("$fileupload_conf") OR log configration file($logback_configurationFile) is not exist,please create then first!"
fi
