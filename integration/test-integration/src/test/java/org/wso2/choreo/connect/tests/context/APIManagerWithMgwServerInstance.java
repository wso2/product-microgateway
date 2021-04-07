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
import org.testcontainers.containers.wait.strategy.Wait;
import org.wso2.choreo.connect.tests.mockbackend.MockBackendServer;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;

/**
 * APIM with Mgw server instance class.
 */
public class APIManagerWithMgwServerInstance extends MgwServerImpl {

    /**
     * initialize a docker environment using docker compose.
     *
     * @throws IOException          if something goes wrong while adding the mock backend to the docker-compose.yaml
     * @throws MicroGWTestException if something goes wrong while copying server configs
     */
    public APIManagerWithMgwServerInstance() throws IOException, MicroGWTestException {
        this(null, false);
    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath external conf.toml path
     * @throws IOException          if something goes wrong while adding the mock backend to the docker-compose.yaml
     * @throws MicroGWTestException if something goes wrong while copying server configs
     */
    public APIManagerWithMgwServerInstance(String confPath) throws IOException, MicroGWTestException {
        this(confPath, false);
    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath   - external conf.toml path
     * @param tlsEnabled - if the backend needs to have the tls enabled server additionally
     * @throws IOException          if something goes wrong while adding the mock backend to the docker-compose.yaml
     * @throws MicroGWTestException if something goes wrong while copying server configs
     */
    public APIManagerWithMgwServerInstance(String confPath, boolean tlsEnabled) throws MicroGWTestException,
                                                                                       IOException {
        createTmpMgwSetup(false);
        if (!StringUtils.isEmpty(confPath)) {
            Utils.copyFile(confPath,
                    mgwTmpServerPath + File.separator + "docker-compose" + File.separator + "conf" + File.separator
                            + "config.toml");
        }
        String dockerComposePath =
                mgwTmpServerPath + File.separator + "docker-compose" + File.separator + "choreo-connect-with-apim"
                        + File.separator + "docker-compose.yaml";
        // add mock backend service to the docker-compose.yaml file
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerComposePath, tlsEnabled);

        environment = new DockerComposeContainer(new File(dockerComposePath)).withLocalCompose(true).waitingFor(
                TestConstant.APIM_SERVICE_NAME_IN_DOCKER_COMPOSE,
                Wait.forLogMessage("\\/localhost:9443\\/carbon\\/", 1));
    }
}
