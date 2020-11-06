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

function exit_with_error() {
    echo "BUILD FAILURE!"
    exit 1
}

pushd filter-core
mvn clean install
if [ $? -ne 0 ]; then
    exit_with_error
fi
popd

rm -rf target/
GOOS=linux GOARCH=amd64 go build -v -o target/micro-gw-ubuntu main.go
if [ $? -ne 0 ]; then
    exit_with_error
fi

cd docker/with-external-build
docker-compose up
