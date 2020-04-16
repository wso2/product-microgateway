/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.micro.gateway.tests.interceptor;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * This test class is used to test java request and response interceptors data retrieval operations. I.E get operations
 * of the request and response
 */
public class JavaInterceptorTestCase extends InterceptorTestCase {

    private final String jsonArrayPayload = "[{'name' : 'foo', 'age': '20'}, "
            + "{'name': 'bar', 'age' : '30'}]";
    @Test(description = "Test java interceptor request data retrieval with json payload")
    public void testGetRequestJsonBodyInterceptor() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/petstore/v1/user?test=value1&test2=value2"), "{'hello':'world'}", headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.JSON_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");

        //test json array payloads
        headers.put("X_JWT", "true");
        response = HttpClientRequest
                .doPost(getServiceURLHttp("/petstore/v1/user?test=value1&test2=value2"), jsonArrayPayload, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.JSON_ARRAY_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test java interceptor request data retrieval with  xml payload")
    public void testGetRequestXmlBodyInterceptor() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "text/xml");
        String xmlPayload = "<test><msg>hello</msg></test>";
        HttpResponse response = HttpClientRequest
                .doPost(getServiceURLHttp("/petstore/v1/user?test=value1&test2=value2"), xmlPayload, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getData(), ResponseConstants.XML_RESPONSE);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(description = "Test java interceptor response set json payload")
    public void testSetResponseXmlBodyInterceptor() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/1"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getHeaders().get("test"), "value1");
        Assert.assertTrue(response.getData().contains("jon doe"));
        Assert.assertEquals(response.getResponseCode(), 201, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
