/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;

/**
 * Base test class for CLI based tests
 */
public class BaseTestCase {
    protected ServerInstance microGWServer;
    protected CLIExecutor cliExecutor;
    protected MockHttpServer mockHttpServer;

    public void init(String label) throws Exception {
        microGWServer = ServerInstance.initMicroGwServer(TestConstant.GATEWAY_LISTENER_PORT);
        String cliHome = microGWServer.getServerHome();

        mockHttpServer = new MockHttpServer(9443);
        mockHttpServer.start();
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label);

        String balPath = CLIExecutor.getInstance().getLabelBalx(label);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();
        String[] args = { "--config", configPath };
        microGWServer.startMicroGwServer(balPath, args);
    }

    public void finalize() throws Exception {
        mockHttpServer.stopIt();
        microGWServer.stopServer(false);
    }
}
