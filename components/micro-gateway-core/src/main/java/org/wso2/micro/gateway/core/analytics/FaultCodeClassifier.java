/*
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.micro.gateway.core.analytics;

import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategories;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.micro.gateway.core.Constants;

/**
 * Classify faulty codes
 */
public class FaultCodeClassifier {

    private final int errorCode;

    public FaultCodeClassifier(int errorCode) {
        this.errorCode = errorCode;
    }

    public FaultSubCategory getFaultSubCategory(FaultCategory faultCategory) {
        switch (faultCategory) {
            case AUTH:
                return getAuthFaultSubCategory();
            case TARGET_CONNECTIVITY:
                return getTargetFaultSubCategory();
            case THROTTLED:
                return getThrottledFaultSubCategory();
            case OTHER:
                return getOtherFaultSubCategory();
        }
        return null;
    }

    protected FaultSubCategory getAuthFaultSubCategory() {
        switch (errorCode) {
            case Constants.APISecurityConstants.API_AUTH_GENERAL_ERROR:
            case Constants.APISecurityConstants.API_AUTH_INVALID_CREDENTIALS:
            case Constants.APISecurityConstants.API_AUTH_MISSING_CREDENTIALS:
            case Constants.APISecurityConstants.API_AUTH_ACCESS_TOKEN_EXPIRED:
            case Constants.APISecurityConstants.API_AUTH_ACCESS_TOKEN_INACTIVE:
                return FaultSubCategories.Authentication.AUTHENTICATION_FAILURE;
            case Constants.APISecurityConstants.API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE:
            case Constants.APISecurityConstants.INVALID_SCOPE:
            case Constants.APISecurityConstants.API_AUTH_INCORRECT_API_RESOURCE:
                return FaultSubCategories.Authentication.AUTHORIZATION_FAILURE;
            case Constants.APISecurityConstants.API_BLOCKED:
            case Constants.APISecurityConstants.API_AUTH_FORBIDDEN:
            case Constants.APISecurityConstants.SUBSCRIPTION_INACTIVE:
                return FaultSubCategories.Authentication.SUBSCRIPTION_VALIDATION_FAILURE;
            default:
                return FaultSubCategories.Authentication.OTHER;
        }
    }

    protected FaultSubCategory getTargetFaultSubCategory() {
        switch (errorCode) {
            case Constants.TargetConnectivityConstants.HTTP_CONNECTION_TIMEOUT:
                return FaultSubCategories.TargetConnectivity.CONNECTION_TIMEOUT;
            case Constants.TargetConnectivityConstants.BACKEND_CONNECTION_ERROR:
            case Constants.TargetConnectivityConstants.MALFORMED_URL:
            default:
                return FaultSubCategories.TargetConnectivity.OTHER;
        }
    }

    protected FaultSubCategory getThrottledFaultSubCategory() {
        switch (errorCode) {
            case Constants.APIThrottleConstants.API_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.API_LEVEL_LIMIT_EXCEEDED;
            case Constants.APIThrottleConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.RESOURCE_LEVEL_LIMIT_EXCEEDED;
            case Constants.APIThrottleConstants.APPLICATION_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.APPLICATION_LEVEL_LIMIT_EXCEEDED;
            case Constants.APIThrottleConstants.SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.SUBSCRIPTION_LIMIT_EXCEEDED;
            case Constants.APIThrottleConstants.BLOCKED_ERROR_CODE:
                return FaultSubCategories.Throttling.BLOCKED;
            case Constants.APIThrottleConstants.CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.CUSTOM_POLICY_LIMIT_EXCEEDED;
            default:
                return FaultSubCategories.Throttling.OTHER;
        }
    }

    protected FaultSubCategory getOtherFaultSubCategory() {
        return FaultSubCategories.Other.UNCLASSIFIED;
    }
}
