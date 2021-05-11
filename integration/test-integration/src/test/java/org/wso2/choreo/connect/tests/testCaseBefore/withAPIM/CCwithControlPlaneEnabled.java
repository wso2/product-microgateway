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
package org.wso2.choreo.connect.tests.testCaseBefore.withAPIM;

import org.awaitility.Awaitility;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CCwithControlPlaneEnabled {
    CcInstance ccInstance;

    @BeforeTest
    public void startChoreoConnect() throws IOException, CCTestException {
        ccInstance = new CcInstance.Builder().withNewDockerCompose("cc-in-common-network-docker-compose.yaml")
                .withNewConfig("controlplane-enabled-config.toml").withBackendTsl().build();
        ccInstance.start();
        Awaitility.await().atMost(2, TimeUnit.MINUTES);
//        Awaitility.await().pollDelay(1, TimeUnit.MINUTES).pollInterval(20, TimeUnit.SECONDS)
//                .atMost(4, TimeUnit.MINUTES).until(ccInstance.isHealthy());
    }

    @AfterTest
    public void stop() {
        ccInstance.stop();
    }
}
