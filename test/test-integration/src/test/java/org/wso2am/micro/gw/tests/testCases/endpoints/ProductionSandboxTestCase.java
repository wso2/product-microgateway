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
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.util.ApiDeployment;
import org.wso2am.micro.gw.tests.util.ApiProjectGenerator;
import org.wso2am.micro.gw.tests.util.HttpResponse;
import org.wso2am.micro.gw.tests.util.HttpsClientRequest;
import org.wso2am.micro.gw.tests.util.TestConstant;
import org.wso2am.micro.gw.mockbackend.ResponseConstants;

import java.util.HashMap;
import java.util.Map;

public class ProductionSandboxTestCase extends BaseTestCase {
    protected String jwtTokenProd;
    protected String jwtTokenSand;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        super.startMGW();

        //deploy the api
        //api yaml file should put to the resources/apis/openApis folder
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
        //generate JWT token from APIM
        API api = new API();
        api.setName("PetStoreAPI");
        api.setContext("petstore/v1");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setVersion("1.0.0");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600, null);
        jwtTokenSand = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_SANDBOX, 3600, null);
    }

    @Test(description = "Invoke Production and Sandbox endpoint when both endpoints provided")
    public void invokeProdSandEndpoints() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/findByStatus") , prodHeaders);

        Assert.assertNotNull(prodResponse);
        Assert.assertEquals(prodResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(prodResponse.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(getServiceURLHttps(
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
        HttpResponse sandResponse = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/sand/pet/findByStatus") , sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(sandResponse.getData(), ResponseConstants.API_SANDBOX_RESPONSE,
                "Response message mismatch.");

        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse prodResponse = HttpsClientRequest.doGet(getServiceURLHttps(
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
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/prod/pet/findByStatus") , headers);

        Assert.assertNotNull(response, "Production endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");

        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenSand);
        HttpResponse sandResponse = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/prod/pet/findByStatus"), sandHeaders);

        Assert.assertNotNull(sandResponse, "Sandbox endpoint response should not be null");
        Assert.assertEquals(sandResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,"Response code mismatched");
        Assert.assertTrue(sandResponse.getData().contains("Sandbox key offered to the API with no sandbox endpoint"));
    }

}
