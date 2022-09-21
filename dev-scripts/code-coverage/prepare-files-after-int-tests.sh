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
echo "Preparing code coverage files after integration tests...."
rm -f ../../resources/enforcer/dropins/aggregate.exec
mkdir ../../covReports
cp ../../adapter/target/coverage.out ../../covReports
cp ../../integration/test-integration/target/site/jacoco-aggregate/jacoco.xml ../../covReports
echo "Preparing code coverage files after integration tests completed successfully...."
