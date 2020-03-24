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

# set base Docker image to AdoptOpenJDK Ubuntu Docker image
FROM adoptopenjdk:8u222-b10-jdk-hotspot
LABEL maintainer="dev@wso2.org"

ENV LANG=C.UTF-8

# set Docker image build arguments
# build arguments for user/group configurations
ARG USER=ballerina
ARG USER_ID=802
ARG USER_GROUP=ballerina
ARG USER_GROUP_ID=802
ARG USER_HOME=/home/${USER}

# build arguments for WSO2 product installation
ARG MGW_SERVER_NAME=wso2am-micro-gw-linux
ARG MGW_SERVER_VERSION=3.1.0
ARG MGW_SERVER=${MGW_SERVER_NAME}-${MGW_SERVER_VERSION}
ARG MGW_RUNTIME_HOME=${USER_HOME}/wso2
ARG MGW_SERVER_DIST_URL=https://github.com/wso2/product-microgateway/releases/download/v${MGW_SERVER_VERSION}/${MGW_SERVER}.zip

# build argument for MOTD
ARG MOTD="\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, running with its latest GA release \n\
 which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"

# create the non-root user and group and set MOTD login message
RUN \
    groupadd --system -g ${USER_GROUP_ID} ${USER_GROUP} \
    && useradd --system --create-home --home-dir ${USER_HOME} --no-log-init -g ${USER_GROUP_ID} -u ${USER_ID} ${USER} \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && cat /etc/motd' >> /etc/bash.bashrc; echo "${MOTD}" > /etc/motd

# install required packages
RUN \
    apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        libxml2-utils \
        netcat \
        unzip \
        wget \
    && rm -rf /var/lib/apt/lists/*

# Prepare image with users, groups, packages and other configs.
RUN \
    wget --no-check-certificate -O ${MGW_SERVER}.zip "${MGW_SERVER_DIST_URL}" \
    && unzip -d ${USER_HOME} ${MGW_SERVER}.zip \
    && mkdir -p /home/ballerina/conf \
    && mv /home/${USER}/${MGW_SERVER} ${MGW_RUNTIME_HOME} \
    && mv ${MGW_RUNTIME_HOME}/conf/micro-gw.conf ${USER_HOME}/conf/micro-gw.conf \
    && chown ${USER}:${USER_GROUP} -R ${MGW_RUNTIME_HOME}  \
    && rm -rf ${MGW_SERVER}.zip \
    && rm "/root/.wget-hsts"


ENV BALLERINA_HOME ${MGW_RUNTIME_HOME}/runtime
ENV GW_HOME ${MGW_RUNTIME_HOME}
ENV PATH $GW_HOME/bin:$PATH
ENV MGW_CONF_DIR=${USER_HOME}/conf
ENV JAVA_OPTS="${JAVA_OPTS}"

WORKDIR ${USER_HOME}

USER ${USER}

RUN \
    touch ${USER_HOME}/gc.log \
    && touch ${USER_HOME}/heapdump.hprof

# expose ports
EXPOSE 9090 9095

CMD gateway /home/exec/${project}.jar
