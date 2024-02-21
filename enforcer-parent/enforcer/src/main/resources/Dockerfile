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

FROM adoptopenjdk/openjdk11:jre-11.0.22_7-alpine
LABEL maintainer="WSO2 Docker Maintainers <wso2.com>"

RUN apk update && apk upgrade --no-cache

LABEL maintainer="WSO2 Docker Maintainers <dev@wso2.org>" 

ENV LANG=C.UTF-8

ARG MG_USER=wso2
ARG MG_USER_ID=10500
ARG MG_USER_GROUP=wso2
ARG MG_USER_GROUP_ID=10500
ARG MG_USER_HOME=/home/${MG_USER}
ARG MG_VERSION=1.0-SNAPSHOT
ARG GRPC_HEALTH_PROBE_PATH=/bin/grpc_health_probe

ENV VERSION=${MG_VERSION}
ENV JAVA_OPTS=""
ENV ENFORCER_HOME=${MG_USER_HOME}

ENV ENFORCER_PRIVATE_KEY_PATH=/home/wso2/security/keystore/mg.key
ENV ENFORCER_PUBLIC_CERT_PATH=/home/wso2/security/keystore/mg.pem
ENV TRUSTED_CA_CERTS_PATH=/home/wso2/security/truststore
ENV ADAPTER_HOST_NAME=adapter
ENV ADAPTER_HOST=adapter
ENV ADAPTER_XDS_PORT=18000
ENV ENFORCER_LABEL="Default"
ENV XDS_MAX_MSG_SIZE=41943040
ENV XDS_MAX_RETRIES=3
#todo update the connection string
ENV APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=;IngestionEndpoint=https://westus2-2.in.applicationinsights.azure.com/

ARG MOTD="\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"

RUN \
    addgroup -S -g ${MG_USER_GROUP_ID} ${MG_USER_GROUP} \
    && adduser -S -u ${MG_USER_ID} -h ${MG_USER_HOME} -G ${MG_USER_GROUP} ${MG_USER} \
    && mkdir ${MG_USER_HOME}/lib && mkdir -p ${MG_USER_HOME}/logs \
    && chown ${MG_USER}:${MG_USER_GROUP} -R ${MG_USER_HOME} \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && cat /etc/motd' >> /etc/bash.bashrc; echo "${MOTD}" > /etc/motd

COPY maven/grpc_health_probe-linux-amd64 ${GRPC_HEALTH_PROBE_PATH}
RUN chmod +x ${GRPC_HEALTH_PROBE_PATH}

WORKDIR ${MG_USER_HOME}
USER ${MG_USER}

COPY maven/lib/ lib/
COPY maven/conf/log4j2.properties conf/log4j2.properties
COPY maven/security security
COPY maven/check_health.sh .
COPY maven/LICENSE.txt .

#todo add applicationinsights.json file and point it to the appInsightsAgent jar
#Add the agent using JVM arg -javaagent:/home/wso2/conf/applicationinsights-agent-3.1.1.jar
#Add the config file using System property -Dapplicationinsights.configuration.file=/home/wso2/conf/applicationinsights.json

EXPOSE 8081 9001

CMD java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="${ENFORCER_HOME}/logs/heap-dump.hprof" $JAVA_OPTS -Dlog4j.configurationFile="${ENFORCER_HOME}/conf/log4j2.properties" -DtracingEnabled="true" -cp "lib/*:lib/dropins/*" org.wso2.choreo.connect.enforcer.server.AuthServer
