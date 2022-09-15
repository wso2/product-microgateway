/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ScopesAsArrayTestCase {
    private String jwtWithEmptyArrayScope;
    private String jwtWithInvalidScope;
    private String jwtWithAPIScope;
    private String jwtWithOneResourceScope;
    private String jwtWithMultipleResourceScopes;
    private String jwtWithMultipleInvalidScopes;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        API api = new API();
        ApplicationDTO application = new ApplicationDTO();
        jwtWithEmptyArrayScope = createJwtWithScopesArray(api, application, new String[]{});
        jwtWithInvalidScope = createJwtWithScopesArray(api, application, new String[]{"write:other"});
        jwtWithAPIScope = createJwtWithScopesArray(api, application, new String[]{"write:scopes"});
        jwtWithOneResourceScope = createJwtWithScopesArray(api, application, new String[]{"read:scopes"});
        jwtWithMultipleResourceScopes = createJwtWithScopesArray(api, application, new String[]{"write:scopes", "read:scopes"});
        jwtWithMultipleInvalidScopes = createJwtWithScopesArray(api, application, new String[]{"foo", "bar"});

    }

    @Test
    public void testEmptyScopesArray() throws Exception {
        invokeWithToken(jwtWithEmptyArrayScope, "/scopes/v2/pet/findByStatus", HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testInvalidScopeInArray() throws Exception {
        invokeWithToken(jwtWithInvalidScope, "/scopes/v2/pet/findByStatus", HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testApiLevelScopeInArray() throws Exception {
        invokeWithToken(jwtWithAPIScope, "/scopes/v2/pet/findByStatus", HttpStatus.SC_OK);
    }

    @Test
    public void testOneResourceLevelScopeInArray() throws Exception {
        invokeWithToken(jwtWithOneResourceScope, "/scopes/v2/pets/findByTags", HttpStatus.SC_OK);
    }

    @Test
    public void testMultipleResourceLevelScopesInArray() throws Exception {
        invokeWithToken(jwtWithMultipleResourceScopes, "/scopes/v2/pet/findByStatus", HttpStatus.SC_OK);
    }

    @Test
    public void testMultipleInvalidScopesInArray() throws Exception {
        invokeWithToken(jwtWithMultipleInvalidScopes, "/scopes/v2/pet/findByStatus", HttpStatus.SC_FORBIDDEN);
    }

    private static void invokeWithToken(String token, String path, int responseCode) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpsClientRequest.retryGetRequestUntilDeployed(Utils.getServiceURLHttps(
                path), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), responseCode, "Response code mismatched");
    }

    private static String createJwtWithScopesArray(API api, ApplicationDTO application, String[] scopes)
            throws Exception {
        JSONObject specificClaims = new JSONObject();
        specificClaims.put("scope", scopes);
        return TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, specificClaims);
    }
}
