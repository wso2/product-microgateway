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
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.*;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.LogUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * API Manager instance class.
 */
public class ApimInstance {
    private static final Logger log = LoggerFactory.getLogger(ApimInstance.class);
    private static volatile ApimInstance instance = null;
    DockerComposeContainer environment;

    /**
     * Initialize a docker compose container environment for API Manager
     *
     * @throws CCTestException if something goes wrong while copying server configs
     */
    private ApimInstance() throws CCTestException {
        String dockerComposePath = createApimSetup();
        Logger apimLogger = LoggerFactory.getLogger("APIM");
        Slf4jLogConsumer apimLogConsumer = new Slf4jLogConsumer(apimLogger);
        environment = new DockerComposeContainer(new File(dockerComposePath))
                .withLocalCompose(true)
                .withLogConsumer("apim", apimLogConsumer);
    }

    public static ApimInstance createNewInstance() throws CCTestException {
        instance = new ApimInstance();
        return instance;
    }

    public static ApimInstance getInstance() throws CCTestException {
        if (instance != null) {
            return instance;
        } else throw new CCTestException("ApimInstance not initialized");
    }

    public void startAPIM() {
        try {
            environment.start();
        } catch (Exception e) {
            log.error("Error occurred when APIM docker-compose up: {}", e.getMessage());
        }
    }

    public void restartAPIM() {
        // Currently DockerComposeContainer only provides means for "docker-compose up" and "docker-compose down"
        // via the methods start() and stop(). Therefore, the following lines accesses the container started
        // by docker-compose and does the restart using DockerClient.
        Optional<ContainerState> containerStateOptional = environment.getContainerByServiceName("apim_1");
        if (containerStateOptional.isPresent()) {
            ContainerState containerState = containerStateOptional.get();
            String containerId =  containerState.getContainerId();
            Logger apimLogger = LoggerFactory.getLogger("Restarted APIM");
            Slf4jLogConsumer apimLogConsumer = new Slf4jLogConsumer(apimLogger);
            LogUtils.followOutput(DockerClientFactory.instance().client(), containerId, apimLogConsumer);

            DockerClientFactory.instance().client().stopContainerCmd(containerId).exec();
            DockerClientFactory.instance().client().startContainerCmd(containerId).exec();
        } else {
            log.error("Unable to restart APIM container");
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
        //This is to encourage starting API Manager only once for the complete test suite.
        String dockerComposeSource = testResourcesDir + TestConstant.TEST_DOCKER_COMPOSE_DIR
                + File.separator + "apim-in-common-network-docker-compose.yaml";
        String dockerComposeDest = apimSetupDir + TestConstant.DOCKER_COMPOSE_YAML_PATH;

        String deploymentTomlSource = testResourcesDir + TestConstant.CONFIGS_DIR
                + File.separator + "vhost-deployment.toml";
        String deploymentTomlDest = apimSetupDir + TestConstant.DEPLYMNT_TOML_PATH;

        String databaseSource = testResourcesDir + TestConstant.DATABASE_DIR;
        String databaseDest = apimSetupDir + TestConstant.DATABASE_DIR;

        Utils.copyFile(dockerComposeSource, dockerComposeDest);
        Utils.copyFile(deploymentTomlSource, deploymentTomlDest);
        Utils.copyDirectory(databaseSource, databaseDest);

        return dockerComposeDest;
    }

    public static Boolean checkForAPIMServerStartup() throws IOException {
        HttpResponse response = HttpClientRequest.doGet(Utils.getAPIMServiceURLHttp("/services/Version"));
        return Objects.nonNull(response) && response.getResponseCode() == 200;
    }
}
