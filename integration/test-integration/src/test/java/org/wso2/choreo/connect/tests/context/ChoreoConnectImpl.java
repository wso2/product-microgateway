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
import org.wso2.choreo.connect.tests.util.SourceControlUtils;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
     * Check if the Git Service instance is healthy
     *
     * @return a Callable that checks if the Git Service instance is healthy
     */
    public Callable<Boolean> isGitHealthy() throws IOException {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return checkGitInstanceHealth();
            }
        };
    }

    public static Boolean checkGitInstanceHealth() throws IOException {
        Map<String, String> headers = new HashMap<>(0);
        try {
            HttpResponse response = HttpClientRequest.doGet(SourceControlUtils.GIT_HEALTH_URL, headers);
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
    void createTmpMgwSetup(boolean isInitialStartup) throws CCTestException {
        log.info(">>>>>>> ############# Creating a new CC startup....");
        if (!isInitialStartup) {
            log.info(">>>>>>> ############# Creating a new CC startup after initialization....");
            File myObj = new File(System.getProperty("root_pom_path")+"/enforcer-parent/enforcer/target/coverage-aggregate-reports/aggregate.exec");
            if (myObj.delete()) {
                System.out.println("Deleted the file: " + myObj.getName());
            } else {
                System.out.println("Failed to delete the file.");
            }
            Utils.copyFile2(Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH + TestConstant.DROPINS_FOLDER_PATH + File.separator + "aggregate.exec",
                    System.getProperty("root_pom_path")+"/enforcer-parent/enforcer/target/coverage-aggregate-reports/aggregate.exec");
        }
        Utils.deleteQuietly(ccTempPath);
        Utils.copyDirectory(ccExtractedPath, ccTempPath);
        log.info(">>" + System.getProperty("root_pom_path"));
    }

    public void addCcLoggersToEnv() {
        environment.withLogConsumer("enforcer", getLogConsumer("Enforcer"))
                .withLogConsumer("adapter", getLogConsumer("Adapter"))
                .withLogConsumer("router", getLogConsumer("Router"))
                .withLogConsumer("mockBackend", getLogConsumer("MockBackend"));
        if (Boolean.parseBoolean(System.getenv(ENFORCER_DEBUG_ENV))) {
            environment.withEnv("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006");
        }
    }

    private Slf4jLogConsumer getLogConsumer(String loggerName) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        return logConsumer;
    }

    public static void addCustomJwtTransformer() throws CCTestException {
        Utils.copyFile(System.getProperty("jwt_transformer_jar"),
                Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH + TestConstant.DROPINS_FOLDER_PATH
                        + File.separator + "jwt-transformer.jar");
    }

    public static void addCodeCovExec() throws CCTestException {
        log.info(">>>>>>>> |||||||||||| "+ System.getProperty("root_pom_path")+"/enforcer-parent/enforcer/target/coverage-aggregate-reports/aggregate.exec");
        Utils.copyFile2(System.getProperty("root_pom_path")+"/enforcer-parent/enforcer/target/coverage-aggregate-reports/aggregate.exec",
                Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH + TestConstant.DROPINS_FOLDER_PATH
                        + File.separator + "aggregate.exec");
    }

    public static void addInterceptorCertToRouterTruststore() throws IOException {
        String routerTruststore = ChoreoConnectImpl.class.getClassLoader()
                .getResource("certs/" + TestConstant.CA_CERTS_FILE).getPath();
        String interceptorCert = ChoreoConnectImpl.class.getClassLoader()
                .getResource("certs/interceptor.crt").getPath();

        // Input files
        List<Path> inputs = Arrays.asList(
                Paths.get(routerTruststore),
                Paths.get(interceptorCert)
        );

        // Output file
        String newRouterTruststore = Utils.getTargetDirPath() + File.separator + TestConstant.CC_TEMP_PATH
                + TestConstant.DOCKER_COMPOSE_DIR + TestConstant.ROUTER_TRUSTSTORE_DIR
                + TestConstant.CA_CERTS_FILE;
        File fileTmp = new File(newRouterTruststore);
        fileTmp.createNewFile();
        Path newRouterTruststorePath = Paths.get(newRouterTruststore);

        // Charset for read and write
        Charset charset = StandardCharsets.UTF_8;

        // Join files (lines)
        for (Path path : inputs) {
            List<String> lines = Files.readAllLines(path, charset);
            Files.write(newRouterTruststorePath, lines, charset, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        }
    }

    public static void addCaCertToRouterTruststore() throws CCTestException {
        String certPath = ChoreoConnectImpl.class.getClassLoader()
                .getResource("certs/rootCA.crt").getPath();
        Utils.copyFile(certPath, Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH
                + TestConstant.DOCKER_COMPOSE_DIR + TestConstant.ROUTER_TRUSTSTORE_DIR + File.separator
                + "rootCA.crt");
    }

    public static void addCertsToEnforcerTruststore(String certsDir) throws CCTestException {
        String certsDirPath = ChoreoConnectImpl.class.getClassLoader()
                .getResource("certs/" + certsDir).getPath();
        Utils.copyDirectory(certsDirPath, Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH
                + TestConstant.DOCKER_COMPOSE_DIR + TestConstant.ENFORCER_TRUSTSTORE_DIR);
    }

    public static void copyVolumeMountDirToDockerContext(String mountDir) throws CCTestException {
        String volumeMountSrcDir = ChoreoConnectImpl.class.getClassLoader()
                .getResource("dockerVolumeMounts/" + mountDir).getPath();
        Utils.copyDirectory(volumeMountSrcDir, Utils.getTargetDirPath() + TestConstant.CC_TEMP_PATH
                + TestConstant.DOCKER_COMPOSE_CC_DIR + File.separator + mountDir);
    }
}
