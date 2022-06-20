#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

ENFORCER_PORT="${ENFORCER_PORT:-8081}"
grpc_health_probe -addr "127.0.0.1:${ENFORCER_PORT}" \
    -tls \
    -tls-ca-cert "${ENFORCER_PUBLIC_CERT_PATH}" \
    -tls-client-cert "${ENFORCER_PUBLIC_CERT_PATH}" \
    -tls-client-key "${ENFORCER_PRIVATE_KEY_PATH}" \
    -connect-timeout=3s
