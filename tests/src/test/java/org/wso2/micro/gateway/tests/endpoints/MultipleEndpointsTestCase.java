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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test load balance and failover endpoints
 */
public class MultipleEndpointsTestCase extends EndpointOverrideTestCase {


    @Test(description = "Test Invoking the load balanced endpoints in resource level")
    public void testLoadBalancedMultiEndpointResourceLevel() throws Exception {
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

    @Test(description = "Test Invoking the load balanced endpoints in API level")
    public void testLoadBalancedMultiEndpointAPILevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v4/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBodyV1);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v4/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBody);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test Invoking the fail over endpoints")
    public void testFailOverMultiEndpointResourceLevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/store/inventory"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.storeInventoryResponse);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

    }

    @Test(description = "Test Invoking the load balanced endpoints in API level")
    public void testFailOverMultiEndpointAPILevel() throws Exception {
        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v3/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.responseBody);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
