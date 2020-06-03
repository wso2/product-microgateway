/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.tests.jwtGenerator;

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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JWTGenerationTestCase extends BaseTestCase {
    private static String JWT_GENERATOR_ISSUER = "wso2.org/products/am";
    private static String JWT_GENERATOR_AUDIENCE = "http://org.wso2.apimgt/gateway";

    protected String jwtTokenProd;

    @BeforeClass
    public void start() throws Exception {
        String project = "jwtGeneratorProject";
        // Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        // Create map with custom claims
        Map<String, String> customClaims = new HashMap<>();
        customClaims.put("claim1", "testValue1");
        customClaims.put("claim2", "testValue2");

        jwtTokenProd = TokenUtil.getJwtWithCustomClaims(application, new JSONObject(),
                TestConstant.KEY_TYPE_PRODUCTION, 3600, customClaims);

        // generate apis with CLI and start the micro gateway server
        super.init(project, new String[]{"jwtGeneration/jwt_generation.yaml"}, null,"confs/jwt-generator-test-config.conf");
    }

    @Test(description = "Test the availability of JWT Generator")
    public void testResponseJWTGenerationHeader() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/jwtheader"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_JWT_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test JWT Generator token skew time configuration")
    public void testResponseJWTGenerationSkewTime() throws Exception {
        Map<String, String> headers = new HashMap<>();
        // test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse responseBefore = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/jwttoken"), headers);
        Assert.assertNotNull(responseBefore);
        Assert.assertEquals(responseBefore.getResponseCode(), 200, "Response code mismatched");
        String oldToken = responseBefore.getData();

        org.wso2.micro.gateway.tests.util.HttpResponse responseInstant = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/jwttoken"), headers);
        Assert.assertNotNull(responseBefore);
        Assert.assertEquals(responseInstant.getResponseCode(), 200, "Response code mismatched");
        String instantToken = responseInstant.getData();

        // checking the token from cache
        Assert.assertEquals(oldToken, instantToken, "Token not taken from the cache");

        // wait for skew time to take effect
        try {
            Thread.sleep(50000);
        } catch (InterruptedException ex) {
            Assert.fail("thread sleep interrupted!");
        }
        org.wso2.micro.gateway.tests.util.HttpResponse responseAfter = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/jwttoken"), headers);
        Assert.assertNotNull(responseAfter);
        Assert.assertEquals(responseAfter.getResponseCode(), 200, "Response code mismatched");
        String newToken = responseAfter.getData();

        // check whether new token has been generated
        Assert.assertNotEquals(oldToken, newToken, "New token not generated because of skew time");
    }

    @Test(description = "Test JWT Generator token cache and the properties")
    public void testResponseJWTGenerationToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v2/jwttoken"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        JSONObject responseJSON = new JSONObject(response.getData());
        String tokenFull = responseJSON.get("token").toString();
        String strTokenBody = tokenFull.split("\\.")[1];
        String decodedTokenBody = new String(Base64.getUrlDecoder().decode(strTokenBody));
        JSONObject tokenBody = new JSONObject(decodedTokenBody);

        Assert.assertEquals(tokenBody.get("iss"), JWT_GENERATOR_ISSUER,
                "JWT generator issuer not set correctly");
        Assert.assertEquals(tokenBody.get("aud"), JWT_GENERATOR_AUDIENCE,
                "JWT generator audience not set correctly");
        Assert.assertTrue(tokenBody.keySet().contains("claim1"), "JWT generator custom claims not set correctly");
        Assert.assertFalse(tokenBody.keySet().contains("claim2"), "JWT generator restricted claims not removed");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
