#!/bin/sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

APP_HOME=$( cd "$( dirname "$0" )" && pwd )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -z "$JAVA_EXE" ] && [ -z "$JAVA_HOME" ]; then
    JAVA_EXE=java
elif [ -z "$JAVA_EXE" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$JAVA_EXE" -Xmx64m -Xms64m -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
