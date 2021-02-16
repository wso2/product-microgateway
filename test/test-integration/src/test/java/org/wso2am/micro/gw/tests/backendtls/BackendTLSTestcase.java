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

package org.wso2am.micro.gw.tests.backendtls;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
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

import java.util.HashMap;
import java.util.Map;

public class BackendTLSTestcase extends BaseTestCase {
    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        super.startMGW(null, true);

        //deploy the api
        //api yaml file should put to the resources/apis/openApis folder
        String apiZipfile = ApiProjectGenerator.createApictlProjZip("backendtls/openapi.yaml",
                "backendtls/backend.crt");
        ApiDeployment.deployAPI(apiZipfile);

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

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600, "write:pets");
    }

    @AfterClass(description = "stop the setup")
    void stop() {
        super.stopMGW();
    }

    @Test(description = "Invoke HTTP endpoint")
    public void invokeHttpEndpointSuccess() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/findByStatus") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Invoke Https endpoint")
    public void invokeHttpsEndpointSuccess() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Test to check the JWT auth working")
    public void invokeMTLSEndpointSuccess() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/findByTags") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
