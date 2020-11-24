/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2am.micro.gw.tests.context;

import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class MgwServerInstance implements MgwServer {

    public static final DockerComposeContainer environment =
            new DockerComposeContainer(new File("/home/chashika/Documents/wso2/Envoy/product-microgateway/" +
                    "test-integration/target/micro-gwtmp/wso2am-micro-gw-4.0.0-m2-SNAPSHOT/docker-compose.yaml"))
                    .withLocalCompose(true);


    public static void startMGW() throws MicroGWTestException {
        //Utils.checkPortAvailability(TestConstant.ADAPTER_IMPORT_API_PORT);
        //Utils.checkPortAvailability(TestConstant.GATEWAY_LISTENER_HTTPS_PORT);
        environment.start();
    }

    public static void stopMGW() {
        environment.stop();
    }
}
