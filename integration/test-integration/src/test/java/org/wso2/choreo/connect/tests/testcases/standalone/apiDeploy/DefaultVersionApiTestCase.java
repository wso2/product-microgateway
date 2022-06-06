/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test default versioned APIs.
 */
public class DefaultVersionApiTestCase {
    private String testKeyV1, testKeyV2;
    private final String basePath = "/defaultVersion/";
    private final String v1 = "1.0.0";
    private final String v2 = "2.0.0";
    private final String v1Context = basePath + v1;
    private final String v2Context = basePath + v2;

    @BeforeClass
    public void deployAPIs() throws Exception {
        ApictlUtils.login("test");
        ApictlUtils.createProject("default_version_v1_OpenAPI.yaml", "default_version_v1", null, null, null,
                "default_version_v1.yaml");
        ApictlUtils.createProject("default_version_v2_OpenAPI.yaml", "default_version_v2", null, null, null,
                "default_version_v2.yaml");

        ApictlUtils.deployAPI("default_version_v1", "test");
        ApictlUtils.deployAPI("default_version_v2", "test");

        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait till deployment completion.");

        API api = new API();
        api.setName("DefaultVersion");
        api.setContext(v1Context);
        api.setVersion(v1);
        api.setProvider("admin");

        ApplicationDTO app = new ApplicationDTO();
        app.setName("jwtApp");
        app.setTier("Unlimited");
        app.setId((int) (Math.random() * 1000));

        testKeyV1 = TokenUtil.getJWT(api, app, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600, null, true);
        api.setContext(v2Context);
        api.setVersion(v2);
        testKeyV2 = TokenUtil.getJWT(api, app, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600, null, true);
    }

    @Test(description = "Test invoking default versioned API without version in the context")
    public void testInvokingDefaultVersion() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV2);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + basePath + "store/inventory", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("233539"), "Response body mismatched");
    }

    @Test(description = "Test invoking default versioned API with version in the context")
    public void testInvokingDefaultVersionWithVersion() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV2);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + v2Context + "/store/inventory", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("233539"), "Response body mismatched");
    }

    @Test(description = "Test invoking `non` default versioned API")
    public void testInvokingNoneDefaultVersion() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV1);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + v1Context + "/pet/3", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("John Doe"), "Response body mismatched");

        // V1 doesn't have /store/inventory resource. We should try to invoke it and see if the traffic is routing to the
        // correct API
        response = HttpsClientRequest.doGet("https://localhost:9095" + v1Context + "/store/inventory", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND, "Response code mismatched");
    }

    /**
     * Test default versioned APIs with trailing a slash in the path
     */

    @Test(description = "Test invoking default versioned API without version in the context with trailing slash in path")
    public void testInvokingDefaultVersionWithTrailingSlashInPath() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV2);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + basePath + "store/inventory/", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("233539"), "Response body mismatched");
    }

    @Test(description = "Test invoking default versioned API with version in the context with trailing slash in path")
    public void testInvokingDefaultVersionWithVersionWithTrailingSlashInPath() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV2);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + v2Context + "/store/inventory/", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("233539"), "Response body mismatched");
    }

    @Test(description = "Test invoking `non` default versioned API with trailing slash in path")
    public void testInvokingNoneDefaultVersionWithTrailingSlashInPath() throws CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKeyV1);
        HttpResponse response = HttpsClientRequest.doGet("https://localhost:9095" + v1Context + "/pet/3/", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("John Doe"), "Response body mismatched");

        // V1 doesn't have /store/inventory resource. We should try to invoke it and see if the traffic is routing to the
        // correct API
        response = HttpsClientRequest.doGet("https://localhost:9095" + v1Context + "/store/inventory/", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND, "Response code mismatched");
    }
}
