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
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MockApiTestCase extends ApimBaseTest {

    @BeforeClass
    public void createApiProject() throws IOException, CCTestException {
        ApictlUtils.createProject( "mock_endpoint_openAPI.yaml", "apictl_mock_api_test",
                null, null, null,"apictl_prototype_test.yaml");
    }

    @Test
    public void deployAPI() throws CCTestException {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("apictl_mock_api_test", "test");
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");

    }

    //    Invokes mocked API implementation using header value
    @Test(description = "Test to detect wrong API keys")
    public void invokeMockedAPIImplementation() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("mockApiValue", "success");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mock/1.0.0/testChoreoConnect", headers);
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
    public void invokeMockedAPIImplementationWithQueryParam() throws Exception {
        String headerName = "x-wso2-q-header";
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/xml");
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095/mock/1.0.0/testQueryParam?mockApiQueryVal=success", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Response header not available");
        if (response.getHeaders().containsKey(headerName)) {
            Assert.assertTrue(response.getHeaders().get(headerName).equalsIgnoreCase("Header value for query param"),
                    "Response header value mismatched");
        }
        Assert.assertTrue(response.getData().contains("<name>choreo connect</name>"), "Error response message mismatch");
    }
}
