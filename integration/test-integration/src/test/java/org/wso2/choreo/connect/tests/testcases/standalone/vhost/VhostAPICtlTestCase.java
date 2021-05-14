/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.standalone.vhost;

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

public class VhostAPICtlTestCase {
    private String jwtTokenProd;
    private String jwtTokenSand;
    private static final String LOCALHOST = "localhost";
    private static final String US_HOST = "us.wso2.com";
    private static final String RESOURCE_PATH_FIND_BY_STATUS = "/v2/vhost/pet/findByStatus";
    private static final String RESOURCE_PATH_INVENTORY = "/v2/vhost/store/inventory";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        //TODO: (VirajSalaka) change the token
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke APIs with same API name, version, context and different backends - vhost: localhost")
    public void invokeSameAPINameVhost1APIDiffBackend() throws Exception {
        // APIs deployed in vhost: localhost -------------------
        // Production endpoint
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        prodHeaders.put(HttpHeaderNames.HOST.toString(), LOCALHOST);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_FIND_BY_STATUS), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        // response from backend 1
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        // Sandbox endpoint
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        sandHeaders.put(HttpHeaderNames.HOST.toString(), LOCALHOST);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_FIND_BY_STATUS), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        // response from backend 2
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke APIs with same API name, version, context and different backends - vhost: us.wso2.com")
    public void invokeSameAPINameVhost2APIDiffBackend() throws Exception {
        // APIs deployed in vhost: us.wso2.com -------------------
        // Production endpoint
        Map<String, String> prodHeaders = new HashMap<>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        prodHeaders.put(HttpHeaderNames.HOST.toString(), US_HOST);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_FIND_BY_STATUS) , prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        // response from backend 2
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        // Sandbox endpoint
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        sandHeaders.put(HttpHeaderNames.HOST.toString(), US_HOST);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_FIND_BY_STATUS), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        // response from backend 1
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "Invoke APIs with same API name, version, context and different resources - vhost: localhost")
    public void invokeSameAPINameVhost1APIxistingResource () throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.HOST.toString(), LOCALHOST);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_INVENTORY) , headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.STORE_INVENTORY_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke APIs with same API name, version, context and different resources - vhost: us.wso2.com")
    public void invokeSameAPINameVhost2APINonExistingResource () throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.HOST.toString(), US_HOST);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                RESOURCE_PATH_INVENTORY) , headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched");
    }
}
