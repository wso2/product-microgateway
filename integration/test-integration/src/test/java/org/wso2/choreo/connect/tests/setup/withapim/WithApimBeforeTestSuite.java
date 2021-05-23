/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.wso2.choreo.connect.tests.context.ApimInstance;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * This class starts the API Manager instance before the entire test suite.
 */
public class WithApimBeforeTestSuite {
    ApimInstance apimInstance;

    @BeforeSuite(description = "start API Manager")
    void startAPIM() throws Exception {
        apimInstance = new ApimInstance();
        apimInstance.startAPIM();
        Awaitility.await().pollDelay(2, TimeUnit.MINUTES).pollInterval(15, TimeUnit.SECONDS)
                .atMost(4, TimeUnit.MINUTES).until(isAPIMServerStarted());
    }

    @AfterSuite(description = "stop API Manager")
    public void stopAPIM() {
        apimInstance.stopAPIM();
    }

    private Callable<Boolean> isAPIMServerStarted() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return checkForAPIMServerStartup();
            }
        };
    }

    private Boolean checkForAPIMServerStartup() throws IOException {
        HttpResponse response = HttpClientRequest.doGet(Utils.getAPIMServiceURLHttp("/services/Version"));
        return Objects.nonNull(response) && response.getResponseCode() == 200;
    }
}
