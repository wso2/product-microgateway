/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.micro.gateway.tests.http2;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.HttpConversionUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.HTTP2Server.MockHttp2Server;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HTTP2Client.Http2ClientRequest;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTPS_PORT;
import static org.wso2.micro.gateway.tests.util.TestConstant.GATEWAY_LISTENER_HTTP_PORT;

public class HTTP2RequestsWithHTTP2BackEndTestCase extends BaseTestCase {

    protected final static int MOCK_HTTP2_SERVER_PORT = 8443;
    private static final Log log = LogFactory.getLog(HTTP2RequestsWithHTTP2BackEndTestCase.class);
    protected MockHttp2Server mockHttp2Server;
    protected Http2ClientRequest http2ClientRequest;
    private String jwtTokenProd;

    @BeforeClass
    private void setup() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setEndpoint("https://localhost:8443");
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

        String configPath = getClass().getClassLoader()
                .getResource("confs" + File.separator + "http2-test.conf").getPath();
        microGWServer = ServerInstance.initMicroGwServer(configPath);
        String cliHome = microGWServer.getToolkitDir();

        boolean isOpen = Utils.isPortOpen(MOCK_SERVER_PORT);
        Assert.assertFalse(isOpen, "Port: " + MOCK_SERVER_PORT + " already in use.");
        mockHttpServer = new MockHttpServer(MOCK_SERVER_PORT);
        mockHttpServer.start();

        cliExecutor = CLIExecutor.getInstance();
        cliExecutor.setCliHome(cliHome);
        cliExecutor.generate(label, project);

        String balPath = CLIExecutor.getInstance().getLabelBalx(project);
        microGWServer.startMicroGwServer(balPath);

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);

        boolean isOpen2 = Utils.isPortOpen(MOCK_HTTP2_SERVER_PORT);
        Assert.assertFalse(isOpen2, "Port: " + MOCK_HTTP2_SERVER_PORT + " already in use.");
        mockHttp2Server = new MockHttp2Server(MOCK_HTTP2_SERVER_PORT, true);
        mockHttp2Server.start();
    }

    @Test(description = "Test API invocation with an HTTP/2.0 request via insecure connection sending to HTTP/2.0 BE")
    public void testHTTP2RequestsViaInsecureConnectionWithHTTP2BE() throws Exception {
        http2ClientRequest = new Http2ClientRequest(false, GATEWAY_LISTENER_HTTP_PORT, jwtTokenProd);
        http2ClientRequest.start();
    }

    @Test(description = "Test API invocation with an HTTP/2.0 request via secure connection sending to HTTP/2.0 BE")
    public void testHTTP2RequestsViaSecureConnectionWithHTTP2BE() throws Exception {
        http2ClientRequest = new Http2ClientRequest(true, GATEWAY_LISTENER_HTTPS_PORT, jwtTokenProd);
        http2ClientRequest.start();
    }

    @Test(description = "Test API invocation with an HTTP/1.1 request via insecure connection sending to HTTP/2.0 BE")
    public void testHTTP1RequestsViaInsecureConnectionWithHTTP2BE() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.HOST.toString(), "127.0.0.1:9590");
        headers.put(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text().toString(), HttpScheme.HTTP.toString());
        headers.put(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.GZIP.toString());
        headers.put(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.DEFLATE.toString());
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        log.info("Response: " + response.getResponseMessage() + " , " + response.getResponseCode());
    }

    @Test(description = "Test API invocation with an HTTP/1.1 request via secure connection sending to HTTP/2.0 BE")
    public void testHTTP1RequestsViaSecureConnectionWithHTTP2BE() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.HOST.toString(), "127.0.0.1:9595");
        headers.put(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text().toString(), HttpScheme.HTTP.toString());
        headers.put(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.GZIP.toString());
        headers.put(HttpHeaderNames.ACCEPT_ENCODING.toString(), HttpHeaderValues.DEFLATE.toString());
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        log.info("Response: " + response.getResponseMessage() + " , " + response.getResponseCode());
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
