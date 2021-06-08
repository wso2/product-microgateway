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

import org.testng.annotations.BeforeTest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

/**
 * APIs, Apps, Subs created here will be used to test whether
 * resources that existed in APIM were pulled by CC during startup
 * (in StartupDiscoveryTestCase). This class must run before CcStartupExecutor
 */
public class ApimPreparer extends ApimBaseTest {
    /**
     * Initialize the clients in the super class and create APIs, Apps, Subscriptions etc.
     */
    @BeforeTest
    private void createApiAppSubsEtc() throws Exception {
        super.initWithSuperTenant();
        // Here, there is a reason we clean first: Within the test tag "apis-apps-subs-received-via-eventhub", we
        // not only test the "CREATE" events, but also the "DELETE" events
        StoreUtils.removeAllSubscriptionsAndAppsFromStore(storeRestClient);
        PublisherUtils.removeAllApisFromPublisher(publisherRestClient);

        ApimResourceProcessor apimResourceProcessor = new ApimResourceProcessor();
        apimResourceProcessor.createApisAppsSubs(user.getUserName(), publisherRestClient, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for DELETE and" +
                " CREATE events to be deployed");
    }
}
