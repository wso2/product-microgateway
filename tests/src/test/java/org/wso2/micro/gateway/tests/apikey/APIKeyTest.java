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

    /**
     * Method to start the mock ETCD server
     */
    @Test(description = "Test to check jwt token is issued successfully")
    private void getTokenTest() throws Exception {

        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        Map<String, String> headers = new HashMap<>();
        //get token from token endpoint
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + basicAuthToken);
        HttpResponse response = HttpClientRequest
                .doGet("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_TOKEN_PORT + "/token", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        token = response.getData();
    }

    @Test(description = "Test to check the issued token is a valid jwt token", dependsOnMethods = "getTokenTest")
    private void invokeApiWithTokenTest() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to check the issued token is a valid jwt token", dependsOnMethods = "getTokenTest")
    private void invokeApiWithAPIKeyTest() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("apikey", token);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Server restart", dependsOnMethods = {"invokeApiWithAPIKeyTest","invokeApiWithTokenTest"} )
    private void restartServer() throws Exception {
        microGWServer.stopServer(true);
        mockHttpServer.stopIt();
        String withoutUserConfigPath = "confs/base.conf";
        super.init(label, project, withoutUserConfigPath);
    }

    @Test(description = "Test to check api key is validating the user", dependsOnMethods = "restartServer")
    private void invokeApiWithUnauthenticatedTokenTest() throws Exception {      
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check api key is validating the user", dependsOnMethods = "restartServer")
    private void invokeApiWithUnauthenticatedAPIKeyTest() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("apikey", token);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("/pizzashack/1.0.0/menu"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
