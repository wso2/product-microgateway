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

# TODO: (VirajSalaka) finalize jdk 11 image
FROM adoptopenjdk:11.0.8_10-jre-hotspot-bionic
LABEL maintainer="WSO2 Docker Maintainers <dev@wso2.org>" 

ENV LANG=C.UTF-8

ARG MG_USER=wso2
ARG MG_USER_ID=802
ARG MG_USER_GROUP=wso2
ARG MG_USER_GROUP_ID=802
ARG MG_USER_HOME=/home/${MGW_USER}

ARG MG_VERSION=1.0-SNAPSHOT
ENV VERSION=${MG_VERSION}

ARG MOTD="\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, running with its latest GA release \n\
 which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"

RUN \
    groupadd --system -g ${MG_USER_GROUP_ID} ${MG_USER_GROUP} \
    && useradd --system --create-home --home-dir ${MG_USER_HOME} --no-log-init -g ${MG_USER_GROUP_ID} -u ${MG_USER_ID} ${MG_USER} \
    && mkdir ${MG_USER_HOME}/lib && mkdir ${MG_USER_HOME}/mg/security \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && cat /etc/motd' >> /etc/bash.bashrc; echo "${MOTD}" > /etc/motd

WORKDIR ${MG_USER_HOME}
USER ${MG_USER}

ENV FILTER_HOME=${MG_USER_HOME}

COPY maven/ lib/
COPY maven/resources/conf conf
COPY maven/resources/security mg/security
CMD java -cp "lib/*" org.wso2.micro.gateway.filter.core.grpc.server.AuthServer
