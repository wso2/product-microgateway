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

package org.wso2am.micro.gw.tests.testCases.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2am.micro.gw.tests.util.*;
import org.wso2am.micro.gw.mockbackend.ResponseConstants;

import java.util.HashMap;
import java.util.Map;

@Test(groups = { TestGroup.MGW_WITH_NO_APIS })
public class ProductionSandboxTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        // Note: Cannot use MGW_WITH_ONE_API group because although its API has a different name
        // its base path overlaps with base path in prodSand_swagger.yaml. Therefore fails
        // line 72 here.
        String prodSandApiZipfile = ApiProjectGenerator.createApictlProjZip(
                "prod-sand/prodSand_api.yaml", "prod-sand/prodSand_swagger.yaml");
        String prodOnlyApiZipfile = ApiProjectGenerator.createApictlProjZip(
                "prod-sand/prod_api.yaml", "prod-sand/prod_swagger.yaml");
        String sandOnlyApiZipfile = ApiProjectGenerator.createApictlProjZip(
                "prod-sand/sand_api.yaml", "prod-sand/sand_swagger.yaml");
        ApiDeployment.deployAPI(prodSandApiZipfile);
        ApiDeployment.deployAPI(prodOnlyApiZipfile);
        ApiDeployment.deployAPI(sandOnlyApiZipfile);

        //TODO: (VirajSalaka) change the token
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null);
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both endpoints provided")
    public void invokeProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/pet/findByStatus") , prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Sandbox endpoint when sandbox endpoints provided alone")
    public void invokeSandboxEndpointOnly() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/sand/pet/findByStatus") , sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/sand/pet/findByStatus") , prodHeaders);

        Assert.assertNotNull(prodResponse, "Production endoint response should not be null");
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(
                prodResponse.getData().contains("Production key offered to the API with no production endpoint"));
    }

    @Test(description = "Invoke Production endpoint when production endpoints provided alone")
    public void invokeProdEndpointOnly() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/prod/pet/findByStatus") , headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(URLs.getServiceURLHttps(
                "/v2/prod/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(sandResponse.getData().contains("Sandbox key offered to the API with no sandbox endpoint"));
    }
}
