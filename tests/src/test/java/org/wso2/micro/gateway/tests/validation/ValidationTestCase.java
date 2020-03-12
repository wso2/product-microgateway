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
package org.wso2.micro.gateway.tests.validation;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.ResponseConstants;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public class ValidationTestCase extends BaseTestCase {
    private String apikey;

    @BeforeClass
    public void start() throws Exception {
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        super.init("validation-project", new String[]{"validation/validation_api.yaml"}, null,
                "confs/validation.conf");
        apikey = getAPIKey();
    }

   private String getAPIKey() throws Exception {

        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        Map<String, String> headers = new HashMap<>();
        //get token from token endpoint
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + basicAuthToken);
        HttpResponse response = HttpClientRequest
                .doGet("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT + "/apikey", headers);
        return response.getData();
    }

    @Test(description = "Test invalid request body for the Post request")
    private void testInvalidRequest() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put("api_key", apikey);
        headers.put("Content-Type", "application/json");
        HttpResponse response =
                HttpClientRequest.doPost(getServiceURLHttp("petstore/v1/pet"), "{ \"id\": 0}",
                        headers);
        Assert.assertEquals(response.getData(), ResponseConstants.VALIDATION_RESPONSE, "Response code mismatched");
    }


    @Test(description = "Test invalid response body for the Get request")
    private void testInvalidResponse() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put("api_key", apikey);
        headers.put("accept", "application/json");
        HttpResponse response =
                HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/2"), headers);
        Assert.assertEquals(response.getData(), ResponseConstants.INVALID_RESPONSE, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
