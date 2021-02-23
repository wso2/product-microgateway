/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.micro.gateway.enforcer.analytics;

/**
 * Response event data DTO
 */
public class ResponseEvent {
    private String correlationId;
    private String keyType;
    private String apiId;
    private String apiName;
    private String apiVersion;
    private String apiCreator;
    private String apiMethod;
    private String apiCreatorTenantDomain;
    private String apiResourceTemplate;
    private String destination;
    private String applicationId;
    private String applicationName;
    private String applicationOwner;
    private String regionId;
    private String gatewayType;
    private String userAgent;
    private String proxyResponseCode;
    private String targetResponseCode;
    private String responseCacheHit;
    private String responseLatency;
    private String backendLatency;
    private String requestMediationLatency;
    private String responseMediationLatency;
    private String deploymentId;
    private String eventType;
    private String requestTimeStamp;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiCreator() {
        return apiCreator;
    }

    public void setApiCreator(String apiCreator) {
        this.apiCreator = apiCreator;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public String getApiCreatorTenantDomain() {
        return apiCreatorTenantDomain;
    }

    public void setApiCreatorTenantDomain(String apiCreatorTenantDomain) {
        this.apiCreatorTenantDomain = apiCreatorTenantDomain;
    }

    public String getApiResourceTemplate() {
        return apiResourceTemplate;
    }

    public void setApiResourceTemplate(String apiResourceTemplate) {
        this.apiResourceTemplate = apiResourceTemplate;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationOwner() {
        return applicationOwner;
    }

    public void setApplicationOwner(String applicationOwner) {
        this.applicationOwner = applicationOwner;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getProxyResponseCode() {
        return proxyResponseCode;
    }

    public void setProxyResponseCode(String proxyResponseCode) {
        this.proxyResponseCode = proxyResponseCode;
    }

    public String getResponseCacheHit() {
        return responseCacheHit;
    }

    public void setResponseCacheHit(String responseCacheHit) {
        this.responseCacheHit = responseCacheHit;
    }

    public String getResponseLatency() {
        return responseLatency;
    }

    public void setResponseLatency(String responseLatency) {
        this.responseLatency = responseLatency;
    }

    public String getBackendLatency() {
        return backendLatency;
    }

    public void setBackendLatency(String backendLatency) {
        this.backendLatency = backendLatency;
    }

    public String getRequestMediationLatency() {
        return requestMediationLatency;
    }

    public void setRequestMediationLatency(String requestMediationLatency) {
        this.requestMediationLatency = requestMediationLatency;
    }

    public String getResponseMediationLatency() {
        return responseMediationLatency;
    }

    public void setResponseMediationLatency(String responseMediationLatency) {
        this.responseMediationLatency = responseMediationLatency;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTargetResponseCode() {
        return targetResponseCode;
    }

    public void setTargetResponseCode(String targetResponseCode) {
        this.targetResponseCode = targetResponseCode;
    }

    public String getRequestTimeStamp() {
        return requestTimeStamp;
    }

    public void setRequestTimeStamp(String requestTimeStamp) {
        this.requestTimeStamp = requestTimeStamp;
    }
}
