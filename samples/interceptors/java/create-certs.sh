#!/bin/bash

DOMAIN=xml-interceptor
KEYSTORE="spring-server-generated/src/main/resources/interceptor.jks"
STORE_PASS=wso2carbon

CC_REPO_ROOT=$(realpath "../../../")
mkdir certs
CERTS=$(realpath certs)

set -ex
cp "${CC_REPO_ROOT}/resources/security/mg.pem" "${CERTS}/mg.pem"
cp "${CC_REPO_ROOT}/resources/security/mg.key" "${CERTS}/mg.key"

# Generate server RSA 2048 key pair
rm -f $KEYSTORE
keytool -genkeypair -alias interceptor -keyalg RSA -keysize 2048 \
  -dname "CN=${DOMAIN},O=wso2,OU=Choreo Connect,S=CA,C=US" \
  -ext SAN=dns:${DOMAIN},dns:localhost \
  -keypass ${STORE_PASS} -keystore ${KEYSTORE} -storepass ${STORE_PASS}

# Export public certificates of server
keytool -exportcert -alias interceptor -file "${CERTS}/interceptor.crt" \
  -keystore ${KEYSTORE} -storepass ${STORE_PASS}
openssl x509 -inform der -in "${CERTS}/interceptor.crt" -out "${CERTS}/interceptor.pem"
rm "${CERTS}/interceptor.crt"

# Import the CC public certificates into server truststore
keytool -importcert -keystore ${KEYSTORE} -alias mg \
  -file "${CERTS}/mg.pem" \
  -storepass ${STORE_PASS} -noprompt