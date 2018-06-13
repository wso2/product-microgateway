#!/bin/bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
# Startup Script for API Manager Micro Gateway for ${label} APIs
#
# NOTE: Borrowed generously from Apache Tomcat startup scripts.
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
#ulimit -n 100000

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
GWHOME=`cd "$PRGDIR/.." ; pwd`
export BALLERINA_HOME=$GWHOME'/runtime'

# run the balx created for ${label} APIs
bash $BALLERINA_HOME/bin/ballerina run $GWHOME/exec/${label}.balx --config $GWHOME/conf/micro-gw.conf