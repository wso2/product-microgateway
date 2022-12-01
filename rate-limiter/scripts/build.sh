#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2022, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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

set -ex

# Build envoy rate limit service with the xds implementation done in https://github.com/renuka-fernando/ratelimit/tree/choreo
# This should be removed once the PR https://github.com/envoyproxy/ratelimit/pull/373 is merged to upstream
# The branch is protected not to force push or delete https://github.com/renuka-fernando/ratelimit/settings/branches
git submodule update --init -- rate-limiter-service
docker build -t envoy-ratelimit-service:patched rate-limiter-service
