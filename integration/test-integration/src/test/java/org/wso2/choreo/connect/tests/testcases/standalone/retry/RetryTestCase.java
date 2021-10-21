package org.wso2.choreo.connect.tests.testcases.standalone.retry;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;

import java.util.HashMap;
import java.util.Map;

public class RetryTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "Get Prod and Sandbox tokens")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke Production and Sandbox endpoints that returns success only after 3 retries")
    public void testAPILevelRetryForProdAndSand() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-three") , prodHeaders);
        //TODO: Change the basepath in the OpenAPI definition to /retry when the issue
        // https://github.com/wso2/product-microgateway/issues/2308 is fixed

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-three"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoints that returns success only after 4 retries")
    public void testResourceLevelRetryForProdAndSand() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-four") , prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/retry1/retry-four"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }
}
