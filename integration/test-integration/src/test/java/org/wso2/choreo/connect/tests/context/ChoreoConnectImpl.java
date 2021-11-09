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
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


/**
 * Implementation class to be extended by the Choreo Connect instance
 */
public abstract class ChoreoConnectImpl implements ChoreoConnect {

    static final String ENFORCER_DEBUG_ENV = "ENFORCER_DEBUG";
    private static final Logger log = LoggerFactory.getLogger(ChoreoConnectImpl.class);
    DockerComposeContainer environment;

    String ccTempPath;
    String ccExtractedPath;

    ChoreoConnectImpl() throws IOException {
        final Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("project.properties"));

        ccExtractedPath = Utils.getTargetDirPath() + File.separator + "choreo-connect-" + properties.getProperty("version");
        ccTempPath = Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH;
    }

    public void start() throws CCTestException {
        try {
            environment.start();
        } catch (Exception e) {
            throw new CCTestException("Error occurred when Choreo Connect docker-compose up: {}", e);
        }
        Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(150, TimeUnit.SECONDS)
                .until(isBackendAvailable());
    }

    public void stop() {
        environment.stop();
    }

    /**
     * Check if the Choreo Connect instance is healthy
     *
     * @return a Callable that checks if the CC instance is healthy
     */
    public Callable<Boolean> isHealthy() throws IOException {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return checkCCInstanceHealth();
            }
        };
    }

    public static Boolean checkCCInstanceHealth() throws IOException {
        Map<String, String> headers = new HashMap<>(0);
        try {
            HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttp(
                    "/health"), headers);
            return response != null && response.getResponseCode() == HttpStatus.SC_OK;
        } catch (ConnectException e) {
            return false;
        }
    }

    /**
     * wait till the mock backend is available.
     *
     * @return a Callable that checks if the backend is available
     */
    private Callable<Boolean> isBackendAvailable() {
        return this::checkForBackendAvailability;
    }

    /**
     * Check if the backend is available.
     *
     * @return true if the backend is available and returns 200-OK, false otherwise
     * @throws IOException if an error occurs when invoking the backend URL
     */
    private Boolean checkForBackendAvailability() throws IOException {
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockServiceURLHttp(
                "/v2/pet/3"), headers);
        return response != null && response.getResponseCode() == HttpStatus.SC_OK;
    }

    /**
     * This will create a separate mgw setup in the target directory to execute the tests.
     *
     * @throws CCTestException if an error occurs while file copy operation
     */
    void createTmpMgwSetup() throws CCTestException {
        Utils.copyDirectory(ccExtractedPath, ccTempPath);
    }

    public void addCcLoggersToEnv() {
        Logger enforcerLogger = LoggerFactory.getLogger("Enforcer");
        Logger adapterLogger = LoggerFactory.getLogger("Adapter");
        Logger routerLogger = LoggerFactory.getLogger("Router");
        Logger mockBackendLogger = LoggerFactory.getLogger("MockBackend");
        Slf4jLogConsumer enforcerLogConsumer = new Slf4jLogConsumer(enforcerLogger);
        Slf4jLogConsumer adapterLogConsumer = new Slf4jLogConsumer(adapterLogger);
        Slf4jLogConsumer routerLogConsumer = new Slf4jLogConsumer(routerLogger);
        Slf4jLogConsumer mockBackendLogConsumer = new Slf4jLogConsumer(mockBackendLogger);
        environment.withLogConsumer("enforcer", enforcerLogConsumer)
                .withLogConsumer("adapter", adapterLogConsumer)
                .withLogConsumer("router", routerLogConsumer)
                .withLogConsumer("mockBackend", mockBackendLogConsumer);
        if (Boolean.parseBoolean(System.getenv(ENFORCER_DEBUG_ENV))) {
            environment.withEnv("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006");
        }
    }

    public static void addCustomJwtTransformer() throws CCTestException {
        Utils.copyFile(System.getProperty("jwt_transformer_jar"),
                Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH + TestConstant.DROPINS_FOLDER_PATH
                        + File.separator + "jwt-transformer.jar");
    }
}
