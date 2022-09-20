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
package org.wso2.choreo.connect.tests.setup.standalone;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.ZipDir;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CcWithDefaultConf {
    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        ApictlUtils.createProject( "openAPI_startup_zipped.yaml", "openAPI_startup_zipped");
        ApictlUtils.createProject( "openAPI_startup.yaml", "openAPI_startup");
        ZipDir.createZipFile(Utils.getTargetDirPath() + ApictlUtils.API_PROJECTS_PATH + "openAPI_startup_zipped");
        ccInstance = new CcInstance.Builder()
                .withStartupAPI(Utils.getTargetDirPath() + ApictlUtils.API_PROJECTS_PATH +
                        "openAPI_startup_zipped.zip")
                .withStartupAPI(Utils.getTargetDirPath() + ApictlUtils.API_PROJECTS_PATH + "openAPI_startup")
                .build(true);
        ccInstance.start();
        Awaitility.await().pollDelay(5, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());

        ApictlUtils.createProject( "trailing_slash_openAPI.yaml", "trailing_slash");
        ApictlUtils.createProject( "all_http_methods_for_wildcard_openAPI.yaml", "all_http_methods_for_wildcard_openAPI");
        ApictlUtils.createProject( "prod_and_sand_openAPI.yaml", "prod_and_sand_petstore");
        ApictlUtils.createProject("prod_sand_diff_endpoints_openAPI.yaml", "prod_sand_diff_endpoints");
        //todo:(amali) enable this test once apictl side get fixed.
        // ApictlUtils.createProject( "endpoint_ref_openAPI.yaml", "ep_ref_petstore", null, null);
        ApictlUtils.createProject( "prod_openAPI.yaml", "prod_petstore");
        ApictlUtils.createProject( "sand_openAPI.yaml", "sand_petstore");
        ApictlUtils.createProject( "security_openAPI.yaml", "custom_authheader_petstore");
        ApictlUtils.createProject( "vhost1_openAPI.yaml", "vhost1_petstore", null, "vhost1_deploy_env.yaml", null, null);
        ApictlUtils.createProject( "vhost2_openAPI.yaml", "vhost2_petstore", null, "vhost2_deploy_env.yaml", null, null);
        ApictlUtils.createProject( "openAPI_v3_standard_valid.yaml", "apictl_petstore_v3");
        ApictlUtils.createProject( "malformed_endpoint_openAPI.yaml", "apictl_malformed_endpoint");
        ApictlUtils.createProject( "retry_openAPI.yaml", "retry");
        ApictlUtils.createProject( "intercept_request_openAPI.yaml", "intercept_request_default_setup_petstore", "backend_tls.crt", null, null, null);
        ApictlUtils.createProject( "intercept_response_openAPI.yaml", "intercept_response_default_setup_petstore", "backend_tls.crt", null, null, null);
        ApictlUtils.createProject( "circuit_breakers_openAPI.yaml", "circuit_breakers");
        ApictlUtils.createProject( "disable_security_openAPI.yaml", "disable_security");
        ApictlUtils.createProject( "backend_security_openAPI.yaml", "backend_security");
        ApictlUtils.createProject( "scopes_openAPI.yaml", "scopes");

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("petstore", "test");
        ApictlUtils.deployAPI("trailing_slash", "test");
        ApictlUtils.deployAPI("all_http_methods_for_wildcard_openAPI", "test");
        ApictlUtils.deployAPI("prod_and_sand_petstore", "test");
        ApictlUtils.deployAPI("prod_sand_diff_endpoints", "test");
//        ApictlUtils.deployAPI("ep_ref_petstore", "test");
        ApictlUtils.deployAPI("prod_petstore", "test");
        ApictlUtils.deployAPI("sand_petstore", "test");
        ApictlUtils.deployAPI("custom_authheader_petstore", "test");
        ApictlUtils.deployAPI("vhost1_petstore", "test");
        ApictlUtils.deployAPI("vhost2_petstore", "test");
        ApictlUtils.deployAPI("apictl_petstore_v3", "test");
        ApictlUtils.deployAPI("retry", "test");
        ApictlUtils.deployAPI("intercept_request_default_setup_petstore", "test");
        ApictlUtils.deployAPI("intercept_response_default_setup_petstore", "test");
        ApictlUtils.deployAPI("circuit_breakers", "test");
        ApictlUtils.deployAPI("disable_security", "test");
        ApictlUtils.deployAPI("backend_security", "test");
        ApictlUtils.deployAPI("scopes", "test");
        TimeUnit.SECONDS.sleep(5);
    }

    @Test(description = "Invoke Health endpoint")
    public void invokeHttpsEndpointSuccess() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<String, String>(0);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/health") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), TestConstant.HEALTH_ENDPOINT_RESPONSE,
                "Response message mismatched");
    }

    @Test(description = "Invoke Health endpoint (secured)")
    public void invokeHttpEndpointSuccess() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<String, String>(0);
        HttpResponse response = HttpClientRequest.doGet(Utils.getServiceURLHttp(
                "/health") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), TestConstant.HEALTH_ENDPOINT_RESPONSE,
                "Response message mismatched");
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
