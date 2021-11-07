#!/bin/bash
set -ex
IMAGE_NAME=wso2am/cc-sample-xml-interceptor-java
VERSION=v1.0.0

cd "spring-server-generated"
mvn clean package;
mkdir -p target/dependency && (cd target/dependency || exit; jar -xf ../*.jar)
docker build -t $IMAGE_NAME:$VERSION .
