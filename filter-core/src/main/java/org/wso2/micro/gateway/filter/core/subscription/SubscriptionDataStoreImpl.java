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

package org.wso2.micro.gateway.filter.core.subscription;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.common.CacheableEntity;
import org.wso2.micro.gateway.filter.core.common.ReferenceHolder;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.exception.DataLoadingException;
import org.wso2.micro.gateway.filter.core.exception.MGWException;
import org.wso2.micro.gateway.filter.core.models.API;
import org.wso2.micro.gateway.filter.core.models.ApiPolicy;
import org.wso2.micro.gateway.filter.core.models.Application;
import org.wso2.micro.gateway.filter.core.models.ApplicationKeyMapping;
import org.wso2.micro.gateway.filter.core.models.ApplicationKeyMappingCacheKey;
import org.wso2.micro.gateway.filter.core.models.ApplicationPolicy;
import org.wso2.micro.gateway.filter.core.models.Policy;
import org.wso2.micro.gateway.filter.core.models.Subscription;
import org.wso2.micro.gateway.filter.core.models.SubscriptionPolicy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementation of the subscription data store.
 */
public class SubscriptionDataStoreImpl implements SubscriptionDataStore {

    private static final Logger log = LogManager.getLogger(SubscriptionDataStoreImpl.class);
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
    private boolean apisInitialized;
    private boolean applicationsInitialized;
    private boolean subscriptionsInitialized;
    private boolean applicationKeysInitialized;
    private boolean applicationPoliciesInitialized;
    private boolean subscriptionPoliciesInitialized;
    private boolean apiPoliciesInitialized;
    public static final int LOADING_POOL_SIZE = 7;
    private String tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(LOADING_POOL_SIZE);

    public SubscriptionDataStoreImpl(String tenantDomain) {

        this.tenantDomain = tenantDomain;
        initializeStore();
    }

    public SubscriptionDataStoreImpl() {

        initializeStore();
    }

    private void initializeStore() {

        this.applicationKeyMappingMap = new ConcurrentHashMap<>();
        this.applicationMap = new ConcurrentHashMap<>();
        this.apiMap = new ConcurrentHashMap<>();
        this.subscriptionPolicyMap = new ConcurrentHashMap<>();
        this.appPolicyMap = new ConcurrentHashMap<>();
        this.apiPolicyMap = new ConcurrentHashMap<>();
        this.subscriptionMap = new ConcurrentHashMap<>();
        //TODO: Enable data loading tasks if event hub is enabled
        if (ReferenceHolder.getInstance().getMGWConfiguration().getEventHubConfiguration().isEnabled()) {
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

    public void initializeLoadingTasks() {

        Runnable apiTask = new PopulateTask<String, API>(apiMap,
                () -> {
                    try {
                        log.debug("Calling loadAllApis. ");
                        List<API> apiList = new SubscriptionDataLoaderImpl().loadAllApis(tenantDomain);
                        apisInitialized = true;
                        return apiList;
                    } catch (MGWException e) {
                        log.error("Exception while loading APIs " + e);
                    }
                    return null;
                });

        executorService.schedule(apiTask, 0, TimeUnit.SECONDS);

        Runnable subscriptionLoadingTask = new PopulateTask<String, Subscription>(subscriptionMap,
                () -> {
                    try {
                        log.debug("Calling loadAllSubscriptions.");
                        List<Subscription> subscriptionList =
                                new SubscriptionDataLoaderImpl().loadAllSubscriptions(tenantDomain);
                        subscriptionsInitialized = true;
                        return subscriptionList;
                    } catch (MGWException e) {
                        log.error("Exception while loading Subscriptions " + e);
                    }
                    return null;
                });

        executorService.schedule(subscriptionLoadingTask, 0, TimeUnit.SECONDS);

        Runnable applicationLoadingTask = new PopulateTask<Integer, Application>(applicationMap,
                () -> {
                    try {
                        log.debug("Calling loadAllApplications.");
                        List<Application> applicationList =
                                new SubscriptionDataLoaderImpl().loadAllApplications(tenantDomain);
                        applicationsInitialized = true;
                        return applicationList;
                    } catch (MGWException e) {
                        log.error("Exception while loading Applications " + e);
                    }
                    return null;
                });

        executorService.schedule(applicationLoadingTask, 0, TimeUnit.SECONDS);

        Runnable keyMappingsTask =
                new PopulateTask<ApplicationKeyMappingCacheKey, ApplicationKeyMapping>(applicationKeyMappingMap,
                        () -> {
                            try {
                                log.debug("Calling loadAllKeyMappings.");
                                List<ApplicationKeyMapping> applicationKeyMappingList =
                                        new SubscriptionDataLoaderImpl().loadAllKeyMappings(tenantDomain);
                                applicationKeysInitialized = true;
                                return applicationKeyMappingList;
                            } catch (MGWException e) {
                                log.error("Exception while loading ApplicationKeyMapping " + e);
                            }
                            return null;
                        });

        executorService.schedule(keyMappingsTask, 0, TimeUnit.SECONDS);

        Runnable apiPolicyLoadingTask =
                new PopulateTask<String, ApiPolicy>(apiPolicyMap,
                        () -> {
                            try {
                                log.debug("Calling loadAllSubscriptionPolicies.");
                                List<ApiPolicy> apiPolicyList =
                                        new SubscriptionDataLoaderImpl().loadAllAPIPolicies(tenantDomain);
                                apiPoliciesInitialized = true;
                                return apiPolicyList;
                            } catch (MGWException e) {
                                log.error("Exception while loading api Policies " + e);
                            }
                            return null;
                        });

        executorService.schedule(apiPolicyLoadingTask, 0, TimeUnit.SECONDS);

        Runnable subPolicyLoadingTask =
                new PopulateTask<String, SubscriptionPolicy>(subscriptionPolicyMap,
                        () -> {
                            try {
                                log.debug("Calling loadAllSubscriptionPolicies.");
                                List<SubscriptionPolicy> subscriptionPolicyList =
                                        new SubscriptionDataLoaderImpl().loadAllSubscriptionPolicies(tenantDomain);
                                subscriptionPoliciesInitialized = true;
                                return subscriptionPolicyList;
                            } catch (MGWException e) {
                                log.error("Exception while loading Subscription Policies " + e);
                            }
                            return null;
                        });

        executorService.schedule(subPolicyLoadingTask, 0, TimeUnit.SECONDS);

        Runnable appPolicyLoadingTask =
                new PopulateTask<String, ApplicationPolicy>(appPolicyMap,
                        () -> {
                            try {
                                log.debug("Calling loadAllAppPolicies.");
                                List<ApplicationPolicy> applicationPolicyList =
                                        new SubscriptionDataLoaderImpl().loadAllAppPolicies(tenantDomain);
                                applicationPoliciesInitialized = true;
                                return applicationPolicyList;
                            } catch (MGWException e) {
                                log.error("Exception while loading Application Policies " + e);
                            }
                            return null;
                        });

        executorService.schedule(appPolicyLoadingTask, 0, TimeUnit.SECONDS);
    }

    private <T extends Policy> T getPolicy(String policyName, int tenantId,
            Map<String, T> policyMap) {

        return policyMap.get(SubscriptionDataStoreUtil.getPolicyCacheKey(policyName, tenantId));
    }

    private class PopulateTask<K, V extends CacheableEntity<K>> implements Runnable {

        private Map<K, V> entityMap;
        private Supplier<List<V>> supplier;

        PopulateTask(Map<K, V> entityMap, Supplier<List<V>> supplier) {

            this.entityMap = entityMap;
            this.supplier = supplier;
        }

        public void run() {

            List<V> list = supplier.get();
            HashMap<K, V> tempMap = new HashMap<>();

            if (list != null) {
                for (V v : list) {
                    tempMap.put(v.getCacheKey(), v);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Adding entry Key : %s Value : %s", v.getCacheKey(), v));
                    }

                    if (!tempMap.isEmpty()) {
                        entityMap.clear();
                        entityMap.putAll(tempMap);
                    }
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("List is null for " + supplier.getClass());
                }
            }
        }
    }

    public boolean isApisInitialized() {

        return apisInitialized;
    }

    public boolean isApplicationsInitialized() {

        return applicationsInitialized;
    }

    public boolean isSubscriptionsInitialized() {

        return subscriptionsInitialized;
    }

    public boolean isApplicationKeysInitialized() {

        return applicationKeysInitialized;
    }

    public boolean isApplicationPoliciesInitialized() {

        return applicationPoliciesInitialized;
    }

    public boolean isSubscriptionPoliciesInitialized() {

        return subscriptionPoliciesInitialized;
    }

    public boolean isApiPoliciesInitialized() {

        return apiPoliciesInitialized;
    }

    public boolean isSubscriptionValidationDataInitialized() {

        return apisInitialized &&
                applicationsInitialized &&
                subscriptionsInitialized &&
                applicationKeysInitialized &&
                applicationPoliciesInitialized &&
                subscriptionPoliciesInitialized &&
                apiPoliciesInitialized;
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
        try {
            API newAPI = new SubscriptionDataLoaderImpl().getApi(api.getContext(), api.getApiVersion());
            apiMap.put(api.getCacheKey(), newAPI);
        } catch (DataLoadingException e) {
            log.error("Exception while loading api for " + api.getContext() + " " + api.getApiVersion(), e);
        }

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
        try {
            ApiPolicy policy = new SubscriptionDataLoaderImpl().getAPIPolicy(apiPolicy.getName(), tenantDomain);
            apiPolicyMap.remove(apiPolicy.getCacheKey());
            apiPolicyMap.put(apiPolicy.getCacheKey(), policy);
        } catch (DataLoadingException e) {
            log.error("Exception while loading api policy for " + apiPolicy.getName() + " for domain " + tenantDomain,
                    e);
        }
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

