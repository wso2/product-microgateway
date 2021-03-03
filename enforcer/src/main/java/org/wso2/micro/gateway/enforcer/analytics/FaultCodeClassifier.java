/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.enforcer.analytics;

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import io.envoyproxy.envoy.data.accesslog.v3.ResponseFlags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultSubCategories;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.micro.gateway.enforcer.constants.APISecurityConstants;

import java.util.HashMap;
import java.util.Map;

public class FaultCodeClassifier {
    private static final Logger log = LogManager.getLogger(FaultCodeClassifier.class);
    private final HTTPAccessLogEntry logEntry;
    private Map<String, Integer> flagErrorCodeMap = new HashMap<>(10);

    public FaultCodeClassifier(HTTPAccessLogEntry logEntry) {
        this.logEntry = logEntry;
    }

    public FaultSubCategory getFaultSubCategory(FaultCategory faultCategory) {
        switch (faultCategory) {
            case AUTH:
                return getAuthFaultSubCategory();
            case THROTTLED:
                return getThrottledFaultSubCategory();
            case TARGET_CONNECTIVITY:
                return getTargetFaultSubCategory();
            case OTHER:
                return getOtherFaultSubCategory();
        }
        return null;
    }

    protected FaultSubCategory getAuthFaultSubCategory() {
        int errorCode = getErrorCode();
        switch (errorCode) {
            case APISecurityConstants.API_AUTH_GENERAL_ERROR:
            case APISecurityConstants.API_AUTH_INVALID_CREDENTIALS:
            case APISecurityConstants.API_AUTH_MISSING_CREDENTIALS:
            case APISecurityConstants.API_AUTH_ACCESS_TOKEN_EXPIRED:
            case APISecurityConstants.API_AUTH_ACCESS_TOKEN_INACTIVE:
                return FaultSubCategories.Authentication.AUTHENTICATION_FAILURE;
            case APISecurityConstants.API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE:
            case APISecurityConstants.INVALID_SCOPE:
                return FaultSubCategories.Authentication.AUTHORIZATION_FAILURE;
            case APISecurityConstants.API_BLOCKED:
            case APISecurityConstants.API_AUTH_FORBIDDEN:
            case APISecurityConstants.SUBSCRIPTION_INACTIVE:
                return FaultSubCategories.Authentication.SUBSCRIPTION_VALIDATION_FAILURE;
            default:
                return FaultSubCategories.TargetConnectivity.OTHER;
        }
    }

    protected FaultSubCategory getTargetFaultSubCategory() {
        int errorCode = getErrorCode();
        switch (errorCode) {
//            case SynapseConstants.NHTTP_CONNECTION_TIMEOUT:
//            case SynapseConstants.NHTTP_CONNECT_TIMEOUT:
//                return FaultSubCategories.TargetConnectivity.CONNECTION_TIMEOUT;
//            case Constants.ENDPOINT_SUSPENDED_ERROR_CODE:
//                return FaultSubCategories.TargetConnectivity.CONNECTION_SUSPENDED;
            default:
                return FaultSubCategories.TargetConnectivity.OTHER;
        }
    }

    protected FaultSubCategory getThrottledFaultSubCategory() {
        // TODO: (VirajSalaka) Complete function body.
        int errorCode = getErrorCode();
        switch (errorCode) {
//            case APIThrottleConstants.API_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.API_LEVEL_LIMIT_EXCEEDED;
//            case APIThrottleConstants.HARD_LIMIT_EXCEEDED_ERROR_CODE:
//                return FaultSubCategories.Throttling.HARD_LIMIT_EXCEEDED;
//            case APIThrottleConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.RESOURCE_LEVEL_LIMIT_EXCEEDED;
//            case APIThrottleConstants.APPLICATION_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.APPLICATION_LEVEL_LIMIT_EXCEEDED;
//            case APIThrottleConstants.SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.SUBSCRIPTION_LIMIT_EXCEEDED;
//            case APIThrottleConstants.BLOCKED_ERROR_CODE:
//                return FaultSubCategories.Throttling.BLOCKED;
//            case APIThrottleConstants.CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.CUSTOM_POLICY_LIMIT_EXCEEDED;
//            case APIThrottleConstants.SUBSCRIPTION_BURST_THROTTLE_OUT_ERROR_CODE:
//                return FaultSubCategories.Throttling.BURST_CONTROL_LIMIT_EXCEEDED;
//            case APIThrottleConstants.GRAPHQL_QUERY_TOO_DEEP:
//                return FaultSubCategories.Throttling.QUERY_TOO_DEEP;
//            case APIThrottleConstants.GRAPHQL_QUERY_TOO_COMPLEX:
//                return FaultSubCategories.Throttling.QUERY_TOO_COMPLEX;
            default:
                return FaultSubCategories.Throttling.OTHER;
        }
    }

    protected FaultSubCategory getOtherFaultSubCategory() {
        if (isMethodNotAllowed()) {
            return FaultSubCategories.Other.METHOD_NOT_ALLOWED;
        } else if (isResourceNotFound()) {
            return FaultSubCategories.Other.RESOURCE_NOT_FOUND;
        } else {
            return FaultSubCategories.Other.UNCLASSIFIED;
        }
    }

    public boolean isResourceNotFound() {
//        if (messageContext.getPropertyKeySet().contains(SynapseConstants.ERROR_CODE)) {
//            int errorCode = (int) messageContext.getProperty(SynapseConstants.ERROR_CODE);
//            return messageContext.getPropertyKeySet().contains(RESTConstants.PROCESSED_API)
//                    && errorCode == Constants.RESOURCE_NOT_FOUND_ERROR_CODE;
//        }
        return false;
    }

    public boolean isMethodNotAllowed() {
//        if (messageContext.getPropertyKeySet().contains(SynapseConstants.ERROR_CODE)) {
//            int errorCode = (int) messageContext.getProperty(SynapseConstants.ERROR_CODE);
//            return messageContext.getPropertyKeySet().contains(RESTConstants.PROCESSED_API)
//                    && errorCode == Constants.METHOD_NOT_ALLOWED_ERROR_CODE;
//        }
        return false;
    }

    private int getErrorCode() {
        int errorCode = -1;
        // TODO: (VirajSalaka) Handle possible null pointer exception
        if (logEntry.getCommonProperties().getMetadata().getFilterMetadataMap().containsKey("ErrorCode")) {
            errorCode = (int) logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                    .get("envoy.filters.http.ext_authz").getFieldsMap().get("ErrorCode")
                    .getNumberValue();
        }
        return errorCode;
    }

    private int getErrorCodeFromFlags() {
//        ResponseFlags responseFlags = logEntry.getCommonProperties().getResponseFlags();
//        if (responseFlags == null) {
//            return -1;
//        }
//        if (responseFlags.getDownstreamConnectionTermination() || )
    }
}