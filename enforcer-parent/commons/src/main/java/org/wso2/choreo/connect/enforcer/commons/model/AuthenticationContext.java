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
package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.List;

/**
 * AuthenticationContext contains the details populated after applying authentication filter.
 */
public class AuthenticationContext {
    private boolean authenticated;
    private String username;
    private String applicationTier;
    private String tier;
    private boolean isContentAwareTierPresent;
    private String apiKey;
    private String keyType;
    private String callerToken;
    private int applicationId;
    private String applicationUUID;
    private String applicationName;
    private String consumerKey;
    private String subscriber;
    private List<String> throttlingDataList;
    private int spikeArrestLimit;
    private String subscriberTenantDomain;
    private String spikeArrestUnit;
    private boolean stopOnQuotaReach;
    private String productName;
    private String productProvider;
    private String apiName;
    private String apiPublisher;
    private String apiVersion;
    private String apiUUID;
    private String rawToken;
    private String tokenType;

    public static final String UNKNOWN_VALUE = "__unknown__";

    public AuthenticationContext() {
        this.apiPublisher = UNKNOWN_VALUE;
        this.applicationUUID = UNKNOWN_VALUE;
        this.applicationId = -1;
        this.applicationName = UNKNOWN_VALUE;
        this.applicationTier = "Unlimited";
        this.consumerKey = UNKNOWN_VALUE;
        this.spikeArrestUnit = "";
        this.subscriber = UNKNOWN_VALUE;
        this.subscriberTenantDomain = UNKNOWN_VALUE;
    }

    public List<String> getThrottlingDataList() {
        return throttlingDataList;
    }

    public void setThrottlingDataList(List<String> throttlingDataList) {
        this.throttlingDataList = throttlingDataList;
    }
    //Following throttle data list can be use to hold throttle data and api level throttle key
    //should be its first element.

    /**
     * Returns true if there is a content aware throttling tier is present.
     *
     * @return true if there is a content aware tier is present.
     */
    public boolean isContentAwareTierPresent() {
        return isContentAwareTierPresent;
    }

    public void setIsContentAware(boolean isContentAware) {
        this.isContentAwareTierPresent = isContentAware;
    }

    /**
     * Get Subscriber of the relevant subscription.
     *
     * @return Subscriber
     */
    public String getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    // Username of the authenticated User

    /**
     * Username of the authenticated user.
     * @return User
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get Subscription level throttling tier.
     *
     * @return subscription throttling tier.
     */
    public String getTier() {
        return tier;
    }

    /**
     * If JWT token is used for authentication, JTI value can be retrieved.
     *
     * Note:
     *  If the API is unsecured, this would be assigned with client IP address.
     *
     * @return Token Identifier (JTI).
     */
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * Returns the key-type corresponding to the token.
     * It is one of the following,
     * "PRODUCTION" , "SANDBOX".
     *
     * @return
     */
    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    /**
     * Generated Backend JWT Token (If JWT generation is enabled)
     *
     * @return Backend JWT Token
     */
    public String getCallerToken() {
        return callerToken;
    }

    public void setCallerToken(String callerToken) {
        this.callerToken = callerToken;
    }

    /**
     * Get the assigned application throttling tier assigned for the matched application.
     * @return application throttling tier.
     */
    public String getApplicationTier() {
        return applicationTier;
    }

    public void setApplicationTier(String applicationTier) {
        this.applicationTier = applicationTier;
    }

    /**
     * Get the application ID for the matched application.
     *
     * @return
     */
    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Get the application Name for the matched application.
     * @return
     */
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Get the consumer Key for the matched application.
     *
     * @return Consumer Key
     */
    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    /**
     * Get Spike arrest Limit assigned for the matched subscription.
     *
     * @return Spike arrest limit.
     */
    public int getSpikeArrestLimit() {
        return spikeArrestLimit;
    }

    public void setSpikeArrestLimit(int spikeArrestLimit) {
        this.spikeArrestLimit = spikeArrestLimit;
    }

    /**
     * Get Subscriber Tenant Domain for the matched subscription.
     *
     * @return Subscriber Tenant Domain
     */
    public String getSubscriberTenantDomain() {
        return subscriberTenantDomain;
    }

    public void setSubscriberTenantDomain(String subscriberTenantDomain) {
        this.subscriberTenantDomain = subscriberTenantDomain;
    }

    /**
     * Get Spike Arrest Time Unit if provided under the matched subscription.
     * (Currently not supported in Throttle Filter)
     *
     * @return Spike Arrest Time Unit
     */
    public String getSpikeArrestUnit() {
        return spikeArrestUnit;
    }

    public void setSpikeArrestUnit(String spikeArrestUnit) {
        this.spikeArrestUnit = spikeArrestUnit;
    }

    /**
     * Returns true if stop On Quota Reach is set to true for the matched subscription policy.
     * (Currently not supported in Throttle Filter)
     *
     * @return true if stop On Quota Reach is set to true
     */
    public boolean isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(boolean stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Matched API Product Name (If the request is from API Product)
     *
     * @return API Product Name
     */
    public String getProductName() {
        return productName;
    }

    public void setProductProvider(String productProvider) {
        this.productProvider = productProvider;
    }

    /**
     * API Product Provider of the matched API.
     *
     * @return API Product provider.
     */
    public String getProductProvider() {
        return productProvider;
    }

    /**
     * API Name of the matched API.
     *
     * @return API Name
     */
    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    /**
     * API Publisher of the matched API.
     *
     * @return API Publisher
     */
    public String getApiPublisher() {
        return apiPublisher;
    }

    public void setApiPublisher(String apiPublisher) {
        this.apiPublisher = apiPublisher;
    }

    /**
     * API Version of the matched API
     *
     * @return API Version
     */
    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * API UUID of the corresponding API.
     *
     * @return API UUID
     */
    public String getApiUUID() {
        return apiUUID;
    }

    public void setApiUUID(String apiUUID) {
        this.apiUUID = apiUUID;
    }

    /**
     * Raw token used to authenticate the API
     * @return Raw token
     */
    public String getRawToken() {
        return rawToken;
    }

    public void setRawToken(String rawToken) {
        this.rawToken = rawToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getApplicationUUID() {
        return applicationUUID;
    }

    public void setApplicationUUID(String applicationUUID) {
        this.applicationUUID = applicationUUID;
    }
}
