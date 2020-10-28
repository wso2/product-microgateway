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

package org.wso2.micro.gateway.filter.core.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represent the API key validation Info DTO.
 */
public class APIKeyValidationInfoDTO implements Serializable {

    private static final long serialVersionUID = 12345L;

    private boolean authorized;
    private String subscriber;
    private String tier;
    private String type;
    //isContentAware property is here to notify if there is at least one content based tier associated with request
    //If this property is true then throttle handler should build message or get content length and pass it to
    //throttle server.
    private boolean contentAware;
    //Form API Manager 2.0 onward API specific tiers can define and this property is here to pass it.
    private String apiTier;
    //JWT or SAML token containing details of API invoker
    private String userType;
    private String endUserToken;
    private String endUserName;
    private String applicationId;
    private String applicationName;
    private String applicationTier;
    //use this to pass key validation status
    private int validationStatus;
    private long validityPeriod;
    private long issuedTime;
    private List<String> authorizedDomains;
    //Following throttle data list can be use to hold throttle data and api level throttle key
    //should be its first element.
    private List<String> throttlingDataList;
    private int spikeArrestLimit;
    private String subscriberTenantDomain;
    private String spikeArrestUnit;
    private boolean stopOnQuotaReach;
    //keeps productId of product for which the key was validated, if key was validated for an api this will be null
    private String productName;
    private String productProvider;
    private String keyManager;
    private int graphQLMaxDepth;
    private int graphQLMaxComplexity;
    private String apiVersion;
    private String applicationUUID;
    private Map<String, String> appAttributes;

    public List<String> getThrottlingDataList() {
        return throttlingDataList;
    }

    public void setThrottlingDataList(List<String> throttlingDataList) {
        this.throttlingDataList = throttlingDataList;
    }

    public String getApiTier() {
        return apiTier;
    }

    public void setApiTier(String apiTier) {
        this.apiTier = apiTier;
    }

    public boolean isContentAware() {
        return contentAware;
    }

    public void setContentAware(boolean contentAware) {
        this.contentAware = contentAware;
    }

    private Set<String> scopes;

    private String apiName;

    private String consumerKey;

    private String apiPublisher;

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndUserToken() {
        return endUserToken;
    }

    public void setEndUserToken(String endUserToken) {
        this.endUserToken = endUserToken;
    }

    public String getEndUserName() {
        return endUserName;
    }

    public void setEndUserName(String endUserName) {
        this.endUserName = endUserName;
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

    public String getApplicationTier() {
        return applicationTier;
    }

    public void setApplicationTier(String applicationTier) {
        this.applicationTier = applicationTier;
    }

    public int getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(int validationStatus) {
        this.validationStatus = validationStatus;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public long getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(long issuedTime) {
        this.issuedTime = issuedTime;
    }

    public List<String> getAuthorizedDomains() {
        return authorizedDomains;
    }

    public void setAuthorizedDomains(List<String> authorizedDomains) {
        this.authorizedDomains = authorizedDomains;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getApiPublisher() {
        return apiPublisher;
    }

    public void setApiPublisher(String apiPublisher) {
        this.apiPublisher = apiPublisher;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String toString() {

        StringBuilder builder = new StringBuilder(20);
        builder.append("APIKeyValidationInfoDTO = { authorized:").append(authorized).
                append(" , subscriber:").append(subscriber).
                append(" , tier:").append(tier).
                append(" , type:").append(type).
                append(" , userType:").append(userType).
                append(" , endUserToken:").append(endUserToken).
                append(" , endUserName:").append(endUserName).
                append(" , applicationId:").append(applicationId).
                append(" , applicationName:").append(applicationName).
                append(" , applicationTier:").append(applicationTier).
                append(" , validationStatus:").append(validationStatus).
                append(" , validityPeriod:").append(validityPeriod).
                append(" , issuedTime:").append(issuedTime).
                append(" , apiName:").append(apiName).
                append(" , consumerKey:").append(consumerKey).
                append(" , spikeArrestLimit:").append(spikeArrestLimit).
                append(" , spikeArrestUnit:").append(spikeArrestUnit).
                append(" , subscriberTenantDomain:").append(subscriberTenantDomain).
                append(" , stopOnQuotaReach:").append(stopOnQuotaReach).
                append(" , productName:").append(productName).
                append(" , productProvider:").append(productProvider).
                append(" , apiPublisher:").append(apiPublisher).
                append(" , graphQLMaxDepth:").append(graphQLMaxDepth).
                append(" , graphQLMaxComplexity:").append(graphQLMaxComplexity);

        if (authorizedDomains != null && !authorizedDomains.isEmpty()) {
            builder.append(" , authorizedDomains:[");
            for (String domain : authorizedDomains) {
                builder.append(domain).append(',');
            }
            builder.replace(builder.length() - 1, builder.length() - 1, "]");
        } else {
            builder.append(']');
        }

        if (scopes != null && !scopes.isEmpty()) {
            builder.append(" , scopes:[");
            for (String scope : scopes) {
                builder.append(scope).append(',');
            }
            builder.replace(builder.length() - 1, builder.length() - 1, "]");
        } else {
            builder.append(']');
        }

        return builder.toString();
    }

    public int getSpikeArrestLimit() {
        return spikeArrestLimit;
    }

    public void setSpikeArrestLimit(int spikeArrestLimit) {
        this.spikeArrestLimit = spikeArrestLimit;
    }

    public String getSpikeArrestUnit() {
        return spikeArrestUnit;
    }

    public void setSpikeArrestUnit(String spikeArrestUnit) {
        this.spikeArrestUnit = spikeArrestUnit;
    }

    public boolean isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public String getSubscriberTenantDomain() {
        return subscriberTenantDomain;
    }

    public void setSubscriberTenantDomain(String subscriberTenantDomain) {
        this.subscriberTenantDomain = subscriberTenantDomain;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductProvider(String productProvider) {
        this.productProvider = productProvider;
    }

    public String getProductProvider() {
        return productProvider;
    }

    public void setKeyManager(String keyManager) {

        this.keyManager = keyManager;
    }

    public String getKeyManager() {

        return keyManager;
    }

    public int getGraphQLMaxDepth() {
        return graphQLMaxDepth;
    }

    public void setGraphQLMaxDepth(int graphQLMaxDepth) {
        this.graphQLMaxDepth = graphQLMaxDepth;
    }

    public int getGraphQLMaxComplexity() {
        return graphQLMaxComplexity;
    }

    public void setGraphQLMaxComplexity(int graphQLMaxComplexity) {
        this.graphQLMaxComplexity = graphQLMaxComplexity;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApplicationUUID() {

        return applicationUUID;
    }

    public void setApplicationUUID(String applicationUUID) {

        this.applicationUUID = applicationUUID;
    }

    public Map<String, String> getAppAttributes() {

        return appAttributes;
    }

    public void setAppAttributes(Map<String, String> appAttributes) {

        this.appAttributes = appAttributes;
    }
}

