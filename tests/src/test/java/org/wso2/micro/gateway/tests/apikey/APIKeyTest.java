package org.wso2.micro.gateway.tests.apikey;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class APIKeyTest extends BaseTestCase {

    private String token;
    private String label = "apimTestLabel";
    private String project = "apimTestProject";

    @BeforeClass
    public void start() throws Exception {
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PizzaShackAPI");
        api.setContext("/pizzashack");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setSandEndpoint(getMockServiceURLHttp("/echo/sand"));
        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);
        String configPath = "confs/api-key.conf";
        super.init(label, project, configPath);
    }

    @Test(description = "Test to check jwt token is issued successfully")
    private void getTokenTest() throws Exception {

        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        Map<String, String> headers = new HashMap<>();
        //get token from token endpoint
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + basicAuthToken);
        HttpResponse response = HttpClientRequest
                .doGet("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT + "/apikey", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        token = response.getData();
    }

    @Test(description = "Test to check the issued token is a valid apikey", dependsOnMethods = "getTokenTest")
    private void invokeNoAPIKeySchemeApiWithAPIKeyTest() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("apikey", token);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

//    @Test(description = "Server restart", dependsOnMethods = "invokeApiWithAPIKeyTest" )
//    private void restartServer() throws Exception {
//        microGWServer.stopServer(true);
//        mockHttpServer.stopIt();
//        String withoutUserConfigPath = "confs/base.conf";
//        super.init(label, project, withoutUserConfigPath);
//    }


    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
