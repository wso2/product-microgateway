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

package org.wso2.micro.gateway.enforcer.api;

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
public class ResponseObject{
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

    public Struct getMetadataStruct(){
        Struct.Builder structBuilder = Struct.newBuilder();
        return structBuilder.putFields(APIConstants.WEBSOCKET_STREAM_ID, Value.newBuilder().setStringValue(UUID.randomUUID().toString()).build())
                .putFields(APIConstants.GW_API_NAME_PARAM, Value.newBuilder().setStringValue(apiConfig.getName()).build())
                .putFields(APIConstants.GW_VERSION_PARAM, Value.newBuilder().setStringValue(apiConfig.getVersion()).build())
                .putFields(APIConstants.GW_BASE_PATH_PARAM, Value.newBuilder().setStringValue(apiConfig.getBasePath()).build()).build();
//                .putFields(AuthenticationConstants.USERNAME, Value.newBuilder().setStringValue(authenticationContext.getUsername()).build())
//                .putFields(AuthenticationConstants.APP_TIER, Value.newBuilder().setStringValue(authenticationContext.getApplicationTier()).build())
//                .putFields(AuthenticationConstants.TIER, Value.newBuilder().setStringValue(authenticationContext.getTier()).build())
//                .putFields(AuthenticationConstants.API_TIER, Value.newBuilder().setStringValue(authenticationContext.getApiTier()).build())
//                .putFields(AuthenticationConstants.CONTENT_AWARE_TIER_PRESENT, Value.newBuilder().setBoolValue(authenticationContext.isContentAwareTierPresent()).build())
//                .putFields(AuthenticationConstants.API_KEY, Value.newBuilder().setStringValue(authenticationContext.getApiKey()).build())
//                .putFields(AuthenticationConstants.KEY_TYPE, Value.newBuilder().setStringValue(authenticationContext.getKeyType()).build())
//                .putFields(AuthenticationConstants.CALLER_TOKEN, Value.newBuilder().setStringValue(authenticationContext.getCallerToken()).build())
//                .putFields(AuthenticationConstants.APP_ID, Value.newBuilder().setStringValue(authenticationContext.getApplicationId()).build())
//                .putFields(AuthenticationConstants.APP_NAME, Value.newBuilder().setStringValue(authenticationContext.getApplicationName()).build())
//                .putFields(AuthenticationConstants.CONSUMER_KEY, Value.newBuilder().setStringValue(authenticationContext.getConsumerKey()).build())
//                .putFields(AuthenticationConstants.SUBSCRIBER, Value.newBuilder().setStringValue(authenticationContext.getSubscriber()).build())
//                .putFields(AuthenticationConstants.SPIKE_ARREST_LIMIT, Value.newBuilder().setNumberValue(authenticationContext.getSpikeArrestLimit()).build())
//                .putFields(AuthenticationConstants.SUBSCRIBER_TENANT_DOMAIN, Value.newBuilder().setStringValue(authenticationContext.getSubscriberTenantDomain()).build())
//                .putFields(AuthenticationConstants.SPIKE_ARREST_UNIT, Value.newBuilder().setStringValue(authenticationContext.getSpikeArrestUnit()).build())
//                .putFields(AuthenticationConstants.STOP_ON_QUOTA, Value.newBuilder().setBoolValue(authenticationContext.isStopOnQuotaReach()).build())
//                .putFields(AuthenticationConstants.PRODUCT_NAME, Value.newBuilder().setStringValue(authenticationContext.getProductName()).build())
//                .putFields(AuthenticationConstants.PRODUCT_PROVIDER, Value.newBuilder().setStringValue(authenticationContext.getProductProvider()).build())
//                .putFields(AuthenticationConstants.API_PUBLISHER, Value.newBuilder().setStringValue(authenticationContext.getApiPublisher()).build()).build();
    }
}
