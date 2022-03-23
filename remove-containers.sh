#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

# Just to assist developers by removing all the containers.
cc_version=1.1.0-beta

docker kill "$(docker ps -a | grep wso2/choreo-connect-enforcer:$cc_version | awk '{print $1}')"
docker rm "$(docker ps -a | grep wso2/choreo-connect-enforcer:$cc_version | awk '{print $1}')"

docker kill "$(docker ps -a | grep wso2/choreo-connect-adapter:$cc_version | awk '{print $1}')"
docker rm "$(docker ps -a | grep wso2/choreo-connect-adapter:$cc_version | awk '{print $1}')"

docker kill "$(docker ps -a | grep wso2/choreo-connect-router:$cc_version | awk '{print $1}')"
docker rm "$(docker ps -a | grep wso2/choreo-connect-router:$cc_version | awk '{print $1}')"

docker kill "$(docker ps -a | grep wso2/choreo-connect-mock-backend:$cc_version | awk '{print $1}')"
docker rm "$(docker ps -a | grep wso2/choreo-connect-mock-backend:$cc_version | awk '{print $1}')"
