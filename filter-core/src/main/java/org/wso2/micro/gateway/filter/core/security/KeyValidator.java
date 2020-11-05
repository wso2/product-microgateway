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

package org.wso2.micro.gateway.filter.core.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.dto.APIKeyValidationInfoDTO;
import org.wso2.micro.gateway.filter.core.exception.DataLoadingException;
import org.wso2.micro.gateway.filter.core.exception.MGWException;
import org.wso2.micro.gateway.filter.core.models.API;
import org.wso2.micro.gateway.filter.core.models.ApiPolicy;
import org.wso2.micro.gateway.filter.core.models.Application;
import org.wso2.micro.gateway.filter.core.models.ApplicationKeyMapping;
import org.wso2.micro.gateway.filter.core.models.ApplicationPolicy;
import org.wso2.micro.gateway.filter.core.models.Subscription;
import org.wso2.micro.gateway.filter.core.models.SubscriptionPolicy;
import org.wso2.micro.gateway.filter.core.models.URLMapping;
import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataHolder;
import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataLoaderImpl;
import org.wso2.micro.gateway.filter.core.subscription.SubscriptionDataStore;
import org.wso2.micro.gateway.filter.core.util.FilterUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Does the subscription and scope validation.
 */
public class KeyValidator {

    private static final Logger log = LogManager.getLogger(KeyValidator.class);

    public APIKeyValidationInfoDTO validateSubscription(String apiContext, String apiVersion, String consumerKey,
                                                        String keyManager) {
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();

        try {
            if (log.isDebugEnabled()) {
                log.debug("Before validating subscriptions");
                log.debug("Validation Info : { context : " + apiContext + " , " + "version : " + apiVersion
                        + " , consumerKey : " + consumerKey + " }");
            }
            validateSubscriptionDetails(apiContext, apiVersion, consumerKey, keyManager, apiKeyValidationInfoDTO);
            if (log.isDebugEnabled()) {
                log.debug("After validating subscriptions");
            }
        } catch (MGWException e) {
            log.error("Error Occurred while validating subscription.", e);
        }
        return apiKeyValidationInfoDTO;
    }

    public boolean validateScopes(TokenValidationContext validationContext) throws MGWException {

        if (validationContext.isCacheHit()) {
            return true;
        }
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = validationContext.getValidationInfoDTO();

        if (apiKeyValidationInfoDTO == null) {
            throw new MGWException("Key Validation information not set");
        }
        String tenantDomain = "carbon.super"; //TODO: get correct tenant domain.
        String httpVerb = validationContext.getHttpVerb();
        String[] scopes;
        Set<String> scopesSet = apiKeyValidationInfoDTO.getScopes();
        StringBuilder scopeList = new StringBuilder();

        if (scopesSet != null && !scopesSet.isEmpty()) {
            scopes = scopesSet.toArray(new String[scopesSet.size()]);
            if (log.isDebugEnabled() && scopes != null) {
                for (String scope : scopes) {
                    scopeList.append(scope);
                    scopeList.append(",");
                }
                scopeList.deleteCharAt(scopeList.length() - 1);
                log.debug("Scopes allowed for token : " + validationContext.getAccessToken() + " : " + scopeList
                        .toString());
            }
        }

        String resourceList = validationContext.getMatchingResource();
        List<String> resourceArray = new ArrayList<>(Arrays.asList(resourceList.split(",")));
        SubscriptionDataStore tenantSubscriptionStore = SubscriptionDataHolder.getInstance()
                .getTenantSubscriptionStore(tenantDomain);
        API api = tenantSubscriptionStore
                .getApiByContextAndVersion(validationContext.getContext(), validationContext.getVersion());
        boolean scopesValidated = true; //TODO: enable proper scope validation
        if (api != null) {

            for (String resource : resourceArray) {
                List<URLMapping> resources = api.getResources();
                URLMapping urlMapping = null;
                for (URLMapping mapping : resources) {
                    if (httpVerb.equals(mapping.getHttpMethod())) {
                        if (isResourcePathMatching(resource, mapping)) {
                            urlMapping = mapping;
                            break;
                        }
                    }
                }
                if (urlMapping != null) {
                    if (urlMapping.getScopes().size() == 0) {
                        scopesValidated = true;
                        continue;
                    }
                    List<String> mappingScopes = urlMapping.getScopes();
                    boolean validate = false;
                    for (String scope : mappingScopes) {
                        if (scopesSet.contains(scope)) {
                            scopesValidated = true;
                            validate = true;
                            break;
                        }
                    }
                    if (!validate && urlMapping.getScopes().size() > 0) {
                        scopesValidated = false;
                        break;
                    }
                }
            }
        }
        if (!scopesValidated) {
            apiKeyValidationInfoDTO.setAuthorized(false);
            apiKeyValidationInfoDTO.setValidationStatus(APIConstants.KeyValidationStatus.INVALID_SCOPE);
        }
        return scopesValidated;
    }

    private boolean validateSubscriptionDetails(String context, String version, String consumerKey, String keyManager,
            APIKeyValidationInfoDTO infoDTO) throws MGWException {
        boolean defaultVersionInvoked = false;
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(context);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        // Check if the api version has been prefixed with _default_
        if (version != null && version.startsWith(APIConstants.DEFAULT_VERSION_PREFIX)) {
            defaultVersionInvoked = true;
            // Remove the prefix from the version.
            version = version.split(APIConstants.DEFAULT_VERSION_PREFIX)[1];
        }

        validateSubscriptionDetails(infoDTO, context, version, consumerKey, keyManager, defaultVersionInvoked);
        return infoDTO.isAuthorized();
    }

    private APIKeyValidationInfoDTO validateSubscriptionDetails(APIKeyValidationInfoDTO infoDTO, String context,
            String version, String consumerKey, String keyManager, boolean defaultVersionInvoked) {
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(context);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        int tenantId = -1234; //TODO: get the correct tenant domain.
        API api = null;
        ApplicationKeyMapping key = null;
        Application app = null;
        Subscription sub = null;

        SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                .getTenantSubscriptionStore(apiTenantDomain);
        //TODO add a check to see whether datastore is initialized an load data using rest api if it is not loaded
        if (datastore != null) {
            api = datastore.getApiByContextAndVersion(context, version);
            if (api == null && APIConstants.DEFAULT_WEBSOCKET_VERSION.equals(version)) {
                // for websocket default version.
                api = datastore.getDefaultApiByContext(context);
            }
            if (api != null) {
                key = datastore.getKeyMappingByKeyAndKeyManager(consumerKey, keyManager);
                if (key != null) {
                    app = datastore.getApplicationById(key.getApplicationId());
                    if (app != null) {
                        sub = datastore.getSubscriptionById(app.getId(), api.getApiId());
                        if (sub != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("All information is retrieved from the inmemory data store.");
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Valid subscription not found for appId " + app.getId() + " and apiId " + api
                                        .getApiId());
                            }
                            loadInfoFromRestAPIAndValidate(api, app, key, sub, context, version, consumerKey,
                                            keyManager, datastore, apiTenantDomain, infoDTO, tenantId);
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Application not found in the datastore for id " + key.getApplicationId());
                        }
                        loadInfoFromRestAPIAndValidate(api, app, key, sub, context, version, consumerKey, keyManager,
                                datastore, apiTenantDomain, infoDTO, tenantId);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Application keymapping not found in the datastore for id consumerKey " + consumerKey);
                    }
                    loadInfoFromRestAPIAndValidate(api, app, key, sub, context, version, consumerKey, keyManager,
                            datastore, apiTenantDomain, infoDTO, tenantId);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("API not found in the datastore for " + context + ":" + version);
                }
             loadInfoFromRestAPIAndValidate(api, app, key, sub, context, version, consumerKey, keyManager, datastore,
                        apiTenantDomain, infoDTO, tenantId);
            }
        } else {
            log.error("Subscription datastore is null for tenant domain " + apiTenantDomain);
            loadInfoFromRestAPIAndValidate(api, app, key, sub, context, version, consumerKey, keyManager, datastore,
                    apiTenantDomain, infoDTO, tenantId);
        }

        if (api != null && app != null && key != null && sub != null) {
            validate(infoDTO, apiTenantDomain, tenantId, datastore, api, key, app, sub, keyManager);
        } else if (!infoDTO.isAuthorized() && infoDTO.getValidationStatus() == 0) {
            //Scenario where validation failed and message is not set
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_AUTH_RESOURCE_FORBIDDEN);
        }

        return infoDTO;
    }

    private APIKeyValidationInfoDTO validate(APIKeyValidationInfoDTO infoDTO, String apiTenantDomain, int tenantId,
            SubscriptionDataStore datastore, API api, ApplicationKeyMapping key, Application app, Subscription sub,
            String keyManager) {
        String subscriptionStatus = sub.getSubscriptionState();
        String type = key.getKeyType();
        if (APIConstants.SubscriptionStatus.BLOCKED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setAuthorized(false);
            return infoDTO;
        } else if (APIConstants.SubscriptionStatus.ON_HOLD.equals(subscriptionStatus)
                || APIConstants.SubscriptionStatus.REJECTED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.SUBSCRIPTION_INACTIVE);
            infoDTO.setAuthorized(false);
            return infoDTO;
        } else if (APIConstants.SubscriptionStatus.PROD_ONLY_BLOCKED.equals(subscriptionStatus)
                && !APIConstants.API_KEY_TYPE_SANDBOX.equals(type)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setType(type);
            infoDTO.setAuthorized(false);
            return infoDTO;
        }
        infoDTO.setTier(sub.getPolicyId());
        infoDTO.setSubscriber(app.getSubName());
        infoDTO.setApplicationId(app.getId().toString());
        infoDTO.setApiName(api.getApiName());
        infoDTO.setApiVersion(api.getApiVersion());
        infoDTO.setApiPublisher(api.getApiProvider());
        infoDTO.setApplicationName(app.getName());
        infoDTO.setApplicationTier(app.getPolicy());
        infoDTO.setApplicationUUID(app.getUUID());
        infoDTO.setAppAttributes(app.getAttributes());
        infoDTO.setType(type);

        // Advanced Level Throttling Related Properties
        String apiTier = api.getApiTier();
        String subscriberTenant = "carbon.super"; //TODO : get correct tenant domain

        ApplicationPolicy appPolicy = datastore.getApplicationPolicyByName(app.getPolicy(), tenantId);
        if (appPolicy == null) {
            try {
                appPolicy = new SubscriptionDataLoaderImpl().getApplicationPolicy(app.getPolicy(), apiTenantDomain);
                datastore.addOrUpdateApplicationPolicy(appPolicy);
            } catch (DataLoadingException e) {
                log.error("Error while loading ApplicationPolicy");
            }
        }
        SubscriptionPolicy subPolicy = datastore.getSubscriptionPolicyByName(sub.getPolicyId(), tenantId);
        if (subPolicy == null) {
            try {
                subPolicy = new SubscriptionDataLoaderImpl().getSubscriptionPolicy(sub.getPolicyId(), apiTenantDomain);
                datastore.addOrUpdateSubscriptionPolicy(subPolicy);
            } catch (DataLoadingException e) {
                log.error("Error while loading SubscriptionPolicy");
            }
        }
        ApiPolicy apiPolicy = datastore.getApiPolicyByName(api.getApiTier(), tenantId);

        boolean isContentAware = false;
        if (appPolicy.isContentAware() || subPolicy.isContentAware() || (apiPolicy != null && apiPolicy
                .isContentAware())) {
            isContentAware = true;
        }
        infoDTO.setContentAware(isContentAware);

        // TODO this must implement as a part of throttling implementation.
        int spikeArrest = 0;
        String apiLevelThrottlingKey = "api_level_throttling_key";

        if (subPolicy.getRateLimitCount() > 0) {
            spikeArrest = subPolicy.getRateLimitCount();
        }

        String spikeArrestUnit = null;

        if (subPolicy.getRateLimitTimeUnit() != null) {
            spikeArrestUnit = subPolicy.getRateLimitTimeUnit();
        }
        boolean stopOnQuotaReach = subPolicy.isStopOnQuotaReach();
        int graphQLMaxDepth = 0;
        if (subPolicy.getGraphQLMaxDepth() > 0) {
            graphQLMaxDepth = subPolicy.getGraphQLMaxDepth();
        }
        int graphQLMaxComplexity = 0;
        if (subPolicy.getGraphQLMaxComplexity() > 0) {
            graphQLMaxComplexity = subPolicy.getGraphQLMaxComplexity();
        }
        List<String> list = new ArrayList<String>();
        list.add(apiLevelThrottlingKey);
        infoDTO.setSpikeArrestLimit(spikeArrest);
        infoDTO.setSpikeArrestUnit(spikeArrestUnit);
        infoDTO.setStopOnQuotaReach(stopOnQuotaReach);
        infoDTO.setSubscriberTenantDomain(subscriberTenant);
        infoDTO.setGraphQLMaxDepth(graphQLMaxDepth);
        infoDTO.setGraphQLMaxComplexity(graphQLMaxComplexity);
        if (apiTier != null && apiTier.trim().length() > 0) {
            infoDTO.setApiTier(apiTier);
        }
        // We also need to set throttling data list associated with given API. This need to have
        // policy id and
        // condition id list for all throttling tiers associated with this API.
        infoDTO.setThrottlingDataList(list);
        infoDTO.setAuthorized(true);
        return infoDTO;
    }

    private boolean isResourcePathMatching(String resourceString, URLMapping urlMapping) {

        String resource = resourceString.trim();
        String urlPattern = urlMapping.getUrlPattern().trim();

        if (resource.equalsIgnoreCase(urlPattern)) {
            return true;
        }

        // If the urlPattern is only one character longer than the resource and the urlPattern ends with a '/'
        if (resource.length() + 1 == urlPattern.length() && urlPattern.endsWith("/")) {
            // Check if resource is equal to urlPattern if the trailing '/' of the urlPattern is ignored
            String urlPatternWithoutSlash = urlPattern.substring(0, urlPattern.length() - 1);
            return resource.equalsIgnoreCase(urlPatternWithoutSlash);
        }

        return false;
    }

    private void loadInfoFromRestAPIAndValidate(API api, Application app, ApplicationKeyMapping key, Subscription sub,
                                                String context, String version, String consumerKey, String keyManager,
                                                SubscriptionDataStore datastore, String apiTenantDomain,
                                                APIKeyValidationInfoDTO infoDTO, int tenantId) {
        // TODO Load using a single single rest api.
        if (log.isDebugEnabled()) {
            log.debug("Loading missing information in the datastore by invoking the Rest API");
        }
        try {
            // only loading if the api is not found previously
            if (api == null) {
                api = new SubscriptionDataLoaderImpl().getApi(context, version);
                if (api != null && api.getApiId() != 0) {
                    // load to the memory
                    log.debug("Loading API to the in-memory datastore.");
                    datastore.addOrUpdateAPI(api);
                }
            }
            // only loading if the key is not found previously
            if (key == null) {
                key = new SubscriptionDataLoaderImpl().getKeyMapping(consumerKey);
                if (key != null && !StringUtils.isEmpty(key.getConsumerKey())) {
                    // load to the memory
                    log.debug("Loading Keymapping to the in-memory datastore.");
                    datastore.addOrUpdateApplicationKeyMapping(key);
                }
            }
            // check whether still api and keys are not found
            if (api == null || key == null) {
                // invalid request. nothing to do. return without any further processing
                if (log.isDebugEnabled()) {
                    if (api == null) {
                        log.debug("API not found for the " + context + " " + version);
                    }
                    if (key == null) {
                        log.debug("KeyMapping not found for the " + consumerKey);
                    }
                }
                return;
            } else {
                //go further and load missing objects
                if (app == null) {
                    app = new SubscriptionDataLoaderImpl().getApplicationById(key.getApplicationId());
                    if (app != null && app.getId() != null && app.getId() != 0) {
                        // load to the memory
                        log.debug("Loading Application to the in-memory datastore. applicationId = " + app.getId());
                        datastore.addOrUpdateApplication(app);
                    } else {
                        log.debug("Application not found. applicationId = " + key.getApplicationId());
                    }
                }
                if (app != null) {
                    sub = new SubscriptionDataLoaderImpl().getSubscriptionById(Integer.toString(api.getApiId()),
                            Integer.toString(app.getId()));
                    if (sub != null && !StringUtils.isEmpty(sub.getSubscriptionId())) {
                        // load to the memory
                        log.debug("Loading Subscription to the in-memory datastore.");
                        datastore.addOrUpdateSubscription(sub);
                        validate(infoDTO, apiTenantDomain, tenantId, datastore, api, key, app, sub, keyManager);
                    }
                }
            }
        } catch (DataLoadingException e) {
            log.error("Error while connecting the backend for loading subscription related data ", e);
        }
    }
}
