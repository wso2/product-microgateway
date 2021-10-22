package org.wso2.choreo.connect.tests.testCases.standalone.interceptor;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorBaseTestCase {
    static final String INVOCATION_CONTEXT = "invocationContext";
    final String basePath = "/intercept-request";

    String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void setup() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @BeforeMethod(description = "clear the status of interceptor management service")
    void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    String getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return response.getData();
    }

    void setResponseOfInterceptor(String responseBody, boolean isRequestFlow) throws Exception {
        String servicePath = isRequestFlow ? "interceptor/request" : "interceptor/response";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        HttpResponse response = HttpClientRequest.doPost(Utils.getMockInterceptorManagerHttp(servicePath),
                responseBody, headers);
        Assert.assertNotNull(response, "Invalid response when updating response body of interceptor");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    void testInterceptorHandler(String actualHandler, InterceptorConstants.Handler expectedHandler) {
        Assert.assertEquals(actualHandler, expectedHandler.toString(), "Invalid interceptor handler");
    }

    void testInvocationContext(JSONObject bodyJSON, List<String> supportedMethods, String method, String path, String pathTemplate) {
        JSONObject invocationCtx = bodyJSON.getJSONObject(INVOCATION_CONTEXT);
        Assert.assertNotNull(invocationCtx, "Interceptor invocation context not found");

        Assert.assertEquals(invocationCtx.getString("apiName"), "SwaggerPetstoreRequestIntercept", "API name mismatch");
        Assert.assertEquals(invocationCtx.getString("apiVersion"), "1.0.5", "API version mismatch");
        Assert.assertEquals(invocationCtx.getString("scheme"), "https", "Scheme mismatch");
        Assert.assertEquals(invocationCtx.getString("vhost"), "localhost", "Vhost mismatch");
        Assert.assertEquals(invocationCtx.getString("method"), method, "HTTP method mismatch");
        Assert.assertEquals(invocationCtx.getString("path"), path, "Resource path mismatch");
        Assert.assertEquals(invocationCtx.getString("pathTemplate"), pathTemplate, "Resource path template mismatch");
        Assert.assertEquals(invocationCtx.getString("basePath"), basePath, "Base path mismatch");

        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("source")), "Source not found");
        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("requestId")), "Request ID not found");
        Assert.assertEquals(Arrays.asList(invocationCtx.getString("supportedMethods").split(" ")), supportedMethods, "HTTP supported method mismatch");
    }

    void testInterceptorHeaders(JSONObject bodyJSON, Map<String, String> expectedHeaders, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestHeaders" : "responseHeaders";
        JSONObject headersJSON = bodyJSON.getJSONObject(jsonKey);
        expectedHeaders.forEach((key, value) -> {
            String actualVal = headersJSON.getString(key);
            Assert.assertEquals(actualVal, value, String.format("Header mismatch for header key: %s", key));
        });
    }

    void testInterceptorBody(JSONObject bodyJSON, String expectedBody, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestBody" : "responseBody";
        String base64EncodedBody = Base64.getEncoder().encodeToString(expectedBody.getBytes());
        Assert.assertEquals(bodyJSON.getString(jsonKey), base64EncodedBody, "Request body mismatch");
    }
}
