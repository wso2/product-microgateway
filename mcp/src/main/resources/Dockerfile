# --------------------------------------------------------------------
# Copyright (c) 2025, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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

FROM alpine:3.21

ARG MCP_USER=wso2
ARG MCP_USER_ID=10500
ARG MCP_USER_GROUP=wso2
ARG MCP_USER_GROUP_ID=10500
ARG MCP_USER_HOME=/home/${MCP_USER}

RUN \
    addgroup -S -g ${MCP_USER_GROUP_ID} ${MCP_USER_GROUP} \
    && adduser -S -u ${MCP_USER_ID} -h ${MCP_USER_HOME} -G ${MCP_USER_GROUP} ${MCP_USER}

WORKDIR ${MCP_USER_HOME}

COPY maven/transform-mcp ./transform-mcp
COPY maven/conf/config.toml ./config.toml
COPY maven/security ./security

RUN chown -R ${MCP_USER}:${MCP_USER_GROUP} ${MCP_USER_HOME}

USER ${MCP_USER}

EXPOSE 8080

CMD [ "./transform-mcp" ]
