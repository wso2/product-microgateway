/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.filter;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.*;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BodyPassDisabledTestCase {
    private String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "write:pets", false);
        ApictlUtils.createProject("request_body_pass_disabled_openAPI.yaml", "body_pass_disabled_api_test",
                null, null, null, null);
    }

    @Test
    public void deployBodyPassDisabledAPI() throws Exception {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("body_pass_disabled_api_test", "test");
        TimeUnit.SECONDS.sleep(5);
    }

    @Test(description = "Tests request body passing feature when in disabled mode")
    public void testRequestBodyPassDisabled() throws MalformedURLException, CCTestException {
        String requestBody = "{\"dataField \": \"helloWorld\"}";
        String customHeaderName = "x-wso2-request-body-validated-header";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest
                .doPost(Utils.getServiceURLHttps("/bodyPassDisable/1.0.0/echo"), requestBody,headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatched");
        Assert.assertFalse(response.getHeaders().containsKey(customHeaderName),
                "Request body pass validation header is not expected in the response.");
    }


}
