
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

FROM openjdk:jre-alpine
LABEL maintainer="dev@wso2.org"

# Microgateway runtime distribution filename.
ARG MGW_DIST
ARG USER=ballerina
ARG USER_ID=802
ARG USER_GROUP=ballerina
ARG USER_GROUP_ID=802
ARG USER_HOME=/home/${USER}

# build arguments for WSO2 product installation
ARG MGW_SERVER_NAME=wso2am-micro-gw-linux
ARG MGW_SERVER_VERSION=3.1.0-SNAPSHOT
ARG MGW_SERVER=${MGW_SERVER_NAME}-${MGW_SERVER_VERSION}
ARG MGW_SERVER_HOME=${USER_HOME}/${MGW_SERVER}
ARG MGW_RUNTIME_HOME=${USER_HOME}/wso2
ARG MGW_SERVER_DIST_URL=http://10.100.5.210:8000/${MGW_SERVER}.zip
ARG INTERNAL_JRE=jdk8u202-b08-jre

# build argument for MOTD
ARG MOTD='printf "\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, running with its latest GA release \n\
 which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"'
ENV ENV=${USER_HOME}"/.ashrc"

# create the non-root user and group and set MOTD login message
RUN \
    addgroup -S -g ${USER_GROUP_ID} ${USER_GROUP} \
    && adduser -S -u ${USER_ID} -h ${USER_HOME} -G ${USER_GROUP} ${USER} \
    && echo ${MOTD} > "${ENV}"

RUN \
    apk add --update --no-cache \
        bash \
        libxml2-utils \
        netcat-openbsd \
    && rm -rf /var/cache/apk/*

COPY  runtime/ /root/
# Create folders, unzip distribution
RUN \
    unzip /root/${MGW_SERVER}.zip -d /home/ballerina/ >/dev/null 2>&1 \
    && rm -rf /home/ballerina/${MGW_SERVER}/lib/${INTERNAL_JRE} \
    && mkdir -p /home/ballerina/conf \
    && mv /home/ballerina/${MGW_SERVER} ${MGW_RUNTIME_HOME} \
    && cp ${MGW_RUNTIME_HOME}/conf/micro-gw.conf ${USER_HOME}/conf/micro-gw.conf \
    && chown ${USER}:${USER_GROUP} -R ${USER_HOME}  \
    && rm -rf /root/${MGW_SERVER}.zip > /dev/null 2>&1

ENV BALLERINA_HOME ${MGW_RUNTIME_HOME}/runtime
ENV GW_HOME ${MGW_RUNTIME_HOME}
ENV PATH $GW_HOME/bin:$PATH

WORKDIR /home/ballerina

USER ballerina

ENV JAVA_OPTS="${JAVA_OPTS}"

RUN \
    touch ${USER_HOME}/gc.log \
    && touch ${USER_HOME}/heapdump.hprof

# expose ports
EXPOSE 9090 9095 9096

CMD gateway /home/exec/${project}.jar --b7a.config.file=${USER_HOME}/conf/micro-gw.conf
