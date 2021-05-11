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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.wso2.choreo.connect.tests.mockbackend.MockBackendServer;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;

/**
 * APIM with Mgw server instance class.
 */
public class CcWithApimInstance extends ChoreoConnectImpl {

    /**
     * initialize a docker environment using docker compose with test-integration jar copied with.
     *
     * @param apimDeploymentTomlPath      - external APIM deployment.toml path
     * @param ccConfPath                  - external conf.toml path
     * @param tlsEnabled                  - if the backend needs to have the tls enabled server additionally
     * @throws IOException          if something goes wrong while adding the mock backend to the docker-compose.yaml
     * @throws CCTestException if something goes wrong while copying server configs
     */
    public CcWithApimInstance(String apimDeploymentTomlPath, String ccConfPath, boolean tlsEnabled)
            throws CCTestException,
            IOException {
        createTmpMgwSetup();
        if (StringUtils.isNotEmpty(apimDeploymentTomlPath)) {
            Utils.copyFile(apimDeploymentTomlPath,
                    ccTempPath + TestConstant.DOCKER_COMPOSE_CC_WITH_APIM_DIR
                            + TestConstant.DEPLYMNT_TOML_PATH);
        }
        if (StringUtils.isNotEmpty(ccConfPath)) {
            Utils.copyFile(ccConfPath,
                    ccTempPath + TestConstant.DOCKER_COMPOSE_CC_WITH_APIM_DIR
                            + TestConstant.CONFIG_TOML_PATH);
        }
        String dockerComposePath = ccTempPath + TestConstant.DOCKER_COMPOSE_CC_WITH_APIM_DIR
                        + TestConstant.DOCKER_COMPOSE_YAML_PATH;
        // add mock backend service to the docker-compose.yaml file
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerComposePath, tlsEnabled);

        Logger apimLogger = LoggerFactory.getLogger("APIM");
        Slf4jLogConsumer apimLogConsumer = new Slf4jLogConsumer(apimLogger);
        environment = new DockerComposeContainer(new File(dockerComposePath)).withLocalCompose(true)
                .withLogConsumer("apim", apimLogConsumer)
                .waitingFor(TestConstant.APIM_SERVICE_NAME_IN_DOCKER_COMPOSE,
                        Wait.forLogMessage(".*/apim:9443/carbon/", 1));
        addCcLoggersToEnv();
    }
}
