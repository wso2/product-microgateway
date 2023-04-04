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

distribution_path=""
docker_repo=wso2
docker_tag=""

function usage() {
    echo ""
    echo "Usage: "
    echo "$0 [-d <distribution_path>] [-r <docker_repo>] [-t <docker_tag>]"
    echo ""
    echo "-d: Distribution Path."
    echo "-r: Docker Repository for the Images (Default wso2)."
    echo "-t: Docker Image Tag."
    echo "-h: Display help and exit."
    echo ""
}

while getopts "d:r:t:h" opts; do
    case $opts in
    d)
        distribution_path=${OPTARG}
        ;;
    r)
        docker_repo=${OPTARG}
        ;;
    t)
        docker_tag=${OPTARG}
        ;;
    h)
        usage
        exit 0
        ;;
    \?)
        usage
        exit 1
        ;;
    esac
done

if [[ -z $distribution_path ]]; then
    echo "Please specify distribution path."
    exit 1
fi


if [[ -z $docker_repo ]]; then
    docker_repo=wso2
fi

if [[ -z $docker_tag ]]; then
    echo "Please specify docker tag."
    exit 1
fi

cd "$distribution_path"

echo "Building Adapter image"
mkdir -p adapter/maven
cp -r ADAPTER/\$HOME/* adapter/maven
cp ADAPTER/bin/grpc_health_probe-linux-amd64 adapter/maven
chmod +x adapter/maven/grpc_health_probe-linux*
chmod +x adapter/maven/adapter*
docker build -f ADAPTER/Dockerfile -t "${docker_repo}/choreo-connect-adapter:${docker_tag}" adapter

echo "Building Enforcer image"
mkdir -p enforcer/maven
cp -r ENFORCER/\$HOME/* enforcer/maven
cp ENFORCER/bin/grpc_health_probe-linux-amd64 enforcer/maven
chmod +x enforcer/maven/grpc_health_probe-linux*
docker build -f ENFORCER/Dockerfile -t "${docker_repo}/choreo-connect-enforcer:${docker_tag}" enforcer

echo "Building Router image"
mkdir -p router/maven/security/truststore/
cp -r ROUTER/\$HOME/* router/maven
cp ROUTER/LICENSE.txt router/maven
cp ROUTER/etc/envoy/envoy.yaml router/maven/envoy.yaml
docker build -f ROUTER/Dockerfile -t "${docker_repo}/choreo-connect-router:${docker_tag}" router

echo ""
echo "Successfully build Choreo Connect docker images"
