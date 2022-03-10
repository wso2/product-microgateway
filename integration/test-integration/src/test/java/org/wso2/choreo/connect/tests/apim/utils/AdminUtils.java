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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.AdvancedPolicyCollectionApi;
import org.wso2.am.integration.clients.admin.api.ApplicationPolicyCollectionApi;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyListDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyListDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.util.List;

public class AdminUtils {
    private static final Logger log = LoggerFactory.getLogger(AdminUtils.class);

    public static List<AdvancedThrottlePolicyInfoDTO> getAllAdvancedThrottlingPolicies(RestAPIAdminImpl adminRestClient) throws CCTestException {
        // Currently, RestAPIAdminImpl does not have a method to get all AdvancedThrottlingPolicies.
        // This is a workaround to use a method that is used internally by RestAPIAdminImpl.
        AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
        advancedPolicyCollectionApi.setApiClient(adminRestClient.apiAdminClient);
        ApiResponse<AdvancedThrottlePolicyListDTO> advancedThrottlePolicyListDTOApiResponse;
        try {
            advancedThrottlePolicyListDTOApiResponse = advancedPolicyCollectionApi
                    .throttlingPoliciesAdvancedGetWithHttpInfo(Constants.APPLICATION_JSON, null, null);
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            throw new CCTestException("Error while getting all advanced throttling policies", e);
        }

        if (advancedThrottlePolicyListDTOApiResponse == null || advancedThrottlePolicyListDTOApiResponse.getData() == null) {
            throw new CCTestException("Received null response when getting all advanced throttling policies");
        }

        AdvancedThrottlePolicyListDTO advancedThrottlePolicyListDTO = advancedThrottlePolicyListDTOApiResponse.getData();
        if (advancedThrottlePolicyListDTO.getList() != null) {
            return advancedThrottlePolicyListDTO.getList();
        } else {
            throw new CCTestException("Received null as advanced throttling policy list");
        }
    }

    public static List<ApplicationThrottlePolicyDTO> getAllApplicationThrottlingPolicies(RestAPIAdminImpl adminRestClient)
            throws CCTestException {
        // Currently, RestAPIAdminImpl does not have a method to get all ApplicationThrottlingPolicies.
        // This is a workaround to use a method that is used internally by RestAPIAdminImpl.
        ApplicationPolicyCollectionApi applicationPolicyCollectionApi = new ApplicationPolicyCollectionApi();
        applicationPolicyCollectionApi.setApiClient(adminRestClient.apiAdminClient);
        ApiResponse<ApplicationThrottlePolicyListDTO> applicationThrottlePolicyListDTOApiResponse;
        try {
            applicationThrottlePolicyListDTOApiResponse = applicationPolicyCollectionApi
                    .throttlingPoliciesApplicationGetWithHttpInfo(Constants.APPLICATION_JSON, null, null);
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            throw new CCTestException("Error while getting all advanced throttling policies", e);
        }

        if (applicationThrottlePolicyListDTOApiResponse == null || applicationThrottlePolicyListDTOApiResponse.getData() == null) {
            throw new CCTestException("Received null response when getting all advanced throttling policies");
        }

        ApplicationThrottlePolicyListDTO applicationThrottlePolicyListDTO = applicationThrottlePolicyListDTOApiResponse.getData();
        if (applicationThrottlePolicyListDTO.getList() != null) {
            return applicationThrottlePolicyListDTO.getList();
        } else {
            throw new CCTestException("Received null as advanced throttling policy list");
        }
    }
}
