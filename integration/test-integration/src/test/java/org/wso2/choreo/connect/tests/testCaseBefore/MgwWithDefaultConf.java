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
package org.wso2.choreo.connect.tests.testCaseBefore;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.BaseTestCase;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MgwWithDefaultConf extends BaseTestCase {

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        super.startMGW();
        ApictlUtils.createProject( "prod_and_sand_openAPI.yaml", "prod_and_sand_petstore", null, null);
        ApictlUtils.createProject( "prod_openAPI.yaml", "prod_petstore", null, null);
        ApictlUtils.createProject( "sand_openAPI.yaml", "sand_petstore", null, null);
        ApictlUtils.createProject( "security_openAPI.yaml", "custom_authheader_petstore", null, null);
        ApictlUtils.createProject( "vhost1_openAPI.yaml", "vhost1_petstore", null, "vhost1_deploy_env.yaml");
        ApictlUtils.createProject( "vhost2_openAPI.yaml", "vhost2_petstore", null, "vhost2_deploy_env.yaml");
        ApictlUtils.createProject( "openAPI_v3_standard_valid.yaml", "apictl_petstore_v3", null, null);

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("petstore", "test");
        ApictlUtils.deployAPI("prod_and_sand_petstore", "test");
        ApictlUtils.deployAPI("prod_petstore", "test");
        ApictlUtils.deployAPI("sand_petstore", "test");
        ApictlUtils.deployAPI("custom_authheader_petstore", "test");
        ApictlUtils.deployAPI("vhost1_petstore", "test");
        ApictlUtils.deployAPI("vhost2_petstore", "test");
        ApictlUtils.deployAPI("apictl_petstore_v3", "test");
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
    void stop() throws MicroGWTestException {
        super.stopMGW();
        ApictlUtils.removeEnv("test");
    }
}
