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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.ApimInstance;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ApimRestartExecutor {
    CcInstance ccInstance;
    ApimInstance apimInstance;

    @Test
    public void restartApim() throws CCTestException, IOException {
        ccInstance = CcInstance.getInstance();
        apimInstance = ApimInstance.getInstance();
        ccInstance.stop();
        apimInstance.stopAPIM();
        Utils.delay(10000, "Interrupted while waiting for " +
                "the API Manager to shut down");
        apimInstance.startAPIM();
        ccInstance.start();
        Awaitility.await().pollDelay(1, TimeUnit.MINUTES).pollInterval(5, TimeUnit.SECONDS)
                .atMost(4, TimeUnit.MINUTES).until(ccInstance.isHealthy());
        Assert.assertTrue(ccInstance.checkCCInstanceHealth());
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for " +
                "resources to be pulled from API Manager");
    }
}
