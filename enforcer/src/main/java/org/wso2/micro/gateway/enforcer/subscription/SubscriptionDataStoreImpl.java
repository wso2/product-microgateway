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

package org.wso2.micro.gateway.enforcer.subscription;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.subscription.APIs;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.discovery.ApiListDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.ApplicationDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.ApplicationKeyMappingDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.ApplicationPolicyDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.RevokedTokenDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.SubscriptionDiscoveryClient;
import org.wso2.micro.gateway.enforcer.discovery.SubscriptionPolicyDiscoveryClient;
import org.wso2.micro.gateway.enforcer.models.API;
import org.wso2.micro.gateway.enforcer.models.ApiPolicy;
import org.wso2.micro.gateway.enforcer.models.Application;
import org.wso2.micro.gateway.enforcer.models.ApplicationKeyMapping;
import org.wso2.micro.gateway.enforcer.models.ApplicationKeyMappingCacheKey;
import org.wso2.micro.gateway.enforcer.models.ApplicationPolicy;
import org.wso2.micro.gateway.enforcer.models.Subscription;
import org.wso2.micro.gateway.enforcer.models.SubscriptionPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the subscription data store.
 */
public class SubscriptionDataStoreImpl implements SubscriptionDataStore {

    private static final Logger log = LogManager.getLogger(SubscriptionDataStoreImpl.class);
    private static SubscriptionDataStoreImpl instance = new SubscriptionDataStoreImpl();

    /**
     * ENUM to hold type of policies.
     */
    public enum PolicyType {
        SUBSCRIPTION,
        APPLICATION,
        API
    }

    public static final String DELEM_PERIOD = ":";

    // Maps for keeping Subscription related details.
    private Map<ApplicationKeyMappingCacheKey, ApplicationKeyMapping> applicationKeyMappingMap;
    private Map<Integer, Application> applicationMap;
    private Map<String, API> apiMap;
    private Map<String, ApiPolicy> apiPolicyMap;
    private Map<String, SubscriptionPolicy> subscriptionPolicyMap;
    private Map<String, ApplicationPolicy> appPolicyMap;
    private Map<String, Subscription> subscriptionMap;
    private String tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;

    SubscriptionDataStoreImpl() {
    }

    public static SubscriptionDataStoreImpl getInstance() {
        return instance;
    }

    public void initializeStore() {

        this.applicationKeyMappingMap = new ConcurrentHashMap<>();
        this.applicationMap = new ConcurrentHashMap<>();
        this.apiMap = new ConcurrentHashMap<>();
        this.subscriptionPolicyMap = new ConcurrentHashMap<>();
        this.appPolicyMap = new ConcurrentHashMap<>();
        this.apiPolicyMap = new ConcurrentHashMap<>();
        this.subscriptionMap = new ConcurrentHashMap<>();
        //TODO: Enable data loading tasks if event hub is enabled
        if (ConfigHolder.getInstance().getConfig().getEventHub().isEnabled()) {
            initializeLoadingTasks();
        }
    }

    @Override
    public Application getApplicationById(int appId) {

        return applicationMap.get(appId);
    }

    @Override
    public ApplicationKeyMapping getKeyMappingByKeyAndKeyManager(String key, String keyManager) {
        return applicationKeyMappingMap.get(new ApplicationKeyMappingCacheKey(key, keyManager));
    }

    @Override
    public API getApiByContextAndVersion(String context, String version) {

        String key = context + DELEM_PERIOD + version;
        return apiMap.get(key);
    }

    @Override
    public SubscriptionPolicy getSubscriptionPolicyByName(String policyName, int tenantId) {

        String key = PolicyType.SUBSCRIPTION +
                SubscriptionDataStoreUtil.getPolicyCacheKey(policyName, tenantId);
        return subscriptionPolicyMap.get(key);
    }

    @Override
    public ApplicationPolicy getApplicationPolicyByName(String policyName, int tenantId) {

        String key = PolicyType.APPLICATION + DELEM_PERIOD +
                SubscriptionDataStoreUtil.getPolicyCacheKey(policyName, tenantId);
        return appPolicyMap.get(key);
    }

    @Override
    public Subscription getSubscriptionById(int appId, int apiId) {

        return subscriptionMap.get(SubscriptionDataStoreUtil.getSubscriptionCacheKey(appId, apiId));
    }

    @Override
    public ApiPolicy getApiPolicyByName(String policyName, int tenantId) {

        String key = PolicyType.API + DELEM_PERIOD +
                SubscriptionDataStoreUtil.getPolicyCacheKey(policyName, tenantId);
        return apiPolicyMap.get(key);
    }

    private void initializeLoadingTasks() {
        SubscriptionDiscoveryClient.getInstance().watchSubscriptions();
        ApplicationDiscoveryClient.getInstance().watchApplications();
        ApiListDiscoveryClient.getInstance().watchApiList();
        ApplicationPolicyDiscoveryClient.getInstance().watchApplicationPolicies();
        SubscriptionPolicyDiscoveryClient.getInstance().watchSubscriptionPolicies();
        ApplicationKeyMappingDiscoveryClient.getInstance().watchApplicationKeyMappings();
    }

    public void addSubscriptions(List<org.wso2.gateway.discovery.subscription.Subscription> subscriptionList) {
        Map<String, Subscription> newSubscriptionMap = new ConcurrentHashMap<>();

        for (org.wso2.gateway.discovery.subscription.Subscription subscription : subscriptionList) {
            Subscription newSubscription = new Subscription();
            newSubscription.setSubscriptionId(subscription.getSubscriptionId());
            newSubscription.setPolicyId(subscription.getPolicyId());
            newSubscription.setApiId(subscription.getApiId());
            newSubscription.setAppId(subscription.getAppId());
            newSubscription.setSubscriptionState(subscription.getSubscriptionState());
            newSubscription.setTimeStamp(subscription.getTimeStamp());

            newSubscriptionMap.put(newSubscription.getCacheKey(), newSubscription);
        }

        if (log.isDebugEnabled()) {
            log.debug("Total Subscriptions in new cache: {}", newSubscriptionMap.size());
        }
        this.subscriptionMap = newSubscriptionMap;
    }


    public void addApplications(List<org.wso2.gateway.discovery.subscription.Application> applicationList) {
        Map<Integer, Application> newApplicationMap = new ConcurrentHashMap<>();

        for (org.wso2.gateway.discovery.subscription.Application application : applicationList) {
            Application newApplication = new Application();
            newApplication.setId(application.getId());
            newApplication.setName(application.getName());
            newApplication.setPolicy(application.getPolicy());
            newApplication.setSubId(application.getSubId());
            newApplication.setSubName(application.getSubName());
            newApplication.setTokenType(application.getTokenType());
            newApplication.setUUID(application.getUuid());
            application.getAttributesMap().forEach(newApplication::addAttribute);

            newApplicationMap.put(newApplication.getCacheKey(), newApplication);
        }
        if (log.isDebugEnabled()) {
            log.debug("Total Applications in new cache: {}", newApplicationMap.size());
        }
        this.applicationMap = newApplicationMap;
    }

    public void addApis(List<APIs> apisList) {
        Map<String, API> newApiMap = new ConcurrentHashMap<>();

        for (APIs api : apisList) {
            API newApi = new API();
            newApi.setApiId(Integer.parseInt(api.getApiId()));
            newApi.setApiName(api.getName());
            newApi.setApiProvider(api.getProvider());
            newApi.setApiType(api.getApiType());
            newApi.setApiVersion(api.getVersion());
            newApi.setContext(api.getContext());
            newApi.setApiTier(api.getPolicy());

            newApiMap.put(newApi.getCacheKey(), newApi);
        }
        if (log.isDebugEnabled()) {
            log.debug("Total Apis in new cache: {}", newApiMap.size());
        }
        this.apiMap = newApiMap;
    }

    public void addApplicationPolicies(
            List<org.wso2.gateway.discovery.subscription.ApplicationPolicy> applicationPolicyList) {
        Map<String, ApplicationPolicy> newAppPolicyMap = new ConcurrentHashMap<>();

        for (org.wso2.gateway.discovery.subscription.ApplicationPolicy applicationPolicy : applicationPolicyList) {
            ApplicationPolicy newApplicationPolicy = new ApplicationPolicy();
            newApplicationPolicy.setId(applicationPolicy.getId());
            newApplicationPolicy.setQuotaType(applicationPolicy.getQuotaType());
            newApplicationPolicy.setTenantId(applicationPolicy.getTenantId());
            newApplicationPolicy.setTierName(applicationPolicy.getName());

            newAppPolicyMap.put(newApplicationPolicy.getCacheKey(), newApplicationPolicy);
        }
        if (log.isDebugEnabled()) {
            log.debug("Total Application Policies in new cache: {}", newAppPolicyMap.size());
        }
        this.appPolicyMap = newAppPolicyMap;
    }

    public void addSubscriptionPolicies(
            List<org.wso2.gateway.discovery.subscription.SubscriptionPolicy> subscriptionPolicyList) {
        Map<String, SubscriptionPolicy> newSubscriptionPolicyMap = new ConcurrentHashMap<>();

        for (org.wso2.gateway.discovery.subscription.SubscriptionPolicy subscriptionPolicy : subscriptionPolicyList) {
            SubscriptionPolicy newSubscriptionPolicy = new SubscriptionPolicy();
            newSubscriptionPolicy.setId(subscriptionPolicy.getId());
            newSubscriptionPolicy.setQuotaType(subscriptionPolicy.getQuotaType());
            newSubscriptionPolicy.setRateLimitCount(subscriptionPolicy.getRateLimitCount());
            newSubscriptionPolicy.setRateLimitTimeUnit(subscriptionPolicy.getRateLimitTimeUnit());
            newSubscriptionPolicy.setStopOnQuotaReach(subscriptionPolicy.getStopOnQuotaReach());
            newSubscriptionPolicy.setTenantId(subscriptionPolicy.getTenantId());
            newSubscriptionPolicy.setTierName(subscriptionPolicy.getName());
            newSubscriptionPolicy.setGraphQLMaxComplexity(subscriptionPolicy.getGraphQLMaxComplexity());
            newSubscriptionPolicy.setGraphQLMaxDepth(subscriptionPolicy.getGraphQLMaxDepth());

            newSubscriptionPolicyMap.put(newSubscriptionPolicy.getCacheKey(), newSubscriptionPolicy);
        }
        if (log.isDebugEnabled()) {
            log.debug("Total Subscription Policies in new cache: {}", newSubscriptionPolicyMap.size());
        }
        this.subscriptionPolicyMap = newSubscriptionPolicyMap;
    }

    public void addApplicationKeyMappings(
            List<org.wso2.gateway.discovery.subscription.ApplicationKeyMapping> applicationKeyMappingList) {
        Map<ApplicationKeyMappingCacheKey, ApplicationKeyMapping> newApplicationKeyMappingMap =
                new ConcurrentHashMap<>();

        for (org.wso2.gateway.discovery.subscription.ApplicationKeyMapping applicationKeyMapping :
                applicationKeyMappingList) {
            ApplicationKeyMapping mapping = new ApplicationKeyMapping();
            mapping.setApplicationId(applicationKeyMapping.getApplicationId());
            mapping.setConsumerKey(applicationKeyMapping.getConsumerKey());
            mapping.setKeyType(applicationKeyMapping.getKeyType());
            mapping.setKeyManager(applicationKeyMapping.getKeyManager());

            newApplicationKeyMappingMap.put(mapping.getCacheKey(), mapping);
        }
        if (log.isDebugEnabled()) {
            log.debug("Total Application Key Mappings in new cache: {}", newApplicationKeyMappingMap.size());
        }
        this.applicationKeyMappingMap = newApplicationKeyMappingMap;
    }

    @Override
    public void addOrUpdateSubscription(Subscription subscription) {

        synchronized (subscriptionMap) {
            Subscription retrievedSubscription = subscriptionMap.get(subscription.getCacheKey());
            if (retrievedSubscription == null) {
                subscriptionMap.put(subscription.getCacheKey(), subscription);
            } else {
                if (subscription.getTimeStamp() < retrievedSubscription.getTimeStamp()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Drop the Event " + subscription.toString() + " since the event timestamp was old");
                    }
                } else {
                    subscriptionMap.put(subscription.getCacheKey(), subscription);
                }
            }
        }
    }

    @Override
    public void removeSubscription(Subscription subscription) {
        subscriptionMap.remove(subscription.getCacheKey());
    }

    @Override
    public void addOrUpdateAPI(API api) {
        apiMap.put(api.getCacheKey(), api);
    }

    @Override
    public void addOrUpdateAPIWithUrlTemplates(API api) {
    }

    @Override
    public void removeAPI(API api) {
        apiMap.remove(api.getCacheKey());
    }

    @Override
    public void addOrUpdateApplicationKeyMapping(ApplicationKeyMapping applicationKeyMapping) {

        applicationKeyMappingMap.remove(applicationKeyMapping.getCacheKey());
        applicationKeyMappingMap.put(applicationKeyMapping.getCacheKey(), applicationKeyMapping);
    }

    @Override
    public void removeApplicationKeyMapping(ApplicationKeyMapping applicationKeyMapping) {
        applicationKeyMappingMap.remove(applicationKeyMapping.getCacheKey());
    }

    @Override
    public void addOrUpdateSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
        subscriptionPolicyMap.remove(subscriptionPolicy.getCacheKey());
        subscriptionPolicyMap.put(subscriptionPolicy.getCacheKey(), subscriptionPolicy);
    }

    @Override
    public void addOrUpdateApplicationPolicy(ApplicationPolicy applicationPolicy) {
        appPolicyMap.remove(applicationPolicy.getCacheKey());
        appPolicyMap.put(applicationPolicy.getCacheKey(), applicationPolicy);
    }

    @Override
    public void removeApplicationPolicy(ApplicationPolicy applicationPolicy) {
        appPolicyMap.remove(applicationPolicy.getCacheKey());
    }

    @Override
    public void removeSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
        subscriptionPolicyMap.remove(subscriptionPolicy.getCacheKey());
    }

    @Override
    public void addOrUpdateApplication(Application application) {
        applicationMap.remove(application.getId());
        applicationMap.put(application.getId(), application);
    }

    @Override
    public void removeApplication(Application application) {
        applicationMap.remove(application.getId());
    }

    @Override
    public void addOrUpdateApiPolicy(ApiPolicy apiPolicy) {
    }

    @Override
    public void removeApiPolicy(ApiPolicy apiPolicy) {
        apiPolicyMap.remove(apiPolicy.getCacheKey());
    }

    @Override
    public API getDefaultApiByContext(String context) {
        Set<String> set = apiMap.keySet()
                .stream()
                .filter(s -> s.startsWith(context))
                .collect(Collectors.toSet());
        for (String key : set) {
            API api = apiMap.get(key);
            if (api.isDefaultVersion() && (api.getContext().replace("/" + api.getApiVersion(), "")).equals(context)) {
                return api;
            }
        }
        return null;
    }
}

