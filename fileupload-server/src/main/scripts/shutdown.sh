#!/bin/bash
SERVICE_NAME=fileupload-server

if [[ -z "$JAVA_HOME" && -d /usr/java/latest/ ]]; then
    export JAVA_HOME=/usr/java/latest/
fi

cd `dirname $0`/..

if [[ ! -f $SERVICE_NAME"-0.0.1-SNAPSHOT.jar" && -d current ]]; then
    cd current
fi

if [[ -f $SERVICE_NAME"-0.0.1-SNAPSHOT.jar" ]]; then
  chmod a+x $SERVICE_NAME"-0.0.1-SNAPSHOT.jar"
  ./$SERVICE_NAME"-0.0.1-SNAPSHOT.jar" stop
fi
