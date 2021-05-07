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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;

/**
 * APIM with Mgw server instance class.
 */
public class ApimInstance {
    private static final Logger log = LoggerFactory.getLogger(ApimInstance.class);
    DockerComposeContainer environment;

    /**
     * initialize a docker environment using docker compose with test-integration jar copied with.
     *
     * @throws CCTestException if something goes wrong while copying server configs
     */
    public ApimInstance() throws CCTestException {
        String dockerComposePath = createApimSetup();
        Logger apimLogger = LoggerFactory.getLogger("APIM");
        Slf4jLogConsumer apimLogConsumer = new Slf4jLogConsumer(apimLogger);
        environment = new DockerComposeContainer(new File(dockerComposePath))
                .withLocalCompose(true)
                .withLogConsumer("apim", apimLogConsumer)
                .waitingFor(TestConstant.APIM_SERVICE_NAME_IN_DOCKER_COMPOSE,
                        Wait.forLogMessage(".*/apim:9443/carbon/", 1));
    }

    public void startAPIM() {
        try {
            environment.start();
        } catch (Exception e) {
            log.error("Error occurred when docker-compose up: {}", e.getMessage());
        }
    }

    public void stopAPIM() {
        environment.stop();
    }

    private String createApimSetup() throws CCTestException {
        String targetDir = Utils.getTargetDirPath();
        String testResourcesDir = targetDir + TestConstant.TEST_RESOURCES_PATH;
        String apimSetupDir = targetDir + File.separator + "apim";

        //Both files are directly given here to avoid ApimInstance being configurable.
        //This is to encourage starting API Manager only once for the complete test suit.
        String dockerComposeSource = testResourcesDir + TestConstant.TEST_DOCKER_COMPOSE_DIR
                + File.separator + "apim-in-common-network-docker-compose.yaml";
        String dockerComposeDest = apimSetupDir + TestConstant.DOCKER_COMPOSE_YAML_PATH;

        String deploymentTomlSource = testResourcesDir + TestConstant.CONFIGS_DIR
                + File.separator + "vhost-deployment.toml";
        String deploymentTomlDest = apimSetupDir + TestConstant.DEPLYMNT_TOML_PATH;

        Utils.copyFile(dockerComposeSource, dockerComposeDest);
        Utils.copyFile(deploymentTomlSource, deploymentTomlDest);

        return dockerComposeDest;
    }
}
