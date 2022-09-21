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

rm ../../.github/workflows/coverage.yml
rm ../../enforcer-parent/commons/target/site/jacoco/jacoco.xml
rm ../../enforcer-parent/enforcer/target/site/jacoco/jacoco.xml
rm ../../router/target/mgw-wasm/.cache/bazel/_bazel_envoybuild/ac6b95f765ecc3e5ca8ed36bf09bfca3/external/emscripten_bin_linux/emscripten/system/lib/compiler-rt/lib/sanitizer_common/sanitizer_coverage_interface.inc
echo "Preparing code coverage files after integration tests completed successfully...."
