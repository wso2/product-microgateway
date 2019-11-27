// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/auth;
import ballerina/http;
import ballerina/log;
import ballerina/system;

public type ObservabilityMetricListener object {

    private int listenerPort = 0;
    private string listenerType = "HTTPS";
    public http:Listener metricListener;

    public function __init() {
        string ballerina_home = system:getEnv("BALLERINA_HOME");
        string keyStore_path = ballerina_home + "/bre/security/ballerinaKeystore.p12";
        auth:InboundBasicAuthProvider basicAuthProvider = new;
        http:BasicAuthHandler basicAuthHandler = new (basicAuthProvider);
        self.listenerPort = getConfigIntValue(MICRO_GATEWAY_METRICS_PORTS, SECURE_PORT, 9000);
        self.metricListener = new (self.listenerPort, {
            auth: {
                authHandlers: [basicAuthHandler]
            },
            secureSocket: {
                keyStore: {
                    path: keyStore_path,
                    password: "ballerina"
                }
            }
        });
    }

    public function __start() returns error? {
        error? gwListener = self.metricListener.__start();

        log:printInfo(self.listenerType + " Secured Observability listener is active on port " + self.listenerPort.toString());
        return gwListener;
    }

    public function __gracefulStop() returns error? {
        return self.metricListener.__gracefulStop();
    }

    public function __attach(service s, string? name = ()) returns error? {
        return self.metricListener.__attach(s, name);
    }

    public function __immediateStop() returns error? {
        return self.metricListener.__immediateStop();
    }

    public function __detach(service s) returns error? {
        return self.metricListener.__detach(s);
    }

};
