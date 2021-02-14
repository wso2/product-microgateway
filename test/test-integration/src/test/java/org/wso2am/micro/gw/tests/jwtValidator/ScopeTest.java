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

package org.wso2am.micro.gw.tests.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.mockbackend.ResponseConstants;
import org.wso2am.micro.gw.tests.util.HttpResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Scope validation related test cases.
 *
 */
public class ScopeTest extends JwtTestCase {

    @Test(description = "Test to invoke resource with scopes with a jwt without the proper scope")
    public void testScopeProtectedResourceInvalidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithoutScope);
        HttpResponse response = retryGetRequestUntilDeployed(getServiceURLHttps(
                "/v2/pets/findByTags"), headers);
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
        HttpResponse response = retryGetRequestUntilDeployed(getServiceURLHttps("/v2/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a single correct scope")
    public void testMultipleScopeProtectedResourceValidJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithScope);
        HttpResponse response = retryGetRequestUntilDeployed(getServiceURLHttps("/v2/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with correct jwt having a multiple correct scopes")
    public void testMultipleScopeProtectedResourceValidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleScopes);
        HttpResponse response = retryGetRequestUntilDeployed(getServiceURLHttps("/v2/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Test to invoke resource protected with multiple scopes with  jwt having a multiple incorrect scopes")
    public void testMultipleScopeProtectedResourceInvalidMultiScopeJWT() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithMultipleInvalidScopes);
        HttpResponse response = retryGetRequestUntilDeployed(getServiceURLHttps("/v2/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN,"Response code mismatched");
        Assert.assertTrue(
                response.getData().contains("The access token does not allow you to access the requested resource"),
                "Error response message mismatch");
    }
}
