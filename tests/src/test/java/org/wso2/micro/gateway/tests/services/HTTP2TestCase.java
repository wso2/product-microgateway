/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.micro.gateway.tests.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.*;
import org.wso2.micro.gateway.tests.common.*;
import org.wso2.micro.gateway.tests.common.HTTP2Server.MockHttp2Server;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HTTP2Client.Http2ClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTPS_PORT;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTP_PORT;

public class HTTP2TestCase extends BaseTestCase {

    protected final static int MOCK_HTTP2_SERVER_PORT = 8443;
    protected final static int MOCK_HTTP2_SECURE_SERVER_PORT = 8080;
    private static final Log log = LogFactory.getLog(HTTP2TestCase.class);
    protected MockHttp2Server mockHttp2Server;
    protected MockHttp2Server mockHttp2SecureServer;
    protected Http2ClientRequest http2ClientRequest;
    private String jwtTokenProd;

    @BeforeClass
    private void setup() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        APIDTO api = new APIDTO();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");

        //set production ep
        List<API_endpointDTO> api_endpointDTOS = new ArrayList<>();
        API_endpointDTO prod = new API_endpointDTO();
        prod.setType("Production");
        EndPointDTO endPointDTO_prod = new EndPointDTO();
        endPointDTO_prod.setName("Ep1");
        endPointDTO_prod.setType("http");
        EndPoint_endpointSecurityDTO endPoint_endpointSecurityDTO_prod = new EndPoint_endpointSecurityDTO();
        endPoint_endpointSecurityDTO_prod.setEnabled(false);
        endPointDTO_prod.setEndpointSecurity(endPoint_endpointSecurityDTO_prod);
        EndPoint_endpointConfigDTO endPoint_endpointConfigDTO_prod = new EndPoint_endpointConfigDTO();
        endPoint_endpointConfigDTO_prod.setEndpointType(EndPoint_endpointConfigDTO.EndpointTypeEnum.SINGLE);
        List<EndpointConfigDTO> api_endpointConfigDTOS_prod = new ArrayList<>();
        EndpointConfigDTO endpointConfigDTO_prod = new EndpointConfigDTO();
        endpointConfigDTO_prod.setUrl("http://localhost:8443");
        endpointConfigDTO_prod.setTimeout("1000");
        api_endpointConfigDTOS_prod.add(endpointConfigDTO_prod);
        endPoint_endpointConfigDTO_prod.setList(api_endpointConfigDTOS_prod);
        endPointDTO_prod.setEndpointConfig(endPoint_endpointConfigDTO_prod);
        prod.setInline(endPointDTO_prod);

        //set sand ep
        API_endpointDTO sand = new API_endpointDTO();
        sand.setType("Sandbox");
        EndPointDTO endPointDTO_sand = new EndPointDTO();
        endPointDTO_sand.setName("Ep2");
        endPointDTO_sand.setType("http");
        EndPoint_endpointSecurityDTO endPoint_endpointSecurityDTO_sand = new EndPoint_endpointSecurityDTO();
        endPoint_endpointSecurityDTO_sand.setEnabled(false);
        endPointDTO_sand.setEndpointSecurity(endPoint_endpointSecurityDTO_sand);
        EndPoint_endpointConfigDTO endPoint_endpointConfigDTO_sand = new EndPoint_endpointConfigDTO();
        endPoint_endpointConfigDTO_sand.setEndpointType(EndPoint_endpointConfigDTO.EndpointTypeEnum.SINGLE);
        List<EndpointConfigDTO> api_endpointConfigDTOS_sand = new ArrayList<>();
        EndpointConfigDTO endpointConfigDTO_sand = new EndpointConfigDTO();
        endpointConfigDTO_sand.setUrl("http://localhost:8443");
        endpointConfigDTO_sand.setTimeout("1000");
        api_endpointConfigDTOS_sand.add(endpointConfigDTO_sand);
        endPoint_endpointConfigDTO_sand.setList(api_endpointConfigDTOS_sand);
        endPointDTO_sand.setEndpointConfig(endPoint_endpointConfigDTO_sand);
        sand.setInline(endPointDTO_sand);

        api_endpointDTOS.add(0, prod);
        api_endpointDTOS.add(1, sand);
        api.setEndpoint(api_endpointDTOS);

        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);
        //set security schemas
        String security = "oauth2";

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        //Register a production token with key validation info
        KeyValidationInfo info = new KeyValidationInfo();
        info.setApi(api);
        info.setApplication(application);
        info.setAuthorized(true);
        info.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info.setSubscriptionTier("Unlimited");

        CLIExecutor cliExecutor;

        microGWServer = ServerInstance.initMicroGwServer();
        String cliHome = microGWServer.getServerHome();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();

        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project, security);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "http2-test.conf").getPath();
        String[] args = {"--config", configPath};
        microGWServer.startMicroGwServer(balPath, args);

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
    }


    @Test(description = "Test API invocation with an HTTP/2.0 request via insecure connection")
    public void testHTTP2ForInsecureConnection() throws Exception {

        //http2 server is started with ssl disabled
        boolean isOpen = Utils.isPortOpen(MOCK_HTTP2_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_HTTP2_SERVER_PORT + " already in use.");
        mockHttp2Server = new MockHttp2Server(MOCK_HTTP2_SERVER_PORT, false);
        mockHttp2Server.start();

        //http2 client is initialized with ssl disabled
        http2ClientRequest = new Http2ClientRequest(false, GATEWAY_LISTENER_HTTP_PORT, jwtTokenProd);
        http2ClientRequest.start();


    }

    @Test(description = "Test API invocation with an HTTP/2.0 request via secure connection")
    public void testHTTP2ForSecureConnection() throws Exception {

        //http2 server is started with ssl enabled
        boolean isOpen = Utils.isPortOpen(MOCK_HTTP2_SECURE_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_HTTP2_SECURE_SERVER_PORT + " already in use.");
        mockHttp2SecureServer = new MockHttp2Server(MOCK_HTTP2_SECURE_SERVER_PORT, true);
        mockHttp2SecureServer.start();

        //http2 client is initialized with ssl enabled
        http2ClientRequest = new Http2ClientRequest(true, GATEWAY_LISTENER_HTTPS_PORT, jwtTokenProd);
        http2ClientRequest.start();

    }


    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();

    }
}