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

FROM ubuntu:22.04

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

# install JDK Dependencies
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends tzdata curl ca-certificates fontconfig locales python-is-python3 \
    && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
    && locale-gen en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_VERSION jdk-11.0.14+9

# install OpenJDK 11
RUN set -eux; \
    ARCH="$(dpkg --print-architecture)"; \
    case "${ARCH}" in \
       aarch64|arm64) \
         ESUM='0ba188a2a739733163cd0049344429d2284867e04ca452879be24f3b54320c9a'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.14%2B9/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.14_9.tar.gz'; \
         ;; \
       armhf|arm) \
         ESUM='a0ba2fa6a982fe6c09c721ac9c72c8e5323991a529403daacac323549df4495d'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.14%2B9/OpenJDK11U-jdk_arm_linux_hotspot_11.0.14_9.tar.gz'; \
         ;; \
       ppc64el|powerpc:common64) \
         ESUM='91c63331faba8c842aef312d415b3e67aecf4f662a36c275f5cb278f7bce1410'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.14%2B9/OpenJDK11U-jdk_ppc64le_linux_hotspot_11.0.14_9.tar.gz'; \
         ;; \
       s390x|s390:64-bit) \
         ESUM='4dd43e06830e62d65c698b393db10bab39ec6575de08db8d2f5b66cfe09c8c85'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.14%2B9/OpenJDK11U-jdk_s390x_linux_hotspot_11.0.14_9.tar.gz'; \
         ;; \
       amd64|i386:x86-64) \
         ESUM='1189bee178d11402a690edf3fbba0c9f2ada1d3a36ff78929d81935842ef24a9'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.14%2B9/OpenJDK11U-jdk_x64_linux_hotspot_11.0.14_9.tar.gz'; \
         ;; \
       *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
         ;; \
    esac; \
    curl -LfsSo /tmp/openjdk.tar.gz ${BINARY_URL}; \
    echo "${ESUM} */tmp/openjdk.tar.gz" | sha256sum -c -; \
    mkdir -p /opt/java/openjdk; \
    cd /opt/java/openjdk; \
    tar -xf /tmp/openjdk.tar.gz --strip-components=1; \
    rm -rf /tmp/openjdk.tar.gz;

ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:$PATH"

# Verify Java installation
RUN echo Verifying install ... \
    && echo javac --version && javac --version \
    && echo java --version && java --version \
    && echo Complete.

LABEL maintainer="WSO2 Docker Maintainers <wso2.com>"

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
ENV XDS_MAX_MSG_SIZE=4194304
ENV XDS_MAX_RETRIES=3
#todo update the connection string
ENV APPLICATIONINSIGHTS_CONNECTION_STRING=InstrumentationKey=;IngestionEndpoint=https://westus2-2.in.applicationinsights.azure.com/

ARG MOTD="\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"

RUN \
    groupadd --system -g ${MG_USER_GROUP_ID} ${MG_USER_GROUP} \
    && useradd --system --create-home --home-dir ${MG_USER_HOME} --no-log-init -g ${MG_USER_GROUP_ID} -u ${MG_USER_ID} ${MG_USER} \
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

CMD java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="${ENFORCER_HOME}/logs/heap-dump.hprof" $JAVA_OPTS -Dlog4j.configurationFile="${ENFORCER_HOME}/conf/log4j2.properties" -DtracingEnabled="true" -cp "lib/*:lib/dropins/*" org.wso2.choreo.connect.enforcer.server.AuthServer
