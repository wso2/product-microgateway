/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.context;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class MgwServerImpl implements MgwServer {

    private static final Logger log = LoggerFactory.getLogger(MgwServerImpl.class);
    DockerComposeContainer environment;

    String targetDir;
    String mgwTmpServerPath;
    String mgwServerPath;

    MgwServerImpl() throws IOException {
        targetDir = Utils.getTargetDirPath();
        mgwTmpServerPath = targetDir + File.separator + "server-tmp";

        final Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("project.properties"));
        mgwServerPath = targetDir + File.separator + "micro-gwtmp" + File.separator + "choreo-connect-" + properties
                .getProperty("version");
    }

    @Override
    public void startMGW() throws IOException {
        try {
            environment.start();
        } catch (Exception e) {
            log.error("Error occurred when docker-compose up: {}", e.getMessage());
        }
        Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(150, TimeUnit.SECONDS).until(isBackendAvailable());
        if (!checkForBackendAvailability()) {
            log.error("MockBackend is not started");
        }
    }

    @Override
    public void stopMGW() {
        environment.stop();
    }

    /**
     * wait till the mock backend is available.
     *
     * @return a Callable that checks if the backend is available
     */
    private Callable<Boolean> isBackendAvailable() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return checkForBackendAvailability();
            }
        };
    }

    /**
     * Check if the backend is available.
     *
     * @return true if the backend is available and returns 200-OK, false otherwise
     * @throws IOException if an error occurs when invoking the backend URL
     */
    private Boolean checkForBackendAvailability() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockServiceURLHttp(
                "/v2/pet/3"), headers);
        return response != null && response.getResponseCode() == HttpStatus.SC_OK;
    }

    /**
     * This will create a separate mgw setup in the target directory to execute the tests.
     *
     * @param customJwtTransformerEnabled - whether the custom JWT transformer is enabled or not
     * @throws MicroGWTestException if an error occurs while file copy operation
     */
    void createTmpMgwSetup(boolean customJwtTransformerEnabled) throws MicroGWTestException {
        Utils.copyDirectory(mgwServerPath, mgwTmpServerPath);
        if (customJwtTransformerEnabled) {
            Utils.copyFile(System.getProperty("jwt_transformer_jar"),
                    targetDir + File.separator + "server-tmp" + File.separator + "docker-compose" + File.separator
                            + "resources" + File.separator + "enforcer" + File.separator + "dropins" + File.separator
                            + "jwt-transformer.jar");
        }
    }
}
