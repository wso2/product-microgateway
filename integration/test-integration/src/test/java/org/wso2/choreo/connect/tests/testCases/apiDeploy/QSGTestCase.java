/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testCases.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class QSGTestCase {
    private static final String encodedCredentials = "Basic YWRtaW46YWRtaW4=";

    @BeforeClass
    public void createApiProject() throws IOException, MicroGWTestException {
        ApictlUtils.createProject( "https://petstore.swagger.io/v2/swagger.json", "qsg_petstore", null);
    }

    @Test
    public void deployAPI() throws MicroGWTestException {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("qsg_petstore", "test");
    }

    @Test
    public void invokeAPI() throws IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), encodedCredentials);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/testkey") ,"scope=read:pets",  headers);
        String token = response.getData();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/pet/findByStatus?status=available") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }
}
