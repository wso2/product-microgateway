#!/bin/sh
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

# Sets the Choreo Connect version
echo "Prerequisite: Please start Choreo Connect docker-compose file before executing this script."
read -p "Please enter Choreo Connect Version (Ex: 1.0.0): " CC_VERSION

# Sets the architecture type
ARC_VALUE="linux-amd64"
echo "\033[0;31mPlease select below value correctly. Otherwise copying executables will be incorrect.\033[0m"
read -p "Are you running Choreo Connect UBUNTU based images? (y or n): " IS_RUNNING_UBUNTU_IMAGES
if [ "$IS_RUNNING_UBUNTU_IMAGES" = "y" ]; then
  ARC_VALUE="linux-arm64"
fi

# Sets the Choreo Connect local repository path
read -p "Please enter Choreo Connect local repository path (Ex: /Users/wso2/Desktop/product-microgateway): " CC_REPO_PATH

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
docker cp $ADAPTER_IMAGE_ID:home/wso2/LICENSE.txt       ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:home/wso2/conf              ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:home/wso2/security          ./ADAPTER/\$HOME
docker cp $ADAPTER_IMAGE_ID:bin/grpc_health_probe       ./ADAPTER/bin
mv ./ADAPTER/bin/grpc_health_probe                      ./ADAPTER/bin/grpc_health_probe-$ARC_VALUE

# Copies files from Enforcer container
docker cp $ENFORCER_IMAGE_ID:home/wso2/conf            ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/lib             ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/security        ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/check_health.sh ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:home/wso2/LICENSE.txt     ./ENFORCER/\$HOME
docker cp $ENFORCER_IMAGE_ID:bin/grpc_health_probe     ./ENFORCER/bin
mv ./ENFORCER/bin/grpc_health_probe                    ./ENFORCER/bin/grpc_health_probe-$ARC_VALUE

# Copies files from Router container
docker cp $ROUTER_IMAGE_ID:home/wso2/interceptor        ./ROUTER/\$HOME/
docker cp $ROUTER_IMAGE_ID:home/wso2/wasm               ./ROUTER/\$HOME/
docker cp $ROUTER_IMAGE_ID:LICENSE.txt                  ./ROUTER/
docker cp $ROUTER_IMAGE_ID:/etc/envoy/envoy.yaml        ./ROUTER/etc/envoy/
docker cp $ROUTER_IMAGE_ID:/etc/ssl/certs/ca-certificates.crt  ./ROUTER/etc/ssl/certs

# Copies dockerfiles to the relevant folder
cp $CC_REPO_PATH/adapter/src/main/resources/Dockerfile                          ./ADAPTER
cp $CC_REPO_PATH/adapter/src/main/resources/Dockerfile.ubuntu                   ./ADAPTER
cp $CC_REPO_PATH/enforcer-parent/enforcer/src/main/resources/Dockerfile         ./ENFORCER
cp $CC_REPO_PATH/enforcer-parent/enforcer/src/main/resources/Dockerfile.ubuntu  ./ENFORCER
cp $CC_REPO_PATH/router/src/main/resources/Dockerfile                           ./ROUTER
cp $CC_REPO_PATH/router/src/main/resources/Dockerfile.ubuntu                    ./ROUTER

# Creates update directory txt
touch ./updates/product.txt
echo "choreo-connect-"$CC_VERSION >> ./updates/product.txt

echo "Choreo Connect folder structure prepared successfully!"
