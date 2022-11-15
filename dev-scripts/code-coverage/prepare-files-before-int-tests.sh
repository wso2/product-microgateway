#!/bin/bash
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

echo "Preparing code coverage files before integration tests..."
mkdir ../../resources/enforcer/dropins/
chmod 777 ../../enforcer-parent/enforcer/target/coverage-aggregate-reports/aggregate.exec
export JAVA_AGENT_ARG="-javaagent:\/home\/wso2\/lib\/org.jacoco.agent-0.8.8-runtime.jar=destfile=\/home\/wso2\/lib\/dropins\/aggregate.exec,append=true"
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  sed -i "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG}/g" ../../resources/docker-compose/docker-compose.yaml
  sed -i "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG} -Dhttpclient.hostnameVerifier=AllowAll/g"  ../../resources/docker-compose/apim/docker-compose.yaml
  sed -i "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG}/g" ../../integration/test-integration/src/test/resources/dockerCompose/cc-cacert-mounted-mtls.yaml
  sed -i "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG} -Dhttpclient.hostnameVerifier=AllowAll/g"  ../../integration/test-integration/src/test/resources/dockerCompose/cc-in-common-network-docker-compose.yaml
elif [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i '' "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG}/g" ../../resources/docker-compose/docker-compose.yaml
  sed -i '' "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG} -Dhttpclient.hostnameVerifier=AllowAll/g"  ../../resources/docker-compose/apim/docker-compose.yaml
  sed -i '' "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG}/g" ../../integration/test-integration/src/test/resources/dockerCompose/cc-cacert-mounted-mtls.yaml
  sed -i '' "s/JAVA_OPTS.*/JAVA_OPTS=\${JAVA_OPTS} ${JAVA_AGENT_ARG} -Dhttpclient.hostnameVerifier=AllowAll/g"  ../../integration/test-integration/src/test/resources/dockerCompose/cc-in-common-network-docker-compose.yaml
fi
echo "Preparing code coverage files before integration tests completed successfully..."
