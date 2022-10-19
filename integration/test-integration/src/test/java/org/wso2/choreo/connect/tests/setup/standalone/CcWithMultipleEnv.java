/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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
package org.wso2.choreo.connect.tests.setup.standalone;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CcWithMultipleEnv {
    CcInstance ccInstance;
    private static final String encodedCredentials = "Basic YWRtaW46YWRtaW4=";

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {

        ccInstance = new CcInstance.Builder().withNewConfig("multiple-env-config.toml").build();
        ccInstance.start();
        Awaitility.await().pollDelay(5, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());

        ApictlUtils.deleteAllProjects();

        ApictlUtils.createProject( "deploy_openAPI.yaml", "apictl_petstore2", null,
                "apictl_test_deploy_multiple_env.yaml", null, null);
        ApictlUtils.createProject( "api_key_swagger_security_openAPI.yaml", "apikey_swagger");
        ApictlUtils.createProject( "api_key_openAPI.yaml", "apikey");
        ApictlUtils.createProject( "openAPI.yaml", "openapi");
        ApictlUtils.createProject( "openAPI_startup.yaml", "openAPI_startup");

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("apictl_petstore2", "test");
        ApictlUtils.deployAPI("apikey_swagger", "test");
        ApictlUtils.deployAPI("apikey", "test");
        ApictlUtils.deployAPI("openapi", "test");
        ApictlUtils.deployAPI("openAPI_startup", "test");



        String endpoint = Utils.getServiceURLHttps(
                "/v2/new/pet/findByStatus?status=available");
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResourceURLAvailable(endpoint, new HashMap<>()));
    }

    @Test(description = "Undeploy API From Specific Gateway Env and Specific Vhost.")
    public void invokeAPI() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), encodedCredentials);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/testkey") ,"scope=read:pets",  headers);
        String token = response.getData();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);

        // Check if the API is already deployed.
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/new/pet/findByStatus?status=available") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");

        // Undeploy the API from specific environment
        ApictlUtils.undeployAPI("SwaggerPetstoreDeploy", "1.0.5", "test", "localhost",
                "Default");
        response = HttpsClientRequest.retryUntil404(Utils.getServiceURLHttps(
                "/v2/new/pet/findByStatus?status=available") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND,"Response code mismatched " +
                "for undeployed API");

        // Check if the other deployement (under different host) is successful.
        headers.put("host", "eu.wso2.com");
        response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/new/pet/findByStatus?status=available") , headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched " +
                "for undeployed API");
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
