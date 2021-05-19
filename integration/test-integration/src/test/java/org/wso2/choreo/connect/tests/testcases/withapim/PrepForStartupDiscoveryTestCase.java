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
package org.wso2.choreo.connect.tests.testcases.withapim;

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;

/**
 * APIs, Apps, Subs created here will be used to test whether
 * resources that existed in APIM were pulled by CC during startup
 * (in StartupDiscoveryTestCase). This class must run before CcWithControlPlaneEnabled
 */
public class PrepForStartupDiscoveryTestCase extends ApimBaseTest {
    /**
     * Initialize the clients in the super class and create APIs, Apps, Subscriptions etc.
     */
    @BeforeTest
    private void createApiAppSubsEtc() throws Exception {
        super.initWithSuperTenant();
        ApimResourceProcessor apimResourceProcessor = new ApimResourceProcessor();
        apimResourceProcessor.createApisAppsSubs(user.getUserName(), publisherRestClient, storeRestClient);
    }
}
