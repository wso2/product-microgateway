#!/bin/sh
# --------------------------------------------------------------------
# Copyright (c) 2025, WSO2 LLC. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------

echo "Starting Choreo Connect Enforcer"
java -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath="${ENFORCER_HOME}/logs/heap-dump.hprof" \
    $JAVA_OPTS \
    -Dlog4j.configurationFile="${ENFORCER_HOME}/conf/log4j2.properties" \
    -DtracingEnabled="true" \
    -cp "lib/*:lib/dropins/*" \
    org.wso2.choreo.connect.enforcer.server.AuthServer &

ENFORCER_PID=$!
_term() {
    echo "Stopping Choreo Connect Enforcer..."
    kill -SIGTERM $ENFORCER_PID
    wait $ENFORCER_PID
    ENFORCER_EXIT_CODE=$?
    echo "Choreo Connect Enforcer stopped with exit code: $ENFORCER_EXIT_CODE"
    exit 0 # exit ok
}

trap _term TERM
wait $ENFORCER_PID
