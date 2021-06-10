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
PROTOC_VERSION=1.34_4

# get script location
cur_dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd $cur_dir
rm -rf target
mkdir -p target/deps

ADAPTER_GEN_DIR=$cur_dir/../adapter/pkg/discovery/api
ENFORCER_GEN_DIR=$cur_dir/../enforcer/src/main/gen
GREEN='\033[0;32m'
BOLD="\033[1m"
NC='\033[0m' # No Color

# download dependency proto archives from github
echo "Downloading dependencies..."
wget https://github.com/cncf/udpa/archive/5459f2c994033b0afed7e4a70ac7e90c90c1ffee.tar.gz -nv -O target/deps/udpa.tar.gz
wget https://github.com/envoyproxy/data-plane-api//archive/d6828354ba6b4e67fd34ce41a14cbed9ad081b45.tar.gz -nv -O target/deps/envoy.tar.gz
wget https://github.com/envoyproxy/protoc-gen-validate/archive/refs/tags/v0.5.0.tar.gz -nv -O target/deps/validate.tar.gz
mkdir -p target/deps/udpa && tar -xf target/deps/udpa.tar.gz -C target/deps/udpa/ --strip-components 1
mkdir -p target/deps/envoy && tar -xf target/deps/envoy.tar.gz -C target/deps/envoy/ --strip-components 1
mkdir -p target/deps/validate && tar -xf target/deps/validate.tar.gz -C target/deps/validate/ --strip-components 1

printf "Preparing includes"
# create dependency proto include dir for protoc
mkdir -p target/include/
cp -r target/deps/udpa/udpa target/include
cp -r target/deps/envoy/envoy target/include
cp -r target/deps/validate/validate target/include
printf " - ${GREEN}${BOLD}done${NC}\n"

# generate code for java
printf "protoc java"
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l java -i proto -i target/include/ -o target/gen/java -d proto/wso2/
printf " - ${GREEN}${BOLD}done${NC}\n"

# generate code for go grpc messages
# for golang build we have to generate code for each proto dir separately.
printf "protoc go messages"
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/api/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/config/enforcer/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/keymgt/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/subscription/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/throttle/
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go --go-source-relative -i proto -i target/include/ -o target/gen/go -d proto/wso2/discovery/ga/
printf " - ${GREEN}${BOLD}done${NC}\n"

# map of proto imports for which we need to update the genrated import path
# ex: when the go code containing an import to `envoy/service/discovery/v3/discovery.proto`,
# we need the generated go code's import to be `github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3`
# not just `envoy/service/discovery/v3`
import_map=Menvoy/service/discovery/v3/discovery.proto=github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3

# generate code for go grpc services
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/api
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/config
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/keymgt
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/subscription
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/throttle
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/go --go-package-map $import_map --go-source-relative -d proto/wso2/discovery/service/ga
docker run -v `pwd`:/defs namely/protoc-all:$PROTOC_VERSION -l go -i proto -i target/include/ -o target/gen/ws-go --go-source-relative -d proto/wso2/discovery/service/websocket
printf "protoc go services - ${GREEN}${BOLD}done${NC}\n"

rm -rf $ADAPTER_GEN_DIR/wso2
rm -rf $ENFORCER_GEN_DIR/org
rm -rf target/gen/java/org/wso2/choreo/connect/discovery/ga
rm -rf target/gen/java/org/wso2/choreo/connect/discovery/service/ga
cp -r target/gen/go/ $ADAPTER_GEN_DIR
cp -r target/gen/java/ $ENFORCER_GEN_DIR

# remove all the containers created
docker rm -f $(docker ps -a -q -f "ancestor=namely/protoc-all:$PROTOC_VERSION")

printf "${GREEN}${BOLD}BUILD SUCCESS${NC}\n"
