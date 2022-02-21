/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.mockEndpoint;


import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MockApiTestCase {

    private String testKey;

    @BeforeClass
    public void createApiProject() throws IOException, CCTestException {
        ApictlUtils.createProject("mock_endpoint_openAPI.yaml", "mock_api_test",
                null, null, null, "mock_impl_test.yaml");
    }

    @Test
    public void deployMockAPI() throws Exception {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("mock_api_test", "test");
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");

        API api = new API();
        api.setName("mockApiTest");
        api.setContext("/mockApiTest/1.0.0");
        api.setVersion("1.0.0");
        api.setProvider("admin");

        ApplicationDTO applicationDto = new ApplicationDTO();
        applicationDto.setName("jwtApp");
        applicationDto.setTier("Unlimited");
        applicationDto.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, applicationDto, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
    }

    @Test(description = "Test auth token")
    public void invokeWithoutToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test default example")
    public void getDefaultContent() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header"), "Response header not available");
        if (response.getHeaders().containsKey("x-wso2-header")) {
            Assert.assertEquals(response.getHeaders().get("x-wso2-header"), "\"Sample header value\"", "Response header value mismatched");
        }
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "application/json", "Response header value mismatched");
        }
        Assert.assertEquals(response.getData(), "{\"description\":\"default content\"}", "Error response body mismatch");
    }

    @Test(description = "Test no default example but only one example")
    public void getNoDefaultContent() throws Exception {
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testOneExample", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header"), "Response header not available");
        if (response.getHeaders().containsKey("x-wso2-header")) {
            Assert.assertEquals(response.getHeaders().get("x-wso2-header"), "\"Sample header value\"", "Response header value mismatched");
        }
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "application/json", "Response header value mismatched");
        }
        Assert.assertEquals(response.getData(), "{\"description\":\"json mediatype example1\"}", "Error response message mismatch");
    }

    @Test(description = "Test prefer and code with X")
    public void preferExample() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        headers.put("Accept", "text/*");
        headers.put("Prefer", "code=508, example=example1");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 508, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header"), "Response header not available");
        if (response.getHeaders().containsKey("x-wso2-header")) {
            Assert.assertEquals(response.getHeaders().get("x-wso2-header"), "\"Sample header value\"", "Response header value mismatched");
        }
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "text/html", "Response header value mismatched");
        }
        Assert.assertEquals(response.getData(), "{\"description\":\"content for example 1\"}", "Error response message mismatch");
    }

    @Test(description = "Test when multiple examples but preferred example is not given")
    public void preferNoExample() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        headers.put("Accept", "text/*");
        headers.put("Prefer", "code=508");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 508, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header"), "Response header not available");
        if (response.getHeaders().containsKey("x-wso2-header")) {
            Assert.assertEquals(response.getHeaders().get("x-wso2-header"), "\"Sample header value\"", "Response header value mismatched");
        }
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "text/html", "Response header value mismatched");
        }
        Assert.assertTrue(response.getData().contains("content for example"), "Error response body mismatch");
    }

    @Test(description = "Test not implemented preferred code")
    public void getNotImplementedExample() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        headers.put("Accept", "text/*");
        headers.put("Prefer", "code=508, example=example4");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_IMPLEMENTED, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "application/json", "Response header value mismatched");
        }
        Assert.assertEquals(response.getData(), "{\"error_message\":\"Not Implemented\",\"code\":\"501\",\"error_description\":\"Example preference example4 is not supported for this resource\"}", "Error response message mismatch");
    }

    @Test(description = "Test not implemented preferred code")
    public void getNotImplementedCode() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        headers.put("Prefer", "code=400");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApi", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_IMPLEMENTED, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("content-type"), "Response header not available");
        if (response.getHeaders().containsKey("content-type")) {
            Assert.assertEquals(response.getHeaders().get("content-type").toLowerCase(), "application/json", "Response header value mismatched");
        }
        Assert.assertEquals(response.getData(), "{\"error_message\":\"Not Implemented\",\"code\":\"501\",\"error_description\":\"Preferred code 400 is not supported for this resource.\"}", "Error response message mismatch");
    }
}
