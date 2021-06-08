/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.context;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.DockerComposeContainer;
import org.wso2.choreo.connect.tests.mockbackend.MockBackendServer;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Mgw server instance class.
 */
public class CcInstance extends ChoreoConnectImpl {

    /**
     * initialize a docker environment using docker compose.
     *
     * @throws IOException
     * @throws CCTestException
     */
    public CcInstance() throws IOException, CCTestException {
        this(null, false);

    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath external conf.toml path
     * @throws IOException
     * @throws CCTestException
     */
    public CcInstance(String confPath) throws IOException, CCTestException {
        this(confPath, false);
    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath   external conf.toml path
     * @param tlsEnabled if the backend needs to have the tls enabled server additionally
     * @throws IOException
     * @throws CCTestException
     */
    public CcInstance(String confPath, boolean tlsEnabled) throws IOException, CCTestException {
        createTmpMgwSetup();
        if (!StringUtils.isEmpty(confPath)) {
            Utils.copyFile(confPath,
                    ccTempPath + TestConstant.DOCKER_COMPOSE_CC_DIR + TestConstant.CONFIG_TOML_PATH);
        }
        String dockerComposePath = ccTempPath + TestConstant.DOCKER_COMPOSE_CC_DIR
                        + TestConstant.DOCKER_COMPOSE_YAML_PATH;

        MockBackendServer.addMockBackendServiceToDockerCompose(dockerComposePath, tlsEnabled);
        environment = new DockerComposeContainer(new File(dockerComposePath)).withLocalCompose(true);
        addCcLoggersToEnv();
    }
}
