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

FROM alpine:3.19.1
LABEL maintainer="WSO2 Docker Maintainers <wso2.com>"

RUN apk update && apk upgrade --no-cache

ENV LANG=C.UTF-8

ARG MG_USER=wso2
ARG MG_USER_ID=10500
ARG MG_USER_GROUP=wso2
ARG MG_USER_GROUP_ID=10500
ARG MG_USER_HOME=/home/${MG_USER}
ARG GRPC_HEALTH_PROBE_PATH=/bin/grpc_health_probe

ENV ADAPTER_PRIVATE_KEY_PATH=/home/wso2/security/keystore/mg.key
ENV ADAPTER_PUBLIC_CERT_PATH=/home/wso2/security/keystore/mg.pem

ARG MOTD="\n\
 Welcome to WSO2 Docker Resources \n\
 --------------------------------- \n\
 This Docker container comprises of a WSO2 product, running with its latest GA release \n\
 which is under the Apache License, Version 2.0. \n\
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n"

RUN \
    addgroup -S -g ${MG_USER_GROUP_ID} ${MG_USER_GROUP} \
    && adduser -S -u ${MG_USER_ID} -h ${MG_USER_HOME} -G ${MG_USER_GROUP} ${MG_USER} \
    && mkdir ${MG_USER_HOME}/logs && chown -R ${MG_USER}:${MG_USER_GROUP} ${MG_USER_HOME}/logs  \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && cat /etc/motd' >> /etc/bash.bashrc; echo "${MOTD}" > /etc/motd

COPY maven/grpc_health_probe-linux-amd64 ${GRPC_HEALTH_PROBE_PATH}
RUN chmod +x ${GRPC_HEALTH_PROBE_PATH}

WORKDIR ${MG_USER_HOME}
USER ${MG_USER}
COPY maven/security security
COPY maven/conf/config.toml conf/
COPY maven/conf/log_config.toml conf/
COPY maven/adapter-linux-amd64 adapter
COPY maven/check_health.sh .
COPY maven/LICENSE.txt .

# DEBUG_LEVEL is for debugging go-amqp library.
ENV DEBUG_LEVEL=0

EXPOSE 18000 9843 6060

CMD ./adapter
