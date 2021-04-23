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
        this(null, confPath, tlsEnabled, false);
    }

    /**
     * initialize a docker environment using docker compose with test-integration jar copied with.
     *
     * @param apimConfPath      - external APIM deployment.toml path
     * @param ccConfPath        - external conf.toml path
     * @param tlsEnabled        - if the backend needs to have the tls enabled server additionally
     * @param includeCustomImpl - if the test-integration jar needs to be included
     * @throws IOException          if something goes wrong while adding the mock backend to the docker-compose.yaml
     * @throws MicroGWTestException if something goes wrong while copying server configs
     */
    public APIManagerWithMgwServerInstance(String apimConfPath, String ccConfPath, boolean tlsEnabled, boolean includeCustomImpl)
            throws MicroGWTestException,
            IOException {
        createTmpMgwSetup(includeCustomImpl);
        if (StringUtils.isNotEmpty(apimConfPath)) {
            Utils.copyFile(apimConfPath,
                    mgwTmpServerPath + File.separator + "docker-compose" + File.separator + "choreo-connect-with-apim"
                            + File.separator + "conf" + File.separator + "deployment.toml");
        }
        if (StringUtils.isNotEmpty(ccConfPath)) {
            Utils.copyFile(ccConfPath,
                    mgwTmpServerPath + File.separator + "docker-compose" + File.separator + "choreo-connect-with-apim"
                            + File.separator + "conf" + File.separator + "config.toml");
        }
        Logger enforcerLogger = LoggerFactory.getLogger("Enforcer");
        Logger adapterLogger = LoggerFactory.getLogger("Adapter");
        Logger routerLogger = LoggerFactory.getLogger("Router");
        Logger apimLogger = LoggerFactory.getLogger("APIM");
        Slf4jLogConsumer enforcerLogConsumer = new Slf4jLogConsumer(enforcerLogger);
        Slf4jLogConsumer adapterLogConsumer = new Slf4jLogConsumer(adapterLogger);
        Slf4jLogConsumer routerLogConsumer = new Slf4jLogConsumer(routerLogger);
        Slf4jLogConsumer apimLogConsumer = new Slf4jLogConsumer(apimLogger);
        String dockerComposePath =
                mgwTmpServerPath + File.separator + "docker-compose" + File.separator + "choreo-connect-with-apim"
                        + File.separator + "docker-compose.yaml";
        // add mock backend service to the docker-compose.yaml file
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerComposePath, tlsEnabled);

        environment = new DockerComposeContainer(new File(dockerComposePath)).withLocalCompose(true)
                .withLogConsumer("enforcer", enforcerLogConsumer)
                .withLogConsumer("adapter", adapterLogConsumer)
                .withLogConsumer("router", routerLogConsumer)
                .withLogConsumer("apim", apimLogConsumer)
                .waitingFor(TestConstant.APIM_SERVICE_NAME_IN_DOCKER_COMPOSE,
                        Wait.forLogMessage(".*/apim:9443/carbon/", 1));

        if (Boolean.parseBoolean(System.getenv(MgwServerInstance.ENFORCER_DEBUG_ENV))) {
            environment.withEnv("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006");
        }

    }
}
