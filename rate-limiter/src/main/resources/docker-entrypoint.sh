#!/bin/sh
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

export CONFIG_TYPE=GRPC_XDS_SOTW
export CONFIG_GRPC_XDS_NODE_ID="${RATE_LIMITER_LABEL:-Default}"
INSTANCE_IDENTIFIER="${HOST:-Undefined}"
export CONFIG_GRPC_XDS_NODE_METADATA="instanceIdentifier=${INSTANCE_IDENTIFIER}"

# Start the server
ratelimit "$@" &

RL_PID_PID=$!
_term() {
    echo "Stopping Choreo Connect Rate-Limiter..."
    kill -SIGTERM $RL_PID_PID
    wait $RL_PID_PID
    echo "Choreo Connect Rate-Limiter stopped."
    exit 0
}

# trap handle_signal SIGTERM (TERM in alpine)
trap _term TERM

wait $RL_PID_PID
