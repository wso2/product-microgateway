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
ARG MGW_SERVER_NAME=wso2am-micro-gw
ARG MGW_SERVER_VERSION=3.0.2
ARG MGW_SERVER=${MGW_SERVER_NAME}-${MGW_SERVER_VERSION}
ARG MGW_SERVER_HOME=${USER_HOME}/${MGW_SERVER}
ARG MGW_RUNTIME_HOME=${USER_HOME}/wso2
ARG MGW_SERVER_DIST_URL=https://bintray.com/wso2/binaryGA/download_file?file_path=${MGW_SERVER}.zip

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

# Create folders, unzip distribution
RUN \
    wget --no-check-certificate -O /root/${MGW_SERVER}.zip "${MGW_SERVER_DIST_URL}" \
    && unzip /root/${MGW_SERVER}.zip -d /home/ballerina/ >/dev/null 2>&1 \
    && mkdir -p /home/ballerina/${MGW_SERVER}/runtime \
    && mkdir -p /home/ballerina/conf \
    && unzip ${MGW_SERVER_HOME}/runtime.zip -d ${MGW_SERVER_HOME}/runtime > /dev/null 2>&1 \
    && rm -rf ${MGW_SERVER_HOME}/runtime.zip \
    && cp ${MGW_SERVER_HOME}/lib/gateway/*.jar ${MGW_SERVER_HOME}/runtime/bre/lib/ \
    && mv /home/ballerina/${MGW_SERVER} ${MGW_RUNTIME_HOME} \
    && chown ${USER}:${USER_GROUP} -R ${MGW_RUNTIME_HOME}  \
    && rm -rf /root/${MGW_SERVER}.zip > /dev/null 2>&1

ENV BALLERINA_HOME ${MGW_RUNTIME_HOME}/runtime
ENV PATH $BALLERINA_HOME/bin:$PATH
ENV GWHOME ${MGW_RUNTIME_HOME}

WORKDIR /home/ballerina

USER ballerina

# expose ports
EXPOSE 9090 9095 9096

CMD ballerina run --config ${GWHOME}/conf/micro-gw.conf /home/exec/${project}.balx 2>&1  | tee -a ${GWHOME}/logs/microgateway.log
