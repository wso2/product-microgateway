/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.tests.testcases.standalone.jwtGenerator;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BackendJWTWithAudClaimTestcase {
    private String jwtTokenProd;
    private static final String API_CONTEXT = "backend-security-with-aud-claim";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        Utils.delay(10000, "Could not wait until the test starts");
    }

    @Test(description = "Test the availability of JWT Generator header")
    public void testAudienceClaimsInBackendJWT() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/echo");
        HttpResponse response = HttpsClientRequest
                .doGet(Utils.getServiceURLHttps(endpoint), headers);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getHeaders());
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertTrue(respHeaders.containsKey(ResponseConstants.BACKEND_JWT_DEFAULT_HEADER_NAME),
                "Backend JWT relevant header not found in the response");
        String backendJWT = respHeaders.get(ResponseConstants.BACKEND_JWT_DEFAULT_HEADER_NAME);
        String strTokenBody = backendJWT.split("\\.")[1];
        String decodedTokenBody = new String(Base64.getUrlDecoder().decode(strTokenBody));
        JSONObject tokenBody = new JSONObject(decodedTokenBody);
        JSONArray audList = (JSONArray) tokenBody.get("aud");
        Assert.assertTrue(audList.length() == 1, "Cannot find required audience count in the backend JWT");
        Assert.assertTrue(audList.get(0).equals("https://petstore.swagger.io")
                        && audList.get(1).equals("https://petstore.swagger.io/pet"),
                "Audience claims do not matched.");
    }
}
