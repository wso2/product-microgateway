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
import org.wso2.micro.gateway.enforcer.constants.MetadataConstants;

/**
 * FaultCodeClassifier classifies the fault and returns error code.
 */
public class FaultCodeClassifier {
    private static final Logger log = LogManager.getLogger(FaultCodeClassifier.class);
    private final HTTPAccessLogEntry logEntry;
    private int errorCode;

    public static final int NHTTP_CONNECTION_TIMEOUT = 101504;
    public static final int NHTTP_CONNECT_TIMEOUT = 101508;
    // TODO: (VirajSalaka) Not used
    public static final int ENDPOINT_SUSPENDED_ERROR_CODE = 303001;


    public FaultCodeClassifier(HTTPAccessLogEntry logEntry) {
        this.logEntry = logEntry;
        int errorCodeFromEntry = getErrorCodeFromMetadata();
        if (errorCodeFromEntry == -1) {
            errorCodeFromEntry = getErrorCodeFromFlags();
        }
        errorCode = errorCodeFromEntry;
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
        switch (getErrorCodeFromMetadata()) {
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
        switch (getErrorCodeFromMetadata()) {
            case NHTTP_CONNECTION_TIMEOUT:
            case NHTTP_CONNECT_TIMEOUT:
                return FaultSubCategories.TargetConnectivity.CONNECTION_TIMEOUT;
            case ENDPOINT_SUSPENDED_ERROR_CODE:
                return FaultSubCategories.TargetConnectivity.CONNECTION_SUSPENDED;
            default:
                return FaultSubCategories.TargetConnectivity.OTHER;
        }
    }

    protected FaultSubCategory getThrottledFaultSubCategory() {
        // TODO: (VirajSalaka) Complete function body.
        switch (getErrorCodeFromMetadata()) {
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
            errorCode = 404;
            return FaultSubCategories.Other.METHOD_NOT_ALLOWED;
        } else if (isResourceNotFound()) {
            errorCode = 405;
            return FaultSubCategories.Other.RESOURCE_NOT_FOUND;
        } else {
            return FaultSubCategories.Other.UNCLASSIFIED;
        }
    }

    public boolean isResourceNotFound() {
        ResponseFlags responseFlags = logEntry.getCommonProperties().getResponseFlags();
        if (responseFlags == null) {
            return false;
        }
        return responseFlags.getNoRouteFound();
    }

    public boolean isMethodNotAllowed() {
        // TODO: (VirajSalaka) Implement method not allowed
        return false;
    }

    private int getErrorCodeFromMetadata() {
        int errorCode = -1;
        // TODO: (VirajSalaka) Handle possible null pointer exception
        if (logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .containsKey(MetadataConstants.ERROR_CODE_KEY)) {
            errorCode = Integer.parseInt(logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                    .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
                    .get(MetadataConstants.ERROR_CODE_KEY)
                    .getStringValue());
        }
        return errorCode;
    }

    private int getErrorCodeFromFlags() {
//        // Indicates local server healthcheck failed.
//        FailedLocalHealthcheck
//        // Indicates there was no healthy upstream.
//        NoHealthyUpstream
//        // Indicates an there was an upstream request timeout.
//        UpstreamRequestTimeout
//        // Indicates local codec level reset was sent on the stream.
//        LocalReset
//        // Indicates remote codec level reset was received on the stream.
//        UpstreamRemoteReset
//        // Indicates there was a local reset by a connection pool due to an initial connection failure.
//        UpstreamConnectionFailure
//        // Indicates the stream was reset due to an upstream connection termination.
//        UpstreamConnectionTermination
//        // Indicates the stream was reset because of a resource overflow.
//        UpstreamOverflow
//        // Indicates no route was found for the request.
//        NoRouteFound
//        // Indicates that the request was delayed before proxying.
//        DelayInjected
//        // Indicates that the request was aborted with an injected error code.
//        FaultInjected
//        // Indicates that the request was rate-limited locally.
//        RateLimited
//        // Indicates if the request was deemed unauthorized and the reason for it.
//        UnauthorizedDetails
//        // Indicates that the request was rejected because there was an error in rate limit service.
//        RateLimitServiceError
//        // Indicates the stream was reset due to a downstream connection termination.
//        DownstreamConnectionTermination
//        // Indicates that the upstream retry limit was exceeded, resulting in a downstream error.
//        UpstreamRetryLimitExceeded
//        // Indicates that the stream idle timeout was hit, resulting in a downstream 408.
//        StreamIdleTimeout
//        // Indicates that the request was rejected because an envoy request header failed strict
//        // validation.
//        InvalidEnvoyRequestHeaders
//        // Indicates there was an HTTP protocol error on the downstream request.
//        DownstreamProtocolError
//        // Indicates there was a max stream duration reached on the upstream request.
//        UpstreamMaxStreamDurationReached
//        // Indicates the response was served from a cache filter.
//        ResponseFromCacheFilter
//        // Indicates that a filter configuration is not available.
//        NoFilterConfigFound
//        // Indicates that request or connection exceeded the downstream connection duration.
//        DurationTimeout

        ResponseFlags responseFlags = logEntry.getCommonProperties().getResponseFlags();
        if (responseFlags == null) {
            return -1;
        }
        if (responseFlags.getFailedLocalHealthcheck() || responseFlags.getNoHealthyUpstream()) {
            return 101503;
        }
        if (responseFlags.getUpstreamRequestTimeout()) {
            return 101504;
        }
        // TODO: (VirajSalaka) Confirm ?
        if (responseFlags.getLocalReset()) {
            return 101001;
        }
        // TODO: (VirajSalaka) Confirm ?
        if (responseFlags.getUpstreamRemoteReset()) {
            return 101503;
        }
        if (responseFlags.getUpstreamConnectionFailure()) {
            return 101500;
        }
        if (responseFlags.getUpstreamConnectionTermination()) {
            return 101505;
        }
        // TODO: (VirajSalaka) Decide if it is required to move it to somewhere else
        if (responseFlags.getNoRouteFound()) {
            return 900906;
        }
        if (responseFlags.getDownstreamConnectionTermination()) {
            return 101001;
        }
        if (responseFlags.getStreamIdleTimeout()) {
            return 101504;
        }
        if (responseFlags.getDownstreamProtocolError()) {
            return 101506;
        }
        // https://www.envoyproxy.io/docs/envoy/latest/faq/configuration/timeouts
        if (responseFlags.getUpstreamMaxStreamDurationReached()) {
            return 101504;
        }
        if (responseFlags.getDurationTimeout()) {
            return 101000;
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
    }

    public int getErrorCode() {
        return errorCode;
    }
}
