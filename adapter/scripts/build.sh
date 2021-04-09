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

export MGW_HOME=${PWD}/../resources
go clean -testcache
go test -race -coverprofile=./target/coverage.txt -covermode=atomic ./...
if [ $? -ne 0 ]; then 
  echo "FAILED: Unit tests failure"
  exit 1
fi

golint -set_exit_status ./...
if [ $? -ne 0 ]; then
  echo "INFO: Trying to install golint"
  go get -u golang.org/x/lint/golint
  golint -set_exit_status ./...
  if [ $? -ne 0 ]; then
    echo "FAILED: golint Failure"
    exit 1
  fi
fi

go vet -c=5 ./...
if [ $? -ne 0 ]; then 
  echo "FAILED: go vet Failure"
  exit 1
fi

GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -v -o target/adapter-ubuntu github.com/wso2/adapter/cmd/adapter
if [ $? -ne 0 ]; then 
  echo "FAILED: Build failure"
  exit 1
fi  
