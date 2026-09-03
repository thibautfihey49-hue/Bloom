#!/bin/sh
APP_HOME=$( cd "$( dirname "$0" )" && pwd )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -z "$JAVA_EXE" ] ; then
  if [ -n "$JAVA_HOME" ] ; then
    JAVA_EXE="$JAVA_HOME/bin/java"
  else
    JAVA_EXE="java"
  fi
fi
exec "$JAVA_EXE" -Xmx64m -Xms64m -classpath "$CLASSPATH" -Dorg.gradle.appname=$(basename "$0") org.gradle.wrapper.GradleWrapperMain "$@"
