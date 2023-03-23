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

GA=1.2.0
LATEST=1.2.0.0
EULA_VERSION=3.2

wget "https://wso2.com/license/wso2-update/${EULA_VERSION}/LICENSE.txt" -O LICENSE.txt
chmod 644 LICENSE.txt

docker rmi wso2/choreo-connect-adapter:$GA
docker rmi wso2/choreo-connect-enforcer:$GA
docker rmi wso2/choreo-connect-router:$GA

docker pull wso2/choreo-connect-adapter:$GA
docker pull wso2/choreo-connect-enforcer:$GA
docker pull wso2/choreo-connect-router:$GA

docker images | grep -E "^wso2/choreo-connect-\w+\s*${GA}\s+"
sleep 5

function build_docker() {
   if [[ "router" == "$1" ]]; then
   rm Dockerfile
cat <<EOF > Dockerfile
FROM "wso2/choreo-connect-${1}:${GA}"
COPY LICENSE.txt /LICENSE.txt
EOF
   else
cat <<EOF > Dockerfile
FROM "wso2/choreo-connect-${1}:${GA}"
COPY LICENSE.txt /home/wso2/LICENSE.txt
EOF
   fi


   docker build -t "docker.wso2.com/choreo-connect-${1}:${GA}" .
   docker tag "docker.wso2.com/choreo-connect-${1}:${GA}" "docker.wso2.com/choreo-connect-${1}:${LATEST}"
   docker tag "docker.wso2.com/choreo-connect-${1}:${GA}" "docker.wso2.com/choreo-connect-${1}:latest"
   rm Dockerfile
}

build_docker adapter
build_docker enforcer
build_docker router

docker images | grep docker.wso2.com

# check images
docker run --rm --entrypoint ls docker.wso2.com/choreo-connect-adapter:$GA -alh /home/wso2/LICENSE.txt
docker run --rm --entrypoint ls docker.wso2.com/choreo-connect-enforcer:$GA -alh /home/wso2/LICENSE.txt
docker run --rm --entrypoint ls docker.wso2.com/choreo-connect-router:$GA -alh /LICENSE.txt

docker run --rm --entrypoint cat docker.wso2.com/choreo-connect-adapter:$GA /home/wso2/LICENSE.txt
docker run --rm --entrypoint cat docker.wso2.com/choreo-connect-enforcer:$GA /home/wso2/LICENSE.txt
docker run --rm --entrypoint cat docker.wso2.com/choreo-connect-router:$GA /LICENSE.txt
