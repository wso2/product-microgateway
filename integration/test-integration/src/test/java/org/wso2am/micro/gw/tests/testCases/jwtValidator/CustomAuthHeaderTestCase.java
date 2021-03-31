/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2am.micro.gw.tests.testCases.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.mockbackend.ResponseConstants;
import org.wso2am.micro.gw.tests.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomAuthHeader related test cases.
 */
public class CustomAuthHeaderTestCase {
    protected String jwtTokenProd;
    private String customAuthHeader = "authHeader";
    private String testHeader = "auth-header";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "Invoke api with CustomAuthHeader")
    public void invokeCustomAuthHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(customAuthHeader, "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/security/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Invoke api with invalid AuthHeader")
    public void invokeInvalidCustomAuthHeaderTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(testHeader, "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/security/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(response.getData().contains("Invalid Credentials"), "Error response message mismatch");
    }

    @Test(description = "Invoke api with removing AuthHeader")
    public void invokeRemoveAuthHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(customAuthHeader, "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.
                getServiceURLHttps("/v2/security/removeauthheader"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.VALID_REMOVE_HEADER_RESPONSE);
    }
}
