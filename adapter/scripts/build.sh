#!/bin/bash -x
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

# TODO: (VirajSalaka) remove this after go version migration
# For unit tests
export CC_ADAPTER_SERVER_PORT=9401
export CC_Enforcer_JwtGenerator_Enabled=true
export CC_GlobalAdapter_RetryInterval=25
export CC_Analytics_Adapter_bufferSizeBytes=32768
export CC_Enforcer_JwtIssuer_ValidityPeriod=1800
export CC_Adapter_Consul_PollInterval=2
export cc_analytics_enforcer_configProperties_authToken="test-token"
export cc_enforcer_security_tokenService_1_name=MGW-Test
export adapter_admin_pwd="password"

export cc_test_test_stringarray="foo2, bar2"
export cc_test_test_intarray=1,3
export cc_test_test_floatarray=1.2,2.4
export cc_test_test_int32array=4
export cc_test_test_int64array=21474836479
export cc_test_test_float32val=1.5
export cc_test_test_float64val=6.5
export cc_test_test_uintarray=50
export cc_test_test_uint32array=100
export cc_test_test_uint64array=42949672959
export cc_test_test_uintarray2=-50

# Fault path test
export CC_Adapter_Server_Enabled=string

go clean -testcache
go test -race -coverprofile=./target/coverage.out -covermode=atomic ./...
if [ $? -ne 0 ]; then 
  echo "FAILED: Unit tests failure"
  exit 1
fi

golint -set_exit_status ./...
if [ $? -ne 0 ]; then
  echo "INFO: Trying to install golint"
  go install golang.org/x/lint/golint
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

# To clean go.sum and go.mod
go mod tidy

GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -v -o target/adapter-linux-amd64 github.com/wso2/product-microgateway/adapter/cmd/adapter
if [ $? -ne 0 ]; then 
  echo "FAILED: Build failure for GOARCH=amd64"
  exit 1
fi 

GOOS=linux GOARCH=arm64 CGO_ENABLED=0 go build -v -o target/adapter-linux-arm64 github.com/wso2/product-microgateway/adapter/cmd/adapter
if [ $? -ne 0 ]; then 
  echo "FAILED: Build failure for GOARCH=arm64"
  exit 1
fi  
