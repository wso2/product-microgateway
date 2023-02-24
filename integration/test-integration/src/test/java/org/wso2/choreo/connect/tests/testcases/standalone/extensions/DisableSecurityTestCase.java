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

package org.wso2.choreo.connect.tests.testcases.standalone.extensions;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DisableSecurityTestCase {
    String tokenPayload = "Bearer dGVzdHRva2VuIC1uCg==";

    @Test(description = "Test to check check API invocation without disabling security and without providing token.")
    public void invokeAPIWithoutDisabledSecurity() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/disable_security/pet/findByStatus") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check check disable security extensions is working in resource level")
    public void invokeAPIWithDisabledSecurity() throws Exception {

        String payload = String.valueOf(new Random().nextLong());
        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/disable_security/echo-full"), payload , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertTrue(response.getData().contains(payload), "Response Body Mismatch.");
        Assert.assertNotNull(response.getHeaders(), "Response headers not available");
    }

    @Test(description = "Test to check check disable security extensions is working in operation level")
    public void invokeAPIWithDisabledSecurityInOperationLevel() throws Exception {

        // Set header
        String payload = String.valueOf(new Random().nextLong());
        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/disable_security/echo"), payload , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test to check check disable security extension, x-auth-type is working in operation level")
    public void invokeAPIWithDisabledSecurityInOperationLevelUsingXAuthTypeExtension() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/disable_security/pet/findByTags") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE, "Response Body Mismatch.");
    }

    @Test(description = "Test to check if the auth header is forwarded to the backend")
    public void AuthHeaderForDisabledSecurityAPIPathLevel() throws Exception {
        String payload = String.valueOf(new Random().nextLong());
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", tokenPayload);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/disable_security/echo-full"), payload , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        EchoResponse echoResponse = new Gson().fromJson(response.getData(), EchoResponse.class);
        Assert.assertEquals(echoResponse.getData(), payload, "Response Body Mismatch.");
        Assert.assertNotNull(response.getHeaders(), "Response headers not available");
        Assert.assertEquals(echoResponse.getHeaders().get("Authorization").get(0), tokenPayload,
                "Authorization header mismatch");
    }

    @Test(description = "Test to check check disable security extensions is working in operation level")
    public void AuthHeaderForDisabledSecurityAPIInOperationLevel() throws Exception {

        // Set header
        String payload = String.valueOf(new Random().nextLong());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", tokenPayload);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/disable_security/echo"), payload , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getHeaders(), "Response headers not available");
        Assert.assertTrue(response.getHeaders().containsKey("authorization"));
        Assert.assertEquals(response.getHeaders().get("authorization"), tokenPayload,
                "Authorization header mismatch");
    }
}
