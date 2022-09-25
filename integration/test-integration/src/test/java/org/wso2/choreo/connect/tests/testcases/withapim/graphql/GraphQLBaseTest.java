/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.withapim.graphql;

import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;

/**
 * Consists methods relevant to the GraphQL test cases.
 */
public class GraphQLBaseTest extends ApimBaseTest {

    /**
     * Creates subscription policy for GraphQL API tests.
     *
     * @param subscriptionThrottlePolicyDTO policy DTP relevant to the subscription
     * @param queryDepth query depth value
     * @param queryComplexity query complexity value
     * @param policyName policy name for the subscription
     */
    protected void setSubscriptionThrottlePolicyDTO(SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO,
                                                    int queryDepth, int queryComplexity, String policyName) {
        subscriptionThrottlePolicyDTO.setPolicyName(policyName);
        subscriptionThrottlePolicyDTO.setDisplayName(policyName);
        subscriptionThrottlePolicyDTO.setDescription(policyName);
        subscriptionThrottlePolicyDTO.setRateLimitCount(1000);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("min");
        subscriptionThrottlePolicyDTO.setBillingPlan("COMMERCIAL");
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(true);
        subscriptionThrottlePolicyDTO.setIsDeployed(true);
        subscriptionThrottlePolicyDTO.setGraphQLMaxComplexity(queryComplexity);
        subscriptionThrottlePolicyDTO.setGraphQLMaxDepth(queryDepth);
        subscriptionThrottlePolicyDTO.setSubscriberCount(0);

        ThrottleLimitDTO throttleLimitDTO = new ThrottleLimitDTO();
        throttleLimitDTO.setType(ThrottleLimitDTO.TypeEnum.valueOf("REQUESTCOUNTLIMIT"));
        RequestCountLimitDTO requestCountLimitDTO = new RequestCountLimitDTO();
        requestCountLimitDTO.setRequestCount(Long.valueOf(1000));
        requestCountLimitDTO.setTimeUnit("min");
        requestCountLimitDTO.setUnitTime(10);
        throttleLimitDTO.setRequestCount(requestCountLimitDTO);

        subscriptionThrottlePolicyDTO.setDefaultLimit(throttleLimitDTO);
    }

    /**
     *
     * @param apiId API ID as a string
     * @param appName application name
     * @param tokenType token type
     * @return
     * @throws org.wso2.am.integration.clients.store.api.ApiException if an error occurs while creating app
     * @throws CCTestException if an error occurs while subscribing to the API
     */
    protected String createGraphqlAppAndSubscribeToAPI(String apiId, String appName, Enum tokenType) throws
            org.wso2.am.integration.clients.store.api.ApiException, CCTestException {
        ApplicationDTO application = new ApplicationDTO();
        application.setName(appName);
        application.setDescription("Simple app used for GQL scope tests");
        application.setThrottlingPolicy(TestConstant.APPLICATION_TIER.UNLIMITED);
        application.setTokenType((ApplicationDTO.TokenTypeEnum) tokenType);
        ApplicationDTO createdAppDTO = storeRestClient.applicationsApi.applicationsPost(application);
        StoreUtils.subscribeToAPI(apiId, createdAppDTO.getApplicationId(),
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        return createdAppDTO.getApplicationId();
    }
}
