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

package org.wso2.choreo.connect.tests.testcases.standalone.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Scope validation related test cases.
 */
public class ScopeTest {
    private String jwtWithoutScope;
    private String jwtWithScope;
    private String jwtWithMultipleScopes;
    private String jwtWithMultipleInvalidScopes;
    private String jwtWithAPIScopeToken;
    private String jwtWithoutAPIScopeToken;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtWithoutScope = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null,
                false);
        jwtWithScope = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "write:pets",
                false);
        jwtWithMultipleScopes = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "write:pets read:pets",
                false);
        jwtWithMultipleInvalidScopes = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "foo bar",
                false);

        API api = new API();
        api.setName("SwaggerPetstoreScopes");
        api.setContext("scopes/v2");
        api.setVersion("1.0.5");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        jwtWithAPIScopeToken = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, "write:scopes", false);
        jwtWithoutAPIScopeToken = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, false);
    }

    @Test(description = "Test to invoke resource with scopes with a jwt without the proper scope")
    public void testScopeProtectedResourceInvalidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithoutScope);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps(
                "/v2/standard/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Error response message mismatch");
    }

    @Test(description = "Test to invoke resource with API level scopes with a jwt with the proper scope")
    public void testAPILevelScopeProtectedValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithAPIScopeToken);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps(
                "/scopes/v2/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Test to invoke resource with API level scopes with a jwt without the proper scope")
    public void testAPILevelScopeProtectedInvalidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithoutAPIScopeToken);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps(
                "/scopes/v2/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Error response message mismatch");
    }

    @Test(description = "Test to invoke resource protected with scopes with correct jwt")
    public void testScopeProtectedResourceValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps("/v2/standard/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a single correct scope")
    public void testMultipleScopeProtectedResourceValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps("/v2/standard/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a multiple correct scopes")
    public void testMultipleScopeProtectedResourceValidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleScopes);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps("/v2/standard/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with  jwt having a multiple incorrect scopes")
    public void testMultipleScopeProtectedResourceInvalidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleInvalidScopes);
        HttpResponse response = HttpClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps("/v2/standard/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN,"Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Error response message mismatch");
    }
}
