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

FROM alpine:3.18.0

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

RUN apk add --no-cache fontconfig libretls musl-locales musl-locales-lang ttf-dejavu tzdata zlib \
    && rm -rf /var/cache/apk/*

ENV JAVA_VERSION jdk-11.0.18+10

ENV JAVA_HOME /opt/java/openjdk
ENV PATH $JAVA_HOME/bin:$PATH

RUN set -eux; \
    ARCH="$(apk --print-arch)"; \
    case "${ARCH}" in \
       aarch64|amd64|x86_64) \
         ESUM='81e51ec2da61b227c522881313c7e2a090b29d6cc4171b5e1db3f3d17d863e44'; \
         BINARY_URL='https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.18%2B10/OpenJDK11U-jre_x64_alpine-linux_hotspot_11.0.18_10.tar.gz'; \
         ;; \
       *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
         ;; \
    esac; \
	  wget -O /tmp/openjdk.tar.gz ${BINARY_URL}; \
	  echo "${ESUM} */tmp/openjdk.tar.gz" | sha256sum -c -; \
	  mkdir -p "$JAVA_HOME"; \
	  tar --extract \
	      --file /tmp/openjdk.tar.gz \
	      --directory "$JAVA_HOME" \
	      --strip-components 1 \
	      --no-same-owner \
	  ; \
    rm -f /tmp/openjdk.tar.gz ${JAVA_HOME}/src.zip;

# https://github.com/micrometer-metrics/micrometer/issues/2776
RUN apk --no-cache add gcompat
ENV LD_PRELOAD=/lib/libgcompat.so.0

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
    addgroup -S -g ${MG_USER_GROUP_ID} ${MG_USER_GROUP} \
    && adduser -S -u ${MG_USER_ID} -h ${MG_USER_HOME} -G ${MG_USER_GROUP} ${MG_USER} \
    && mkdir ${MG_USER_HOME}/logs && mkdir -p ${MG_USER_HOME}/lib/dropins \
    && chown -R ${MG_USER}:${MG_USER_GROUP} ${MG_USER_HOME} \
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
