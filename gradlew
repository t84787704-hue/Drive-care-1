#!/usr/bin/env sh

# Resolve links
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
DIRNAME=`dirname "$PRG"`
APP_BASE_NAME=`basename "$0"`
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    if [ -n "$JAVA_HOME" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="java"
    fi
    exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi
