#!/bin/bash

pushd java-filter-chain
ls
mvn clean install
popd
docker-compose up