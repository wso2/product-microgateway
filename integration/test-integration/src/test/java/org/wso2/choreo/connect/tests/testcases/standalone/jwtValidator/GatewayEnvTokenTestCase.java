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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class GatewayEnvTokenTestCase {
    private String jwtWithNoAudClaim;
    private String jwtWithAudAsString;
    private String jwtWithAudAsArray;
    private String jwtWithValidEnv;
    private String jwtWithInValidEnv;
    private String jwtWithValidEnvPlusOtherEnvs;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtWithNoAudClaim = createJWT(new String[]{});
        jwtWithAudAsString = createJWT("abcdefghi1234jklm");
        jwtWithAudAsArray = createJWT(new String[]{"abcdefghi1234jklm"});
        jwtWithValidEnv = createJWT(new String[]{"abcdefghi1234jklm", "choreo:environment:Default"});
        jwtWithInValidEnv = createJWT(new String[]{"abcdefghi1234jklm", "choreo:environment:Other Env"});
        jwtWithValidEnvPlusOtherEnvs = createJWT(new String[]{"abcdefghi1234jklm", "choreo:environment:Default",
                "choreo:environment:Other Env"});
    }

    @Test(description = "Test with no audience claim")
    public void jwtWithNoAudClaimSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithNoAudClaim);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test with audience claim as string")
    public void jwtWithAudAsStringSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithAudAsString);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test with audience claim as an array")
    public void jwtWithAudAsArraySuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithAudAsArray);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test with valid env name in audience array")
    public void jwtWithValidEnvSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithValidEnv);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test without valid env name in audience array")
    public void jwtWithInvalidEnvUnauthorizedTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithInValidEnv);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN,"Response code mismatched");
        Assert.assertTrue(response.getData().contains("The access token is not authorized to access the environment."),
                "Incorrect response message");
    }

    @Test(description = "Test with valid env name in audience array with other envs")
    public void jwtWithValidEnvPlusOtherEnvsSuccessTest() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtWithValidEnvPlusOtherEnvs);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/standard/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    private String createJWT(String[] audience) throws Exception {
        JSONObject specificClaims = new JSONObject();
        specificClaims.put("aud", audience);
        return createJWT(specificClaims);
    }

    private String createJWT(String audience) throws Exception {
        JSONObject specificClaims = new JSONObject();
        specificClaims.put("aud", audience);
        return createJWT(specificClaims);
    }

    private String createJWT(JSONObject specificClaims) throws Exception {
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

        return TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, specificClaims);
    }
}
