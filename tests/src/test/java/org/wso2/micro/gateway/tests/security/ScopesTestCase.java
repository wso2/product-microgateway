/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.security;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test scopes defined under the security schemes in open API definitions
 */
public class ScopesTestCase extends BaseTestCase {

    private String jwtTokenProd;
    private String jwtTokenProdWithScopes;
    private String jwtTokenProdWithSingleScope;
    private String jwtTokenProdWithMultipleScope;

    @BeforeClass
    public void start() throws Exception {

        String project = "scopesProject";
        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        Map<String, String> claimMap = new HashMap<>();
        claimMap.put("scope", "write:pets");
        jwtTokenProdWithScopes = TokenUtil
                .getJwtWithCustomClaims(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600,
                        claimMap);
        claimMap.put("scope", "write:petsNew");
        jwtTokenProdWithSingleScope = TokenUtil
                .getJwtWithCustomClaims(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600,
                        claimMap);
        claimMap.put("scope", "write:petsNew read:pets");
        jwtTokenProdWithMultipleScope = TokenUtil
                .getJwtWithCustomClaims(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600,
                        claimMap);
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[] { "common_api.yaml" });
    }

    @Test(description = "Test Invoking the resource which is protected by scopes without providing the scopes")
    public void testWithoutScopes() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // require scope write:pets or read:pets
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("petstore/v1/pet/1"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("900910"),
                "Response should return the code 900910 for invalid scopes");
        Assert.assertEquals(response.getResponseCode(), 403, "HTTP Response code mismatched");
    }

    @Test(description = "Test Invoking the resource which is protected by two scopes by providing jwt with the scope")
    public void testResourceWithMultipleScopes() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProdWithScopes);
        // require scope write:pets or read:pets
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("petstore/v1/pet/1"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the resource by providing wrong scope than the expected scope")
    public void testResourceWithWrongScopes() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProdWithScopes);
        // require scope write:petsNew
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("petstore/v1/pet"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("900910"),
                "Response should return the code 900910 for invalid scopes");
        Assert.assertEquals(response.getResponseCode(), 403, "HTTP Response code mismatched");
    }

    @Test(description = "Test Invoking the resource which is protected by single scope by providing jwt with the scope")
    public void testResourceWithSingleScopes() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProdWithSingleScope);
        // require scope write:petsNew
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("petstore/v1/pet/"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the resources which have different scopes with same JWT having all the scopes")
    public void testSameTokenWithMultipleResources() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProdWithMultipleScope);
        // require scope write:pets or read:pets
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("petstore/v1/pet/1"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        // require scope write:petsNew
        response = HttpClientRequest.doPost(getServiceURLHttp("petstore/v1/pet/"), "{}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
