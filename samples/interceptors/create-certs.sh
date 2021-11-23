#!/bin/bash
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

DOMAIN=xml-interceptor
STORE_PASS=wso2carbon

CC_REPO_ROOT=$(realpath "../../")
rm -rf resources
mkdir -p resources/certs
RESOURCES=$(realpath resources)
CERTS="${RESOURCES}/certs"

set -ex
cp "${CC_REPO_ROOT}/resources/security/mg.pem" "${CERTS}/mg.pem"
cp "${CC_REPO_ROOT}/resources/security/mg.key" "${RESOURCES}/mg.key"

# Generate server RSA 2048 key pair
keytool -genkeypair -alias interceptor -keyalg RSA -keysize 2048 \
  -dname "CN=${DOMAIN},O=wso2,OU=Choreo Connect,S=CA,C=US" \
  -ext SAN=dns:${DOMAIN},dns:localhost \
  -keypass ${STORE_PASS} -keystore "${RESOURCES}/interceptor.jks" -storepass ${STORE_PASS}

# Export public certificates of server
keytool -exportcert -alias interceptor -file "${CERTS}/interceptor.crt" \
  -keystore "${RESOURCES}/interceptor.jks" -storepass ${STORE_PASS}
openssl x509 -inform der -in "${CERTS}/interceptor.crt" -out "${CERTS}/interceptor.pem"
rm "${CERTS}/interceptor.crt"

# Export private key of server
keytool -importkeystore -srckeystore "${RESOURCES}/interceptor.jks" -srcalias interceptor -srcstorepass ${STORE_PASS}\
    -destkeystore keystore.p12 -deststoretype PKCS12 -deststorepass temp_password -destkeypass temp_password
openssl pkcs12 -in keystore.p12 -nodes -nocerts -out "${CERTS}/interceptor.key" -password pass:temp_password
rm keystore.p12

# Import the CC public certificates into server truststore
keytool -importcert -keystore "${RESOURCES}/interceptor.jks" -alias mg \
  -file "${CERTS}/mg.pem" \
  -storepass ${STORE_PASS} -noprompt
