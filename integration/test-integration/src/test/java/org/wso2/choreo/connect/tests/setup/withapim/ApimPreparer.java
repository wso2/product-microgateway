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

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.ChoreoConnectImpl;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

/**
 * This class creates APIs, Apps, Subscriptions in API-M which are then reflected in Choreo Connect.
 * ApiRequests to create APIs, and custom json objects to create Apps and Subscriptions are read from
 * the relevant folder from integration/test-integration/src/test/resources/apim depending on the parameter
 * apimArtifactsIndex
 */
public class ApimPreparer extends ApimBaseTest {
    /**
     * Initialize the clients in the super class and create APIs, Apps, Subscriptions etc.
     */
    @Test
    @Parameters("apimArtifactsIndex")
    private void createApiAppSubsEtc(String apimArtifactsIndex) throws Exception {
        super.initWithSuperTenant();
        // The tests can be run against the same API Manager instance. Therefore, we clean first
        // in case the tests get interrupted before it ends in the previous run
        StoreUtils.removeAllSubscriptionsAndAppsFromStore(storeRestClient);
        PublisherUtils.removeAllApisFromPublisher(publisherRestClient);

        ApimResourceProcessor apimResourceProcessor = new ApimResourceProcessor(apimArtifactsIndex, user.getUserName(),
                adminRestClient, publisherRestClient, storeRestClient);
        apimResourceProcessor.populateApiManager();

        if(ChoreoConnectImpl.checkCCInstanceHealth()) {
            //wait till all resources deleted and are redeployed
            Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME*8, "Interrupted while waiting for DELETE and" +
                    " CREATE events to be deployed");
        }
    }
}
