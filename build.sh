#!/bin/bash

pushd java-filter-chain
mvn clean install
popd
docker-compose up
