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

package org.wso2.choreo.connect.enforcer.security;

import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.models.API;
import org.wso2.choreo.connect.enforcer.models.ApiPolicy;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicy;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;
import org.wso2.choreo.connect.enforcer.models.URLMapping;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataHolder;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataStore;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does the subscription and scope validation.
 */
public class KeyValidator {
    private static final Logger log = LogManager.getLogger(KeyValidator.class);

    /**
     * Validate the scopes related to the given validationContext.
     *
     * @param validationContext the token validation context
     * @return true is the scopes are valid
     * @throws EnforcerException if key validation information not set
     */
    public static boolean validateScopes(TokenValidationContext validationContext) throws EnforcerException {

        if (validationContext.isCacheHit()) {
            return true;
        }
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = validationContext.getValidationInfoDTO();

        if (apiKeyValidationInfoDTO == null) {
            throw new EnforcerException("Key Validation information not set");
        }
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

        ResourceConfig matchedResource = validationContext.getMatchingResourceConfig();
        boolean scopesValidated = false;
        if (matchedResource.getSecuritySchemas().entrySet().size() > 0) {
            for (Map.Entry<String, List<String>> pair : matchedResource.getSecuritySchemas().entrySet()) {
                boolean validate = false;
                if (pair.getValue() != null && pair.getValue().size() > 0) {
                    scopesValidated = false;
                    for (String scope : pair.getValue()) {
                        if (scopesSet.contains(scope)) {
                            scopesValidated = true;
                            validate = true;
                            break;
                        }
                    }
                } else {
                    scopesValidated = true;
                }
                if (validate) {
                    break;
                }
            }
        } else {
            scopesValidated = true;
        }
        if (!scopesValidated) {
            apiKeyValidationInfoDTO.setAuthorized(false);
            apiKeyValidationInfoDTO.setValidationStatus(APIConstants.KeyValidationStatus.INVALID_SCOPE);
        }
        return scopesValidated;
    }

    /**
     * Validate subscriptions for access tokens.
     *
     * @param uuid uuid of the API
     * @param apiContext API context, used for logging purposes and to extract the tenant domain
     * @param apiVersion API version, used for logging purposes
     * @param consumerKey consumer key related to the token
     * @param keyManager key manager related to the token
     * @return validation information about the request
     */
    public static APIKeyValidationInfoDTO validateSubscription(String uuid, String apiContext, String apiVersion,
                                                         String consumerKey, String keyManager) {
        log.debug("Before validating subscriptions");
        log.debug("Validation Info : { uuid : {}, context : {}, version : {}, consumerKey : {} }",
                uuid, apiContext, apiVersion, consumerKey);
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        API api = null;
        ApplicationKeyMapping key = null;
        Application app = null;
        Subscription sub = null;

        SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                .getTenantSubscriptionStore(apiTenantDomain);
        //TODO add a check to see whether datastore is initialized an load data using rest api if it is not loaded
        // TODO: (VirajSalaka) Handle the scenario where the event is dropped.
        if (datastore != null) {
            api = datastore.getApiByContextAndVersion(uuid);
            if (api != null) {
                key = datastore.getKeyMappingByKeyAndKeyManager(consumerKey, keyManager);
                if (key != null) {
                    app = datastore.getApplicationById(key.getApplicationUUID());
                    if (app != null) {
                        sub = datastore.getSubscriptionById(app.getUUID(), api.getApiUUID());
                        if (sub != null) {
                            log.debug("All information is retrieved from the inmemory data store.");
                        } else {
                            log.info(
                                    "Valid subscription not found for oauth access token. " +
                                            "application: {} app_UUID: {} API_name: {} API_UUID : {}",
                                    app.getName(), app.getUUID(), api.getApiName(), api.getApiUUID());
                        }
                    } else {
                        log.info("Application not found in the data store for uuid " + key.getApplicationUUID());
                    }
                } else {
                    log.info("Application key mapping not found in the data store for id consumerKey " + consumerKey);
                }
            } else {
                log.info("API not found in the data store for API UUID :" + uuid);
            }
        } else {
            log.error("Subscription data store is null for tenant domain " + apiTenantDomain);
        }

        APIKeyValidationInfoDTO infoDTO = new APIKeyValidationInfoDTO();
        if (api != null && app != null && key != null && sub != null) {
            String keyType = key.getKeyType();
            validate(infoDTO, datastore, api, keyType, app, sub);
        }
        if (!infoDTO.isAuthorized() && infoDTO.getValidationStatus() == 0) {
            //Scenario where validation failed and message is not set
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_AUTH_RESOURCE_FORBIDDEN);
        }
        log.debug("After validating subscriptions");
        return infoDTO;
    }

    /**
     * Validate subscriptions for API keys.
     *
     * @param apiUuid uuid of the API
     * @param apiContext API context, used for logging purposes and to extract the tenant domain
     * @param payload JWT claims set extracted from the API key
     * @return validation information about the request
     */
    public static APIKeyValidationInfoDTO validateSubscription(String apiUuid, String apiContext,
                                                               JWTClaimsSet payload) {
        log.debug("Before validating subscriptions with API key. API_uuid: {}, context: {}", apiUuid, apiContext);
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(apiContext);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        API api = null;
        Application app = null;
        Subscription sub = null;

        SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                .getTenantSubscriptionStore(apiTenantDomain);
        //TODO add a check to see whether datastore is initialized an load data using rest api if it is not loaded
        // TODO: (VirajSalaka) Handle the scenario where the event is dropped.
        if (datastore != null) {
            api = datastore.getApiByContextAndVersion(apiUuid);
            if (api != null) {
                JSONObject appObject = (JSONObject) payload.getClaim(APIConstants.JwtTokenConstants.APPLICATION);
                String appUuid = appObject.getAsString("uuid");
                if (!appObject.isEmpty() && !appUuid.isEmpty()) {
                    app = datastore.getApplicationById(appUuid);
                    if (app != null) {
                        sub = datastore.getSubscriptionById(app.getUUID(), api.getApiUUID());
                        if (sub != null) {
                            log.debug("All information is retrieved from the in memory data store.");
                        } else {
                            log.info(
                                    "Valid subscription not found for API key. " +
                                            "application: {} app_UUID: {} API_name: {} API_UUID : {}",
                                    app.getName(), app.getUUID(), api.getApiName(), api.getApiUUID());
                        }
                    } else {
                        log.info("Application not found in the data store for uuid {}", appUuid);
                    }
                } else {
                    log.info("Application claim not found in jwt for uuid");
                }
            } else {
                log.info("API not found in the data store for API UUID :" + apiUuid);
            }
        } else {
            log.error("Subscription data store is null for tenant domain " + apiTenantDomain);
        }

        String keyType = (String) payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE);
        if (keyType == null) {
            keyType = APIConstants.API_KEY_TYPE_PRODUCTION;
        }
        APIKeyValidationInfoDTO infoDTO = new APIKeyValidationInfoDTO();
        if (api != null && app != null && sub != null) {
            validate(infoDTO, datastore, api, keyType, app, sub);
        }
        if (!infoDTO.isAuthorized() && infoDTO.getValidationStatus() == 0) {
            //Scenario where validation failed and message is not set
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_AUTH_RESOURCE_FORBIDDEN);
        }
        log.debug("After validating subscriptions with API key.");
        return infoDTO;
    }

    private static void validate(APIKeyValidationInfoDTO infoDTO, SubscriptionDataStore datastore,
                                             API api, String keyType, Application app, Subscription sub) {
        String subscriptionStatus = sub.getSubscriptionState();
        if (APIConstants.SubscriptionStatus.BLOCKED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setAuthorized(false);
            return;
        } else if (APIConstants.SubscriptionStatus.ON_HOLD.equals(subscriptionStatus)
                || APIConstants.SubscriptionStatus.REJECTED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.SUBSCRIPTION_INACTIVE);
            infoDTO.setAuthorized(false);
            return;
        } else if (APIConstants.SubscriptionStatus.PROD_ONLY_BLOCKED.equals(subscriptionStatus)
                && !APIConstants.API_KEY_TYPE_SANDBOX.equals(keyType)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setType(keyType);
            infoDTO.setAuthorized(false);
            return;
        } else if (APIConstants.LifecycleStatus.BLOCKED.equals(api.getLcState())) {
            infoDTO.setValidationStatus(GeneralErrorCodeConstants.API_BLOCKED_CODE);
            infoDTO.setAuthorized(false);
            return;
        }
        infoDTO.setTier(sub.getPolicyId());
        infoDTO.setSubscriber(app.getSubName());
        infoDTO.setApplicationId(app.getUUID());
        infoDTO.setApiName(api.getApiName());
        infoDTO.setApiVersion(api.getApiVersion());
        infoDTO.setApiPublisher(api.getApiProvider());
        infoDTO.setApplicationName(app.getName());
        infoDTO.setApplicationTier(app.getPolicy());
        infoDTO.setApplicationUUID(app.getUUID());
        infoDTO.setAppAttributes(app.getAttributes());
        infoDTO.setApiUUID(api.getApiUUID());
        infoDTO.setType(keyType);
        infoDTO.setSubscriberTenantDomain(app.getTenantDomain());
        // Advanced Level Throttling Related Properties
        String apiTier = api.getApiTier();

        ApplicationPolicy appPolicy = datastore.getApplicationPolicyByName(app.getPolicy());
        SubscriptionPolicy subPolicy = datastore.getSubscriptionPolicyByName(sub.getPolicyId());
        ApiPolicy apiPolicy = datastore.getApiPolicyByName(api.getApiTier());
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
}
