# Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

FROM adoptopenjdk/openjdk11:jre-11.0.11_9-alpine
RUN addgroup -S wso2am && adduser -S wso2am -G wso2am
USER wso2am:wso2am

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} interceptor-svc.jar

EXPOSE 9081
ENTRYPOINT ["java","-jar","/interceptor-svc.jar"]
