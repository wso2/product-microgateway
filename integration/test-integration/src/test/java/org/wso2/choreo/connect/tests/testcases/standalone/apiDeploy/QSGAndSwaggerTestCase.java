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

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class QSGAndSwaggerTestCase {
    private static final String encodedCredentials = "Basic YWRtaW46YWRtaW4=";
    private String jwtProdToken;
    private String jwtSandToken;

    @BeforeClass
    public void createApiProject() throws Exception {
        ApictlUtils.createProject("https://petstore.swagger.io/v2/swagger.json", "qsg_petstore");
        jwtProdToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, "write:pets",
                false);
        jwtSandToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, "write:pets",
                false);
    }

    @Test
    public void deployAPI() throws CCTestException, MalformedURLException {
        ApictlUtils.login("test");
        ApictlUtils.deployAPI("qsg_petstore", "test");
        String endpoint = Utils.getServiceURLHttps(
                "/v2/pet/findByStatus?status=available");
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResourceURLAvailable(endpoint, new HashMap<>()));
    }

    @Test(description = "QSG test. Invoke with test key", dependsOnMethods = "deployAPI")
    public void invokeAPI() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), encodedCredentials);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/testkey"), "scope=read:pets", headers);
        String token = response.getData();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/pet/findByStatus?status=available"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Invoke Production endpoint when production endpoints provided alone", dependsOnMethods = "deployAPI")
    public void invokeAPIWithProdJWT() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtProdToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/pet/findByStatus?status=available"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    @Test(description = "Invoke Sandbox endpoint when endpoints provided in servers object", dependsOnMethods = "deployAPI")
    public void invokeAPIWithSandJWT() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtSandToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/pet/findByStatus?status=available"), headers);

        Assert.assertNotNull(response, "Sandbox endpoint response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("Sandbox key offered to an API with no sandbox endpoint"));
    }
}
