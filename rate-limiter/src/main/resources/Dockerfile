# --------------------------------------------------------------------
# Copyright (c) 2022, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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

FROM envoyproxy/ratelimit:19f2079f
LABEL maintainer="WSO2 Docker Maintainers <wso2.com>"

RUN apk update && apk upgrade --no-cache

ENV LANG=C.UTF-8

ARG MG_USER=wso2
ARG MG_USER_ID=10500
ARG MG_USER_GROUP=wso2
ARG MG_USER_GROUP_ID=10500
ARG MG_USER_HOME=/home/${MG_USER}

ENV HOST=0.0.0.0
ENV PORT=8090
ENV GRPC_HOST=0.0.0.0
ENV GRPC_PORT=8091
ENV USE_STATSD=false
ENV LOG_LEVEL=INFO
ENV LOCAL_CACHE_SIZE_IN_BYTES=1024000

ENV GRPC_SERVER_USE_TLS=true
ENV GRPC_SERVER_TLS_KEY=/home/wso2/security/keystore/rate-limiter.key
ENV GRPC_SERVER_TLS_CERT=/home/wso2/security/keystore/rate-limiter.pem
ENV GRPC_CLIENT_TLS_CACERT=/home/wso2/security/truststore/mg.pem

ENV CONFIG_GRPC_XDS_SERVER_USE_TLS=true
ENV CONFIG_GRPC_XDS_CLIENT_TLS_KEY=/home/wso2/security/keystore/rate-limiter.key
ENV CONFIG_GRPC_XDS_CLIENT_TLS_CERT=/home/wso2/security/keystore/rate-limiter.pem
ENV CONFIG_GRPC_XDS_SERVER_TLS_CACERT=/home/wso2/security/truststore/mg.pem

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

WORKDIR ${MG_USER_HOME}
USER ${MG_USER}
COPY maven/security security
COPY maven/start-server.sh .
COPY maven/LICENSE.txt .

EXPOSE 8090 8091 6070

ENTRYPOINT ["sh", "start-server.sh"]
