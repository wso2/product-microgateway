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
import org.wso2.choreo.connect.tests.util.Utils;

public class CcWithJwtConfigAndTransformer {
    CcInstance ccInstance;

    @BeforeTest(description = "initialise the setup")
    void start() throws Exception {
        ccInstance = new CcInstance.Builder().withNewConfig("jwt-generator-config.toml").withAllCustomImpls().build();
        ccInstance.start();
        //TODO: (SuKSW) Check why this fails
//        Awaitility.await().pollDelay(20, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
//                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());

        ApictlUtils.addEnv("test");
        ApictlUtils.login("test");
        ApictlUtils.createProject("ratelimit_openAPI.yaml", "ratelimit_test",
                null, null, null, "ratelimit_api.yaml", "rate_limit_api_policies.yaml");
        ApictlUtils.createProject("operation_level_ratelimit_openAPI.yaml", "ratelimit_operation_level_test",
                null, null, null, "operation_level_ratelimit_api.yaml", "rate_limit_api_policies.yaml");
        Utils.delay(10000, "Could not wait till project creation.");

        ApictlUtils.deployAPI("petstore", "test");
        ApictlUtils.deployAPI("ratelimit_test", "test");
        ApictlUtils.deployAPI("ratelimit_operation_level_test", "test");
        Utils.delay(5000, "Could not wait till initial setup completion.");
    }

    @AfterTest(description = "stop the setup")
    void stop() throws CCTestException {
        ccInstance.stop();
        ApictlUtils.removeEnv("test");
    }
}
