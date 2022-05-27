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

package org.wso2.choreo.connect.tests.testcases.standalone.endpoints;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;

import java.util.HashMap;
import java.util.Map;

public class ProductionSandboxTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        //TODO: (VirajSalaka) change the token
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        jwtTokenSand = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API level endpoints provided " +
            "and have same basepaths")
    public void invokeProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both API endpoints provided " +
            "and has different resource endpoints")
    public void invokeProdSandEndpointsDiffBasePath() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pet/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Sandbox endpoint when sandbox endpoints provided alone")
    public void invokeSandboxEndpointOnly() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/sand/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/sand/pet/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse, "Production endoint response should not be null");
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(
                prodResponse.getData().contains("Production key offered to an API with no production endpoint"));
    }

    @Test(description = "Invoke Sandbox endpoint when sandbox endpoints provided alone, " +
            "and has different endpoint basepath than api level")
    public void invokeSandboxEndpointOnlyDiffBasePath() throws Exception {
        Map<String, String> sandHeaders = new HashMap<>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pets/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        // api level prod endpoint should not be added to resource if basepath is different
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pets/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse, "Production endoint response should not be null");
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY);
    }

    @Test(description = "Invoke Production endpoint when production endpoints provided alone")
    public void invokeProdEndpointOnly() throws Exception {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pet/findByStatus"), headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/prod/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(sandResponse.getData().contains("Sandbox key offered to an API with no sandbox endpoint"));
    }

    @Test(description = "Invoke Production endpoint when production endpoints provided alone and " +
            "has different endpoint basepath than api level")
    public void invokeProdEndpointOnlyDiffBasePath() throws Exception {
        // resource level prod endpoint has overridden
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pets/findByTags"), headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "Response message mismatch.");

        // api level sand endpoint should not be added if basepath is different
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/v2/pets/findByTags"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test(description = "x-wso2-cluster-header should be omitted from client request")
    public void testHeaderNameSetByClient() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("x-wso2-cluster-header", "carbon.super_clusterSand_localhost_SwaggerPetstoreProductionandSandbox1.0.5");
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/findByStatus"), headers);

        Assert.assertNotNull(response, "response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "x-wso2-cluster-header should be omitted from client request even when auth security disabled")
    public void testHeaderNameSetByClientWhenNoSecurity() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-wso2-cluster-header", "carbon.super_clusterProd_localhost_SwaggerPetstore1.0.5");
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pets/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
                "The returned payload does not match with the expected payload");
    }

    @Test(description = "Invoke Production and Sandbox resource level endpoints when both endpoints are provided " +
            "and has different endpoint basepath than api level but same basepath at resource level")
    public void invokeProdSandResourceLevelSameBasePath() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/patch/same/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/patch/same/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");
    }

    @Test(description = "Invoke Production and Sandbox resource level endpoints when both endpoints are provided " +
            "and has different endpoint basepath than api level and also different basepath at resource level")
    public void invokeProdSandResourceLevelDiffBasePath() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/patch/diff/findByStatus"), prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/general/pet/patch/diff/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse);
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE_2,
                "Response message mismatch.");
    }

    //todo:(amali) enable this test once apictl side get fixed.
//    @Test(description = "endpoints are defined using endpoint object's reference")
//    public void testEndpointByReference() throws Exception {
//        Map<String, String> prodHeaders = new HashMap<>();
//        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
//        HttpResponse prodResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
//                "/v2/ref/pet/findByStatus"), prodHeaders);
//
//        Assert.assertNotNull(prodResponse);
//        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
//        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
//                "Response message mismatch.");
//
//        Map<String, String> sandHeaders = new HashMap<>();
//        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
//        HttpResponse sandResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
//                "/v2/ref/pet/findByStatus"), sandHeaders);
//
//        Assert.assertNotNull(sandResponse);
//        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
//        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
//                "Response message mismatch.");
//
//        HttpResponse prodResourceResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
//                "/v2/ref/pets/findByTags"), prodHeaders);
//
//        Assert.assertNotNull(prodResourceResponse);
//        Assert.assertEquals(prodResourceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
//        Assert.assertEquals(prodResourceResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
//                "Response message mismatch.");
//
//        HttpResponse sandResourceResponse = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
//                "/v2/ref/pets/findByTags"), sandHeaders);
//
//        Assert.assertNotNull(sandResourceResponse);
//        Assert.assertEquals(sandResourceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
//        Assert.assertEquals(sandResourceResponse.getData(), ResponseConstants.PET_BY_ID_RESPONSE,
//                "Response message mismatch.");
//    }
}
