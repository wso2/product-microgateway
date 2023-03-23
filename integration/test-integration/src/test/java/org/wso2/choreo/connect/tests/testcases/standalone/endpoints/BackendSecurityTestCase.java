package org.wso2.choreo.connect.tests.testcases.standalone.endpoints;

import com.google.common.net.HttpHeaders;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.*;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class BackendSecurityTestCase {
    private String jwtTokenProd;
    private static final String API_CONTEXT = "backend-security";

    @BeforeClass(description = "Get Prod and Sandbox tokens")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "test production endpoint extension security configs")
    public void testBasicAuthBackendExtension() throws CCTestException, MalformedURLException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProd);
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/echo");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpoint);

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey("authorization"), "Backend did not receive auth header");
        Assert.assertEquals(respHeaders.get("authorization"), "Basic YWRtaW46YWRtaW4=",
                "backend basic auth header is incorrect");
    }
}
