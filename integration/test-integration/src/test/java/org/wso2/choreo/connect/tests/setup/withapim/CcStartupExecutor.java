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
import org.testng.annotations.BeforeSuite;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.context.ApimInstance;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CcStartupExecutor extends ApimBaseTest {
    CcInstance ccInstance;
    ApimInstance apimInstance;

    @BeforeSuite // Not BeforeTest because this has to run after ApimPreparer
    public void startChoreoConnect() throws Exception {
        ccInstance = new CcInstance.Builder().withNewDockerCompose("cc-in-common-network-docker-compose.yaml")
                .withNewConfig("controlplane-enabled-config.toml")
                .withBackendServiceFile("backend-service-with-tls-and-network.yaml")
                .withAllCustomImpls().build();
        apimInstance = ApimInstance.createNewInstance();
        apimInstance.startAPIM();
        ccInstance.start();
        Awaitility.await().pollDelay(40, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.MINUTES).until(ccInstance.isHealthy());
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for " +
                "resources to be pulled from API Manager");
        Assert.assertTrue(ccInstance.checkCCInstanceHealth());
    }
}
