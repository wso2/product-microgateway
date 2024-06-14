#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2023, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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

set -e

# Feature flag - Enable/disable the graceful shutdown
export GRACEFUL_SHUTDOWN_ENABLED="${ROUTER_GRACEFUL_SHUTDOWN_ENABLED:-false}"

echo "Configuring Choreo Connect Router"
MG_ENVOY_YAML="$(envsubst < /home/wso2/envoy.yaml.template)"

args=()
if [ -n "$FILE_FLUSH_INTERVAL_MSEC" ]; then
    args+=(--file-flush-interval-msec "${FILE_FLUSH_INTERVAL_MSEC}")
fi

echo "Starting Choreo Connect Router"
/usr/local/bin/envoy \
    -c /etc/envoy/envoy.yaml \
    --config-yaml "${MG_ENVOY_YAML}" \
    --concurrency "${CONCURRENCY}" \
    "${args[@]}" \
    $TRAILING_ARGS &

ENVOY_PID=$!

_term() {
    echo "Stopping Choreo Connect Router..."

    if [ "$GRACEFUL_SHUTDOWN_ENABLED" = "true" ]; then
        echo "Graceful shutdown enabled. Sending SIGTERM to the envoy process..."
        kill -SIGTERM $ENVOY_PID
    else
        echo "Graceful shutdown disabled. Not sending the SIGTERM to the envoy process..."
    fi
    wait $ENVOY_PID
    echo "Choreo Connect Router stopped."
    exit 0
}

# trap handle_signal SIGTERM
trap _term SIGTERM

wait $ENVOY_PID
