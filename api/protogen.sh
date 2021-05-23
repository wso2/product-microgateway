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
set -e
PROTOC_VERSION=1.20_3

# get script location
cur_dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd $cur_dir
rm -rf build
mkdir -p build/deps

ADAPTER_GEN_DIR=$cur_dir/../adapter/internal/discovery/api
ENFORCER_GEN_DIR=$cur_dir/../enforcer/src/main/gen

# download dependency proto archives from github
echo "Downloading dependencies..."
wget https://github.com/cncf/udpa/archive/5459f2c994033b0afed7e4a70ac7e90c90c1ffee.tar.gz -nv -O build/deps/udpa.tar.gz
wget https://github.com/envoyproxy/data-plane-api//archive/d6828354ba6b4e67fd34ce41a14cbed9ad081b45.tar.gz -nv -O build/deps/envoy.tar.gz
wget https://github.com/envoyproxy/protoc-gen-validate/archive/refs/tags/v0.5.0.tar.gz -nv -O build/deps/validate.tar.gz
mkdir -p build/deps/udpa && tar -xf build/deps/udpa.tar.gz -C build/deps/udpa/ --strip-components 1
mkdir -p build/deps/envoy && tar -xf build/deps/envoy.tar.gz -C build/deps/envoy/ --strip-components 1
mkdir -p build/deps/validate && tar -xf build/deps/validate.tar.gz -C build/deps/validate/ --strip-components 1

printf "Preparing includes"
# create dependency proto include dir for protoc
mkdir -p build/include/
cp -r build/deps/udpa/udpa build/include
cp -r build/deps/envoy/envoy build/include
cp -r build/deps/validate/validate build/include
echo " - done"

# generate code for java
printf "protoc java"
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l java -i proto -i build/include/ -o build/gen/java -d proto/wso2/**
echo " - done"

# generate code for go grpc messages
# for golang build we have to generate code for each proto dir separately.
printf "protoc go messages"
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/api/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/config/enforcer/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/keymgt/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/subscription/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/throttle/
echo " - done"

# generate code for go grpc services
printf "protoc go services"
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/service/api
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/service/config
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/service/keymgt
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/service/subscription
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/go -d proto/wso2/discovery/service/throttle
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i build/include/ -o build/gen/ws-go -d proto/wso2/discovery/service/websocket
echo " - done"

rm -rf $ADAPTER_GEN_DIR/wso2
rm -rf $ENFORCER_GEN_DIR/org
cp -r build/gen/go/ $ADAPTER_GEN_DIR
cp -r build/gen/java/ $ENFORCER_GEN_DIR
