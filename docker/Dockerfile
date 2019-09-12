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

# Ballerina runtime distribution filename.
ARG MGW_DIST

# Add Ballerina runtime.
COPY ${MGW_DIST} /root/

# Create folders, unzip distribution, create users, & set permissions.
RUN mkdir -p /wso2/files \
    && addgroup wso2 \
    && adduser -S -s /bin/bash -g 'wso2' -G wso2 -D wso2 \
    && apk add --update --no-cache bash \
    && unzip /root/${MGW_DIST} -d /wso2/ > /dev/null 2>&1 \
    && mv /wso2/wso2* /wso2/mgw \
    && mkdir -p /wso2/mgw/runtime \
    && unzip /wso2/mgw/runtime.zip -d wso2/mgw/runtime \
    && rm -rf /wso2/mgw/runtime.zip \
    && cp /wso2/mgw/lib/gateway/*.jar /wso2/mgw/runtime/bre/lib/ \
    && chown -R wso2:wso2 /wso2 \
    && rm -rf /root/${MGW_DIST} > /dev/null 2>&1 \
    && rm -rf /var/cache/apk/*

ENV BALLERINA_HOME /wso2/mgw/runtime
ENV PATH $BALLERINA_HOME/bin:$PATH

WORKDIR /home/wso2
VOLUME /home/wso2

USER wso2

CMD ballerina run --config /wso2/mgw/conf/micro-gw.conf /home/exec/${project}.jar
