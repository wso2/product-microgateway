/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
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

package org.wso2.choreo.connect.tests.testcases.withapim.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Jwt test cases.
 */
public class JwtAndScopeTestCase extends ApimBaseTest {

    private static final String SAMPLE_API_NAME = "JWTTestAPI";
    private static final String SAMPLE_API_CONTEXT = "/jwt";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private static final String APP_NAME = "JWTTestApp";

    private String endPoint;
    private String jwtWithoutScope;
    private String invalidJwt;
    private String jwtWithScope;
    private String jwtWithMultipleScopes;
    private String jwtWithMultipleInvalidScopes;

    @BeforeClass(description = "Initialise the setup for JWT tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String targetDir = Utils.getTargetDirPath();
        String filePath = targetDir + ApictlUtils.OPENAPIS_PATH + "scopes_openAPI.yaml";

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", SAMPLE_API_NAME);
        apiProperties.put("context", SAMPLE_API_CONTEXT);
        apiProperties.put("version", SAMPLE_API_VERSION);
        apiProperties.put("provider", user.getUserName());
        String apiId = PublisherUtils.createAPIUsingOAS(apiProperties, filePath, publisherRestClient);

        publisherRestClient.changeAPILifeCycleStatus(apiId, "Publish");

        // creating the application
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appWithConsumerKey = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        String applicationId = appWithConsumerKey.getApplicationId();

        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);

        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);

        endPoint = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/" + SAMPLE_API_VERSION);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");

        // Obtain JWT keys
        jwtWithoutScope = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{}, user, storeRestClient);
        invalidJwt = jwtWithoutScope.substring(0, jwtWithoutScope.length() - 400);
        jwtWithScope = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"write:scopes"}, user, storeRestClient);
        jwtWithMultipleScopes = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"write:scopes", "read:scopes"}, user, storeRestClient);
        jwtWithMultipleInvalidScopes = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"foo", "bar"}, user, storeRestClient);
    }

    @Test(description = "Test to check the JWT auth working")
    public void invokeJWTHeaderSuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithoutScope);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                endPoint + "/pet/2"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertFalse(response.getHeaders().containsKey("www-authenticate"),
                "\"www-authenticate\" is available");
    }

    @Test(description = "Test to check the JWT auth validate invalid signature token")
    public void invokeJWTHeaderInvalidTokenTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + invalidJwt);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                endPoint + "/pet/2"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("www-authenticate"),
                "\"www-authenticate\" is not available");
    }

    @Test(description = "Test to invoke resource with API level scopes with a jwt with the proper scope")
    public void testAPILevelScopeProtectedValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint +
                "/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    // apim does not process api level scopes correctly, hence this test case fails
//    @Test(description = "Test to invoke resource with API level scopes with a jwt without the proper scope")
//    public void testAPILevelScopeProtectedInvalidJWT() throws Exception {
//        Map<String, String> headers = new HashMap<>();
//        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithoutScope);
//        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint +
//                "/pet/findByStatus"), headers);
//        Assert.assertNotNull(response);
//        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
//        Assert.assertTrue(
//                response.getData().contains("The access token does not allow you to access the requested resource"),
//                "Error response message mismatch");
//    }

    @Test(description = "Test to invoke resource protected with scopes with correct jwt")
    public void testScopeProtectedResourceValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint + "/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a single correct scope")
    public void testMultipleScopeProtectedResourceValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint + "/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a multiple correct scopes")
    public void testMultipleScopeProtectedResourceValidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleScopes);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint + "/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with  jwt having a multiple incorrect scopes")
    public void testMultipleScopeProtectedResourceInvalidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleInvalidScopes);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(endPoint + "/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Error response message mismatch");
    }
}
