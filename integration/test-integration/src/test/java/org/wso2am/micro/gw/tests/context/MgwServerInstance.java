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

package org.wso2am.micro.gw.tests.context;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.wso2am.micro.gw.tests.mockbackend.MockBackendServer;
import org.wso2am.micro.gw.tests.util.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Mgw server instance class.
 */
public class MgwServerInstance extends MgwServerImpl {

    private static final String ENFORCER_DEBUG_ENV = "ENFORCER_DEBUG";


    /**
     * initialize a docker environment using docker compose.
     *
     * @throws IOException
     * @throws MicroGWTestException
     */
    public MgwServerInstance() throws IOException, MicroGWTestException {
        this(null, false, false);

    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath external conf.toml path
     * @throws IOException
     * @throws MicroGWTestException
     */
    public MgwServerInstance(String confPath) throws IOException, MicroGWTestException {
        this(confPath, false, false);
    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath external conf.toml path
     * @throws IOException
     * @throws MicroGWTestException
     */
    public MgwServerInstance(String confPath, boolean tlsEnabled) throws IOException, MicroGWTestException {
        this(confPath, tlsEnabled, false);
    }

    /**
     * initialize a docker environment using docker compose.
     *
     * @param confPath   external conf.toml path
     * @param tlsEnabled if the backend needs to have the tls enabled server additionally
     * @throws IOException
     * @throws MicroGWTestException
     */
    public MgwServerInstance(String confPath, boolean tlsEnabled, boolean customJwtTransformerEnabled)
            throws IOException, MicroGWTestException {
        createTmpMgwSetup(customJwtTransformerEnabled);
        if (!StringUtils.isEmpty(confPath)) {
            Utils.copyFile(confPath, mgwTmpServerPath + File.separator + "resources" + File.separator +
                    "conf" + File.separator + "config.toml");
        }

        String dockerComposePath = mgwTmpServerPath + File.separator + "docker-compose.yaml";
        Logger enforcerLogger = LoggerFactory.getLogger("Enforcer");
        Logger adapterLogger = LoggerFactory.getLogger("Adapter");
        Logger routerLogger = LoggerFactory.getLogger("Router");
        Slf4jLogConsumer enforcerLogConsumer = new Slf4jLogConsumer(enforcerLogger);
        Slf4jLogConsumer adapterLogConsumer = new Slf4jLogConsumer(adapterLogger);
        Slf4jLogConsumer routerLogConsumer = new Slf4jLogConsumer(routerLogger);
        MockBackendServer.addMockBackendServiceToDockerCompose(dockerComposePath, tlsEnabled);
        environment = new DockerComposeContainer(new File(dockerComposePath)).withLocalCompose(true)
                .withLogConsumer("enforcer", enforcerLogConsumer).withLogConsumer("adapter", adapterLogConsumer)
                .withLogConsumer("router", routerLogConsumer);
        if (Boolean.parseBoolean(System.getenv(ENFORCER_DEBUG_ENV))) {
            environment.withEnv("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006");
        }

    }

    /**
     * This will create a separate mgw setup in the target directory to execute the tests.
     *
     * @param customJwtTransformerEnabled - whether the custom JWT transformer is enabled or not
     * @throws MicroGWTestException
     */
    public void createTmpMgwSetup(boolean customJwtTransformerEnabled) throws MicroGWTestException {
        Utils.copyDirectory(mgwServerPath, mgwTmpServerPath);
        String jarLocation = System.getProperty("jwt_transformer_jar");
        if (customJwtTransformerEnabled) {
            Utils.copyFile(jarLocation, targetDir +
                    File.separator + "server-tmp" + File.separator + "resources" + File.separator + "enforcer" +
                    File.separator + "dropins" + File.separator + "jwt-transformer.jar");
        }
    }
}
