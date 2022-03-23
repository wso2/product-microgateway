/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.apim.utils;

import org.wso2.am.integration.clients.admin.api.AdvancedPolicyCollectionApi;
import org.wso2.am.integration.clients.admin.api.ApplicationPolicyCollectionApi;
import org.wso2.am.integration.clients.admin.api.SubscriptionPolicyCollectionApi;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyListDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyListDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyListDTO;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.util.List;

public class AdminUtils {

    /**
     * Get all Advanced Throttling Policies from Admin Portal.
     *
     * @param adminRestClient an instance of RestAPIAdminImpl
     * @return list of all Advanced Throttling Policies
     * @throws CCTestException if an error occurs while retrieving Advanced Throttling Policies
     */
    public static List<AdvancedThrottlePolicyInfoDTO> getAllAdvancedThrottlingPolicies(RestAPIAdminImpl adminRestClient)
            throws CCTestException {
        // Currently, RestAPIAdminImpl does not have a method to get all AdvancedThrottlingPolicies.
        // This is a workaround to use a method that is used internally by RestAPIAdminImpl.
        AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
        advancedPolicyCollectionApi.setApiClient(adminRestClient.apiAdminClient);
        AdvancedThrottlePolicyListDTO advancedThrottlePolicyListDTO;
        try {
            advancedThrottlePolicyListDTO = advancedPolicyCollectionApi
                    .throttlingPoliciesAdvancedGet(Constants.APPLICATION_JSON, null, null);
        } catch (ApiException e) {
            throw new CCTestException("Error while getting all advanced throttling policies", e);
        }

        if (advancedThrottlePolicyListDTO.getList() != null) {
            return advancedThrottlePolicyListDTO.getList();
        } else {
            throw new CCTestException("Received null as advanced throttling policy list");
        }
    }

    /**
     * Get all Application Throttling Policies from Admin Portal.
     *
     * @param adminRestClient an instance of RestAPIAdminImpl
     * @return list of all Application Throttling Policies
     * @throws CCTestException if an error occurs while retrieving Application Throttling Policies
     */
    public static List<ApplicationThrottlePolicyDTO> getAllApplicationThrottlingPolicies(RestAPIAdminImpl adminRestClient)
            throws CCTestException {
        // Currently, RestAPIAdminImpl does not have a method to get all ApplicationThrottlingPolicies.
        // This is a workaround to use a method that is used internally by RestAPIAdminImpl.
        ApplicationPolicyCollectionApi applicationPolicyCollectionApi = new ApplicationPolicyCollectionApi();
        applicationPolicyCollectionApi.setApiClient(adminRestClient.apiAdminClient);
        ApplicationThrottlePolicyListDTO applicationThrottlePolicyListDTO;
        try {
            applicationThrottlePolicyListDTO = applicationPolicyCollectionApi
                    .throttlingPoliciesApplicationGet(Constants.APPLICATION_JSON, null, null);
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            throw new CCTestException("Error while getting all application throttling policies", e);
        }

        if (applicationThrottlePolicyListDTO.getList() != null) {
            return applicationThrottlePolicyListDTO.getList();
        } else {
            throw new CCTestException("Received null as application throttling policy list");
        }
    }

    /**
     * Get all Subscription Throttling Policies from Admin Portal.
     *
     * @param adminRestClient an instance of RestAPIAdminImpl
     * @return list of all Subscription Throttling Policies
     * @throws CCTestException if an error occurs while retrieving Subscription Throttling Policies
     */
    public static List<SubscriptionThrottlePolicyDTO> getAllSubscriptionThrottlingPolicies(
            RestAPIAdminImpl adminRestClient) throws CCTestException {
        // Currently, RestAPIAdminImpl does not have a method to get all SubscriptionThrottlingPolicies.
        // This is a workaround to use a method that is used internally by RestAPIAdminImpl.
        SubscriptionPolicyCollectionApi subscriptionPolicyCollectionApi = new SubscriptionPolicyCollectionApi();
        subscriptionPolicyCollectionApi.setApiClient(adminRestClient.apiAdminClient);
        SubscriptionThrottlePolicyListDTO subscriptionThrottlePolicyListDTO;
        try {
            subscriptionThrottlePolicyListDTO = subscriptionPolicyCollectionApi
                    .throttlingPoliciesSubscriptionGet(Constants.APPLICATION_JSON, null, null);
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            throw new CCTestException("Error while getting all subscription throttling policies", e);
        }

        if (subscriptionThrottlePolicyListDTO.getList() != null) {
            return subscriptionThrottlePolicyListDTO.getList();
        } else {
            throw new CCTestException("Received null as subscription throttling policy list");
        }
    }
}
