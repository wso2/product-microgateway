# --------------------------------------------------------------------
# Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

FROM ballerina/ballerina-runtime:0.990.5
LABEL maintainer="dev@wso2.org"

COPY runtime/ /ballerina/runtime/bre/lib/
COPY security/ /ballerina/runtime/bre/security/
COPY license/ /home/ballerina/
ADD conf/ /home/ballerina/conf/

CMD ballerina run --config /home/ballerina/conf/micro-gw.conf /home/exec/${project}.balx
