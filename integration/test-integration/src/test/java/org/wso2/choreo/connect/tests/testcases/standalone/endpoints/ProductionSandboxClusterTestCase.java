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

package org.wso2.choreo.connect.tests.testcases.standalone.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ProductionSandboxClusterTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level endpoints are the same")
    public void invokeProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/cluster/pets/findByTags"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/cluster/pets/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both resource level endpoints are the same")
    public void invokeResourceProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/cluster/pet/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/cluster/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }
}
