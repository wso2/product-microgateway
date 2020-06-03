package org.wso2.micro.gateway.tests.endpoints;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;

import java.util.HashMap;
import java.util.Map;

public class AdvanceEndpointConfigTestCase extends EndpointWithSecurityTestCase {

    private static final String BACK_END_ERROR_CODE = "101503";

    @Test(description = "Test given timeout period works for the endpoint define for the /timeout resource")
    public void testEndpointTimeout() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("advanceEp/timeout"), headers);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains(BACK_END_ERROR_CODE));
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");
    }

    @Test(description = "Test endpoint retry is returning the correct response")
    public void testEndpointRetry() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("advanceEp/retry"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBodyV1);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test endpoint circuit breaking functionality")
    public void testEndpointCircuitBreaker() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("advanceEp/circuitBreaker?cb=true"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.ERROR_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");
        //circuit opened. Should get the same error for the subsequent request. The request will not go to the backend.
        // Gateway will send the same error message even without sending the query param cb.
        response = HttpClientRequest.doGet(getServiceURLHttp("advanceEp/circuitBreaker"), headers);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains(BACK_END_ERROR_CODE));
        Assert.assertEquals(response.getResponseCode(), 500, "Response code mismatched");
        Thread.sleep(5000);
        //After circuit breaker timeouts then successful response should some from the back end
        response = HttpClientRequest
                .doGet(getServiceURLHttp("advanceEp/circuitBreaker"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBodyV1);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }
}
