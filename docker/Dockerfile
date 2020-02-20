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

FROM debian:stable-slim
#FROM alpine:3.9.5

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
ARG MGW_SERVER_VERSION=3.1.0-alpha
ARG MGW_SERVER=${MGW_SERVER_NAME}-${MGW_SERVER_VERSION}
ARG MGW_RUNTIME_HOME=${USER_HOME}/wso2
ARG INTERNAL_JRE=jdk8u202-b08-jre
ARG MGW_SERVER_DIST_URL=https://github.com/wso2/product-microgateway/releases/download/v3.1.0-alpha/wso2am-micro-gw-linux-3.1.0-alpha.zip

# build argument for MOTD
ARG MOTD='printf "\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, running with its latest GA release \n\
 which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"'
ENV ENV=${USER_HOME}"/.bashrc"

# create the non-root user and group and set MOTD login message
RUN \
    groupadd -r -g ${USER_GROUP_ID} ${USER_GROUP} \
    && useradd -m -r -u ${USER_ID} -d ${USER_HOME} -g ${USER_GROUP} ${USER} \
    && echo ${MOTD} > "${ENV}"

# To avoid warning message due to APIIUtils
ENV DEBCONF_NOWARNINGS yes

RUN \
    apt-get update \
        && apt-get install -y unzip libxml2-utils netcat-openbsd wget

# Create folders, unzip distribution
RUN \
    wget --no-check-certificate -O /root/${MGW_SERVER}.zip "${MGW_SERVER_DIST_URL}" \
    && unzip /root/${MGW_SERVER}.zip -d /home/${USER}/ >/dev/null 2>&1 \
    && mkdir -p /home/${USER}/conf \
    && mv /home/${USER}/${MGW_SERVER} ${MGW_RUNTIME_HOME} \
    && mv ${MGW_RUNTIME_HOME}/conf/micro-gw.conf ${USER_HOME}/conf/micro-gw.conf \
    && chown ${USER}:${USER_GROUP} -R ${USER_HOME}  \
    && rm -rf /root/${MGW_SERVER}.zip > /dev/null 2>&1

ENV BALLERINA_HOME ${MGW_RUNTIME_HOME}/runtime
ENV GW_HOME ${MGW_RUNTIME_HOME}
ENV PATH $GW_HOME/bin:$PATH
ENV MGW_CONF_DIR=${USER_HOME}/conf

WORKDIR ${USER_HOME}

USER ${USER}

ENV JAVA_OPTS="${JAVA_OPTS}"

RUN \
    touch ${USER_HOME}/gc.log \
    && touch ${USER_HOME}/heapdump.hprof

# expose ports
EXPOSE 9090 9095 9096

CMD gateway /home/exec/${project}.jar
