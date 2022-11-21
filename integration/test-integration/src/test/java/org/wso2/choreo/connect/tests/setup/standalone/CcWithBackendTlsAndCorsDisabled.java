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

import org.awaitility.Awaitility;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.util.concurrent.TimeUnit;

public class CcWithBackendTlsAndCorsDisabled {
    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        ccInstance = new CcInstance.Builder().withNewConfig("cors-disabled-config.toml")
                .withBackendServiceFile("backend-service-tls.yaml").build();
        ccInstance.start();
        Awaitility.await().pollDelay(5, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());

        ApictlUtils.createProject("mtls_from_router_to_backend_openAPI.yaml", "mtls_from_router_to_backend", "backend_tls.crt", null, null,null);
        ApictlUtils.createProject("intercept_request_openAPI.yaml", "intercept_request_petstore", "backend_tls.crt", null, "interceptor.crt",null);
        ApictlUtils.createProject("intercept_response_openAPI.yaml", "intercept_response_petstore", "backend_tls.crt", null, "interceptor.crt",null);
        ApictlUtils.createProject( "cors_openAPI.yaml", "cors_petstore");
        ApictlUtils.createProject( "api_key_swagger_security_openAPI.yaml", "apikey");

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("mtls_from_router_to_backend", "test");
        ApictlUtils.deployAPI("intercept_request_petstore", "test");
        ApictlUtils.deployAPI("intercept_response_petstore", "test");
        ApictlUtils.deployAPI("cors_petstore", "test");
        ApictlUtils.deployAPI("apikey", "test");
        TimeUnit.SECONDS.sleep(5);
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
