// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/java;

# Get the Alias name of the certificate used in handshake.
#
# + cert - Certicate used in Mutual SSL handshake
# + return - Name of the certificate alias or error occurs during the process

public function getAliasFromRequest(string cert) returns handle|error {
    handle certificate = java:fromString(cert);
    handle|error certAlias = jgetAliasFromRequest(certificate);
    return certAlias;
}

function jgetAliasFromRequest(handle cert) returns handle|error = @java:Method {
    name: "getAliasFromRequest",
    class: "org.wso2.micro.gateway.core.mutualssl.CertificateUtils"
} external;

# Get the Alias name of the cert used in header append by load balancer.
#
# + cert - Certicate append in the header
# + return - Name of the certificate alias or error occurs during the process
public function getAliasFromHeaderCert(string cert) returns handle|error {
    handle certificate = java:fromString(cert);
    handle|error certAlias = jgetAliasFromHeaderCert(certificate);
    return certAlias;
}

function jgetAliasFromHeaderCert(handle cert) returns handle|error = @java:Method {
    name: "getAliasFromHeaderCert",
    class: "org.wso2.micro.gateway.core.mutualssl.CertificateUtils"
} external;

# Load the trustore in keystore.
#
# + trustStorePath - truststore location
# + trustStorePassword - truststore password
function loadKeyStore(string trustStorePath,string trustStorePassword) {
    handle trustStorePath1 = java:fromString(trustStorePath);
    handle trustStorePassword1 = java:fromString(trustStorePassword);
    jloadKeyStore(trustStorePath1, trustStorePassword1);
}

function jloadKeyStore(handle trustStorePath,handle trustStorePassword) = @java:Method {
    name: "loadKeyStore",
    class: "org.wso2.micro.gateway.core.mutualssl.LoadKeyStore"
} external;
