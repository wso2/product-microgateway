#!/bin/bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# ----------------------------------------------------------------------------
# Startup Script for Gateway Cli
#
# Environment Variable Prerequisites
#
#   BALLERINA_HOME      Home of Ballerina installation.
#
#   JAVA_HOME           Must point at your Java Development Kit installation.
#
#   JAVA_OPTS           (Optional) Java runtime options used when the commands
#                       is executed.
#
# NOTE: Borrowed generously from Apache Tomcat startup scripts.
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# set BALLERINA_HOME
export CLI_HOME=`cd "$PRGDIR/.." ; pwd`
BALLERINA_HOME="$CLI_HOME/lib/platform"

echo BALLERINA_HOME environment variable is set to $BALLERINA_HOME
echo CLI_HOME environment variable is set to $CLI_HOME

export BALLERINA_HOME=$BALLERINA_HOME
export PATH=$BALLERINA_HOME/bin:$PATH

#reading the micro gateway source root location
file="$CLI_HOME/temp/path.txt"

while IFS= read line
do
	MICRO_GW_PROJECT_DIR=$line
done <"$file"

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$BALLERINA_HOME" ] && BALLERINA_HOME=`cygpath --unix "$BALLERINA_HOME"`
  [ -n "$CLI_HOME" ] && CLI_HOME=`cygpath --unix "$CLI_HOME"`
fi

# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  QIBM_MULTI_THREADED=Y
  export QIBM_MULTI_THREADED
fi

# For Migwn, ensure paths are in UNIX format before anything is touched
if $mingw ; then
  [ -n "$BALLERINA_HOME" ] &&
    BALLERINA_HOME="`(cd "$BALLERINA_HOME"; pwd)`"
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME="`(cd "$JAVA_HOME"; pwd)`"
  [ -n "$CLI_HOME" ] &&
    CLI_HOME="`(cd "$CLI_HOME"; pwd)`"
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=java
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  exit 1
fi

# if JAVA_HOME is not set we're not happy
if [ -z "$JAVA_HOME" ]; then
  echo "You must set the JAVA_HOME variable before running Ballerina."
  exit 1
fi

# ----- Process the input command ----------------------------------------------

for c in "$@"
do
    if [ "$c" = "--java.debug" ] || [ "$c" = "-java.debug" ] || [ "$c" = "java.debug" ]; then
          CMD="--java.debug"
    elif [ "$CMD" = "--java.debug" ] && [ -z "$PORT" ]; then
          PORT=$c
    elif [ "$c" = "run" ] || [ "$c" = "build" ]; then
          CMD_COMMAND=$c
    elif [ "$c" = "--label" ] || [ "$c" = "-l" ]; then
          CMD_LABEL=$c
    elif ([ "$CMD_LABEL" = "--label" ] || [ "$CMD_LABEL" = "-l" ]) && [ -z "$CMD_LABEL_VAL" ]; then
          CMD_LABEL_VAL=$c
    fi
    echo $c
done

#execute build command
if [ "$CMD_COMMAND" = "build" ] && [ "$CMD_LABEL_VAL" != "" ] && [ "$MICRO_GW_PROJECT_DIR" != "" ]; then
    MICRO_GW_LABEL_PROJECT_DIR="$MICRO_GW_PROJECT_DIR/micro-gw-resources/projects/$CMD_LABEL_VAL"
    pushd $MICRO_GW_LABEL_PROJECT_DIR> /dev/null
        echo $PWD
        ballerina build src/ -o $CMD_LABEL_VAL.balx
        exit 1
    popd > /dev/null
elif [ "$CMD_COMMAND" = "run" ] && [ "$CMD_LABEL_VAL" != "" ] && [ "$MICRO_GW_PROJECT_DIR" != "" ]; then
    MICRO_GW_LABEL_PROJECT_TARGET_DIR="$MICRO_GW_PROJECT_DIR/micro-gw-resources/projects/$CMD_LABEL_VAL/target"
    pushd $MICRO_GW_LABEL_PROJECT_TARGET_DIR> /dev/null
        echo $PWD
        ballerina run $CMD_LABEL_VAL.balx
        exit 1
    popd > /dev/null
fi

if [ "$CMD" = "--java.debug" ]; then
  if [ "$PORT" = "" ]; then
    echo "Please specify the debug port after the --java.debug option"
    exit 1
  fi
  if [ -n "$JAVA_OPTS" ]; then
    echo "Warning !!!. User specified JAVA_OPTS will be ignored, once you give the --java.debug option."
  fi
  JAVA_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=$PORT"
  echo "Please start the remote debugging client to continue..."
fi

CLI_CLASSPATH=""
if [ -e "$BALLERINA_HOME/bre/lib/bootstrap/tools.jar" ]; then
    CLI_CLASSPATH="$JAVA_HOME/lib/tools.jar"
fi

for f in "$BALLERINA_HOME"/bre/lib/bootstrap/*.jar
do
    if [ "$f" != "$BALLERINA_HOME/bre/lib/bootstrap/*.jar" ];then
        CLI_CLASSPATH="$CLI_CLASSPATH":$f
    fi
done

for j in "$BALLERINA_HOME"/bre/lib/*.jar
do
    CLI_CLASSPATH="$CLI_CLASSPATH":$j
done

for j in "$CLI_HOME"/target/*.jar
do
    CLI_CLASSPATH="$CLI_CLASSPATH":$j
done

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  BALLERINA_HOME=`cygpath --absolute --windows "$BALLERINA_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  CLI_CLASSPATH=`cygpath --path --windows "$CLI_CLASSPATH"`
fi

# ----- Execute The Requested Command -----------------------------------------

#echo JAVA_HOME environment variable is set to $JAVA_HOME
#echo BALLERINA_HOME environment variable is set to $BALLERINA_HOME

$JAVACMD \
	-Xms256m -Xmx1024m \
	-XX:+HeapDumpOnOutOfMemoryError \
	-XX:HeapDumpPath="$CLI_HOME/heap-dump.hprof" \
	$JAVA_OPTS \
	-classpath "$CLI_CLASSPATH" \
	-Djava.security.egd=file:/dev/./urandom \
	-Dballerina.home=$BALLERINA_HOME \
	-Djava.util.logging.config.class="org.ballerinalang.logging.util.LogConfigReader" \
	-Djava.util.logging.manager="org.ballerinalang.logging.BLogManager" \
	-Dfile.encoding=UTF8 \
	org.wso2.apimgt.gateway.codegen.cmd.Main "$@"