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

ADAPTER_XDS_PORT="${ADAPTER_XDS_PORT:-18000}"
grpc_health_probe -addr "localhost:${ADAPTER_XDS_PORT}" \
    -tls \
    -tls-ca-cert "${ADAPTER_PUBLIC_CERT_PATH}" \
    -tls-client-cert "${ADAPTER_PUBLIC_CERT_PATH}" \
    -tls-client-key "${ADAPTER_PRIVATE_KEY_PATH}" \
    -connect-timeout=3s
