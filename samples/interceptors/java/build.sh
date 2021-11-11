#!/bin/bash
set -ex
IMAGE_NAME=wso2am/cc-sample-xml-interceptor-java
VERSION=v1.0.0

SRC="spring-server-generated"

# copy the keystore file from resources
cp ../resources/interceptor.jks "${SRC}/src/main/resources/interceptor.jks"

cd $SRC
mvn clean package;
docker build -t $IMAGE_NAME:$VERSION .
