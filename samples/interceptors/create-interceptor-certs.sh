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

mkdir certs
set -ex
DOMAIN=xml-interceptor

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}"

# interceptor certs
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 \
    -subj "/O=${DOMAIN} Inc./CN=${DOMAIN}" \
    -extensions SAN \
    -config <(cat /etc/ssl/openssl.cnf \
        <(printf "\n[SAN]\nsubjectAltName=DNS:${DOMAIN},DNS:localhost")) \
    -keyout certs/interceptor.key \
    -out certs/interceptor.crt

# mg.pem
cp ../../resources/security/mg.pem certs/
