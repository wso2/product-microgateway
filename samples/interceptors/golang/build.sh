#!/bin/bash
set -ex
IMAGE_NAME=wso2am/cc-sample-xml-interceptor-go
VERSION=v1.0.0

SRC="go-interceptor-server-generated"

cp -r ../resources/certs "${SRC}/certs"
cd $SRC
docker build -t $IMAGE_NAME:$VERSION --build-arg CERT_DIR=certs .
rm -rf certs
