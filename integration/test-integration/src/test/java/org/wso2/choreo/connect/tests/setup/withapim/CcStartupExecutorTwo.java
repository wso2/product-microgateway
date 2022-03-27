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
package org.wso2.choreo.connect.tests.setup.withapim;

import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.concurrent.TimeUnit;

/**
 * This class is prepared to invoke CC for test cases that cover specific scenarios.
 * Supporting test cases and scenarios:
 *
 * - Using for same header name for API keys and Internal Keys
 * - Updated routeIdleTimeout (this overrides streamIdleTimeout). This is to test an idle websocket connection.
 */
public class CcStartupExecutorTwo extends ApimBaseTest {
    CcInstance ccInstance;

    @Test // Not BeforeTest because this has to run after ApimPreparer
    public void startChoreoConnect() throws Exception {
        ccInstance = new CcInstance.Builder().withNewDockerCompose("cc-in-common-network-docker-compose.yaml")
                .withNewConfig("cc-special-scenarios-with-apim.toml")
                .withBackendServiceFile("backend-service-with-tls-and-network.yaml")
                .withInterceptorCertInRouterTruststore()
                .withAllCustomImpls().build();
        ccInstance.start();
        Awaitility.await().pollDelay(20, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());
        Assert.assertTrue(ccInstance.checkCCInstanceHealth());
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for " +
                "resources to be pulled from API Manager");
    }
}
