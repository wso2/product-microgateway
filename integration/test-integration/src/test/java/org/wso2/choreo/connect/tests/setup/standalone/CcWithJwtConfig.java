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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.util.concurrent.TimeUnit;

public class CcWithJwtConfig {
    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        ApictlUtils.createProject( "global_cors_openAPI.yaml", "global_cors_petstore");
        ApictlUtils.createProject( "timeout_global_openAPI.yaml", "timeout_global");
        ApictlUtils.createProject( "timeout_endpoint_openAPI.yaml", "timeout_endpoint");

        ccInstance = new CcInstance.Builder().withBackendServiceFile("backend-service-gql.yaml")
                .withNewConfig("jwt-generator-config.toml").build();
        ccInstance.start();

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");

        ApictlUtils.deployAPI("petstore", "test");
        ApictlUtils.deployAPI("global_cors_petstore", "test");
        ApictlUtils.deployAPI("timeout_global", "test");
        ApictlUtils.deployAPI("timeout_endpoint", "test");

        ApictlUtils.deploySampleProject("SampleGraphQLApi", "test");

        TimeUnit.SECONDS.sleep(5);
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
