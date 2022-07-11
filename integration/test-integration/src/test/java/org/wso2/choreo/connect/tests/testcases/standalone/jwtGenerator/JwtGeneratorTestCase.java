/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.standalone.jwtGenerator;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Jwt generator test cases.
 */
public class JwtGeneratorTestCase {
    private static final String JWT_GENERATOR_ISSUER = "wso2.org/products/am";

    private String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "Test the availability of JWT Generator header")
    public void testResponseJWTGenerationHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps("v2/standard/jwtheader"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_JWT_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test JWT Generator token cache and the properties")
    public void testResponseJWTGenerationToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/jwttoken") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject responseJSON = new JSONObject(response.getData());
        String tokenFull = responseJSON.get("token").toString();
        String strTokenBody = tokenFull.split("\\.")[1];
        String decodedTokenBody = new String(Base64.getUrlDecoder().decode(strTokenBody));
        JSONObject tokenBody = new JSONObject(decodedTokenBody);
        Assert.assertEquals(tokenBody.get("iss"), JWT_GENERATOR_ISSUER,
                "Issuer is  not set correctly in JWT generator");
        Assert.assertEquals(tokenBody.get("keytype"), TestConstant.KEY_TYPE_PRODUCTION,
                "Key type is not set correctly in JWT generator");
        Assert.assertNotNull(tokenBody.get("iat"));
        Assert.assertNotNull(tokenBody.get("exp"));

        long expValue =  Long.parseLong(String.valueOf(tokenBody.get("exp")));
        long iatValue =  Long.parseLong(String.valueOf(tokenBody.get("iat")));

        long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        Assert.assertEquals(expValue - iatValue, 3600, "Time duration between iat and exp claims is " +
                "not 3600 seconds.");
        Assert.assertTrue(expValue > currentTime, "Expiry time is not greater than currentTime.");
        Assert.assertTrue(iatValue <= currentTime, "IAT value is not less than the current Time");
    }

    @Test(description = "Test JWT Generator token caching")
    public void testResponseJWTGenerationTokenCaching() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/jwttoken") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject responseJSON = new JSONObject(response.getData());
        String token1 = responseJSON.get("token").toString();

        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/jwttoken") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        responseJSON = new JSONObject(response.getData());
        String token2 = responseJSON.get("token").toString();

        Assert.assertEquals(token1, token2, "Generated Backend JWTs for two invocations from the same token" +
                " to same api are different. Hence it is not cached.");

        String newJwtToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + newJwtToken);
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/jwttoken") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        responseJSON = new JSONObject(response.getData());
        String token3 = responseJSON.get("token").toString();

        Assert.assertNotEquals(token1, token3, "Generated Backend JWTs for two invocations from the different " +
                "tokens to same api are same. Hence there is an issue for backend JWT generation where the generated" +
                "JWT remains same for different users.");
    }
}
