/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.filter;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class CustomFilterTestCase {
    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "write:pets", false);
        String certificatesTrustStorePath = HttpsClientRequest.class.getClassLoader()
                .getResource("keystore/client-truststore.jks").getPath();
        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);
    }

    @Test(description = "Test add header via custom filters")
    public void testAddHeaderViaFilter() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/headers"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertNotNull(responseJSON.get("Custom-header-1"), "Header is not attached from the custom filter.");
        String customHeaderValue = responseJSON.get("Custom-header-1").toString();
        Assert.assertEquals(customHeaderValue, "Foo", "mismatch against the custom header attached " +
                "from the filter");
    }

    @Test(description = "Test add header via custom filters based on path parameters")
    public void testPathParameterReadWithinFilter() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/headers/23.api"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertNotNull(responseJSON.get("Headerid"), "Header is not attached from the custom filter.");
        String customHeaderValue = responseJSON.get("Headerid").toString();
        Assert.assertEquals(customHeaderValue, "23", "mismatch against the custom header attached " +
                "from the filter");
    }

    @Test(description = "Test add header via custom filters based on query parameters")
    public void testQueryParameterReadWithinFilter() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/headers?queryParam=abc"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertNotNull(responseJSON.get("Queryparam"), "Header is not attached from the custom filter.");
        String customHeaderValue = responseJSON.get("Queryparam").toString();
        Assert.assertEquals(customHeaderValue, "abc", "mismatch against the custom header attached " +
                "from the filter");
    }

    @Test(description = "Test remove header via custom filters")
    public void testRemoveHeadersWithinFilter() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("Custom-remove-header", "Bar");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/headers"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertFalse(responseJSON.has("custom-remove-header"), "Header is not removed from the custom filter.");
    }

    @Test(description = "Test custom configuration properties")
    public void testFilterConfigProperties() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/headers"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        JSONObject responseJSON = new JSONObject(response.getData());
        Assert.assertNotNull(responseJSON.get("Fookey"), "Header is not attached from the custom filter based on " +
                "custom config properties.");
        String customHeaderValue = responseJSON.get("Fookey").toString();
        Assert.assertEquals(customHeaderValue, "fooVal", "mismatch against the custom header attached " +
                "from the filter base on custom config properties");
    }

    @Test(description = "Test dynamic endpoints for custom filter")
    public void testDynamicEndpointWithinFilter() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // template: <organizationID>_<EndpointName>_xwso2cluster_<vHost>_<API name><API version>
        headers.put("Custom-dynamic-endpoint", "carbon.super_myDynamicEndpoint_xwso2cluster_localhost_SwaggerPetstore1.0.5");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("/v2/standard/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.API_SANDBOX_RESPONSE, "Response body mismatched for dynamic endpoint");
    }
}
