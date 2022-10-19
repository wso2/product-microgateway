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

echo "Preparing code coverage files after integration tests...."
rm -f ../../resources/enforcer/dropins/aggregate.exec
rm ../../.github/workflows/coverage.yml
rm ../../enforcer-parent/commons/target/site/jacoco/jacoco.xml
rm ../../enforcer-parent/enforcer/target/site/jacoco/jacoco.xml
rm -rf ../../router/target/mgw-wasm/.cache/bazel/
echo "Preparing code coverage files after integration tests completed successfully...."
