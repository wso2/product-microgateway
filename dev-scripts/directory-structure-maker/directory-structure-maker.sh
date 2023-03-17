#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2022, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
# Sets the Choreo Connect version
echo "Prerequisite: Please start Choreo Connect docker-compose file before executing this script."
read -p "Please enter Choreo Connect Version (Ex: 1.2.0): " CC_VERSION

# Sets the architecture type
ARC_VALUE="linux-amd64"

# Sets the Choreo Connect local repository path
DEFAULT_TAG="v${CC_VERSION}"
read -p "Please enter GitHub tag in case if you want to point RC release (optional and default to '${DEFAULT_TAG}'): " CC_GIT_TAG
if [ -z "$CC_GIT_TAG" ]
then
    CC_GIT_TAG=DEFAULT_TAG
fi

# EULA License
read -p "Please enter EULA license version (Ex: 3.2): " EULA_VERSION
mkdir -p temp
wget "https://wso2.com/license/wso2-update/${EULA_VERSION}/LICENSE.txt" -O temp/LICENSE.txt
chmod 644 temp/LICENSE.txt

mkdir "choreo-connect-"$CC_VERSION
cd choreo-connect-$CC_VERSION
# Creates root folder structure
mkdir ADAPTER ROUTER ENFORCER bin updates

# Creates folder structure in ADAPTER
mkdir -p ./ADAPTER/\$HOME
mkdir -p ./ADAPTER/bin

# Creates folder structure in ENFORCER
mkdir -p ./ENFORCER/\$HOME
mkdir -p ./ENFORCER/bin

# Creates folder structure in ROUTER
mkdir -p ./ROUTER/\$HOME/
mkdir -p ./ROUTER/etc/envoy
mkdir -p ./ROUTER/etc/ssl/certs

ADAPTER_IMAGE_ID=$(docker ps | grep 'wso2/choreo-connect-adapter' | awk '{ print $1 }')
ENFORCER_IMAGE_ID=$(docker ps | grep 'wso2/choreo-connect-enforcer' | awk '{ print $1 }')
ROUTER_IMAGE_ID=$(docker ps | grep 'wso2/choreo-connect-router' | awk '{ print $1 }')

echo "Prepareing Choreo Connect folder structure..."

# Copies files from Adapter container
docker cp $ADAPTER_IMAGE_ID:home/wso2/adapter           ./ADAPTER/\$HOME
mv ./ADAPTER/\$HOME/adapter                             ./ADAPTER/\$HOME/adapter-$ARC_VALUE
docker cp $ADAPTER_IMAGE_ID:home/wso2/check_health.sh   ./ADAPTER/\$HOME
cp ../temp/LICENSE.txt                                     ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:home/wso2/conf              ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:home/wso2/security          ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:bin/grpc_health_probe       ./ADAPTER/bin
mv ./ADAPTER/bin/grpc_health_probe                      ./ADAPTER/bin/grpc_health_probe-$ARC_VALUE

# Copies files from Enforcer container
docker cp $ENFORCER_IMAGE_ID:home/wso2/conf            ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/lib             ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/security        ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/check_health.sh ./ENFORCER/\$HOME
cp ../temp/LICENSE.txt                                    ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:bin/grpc_health_probe     ./ENFORCER/bin
mv ./ENFORCER/bin/grpc_health_probe                    ./ENFORCER/bin/grpc_health_probe-$ARC_VALUE

# Copies files from Router container
docker cp $ROUTER_IMAGE_ID:home/wso2/interceptor        ./ROUTER/\$HOME/
docker cp $ROUTER_IMAGE_ID:home/wso2/wasm               ./ROUTER/\$HOME/
cp ../temp/LICENSE.txt                                     ./ROUTER/
docker cp $ROUTER_IMAGE_ID:/etc/envoy/envoy.yaml        ./ROUTER/etc/envoy/
docker cp $ROUTER_IMAGE_ID:/home/wso2/envoy.yaml.template  ./ROUTER/\$HOME/
docker cp $ROUTER_IMAGE_ID:/home/wso2/docker-entrypoint.sh ./ROUTER/\$HOME/

# Copies dockerfiles to the relevant folder
# Use GitHub instead of local files, to avoid having any changes in local files.
wget "https://raw.githubusercontent.com/wso2/product-microgateway/${CC_GIT_TAG}/adapter/src/main/resources/Dockerfile" -O ./ADAPTER/Dockerfile
wget "https://raw.githubusercontent.com/wso2/product-microgateway/${CC_GIT_TAG}/enforcer-parent/enforcer/src/main/resources/Dockerfile" -O ./ENFORCER/Dockerfile
wget "https://raw.githubusercontent.com/wso2/product-microgateway/${CC_GIT_TAG}/router/src/main/resources/Dockerfile" -O ./ROUTER/Dockerfile

# Creates update directory txt
touch ./updates/product.txt
echo "choreo-connect-"$CC_VERSION >> ./updates/product.txt

cd ..
zip -r "choreo-connect-${CC_VERSION}.zip" "choreo-connect-${CC_VERSION}"
rm -rf "choreo-connect-${CC_VERSION}"
rm -rf temp

echo "Choreo Connect folder structure prepared successfully!"
