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

FROM alpine:3.11

LABEL maintainer="dev@wso2.org"

ENV LANG=C.UTF-8

# Microgateway runtime distribution filename.
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
 Read more about Apache License, Version 2.0 here @ http://www.apache.org/licenses/LICENSE-2.0.\n\n"
ENV ENV=${USER_HOME}"/.ashrc"

# GLIB based alpine image (alpine-glib) source is generously borrowed
# From https://github.com/Docker-Hub-frolvlad/docker-alpine-glibc
# Here we install GNU libc (aka glibc) and set C.UTF-8 locale as default.
RUN ALPINE_GLIBC_BASE_URL="https://github.com/sgerrand/alpine-pkg-glibc/releases/download" && \
    ALPINE_GLIBC_PACKAGE_VERSION="2.30-r0" && \
    ALPINE_GLIBC_BASE_PACKAGE_FILENAME="glibc-$ALPINE_GLIBC_PACKAGE_VERSION.apk" && \
    ALPINE_GLIBC_BIN_PACKAGE_FILENAME="glibc-bin-$ALPINE_GLIBC_PACKAGE_VERSION.apk" && \
    ALPINE_GLIBC_I18N_PACKAGE_FILENAME="glibc-i18n-$ALPINE_GLIBC_PACKAGE_VERSION.apk" && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub && \
    wget \
        "$ALPINE_GLIBC_BASE_URL/$ALPINE_GLIBC_PACKAGE_VERSION/$ALPINE_GLIBC_BASE_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_BASE_URL/$ALPINE_GLIBC_PACKAGE_VERSION/$ALPINE_GLIBC_BIN_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_BASE_URL/$ALPINE_GLIBC_PACKAGE_VERSION/$ALPINE_GLIBC_I18N_PACKAGE_FILENAME" && \
    apk add --no-cache \
        "$ALPINE_GLIBC_BASE_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_BIN_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_I18N_PACKAGE_FILENAME" && \
    \
    rm "/etc/apk/keys/sgerrand.rsa.pub" && \
    /usr/glibc-compat/bin/localedef --force --inputfile POSIX --charmap UTF-8 "$LANG" || true && \
    echo "export LANG=$LANG" > /etc/profile.d/locale.sh && \
    \
    apk del glibc-i18n && \
    \
    apk del .build-dependencies && \
    rm \
        "$ALPINE_GLIBC_BASE_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_BIN_PACKAGE_FILENAME" \
        "$ALPINE_GLIBC_I18N_PACKAGE_FILENAME"

# Prepare image with users, groups, packages and other configs.
# Add motd for both ash and bash
RUN \
    addgroup -S -g ${USER_GROUP_ID} ${USER_GROUP} \
    && adduser -S -u ${USER_ID} -h ${USER_HOME} -G ${USER_GROUP} ${USER} \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && printf "$(cat /etc/motd)"' >> ${ENV}; echo "${MOTD}" > /etc/motd \
    && echo '[ ! -z "${TERM}" -a -r /etc/motd ] && printf "$(cat /etc/motd)"' >> ${USER_HOME}"/.bashrc" \
    && apk add --update --no-cache bash libxml2-utils netcat-openbsd \
    && wget --no-check-certificate -O /root/${MGW_SERVER}.zip "${MGW_SERVER_DIST_URL}" \
    && unzip /root/${MGW_SERVER}.zip -d /home/${USER}/ >/dev/null 2>&1 \
    && mkdir -p /home/${USER}/conf \
    && mv /home/${USER}/${MGW_SERVER} ${MGW_RUNTIME_HOME} \
    && mv ${MGW_RUNTIME_HOME}/conf/micro-gw.conf ${USER_HOME}/conf/micro-gw.conf \
    && chown ${USER}:${USER_GROUP} -R ${USER_HOME}  \
    && rm /root/${MGW_SERVER}.zip > /dev/null 2>&1 \
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
