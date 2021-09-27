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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class InternalKeyTestCase {
    protected String internalKey;
    protected String tamperedInternalKey;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        internalKey = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, true);
        tamperedInternalKey = internalKey.substring(0, internalKey.length()-4);
    }

    // First invoke with tampered internal key. This should fail.
    @Test(description = "Test to check the InternalKey is working")
    public void invokeWithTamperedInternalKey() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", tamperedInternalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    // When invoke with original token even though the tampered key is in the invalid key cache,
    // original token should pass.
    @Test(description = "Test to check the InternalKey is working", dependsOnMethods = "invokeWithTamperedInternalKey")
    public void invokeInternalKeyHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", internalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test to check the internal key auth validate invalid signature key")
    public void invokeInternalKeyHeaderInvalidTokenTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", TestConstant.INVALID_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    // After invoking with original key, it is cached as a success token. But again using the tampered key should fail.
    @Test(description = "Test to check the InternalKey is working", dependsOnMethods = "invokeInternalKeyHeaderSuccessTest")
    public void invokeAgainWithTamperedInternalKey() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", tamperedInternalKey);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    @Test(description = "Test to check the internal key auth validate expired token")
    public void invokeExpiredInternalKeyTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Internal-Key", TestConstant.EXPIRED_INTERNAL_KEY_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

}
