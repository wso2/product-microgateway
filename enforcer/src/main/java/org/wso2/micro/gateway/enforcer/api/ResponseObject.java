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

package org.wso2.micro.gateway.enforcer.api;

import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.AuthenticationConstants;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.util.Map;
import java.util.UUID;

/**
 * Holds the response data to build the response object to be sent to the envoy proxy.
 */
public class ResponseObject {
    private int statusCode;
    private String errorCode;
    private String errorMessage;
    private String errorDescription;
    private Map<String, String> headerMap;
    private boolean isDirectResponse = false;
    private AuthenticationContext authenticationContext;
    private APIConfig apiConfig;

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public boolean isDirectResponse() {
        return isDirectResponse;
    }

    public void setDirectResponse(boolean directResponse) {
        isDirectResponse = directResponse;
    }

    public AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    public void setAuthenticationContext(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    public APIConfig getApiConfig() {
        return apiConfig;
    }

    public void setApiConfig(APIConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    public Struct getMetadataStruct() {
        Struct.Builder structBuilder = Struct.newBuilder();
        return structBuilder.putFields(APIConstants.WEBSOCKET_STREAM_ID, Value.newBuilder()
                .setStringValue(UUID.randomUUID().toString()).build())
                .putFields(APIConstants.GW_API_NAME_PARAM, getNullableStringValue(apiConfig.getName()))
                .putFields(APIConstants.GW_VERSION_PARAM, getNullableStringValue(apiConfig.getVersion()))
                .putFields(APIConstants.GW_BASE_PATH_PARAM, getNullableStringValue(apiConfig.getBasePath()))
                .putFields(AuthenticationConstants.USERNAME,
                        getNullableStringValue(authenticationContext.getUsername()))
                .putFields(AuthenticationConstants.APP_TIER,
                        getNullableStringValue(authenticationContext.getApplicationTier()))
                .putFields(AuthenticationConstants.TIER, getNullableStringValue(authenticationContext.getTier()))
                .putFields(AuthenticationConstants.API_TIER, getNullableStringValue(authenticationContext.getApiTier()))
                .putFields(AuthenticationConstants.CONTENT_AWARE_TIER_PRESENT,
                        Value.newBuilder().setBoolValue(authenticationContext.isContentAwareTierPresent()).build())
                .putFields(AuthenticationConstants.API_KEY, getNullableStringValue(authenticationContext.getApiKey()))
                .putFields(AuthenticationConstants.KEY_TYPE, getNullableStringValue(authenticationContext.getKeyType()))
                .putFields(AuthenticationConstants.CALLER_TOKEN,
                        getNullableStringValue(authenticationContext.getCallerToken()))
                .putFields(AuthenticationConstants.APP_ID,
                        getNullableStringValue(authenticationContext.getApplicationId()))
                .putFields(AuthenticationConstants.APP_NAME,
                        getNullableStringValue(authenticationContext.getApplicationName()))
                .putFields(AuthenticationConstants.CONSUMER_KEY,
                        getNullableStringValue(authenticationContext.getConsumerKey()))
                .putFields(AuthenticationConstants.SUBSCRIBER,
                        getNullableStringValue(authenticationContext.getSubscriber()))
                .putFields(AuthenticationConstants.SPIKE_ARREST_LIMIT,
                        Value.newBuilder().setNumberValue(authenticationContext.getSpikeArrestLimit()).build())
                .putFields(AuthenticationConstants.SUBSCRIBER_TENANT_DOMAIN,
                        getNullableStringValue(authenticationContext.getSubscriberTenantDomain()))
                .putFields(AuthenticationConstants.SPIKE_ARREST_UNIT,
                        getNullableStringValue(authenticationContext.getSpikeArrestUnit()))
                .putFields(AuthenticationConstants.STOP_ON_QUOTA,
                        Value.newBuilder().setBoolValue(authenticationContext.isStopOnQuotaReach()).build())
                .putFields(AuthenticationConstants.PRODUCT_NAME,
                        getNullableStringValue(authenticationContext.getProductName()))
                .putFields(AuthenticationConstants.PRODUCT_PROVIDER,
                        getNullableStringValue(authenticationContext.getProductProvider()))
                .putFields(AuthenticationConstants.API_PUBLISHER,
                        getNullableStringValue(authenticationContext.getApiPublisher())).build();
    }

    private Value getNullableStringValue(String value) {
        if (value != null) {
            return Value.newBuilder().setStringValue(value).build();
        } else {
            return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        }
    }
}
