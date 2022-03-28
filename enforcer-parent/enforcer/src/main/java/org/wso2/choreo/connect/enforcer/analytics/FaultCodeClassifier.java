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

package org.wso2.choreo.connect.enforcer.analytics;

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import io.envoyproxy.envoy.data.accesslog.v3.ResponseFlags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategories;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;

/**
 * FaultCodeClassifier classifies the fault and returns error code.
 */
public class FaultCodeClassifier {
    private static final Logger log = LogManager.getLogger(FaultCodeClassifier.class);
    private HTTPAccessLogEntry logEntry;
    private int errorCode;

    public FaultCodeClassifier(HTTPAccessLogEntry logEntry) {
        this.logEntry = logEntry;
        errorCode = getErrorCodeFromFlags();
    }

    public FaultCodeClassifier(int errorCode) {
        this.errorCode = errorCode;
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
        switch (errorCode) {
            case APISecurityConstants.API_AUTH_GENERAL_ERROR:
            case APISecurityConstants.API_AUTH_INVALID_CREDENTIALS:
            case APISecurityConstants.API_AUTH_MISSING_CREDENTIALS:
            case APISecurityConstants.API_AUTH_ACCESS_TOKEN_EXPIRED:
            case APISecurityConstants.API_AUTH_ACCESS_TOKEN_INACTIVE:
                return FaultSubCategories.Authentication.AUTHENTICATION_FAILURE;
            case APISecurityConstants.API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE:
            case APISecurityConstants.INVALID_SCOPE:
            case APISecurityConstants.OPA_AUTH_FORBIDDEN:
                return FaultSubCategories.Authentication.AUTHORIZATION_FAILURE;
            case GeneralErrorCodeConstants.API_BLOCKED_CODE:
            case APISecurityConstants.API_SUBSCRIPTION_BLOCKED:
            case APISecurityConstants.API_AUTH_FORBIDDEN:
            case APISecurityConstants.SUBSCRIPTION_INACTIVE:
                return FaultSubCategories.Authentication.SUBSCRIPTION_VALIDATION_FAILURE;
            default:
                return FaultSubCategories.Authentication.OTHER;
        }
    }

    protected FaultSubCategory getTargetFaultSubCategory() {
        switch (errorCode) {
            case AnalyticsConstants.NHTTP_CONNECTION_TIMEOUT:
            case AnalyticsConstants.NHTTP_CONNECT_TIMEOUT:
                return FaultSubCategories.TargetConnectivity.CONNECTION_TIMEOUT;
            default:
                return FaultSubCategories.TargetConnectivity.OTHER;
        }
    }

    protected FaultSubCategory getThrottledFaultSubCategory() {
        switch (errorCode) {
            case  AnalyticsConstants.API_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.API_LEVEL_LIMIT_EXCEEDED;
            case  AnalyticsConstants.HARD_LIMIT_EXCEEDED_ERROR_CODE:
                return FaultSubCategories.Throttling.HARD_LIMIT_EXCEEDED;
            case  AnalyticsConstants.RESOURCE_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.RESOURCE_LEVEL_LIMIT_EXCEEDED;
            case  AnalyticsConstants.APPLICATION_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.APPLICATION_LEVEL_LIMIT_EXCEEDED;
            case  AnalyticsConstants.SUBSCRIPTION_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.SUBSCRIPTION_LIMIT_EXCEEDED;
            case  AnalyticsConstants.BLOCKED_ERROR_CODE:
                return FaultSubCategories.Throttling.BLOCKED;
            case  AnalyticsConstants.CUSTOM_POLICY_THROTTLE_OUT_ERROR_CODE:
                return FaultSubCategories.Throttling.CUSTOM_POLICY_LIMIT_EXCEEDED;
            default:
                return FaultSubCategories.Throttling.OTHER;
        }
    }

    protected FaultSubCategory getOtherFaultSubCategory() {
        if (isMethodNotAllowed()) {
            errorCode = 405;
            return FaultSubCategories.Other.METHOD_NOT_ALLOWED;
        } else if (isResourceNotFound()) {
            errorCode = 404;
            return FaultSubCategories.Other.RESOURCE_NOT_FOUND;
        } else {
            return FaultSubCategories.Other.UNCLASSIFIED;
        }
    }

    public boolean isResourceNotFound() {
        // isResourceNotFound is not used when logEntry is not null, since 404 related events are published based on
        // logEntries (not based on requestContext)
        if (logEntry == null) {
            return false;
        }
        ResponseFlags responseFlags = logEntry.getCommonProperties().getResponseFlags();
        if (responseFlags == null) {
            return false;
        }
        return responseFlags.getNoRouteFound();
    }

    public boolean isMethodNotAllowed() {
        // Method not allowed event will be as same as resource not found in microgateway case
        // To implement method not allowed involves a design complexity as well as can cause a performance hit
        // to the enforcer because the method validation needs to be done at enforcer level if we are to support
        // this specifically.
        return false;
    }

    // TODO: (VirajSalaka) Following method will be reused with next release of envoy
//    private int getErrorCodeFromMetadata() {
//        int errorCode = -1;
//        if (logEntry.getCommonProperties().getMetadata() != null
//                && logEntry.getCommonProperties().getMetadata().getFilterMetadataMap() != null
//                && logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
//                .containsKey(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY)
//                && logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
//                .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
//                .get(MetadataConstants.ERROR_CODE_KEY) != null) {
//            errorCode = Integer.parseInt(logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
//                    .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
//                    .get(MetadataConstants.ERROR_CODE_KEY)
//                    .getStringValue());
//        }
//        return errorCode;
//    }

    private int getErrorCodeFromFlags() {
        if (logEntry == null || logEntry.getCommonProperties().getResponseFlags() == null) {
            return -1;
        }
        ResponseFlags responseFlags = logEntry.getCommonProperties().getResponseFlags();

        if (responseFlags.getFailedLocalHealthcheck() || responseFlags.getNoHealthyUpstream()) {
            return AnalyticsConstants.NHTTP_CONNECTION_FAILED;
        }
        if (responseFlags.getUpstreamRequestTimeout()) {
            return AnalyticsConstants.NHTTP_CONNECTION_TIMEOUT;
        }
        if (responseFlags.getLocalReset()) {
            return AnalyticsConstants.NHTTP_RECEIVER_INPUT_OUTPUT_ERROR_RECEIVING;
        }
        if (responseFlags.getUpstreamRemoteReset()) {
            return AnalyticsConstants.NHTTP_CONNECTION_FAILED;
        }
        if (responseFlags.getUpstreamConnectionFailure()) {
            return AnalyticsConstants.NHTTP_SENDER_INPUT_OUTPUT_ERROR_SENDING;
        }
        if (responseFlags.getUpstreamConnectionTermination()) {
            return AnalyticsConstants.NHTTP_CONNECTION_CLOSED;
        }
        if (responseFlags.getDownstreamConnectionTermination()) {
            return AnalyticsConstants.NHTTP_RECEIVER_INPUT_OUTPUT_ERROR_RECEIVING;
        }
        if (responseFlags.getStreamIdleTimeout()) {
            return AnalyticsConstants.NHTTP_CONNECTION_TIMEOUT;
        }
        if (responseFlags.getDownstreamProtocolError()) {
            return AnalyticsConstants.NHTTP_PROTOCOL_VIOLATION;
        }
        // https://www.envoyproxy.io/docs/envoy/latest/faq/configuration/timeouts
        if (responseFlags.getUpstreamMaxStreamDurationReached()) {
            return AnalyticsConstants.NHTTP_CONNECTION_TIMEOUT;
        }
        if (responseFlags.getDurationTimeout()) {
            return AnalyticsConstants.NHTTP_RECEIVER_INPUT_OUTPUT_ERROR_SENDING;
        }
        return -1;

        // UpstreamOverflow
        // DelayInjected
        // FaultInjected
        // RateLimited
        // UnauthorizedDetails
        // RateLimitServiceError
        // UpstreamRetryLimitExceeded
        // InvalidEnvoyRequestHeaders
        // ResponseFromCacheFilter
        // NoRouteFound // Not used under connectivity issues
    }

    public int getErrorCode() {
        return errorCode;
    }
}
