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
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
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

public class MockApiTestCase extends ApimBaseTest {

    private String testKey;

    @BeforeClass
    public void createApiProject() throws IOException, CCTestException {
        ApictlUtils.createProject("mock_endpoint_openAPI.yaml", "apictl_mock_api_test",
                null, null, null, "apictl_prototype_test.yaml");
    }

    @Test
    public void deployMockAPI() throws Exception {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("apictl_mock_api_test", "test");
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

    //    Invokes mocked API implementation using header value
    @Test(description = "Test to detect wrong API keys")
    public void invokeMockedApiImplementationWithHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("mockApiValue", "success");
        headers.put("Internal-Key", testKey);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockApiWithHeader", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("x-wso2-header1"), "Response header not available");
        if (response.getHeaders().containsKey("x-wso2-header1")) {
            Assert.assertTrue(response.getHeaders().get("x-wso2-header1").equalsIgnoreCase("Sample header value"), "Response header value mismatched");
        }
        Assert.assertTrue(response.getData().contains("{\"name\" : \"choreo connect\""), "Error response message mismatch");
    }

    //    Invokes with mocked API implementation using query param
    @Test(description = "Test to detect wrong API keys")
    public void invokeMockedApiImplementationWithQueryParam() throws Exception {
        String headerName = "x-wso2-q-header";
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/xml");
        headers.put("Internal-Key", testKey);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockWithQueryParam?mockApiQueryVal=success", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Response header not available");
        if (response.getHeaders().containsKey(headerName)) {
            Assert.assertTrue(response.getHeaders().get(headerName).equalsIgnoreCase("Header value for query param"),
                    "Response header value mismatched");
        }
        Assert.assertTrue(response.getData().contains("<name>choreo connect</name>"), "Error response message mismatch");
    }

    //    Invokes with mocked default API implementation
    @Test(description = "Test to detect wrong API keys")
    public void invokeMockedDefaultApiImplementation() throws Exception {
        String headerName = "x-wso2-default-header";
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mockApiTest/1.0.0/testMockDefault", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Response header not available");
        if (response.getHeaders().containsKey(headerName)) {
            Assert.assertTrue(response.getHeaders().get(headerName).equalsIgnoreCase("Default header value"),
                    "Response header value mismatched");
        }
        Assert.assertTrue(response.getData().contains("{\"name\" : \"choreo connect\"}"), "Error response message mismatch");
    }
}
