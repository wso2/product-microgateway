/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.endpoints;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test endpoints when provided as references
 */
public class EndpointsByReferenceTestCase extends BaseTestCase {
    protected String jwtTokenProd;
    protected String context = "v1";

    @BeforeClass()
    public void start() throws Exception {

        String project = "EndpointReferenceProject";
        //Define application info

        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);
        String[] args = {"--myEndpoint1_prod_endpoint_0=https://localhost:2380/v1",
                "--myEndpoint2_prod_endpoint_0=https://localhost:2380/v2",
                "--myEndpoint3_prod_endpoint_0=https://localhost:2380/v1",
                "--myEndpoint3_prod_endpoint_1=https://localhost:2380/v2",
                "--myEndpoint4_prod_endpoint_1=https://localhost:2380/v2",
                "--myEndpoint1_prod_basic_password=admin",
                "--myEndpoint2_prod_basic_password=admin",
                "--myEndpoint3_prod_basic_password=admin",
                "--myEndpoint4_prod_basic_password=admin",
        };
        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[] { "endpoints/endpoints_by_reference.yaml", "endpoints/endpoint_override.yaml",
                "endpoints/load_balance.yaml", "endpoints/fail_over.yaml", "endpoints/endpoint_security.yaml",
                "endpoints/advance_config.yaml" }, args);
    }

    @Test(description = "Test Invoking the resource which  endpoint defined at resource level")
    public void testPerResourceEndpoint() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBodyV1);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the resource which endpoint defined at API level using references")
    public void testPerAPIEndpoint() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the load balanced endpoints in resource level using references")
    public void testLoadBalancedEndpointResourceLevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/pet/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponseV1);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/findByTags"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.petByIdResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the fail over endpoints using references")
    public void testFailOverEndpointResourceLevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/store/inventory"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.storeInventoryResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }
}
