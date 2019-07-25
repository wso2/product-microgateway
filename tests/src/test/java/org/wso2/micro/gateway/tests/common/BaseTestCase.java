/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.common.model.SubscribedApiDTO;
import org.wso2.micro.gateway.tests.context.MicroGWTestException;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;

/**
 * Base test class for CLI based tests.
 */
public class BaseTestCase {
    protected ServerInstance microGWServer;
    protected MockHttpServer mockHttpServer;
    protected final static int MOCK_SERVER_PORT = 9443;

    private void initHttpServer() {
        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
    }

    /**
     * Initialize and Start the microgateway server.
     *
     * @param configFilePath the relative path of config file (stored inside resources directory) if the default config
     *                       file needs to be overwritten. (use forward slash to mention the path)
     * @param balPath        the absolute path of the compiled ballerina project executable(balx file).
     * @param args           commandline arguments.
     * @throws MicroGWTestException
     */
    private void initAndStartMicroGWServer(String configFilePath, String balPath, String[] args)
            throws MicroGWTestException {
        String configPath;
        if (configFilePath == null) {
            configPath = Objects.requireNonNull(getClass().getClassLoader()
                    .getResource("confs/default-test-config.conf")).getPath();
        } else {
            configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(configFilePath)).getPath();
        }
        microGWServer = ServerInstance.initMicroGwServer(configPath);
        if (args == null) {
            microGWServer.startMicroGwServer(balPath);
        } else {
            microGWServer.startMicroGwServer(balPath, args);
        }
    }

    /**
     * Initialize the project by importing APIs from the API Manager publisher.
     *
     * @param label          label
     * @param project        project name
     * @param args           additional commandline arguments
     * @param configFilePath relative path of the config file (use forward slash to mention the path)
     * @throws Exception
     */
    protected void init(String label, String project, String[] args, String configFilePath)
            throws Exception {
        initHttpServer();
        CLIExecutor cliExecutor = CLIExecutor.getInstance();
        cliExecutor.generate(label, project);
        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        initAndStartMicroGWServer(configFilePath, balPath, args);
    }

    /**
     * Initialize the project by importing APIs from the API Manager publisher.
     *
     * @param label          label
     * @param project        project name
     * @param configFilePath relative path of the config file (use forward slash to mention the path)
     * @throws Exception
     */
    protected void init(String label, String project, String configFilePath) throws Exception {
        init(label, project, null, configFilePath);
    }

    /**
     * Initialize the project by importing APIs from the API Manager publisher.
     *
     * @param label   label
     * @param project project name
     * @throws Exception
     */
    protected void init(String label, String project) throws Exception {
        init(label, project, null, null);
    }

    /**
     * Initialize the project using developer first approach (Using openAPI definitions).
     *
     * @param project          project name
     * @param openAPIFileNames relative paths of the openAPI definitions stored inside resources directory.
     *                         (use forward slash to mention the path)
     * @param args             additional commandline arguments
     * @param configFilePath   relative path of the config file (use forward slash to mention the path)
     * @throws Exception
     */
    protected void init(String project, String[] openAPIFileNames, String[] args, String configFilePath)
            throws Exception {
        initHttpServer();
        CLIExecutor cliExecutor = CLIExecutor.getInstance();
        cliExecutor.generateFromDefinition(project, openAPIFileNames);
        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        initAndStartMicroGWServer(configFilePath, balPath, args);
    }

    /**
     * Initialize the project using developer first approach (Using openAPI definitions).
     *
     * @param project          project name
     * @param openAPIFileNames relative paths of the openAPI definitions stored inside resources directory.
     *                         (use forward slash to mention the path)
     * @param args             additional commandline arguments
     * @throws Exception
     */
    protected void init(String project, String[] openAPIFileNames, String[] args) throws Exception {
        init(project, openAPIFileNames, args, null);
    }

    /**
     * Initialize the project using developer first approach (Using openAPI definitions).
     *
     * @param project          project name
     * @param openAPIFileNames relative paths of the openAPI definitions stored inside resources directory.
     *                         (use forward slash to mention the path)
     * @throws MicroGWTestException
     */
    protected void init(String project, String[] openAPIFileNames) throws Exception {
        init(project, openAPIFileNames, null, null);
    }

    /**
     * Stop HTTP server, stop microgateway server and clear the API publisher.
     *
     * @throws Exception if exception is occurred while stopping the microgateway server.
     */
    public void finalize() throws Exception {
        mockHttpServer.stopIt();
        microGWServer.stopServer(true);
        MockAPIPublisher.getInstance().clear();
    }

    protected String getJWT(API api, ApplicationDTO applicationDTO, String tier, String keyType, int validityPeriod)
            throws Exception {
        SubscribedApiDTO subscribedApiDTO = new SubscribedApiDTO();
        subscribedApiDTO.setContext(api.getContext() + "/" + api.getVersion());
        subscribedApiDTO.setName(api.getName());
        subscribedApiDTO.setVersion(api.getVersion());
        subscribedApiDTO.setPublisher("admin");

        subscribedApiDTO.setSubscriptionTier(tier);
        subscribedApiDTO.setSubscriberTenantDomain("carbon.super");

        JSONObject jwtTokenInfo = new JSONObject();
        jwtTokenInfo.put("subscribedAPIs", new JSONArray(Arrays.asList(subscribedApiDTO)));
        return TokenUtil.getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod);
    }

    protected String getServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + TestConstant.GATEWAY_LISTENER_HTTP_PORT), servicePath)
                .toString();
    }

    protected String getMockServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + MOCK_SERVER_PORT), servicePath).toString();
    }
}
