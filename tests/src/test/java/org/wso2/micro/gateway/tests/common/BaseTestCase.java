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
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Base test class for CLI based tests
 */
public class BaseTestCase {
    protected ServerInstance microGWServer;
    protected MockHttpServer mockHttpServer;
    protected final static int MOCK_SERVER_PORT = 9443;
    private String projectPath;

    protected void init(String label, String project, String security) throws Exception {
        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        //System.setProperty(GatewayCliConstants.SYS_PROP_SECURITY, "oauth2");
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project, security);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();
        String[] args = {"--config", configPath, "--experimental"};
        microGWServer.startMicroGwServer(balPath, args);
    }

    protected void init(String project, String[] openAPIFileNames) throws Exception {
        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();
        //System.setProperty(GatewayCliConstants.SYS_PROP_SECURITY, "oauth2");
        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generateFromDefinition(project, openAPIFileNames);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "default-test-config.conf").getPath();
        String[] args = {"--config", configPath, "--experimental"};
        microGWServer.startMicroGwServer(balPath, args);
    }

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
        return TokenUtil.getBasicJWT(applicationDTO,jwtTokenInfo,keyType, validityPeriod);

    }



    protected String getServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("http://localhost:" + TestConstant.GATEWAY_LISTENER_HTTP_PORT), servicePath).toString();
    }

    protected String getMockServiceURLHttp(String servicePath) throws MalformedURLException {
        return new URL(new URL("https://localhost:" + MOCK_SERVER_PORT), servicePath).toString();
    }
}
