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
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.ApimInstance;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ApimRestartExecutor {

    @Test
    public void restartApim() throws CCTestException {
        ApimInstance apimInstance = ApimInstance.getInstance();
        apimInstance.restartAPIM();
        Awaitility.await().pollDelay(1, TimeUnit.MINUTES).pollInterval(15, TimeUnit.SECONDS)
                .atMost(4, TimeUnit.MINUTES).until(isAPIMServerStarted());
        Assert.assertTrue(true); //to make this method run
    }

    private Callable<Boolean> isAPIMServerStarted() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return ApimInstance.checkForAPIMServerStartup();
            }
        };
    }
}
