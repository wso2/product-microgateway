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
import org.wso2.micro.gateway.tests.common.CLIExecutor;
import org.wso2.micro.gateway.tests.common.KeyValidationInfo;
import org.wso2.micro.gateway.tests.common.MockAPIPublisher;
import org.wso2.micro.gateway.tests.common.MockHttpServer;
import org.wso2.micro.gateway.tests.common.model.API;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.context.ServerInstance;
import org.wso2.micro.gateway.tests.context.Utils;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.HttpResponse;
import org.wso2.micro.gateway.tests.util.TestConstant;

import java.io.File;
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

        super.init("api-key-project", new String[]{"common_api.yaml"}, null, "confs/validation.conf");
        String originalInput = "generalUser1:password";
        String basicAuthToken = Base64.getEncoder().encodeToString(originalInput.getBytes());

        Map<String, String> headers = new HashMap<>();
        //get token from token endpoint
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Basic " + basicAuthToken);
        HttpResponse response = HttpClientRequest
                .doGet("https://localhost:" + TestConstant.GATEWAY_LISTENER_HTTPS_PORT + "/apikey", headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        apikey = response.getData();
    }


    @Test(description = "Test to check the apikey auth working")
    private void testAPIResponse() throws Exception {

        Map<String, String> headers = new HashMap<>();
        //test endpoint with token
        headers.put("api_key", apikey);
        HttpResponse response = HttpClientRequest.doGet(getServiceURLHttp("petstore/v1/pet/1"), headers);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }
}
