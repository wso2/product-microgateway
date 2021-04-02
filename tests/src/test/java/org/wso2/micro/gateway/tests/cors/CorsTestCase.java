package org.wso2.micro.gateway.tests.cors;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.util.HashMap;
import java.util.Map;

public class CorsTestCase extends BaseTestCase {
    protected String prodToken, sandToken, jwtTokenProd, jwtTokenSand, expiringJwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        String label = "apimTestLabel";
        String project = "apimTestProject";
        //get mock APIM Instance
        MockAPIPublisher pub = MockAPIPublisher.getInstance();
        API api = new API();
        api.setName("PetstoreAPI");
        api.setContext("/pet");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setSandEndpoint(getMockServiceURLHttp("/echo/sand"));
        api.setVersion("1.0.0");
        api.setProvider("admin");
        //Register API with label
        pub.addApi(label, api);

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId(2);

        //Register a production token with key validation info
        KeyValidationInfo info = new KeyValidationInfo();
        info.setApi(api);
        info.setApplication(application);
        info.setAuthorized(true);
        info.setKeyType(TestConstant.KEY_TYPE_PRODUCTION);
        info.setSubscriptionTier("Unlimited");
        info.setTokenType(TestConstant.KEY_TYPE_PRODUCTION);
        //Register a production token with key validation info
        prodToken = pub.getAndRegisterAccessToken(info);

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);

        super.init(project, new String[]{"cors/cors_sample.yaml"}, null,
                null);
    }

    @Test(description = "Test Cors headers in response")
    public void testCorsHeaders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // Add the Origin header
        headers.put(HttpHeaderNames.ORIGIN.toString(), "http://www.m3.com");
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/10"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        // Check whether cors headers are correctly received in the response.
        Assert.assertEquals(response.getHeader("access-control-allow-origin"), "http://www.m3.com", "access-control-allow-origin header value mismatched");
        Assert.assertEquals(response.getHeader("access-control-expose-headers"), "X-CUSTOM-HEADER", "access-control-expose-headers header value mismatched");
    }

    @Test(description = "Test Cors headers for an origin which is not allowed from the .yaml file")
    public void testCorsHeadersForNonAllowedOrigin() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // Add the Origin header (This origin is not allow from the cors_sample.yaml)
        headers.put(HttpHeaderNames.ORIGIN.toString(), "http://abc.com");
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/10"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        // Check whether cors headers are correctly received in the response.
        Assert.assertNull(response.getHeader("access-control-allow-origin"),"access-control-allow-origin header value mismatched");
        Assert.assertNull(response.getHeader("access-control-expose-headers"),"access-control-expose-headers header value mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
