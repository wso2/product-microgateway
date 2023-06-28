package org.wso2.choreo.connect.tests.testcases.standalone.awsLambda;

import io.netty.handler.codec.http.HttpHeaderNames;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AwsLambdaSupportTestCase {

    protected String jwtTokenProd;
    Map<String, String> headers = new HashMap<>();

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION,
                null, false);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
    }

    @Test(description = "Test invoke Aws Lambda API 1")
    public void testInvokeAwsLambda1() throws Exception {

        //test endpoint with token
        HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/awsLambda/1.0.0/"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject respJSON = new JSONObject(response.getData());
        JSONObject respHeaders = respJSON.getJSONObject("headers");
        Assert.assertTrue(respHeaders.has("Authorization"), "Missing Authorization header");
        Assert.assertTrue(respHeaders.has("X-amz-date"), "Missing X-Amz-Date header");

        String expectedPath = "/2015-03-31/functions/arn:aws:lambda:us-east-1:123456789:function:testFunc/invocations";
        String actualPath = respJSON.getString("path");

        Assert.assertEquals(actualPath,expectedPath, "Path mismatched");

    }

    @Test (description = "Test invoke Aws Lambda API 2")
    public void testInvokeAwsLambda2() throws Exception {
        //test endpoint with token
        HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/awsLambda/1.0.0/order"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject respJSON = new JSONObject(response.getData());
        JSONObject respHeaders = respJSON.getJSONObject("headers");
        Assert.assertTrue(respHeaders.has("Authorization"), "Missing Authorization header");
        Assert.assertTrue(respHeaders.has("X-amz-date"), "Missing X-Amz-Date header");

        String expectedPath = "/2015-03-31/functions/arn:aws:lambda:us-east-1:987654321:function:testOrder/invocations";
        String actualPath = respJSON.getString("path");

        Assert.assertEquals(actualPath,expectedPath, "Path mismatched");
    }
}
