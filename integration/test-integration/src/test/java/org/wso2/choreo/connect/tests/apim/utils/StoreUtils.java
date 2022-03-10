/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.tests.apim.utils;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class StoreUtils {
    private static final Logger log = LoggerFactory.getLogger(StoreUtils.class);

    /**
     * Generates API key for a given applicationID and key type.
     *
     * @param applicationId   - Application ID for the relevant API key
     * @param keyType         - API key type (production or sandbox)
     * @param storeRestClient - Instance of the RestAPIStoreImpl
     * @return APIKeyDTO with API key
     * @throws CCTestException if an error occurs while generating API key
     */
    public static APIKeyDTO generateAPIKey(String applicationId, String keyType, RestAPIStoreImpl storeRestClient)
            throws CCTestException {
        try {
            return storeRestClient.generateAPIKeys(applicationId, keyType, -1,
                    null, null);
        } catch (ApiException e) {
            throw new CCTestException("Error occurred while generating the API key.", e);
        }
    }

    public static String generateUserAccessToken(String apimServiceURLHttps, String applicationId, User user,
                                                 RestAPIStoreImpl storeRestClient) throws CCTestException {
        return generateUserAccessTokenProduction(apimServiceURLHttps, applicationId, user,
                new String[]{}, storeRestClient);
    }

    public static String generateUserAccessTokenProduction(String apimServiceURLHttps, String applicationId, User user,
                                                           RestAPIStoreImpl storeRestClient) throws CCTestException {
        return generateUserAccessToken(apimServiceURLHttps, applicationId, user, storeRestClient);
    }

    public static String generateUserAccessTokenSandbox(String apimServiceURLHttps, String applicationId, User user,
                                                 RestAPIStoreImpl storeRestClient) throws CCTestException {
        return generateUserAccessTokenSandbox(apimServiceURLHttps, applicationId, user,
                new String[]{}, storeRestClient);
    }

    public static String generateUserAccessTokenProduction(String apimServiceURLHttps, String applicationId, User user,
                                                       String[] scopes, RestAPIStoreImpl storeRestClient)
            throws CCTestException {
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for the " +
                "Applications Registration event to be received by the CC");
        return generateUserAccessToken(apimServiceURLHttps,
                applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                scopes, user, storeRestClient);
    }

    public static String generateUserAccessTokenSandbox(String apimServiceURLHttps, String applicationId, User user,
                                                        String[] scopes, RestAPIStoreImpl storeRestClient) throws CCTestException {
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for the " +
                "Applications Registration event to be received by the CC");
        return generateUserAccessToken(apimServiceURLHttps,
                applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                scopes, user, storeRestClient);
    }

    /**
     * Generate the user access token for the grant type password.
     *
     * @param consumerKey    - consumer key
     * @param consumerSecret - consumer secret
     * @param scopes         - scopes
     * @param user           - user
     * @param storeRestClient - instance of the RestAPIStoreImpl
     * @return the user access token
     * @throws CCTestException if an error occurs while generating the user access token
     */
    public static String generateUserAccessToken(String apimServiceURLHttps, String consumerKey, String consumerSecret,
                                                 String[] scopes, User user, RestAPIStoreImpl storeRestClient)
            throws CCTestException {

        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() +
                "&scope=" + String.join(" ", scopes);
        JSONObject accessTokenGenerationResponse;
        try {
            URL tokenEndpointURL = new URL(apimServiceURLHttps + "oauth2/token");
            accessTokenGenerationResponse = new JSONObject(
                    storeRestClient.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                            .getData());
        } catch (MalformedURLException | APIManagerIntegrationTestException e) {
            throw new CCTestException("Error occurred while generating the user access token.", e);
        }
        return accessTokenGenerationResponse.getString("access_token");
    }

    /**
     * Subscribe to an API.
     *
     * @param apiId           - UUID of the API
     * @param applicationId   - UUID of the application
     * @param storeRestClient - Instance of APIPublisherRestClient
     * @return Subscription ID
     * @throws CCTestException if the response of the create subscription is null. This may null when there is an
     *                              error while subscribing to the API or when the subscription already exists.
     */
    public static String subscribeToAPI(String apiId, String applicationId, String tier,
                                          RestAPIStoreImpl storeRestClient) throws CCTestException {
        SubscriptionDTO subscription = new SubscriptionDTO();
        subscription.setApplicationId(applicationId);
        subscription.setApiId(apiId);
        subscription.setThrottlingPolicy(tier);
        subscription.setRequestedThrottlingPolicy(tier);
        try {
            SubscriptionDTO subscriptionResponse = storeRestClient.subscriptionIndividualApi.
                    subscriptionsPost(subscription, storeRestClient.tenantDomain);
            if (StringUtils.isEmpty(subscriptionResponse.getSubscriptionId())) {
                log.error("Error while subscribing to the API. API may not have been published.");
                throw new CCTestException(
                        "Error while subscribing to the API. API Id : " + apiId + ", Application Id: " + applicationId);
            }
            log.info("API Subscribed. " + getSubscriptionInfoString(apiId, applicationId, tier));
            return subscriptionResponse.getSubscriptionId();
        } catch (ApiException e) {
            log.error("Error msg from APIM: {}", e.getResponseBody());
            throw new CCTestException("Error while subscribing to API.", e);
        }

    }

    /**
     * Create the given application using the storeRestClient. Then, retrieve and return the client key and the
     * client secret associated to that application, using the dto AppWithConsumerKey.
     *
     * @param app          - definition of the application to be created
     * @param storeRestClient - an instance of RestAPIStoreImpl
     * @return the created application and associated client key and the client secret
     * @throws CCTestException if an error occurs while creating the application or generating keys
     */
    public static AppWithConsumerKey createApplicationWithKeys(Application app, RestAPIStoreImpl storeRestClient)
            throws CCTestException {
        String applicationId = createApplication(app, storeRestClient);
        ApplicationKeyDTO applicationKeyDTO = generateKeysForApp(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);
        return new AppWithConsumerKey(applicationId, applicationKeyDTO.getConsumerKey(),
                applicationKeyDTO.getConsumerSecret());
    }

    /**
     * Create Application in API Manager and return the App Id
     *
     * @param app               - Application dto object
     * @param storeRestClient   - an instance of RestAPIStoreImpl
     * @return appId
     * @throws CCTestException if an error occurs while creating the application
     */
    public static String createApplication(Application app, RestAPIStoreImpl storeRestClient) throws CCTestException {
        ApplicationDTO application = new ApplicationDTO();
        application.setName(app.getName());
        application.setDescription(app.getDescription());
        application.setThrottlingPolicy(app.getThrottleTier());
        application.setTokenType(app.getTokenType());
        try {
            ApplicationDTO createdApp = storeRestClient.applicationsApi.applicationsPost(application);
            if (StringUtils.isEmpty(createdApp.getApplicationId())) {
                throw new CCTestException("Could not create the application: " + app.getName());
            }
            log.info("Application Created. Name:" + app.getName() + " ThrottleTier:" + app.getThrottleTier());
            return createdApp.getApplicationId();
        } catch (ApiException e) {
            log.error("Error msg from APIM: {}", e.getResponseBody());
            throw new CCTestException("Error while subscribing to API.", e);
        }
    }

    /**
     * Generate Consumer key and Secret for an App
     *
     * @param appId             - App id
     * @param storeRestClient   - an instance of RestAPIStoreImpl
     * @return an ApplicationKeyDTO object containing Consumer key and Secret
     * @throws CCTestException if an error occurs while generating keys
     */
    public static ApplicationKeyDTO generateKeysForApp(String appId,
                                                       ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyType,
                                                       RestAPIStoreImpl storeRestClient) throws CCTestException {
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequest = new ApplicationKeyGenerateRequestDTO();
        applicationKeyGenerateRequest.setValidityTime(TestConstant.DEFAULT_TOKEN_VALIDITY_TIME);
        applicationKeyGenerateRequest.setCallbackUrl("");
        applicationKeyGenerateRequest.setKeyType(keyType);
        applicationKeyGenerateRequest.setScopes(null);
        applicationKeyGenerateRequest.setGrantTypesToBeSupported(grantTypes);
        try {
        ApiResponse<ApplicationKeyDTO> response = storeRestClient.applicationKeysApi.
                applicationsApplicationIdGenerateKeysPostWithHttpInfo(appId, applicationKeyGenerateRequest,
                        storeRestClient.tenantDomain);
            return response.getData();
        } catch (ApiException e) {
            throw new CCTestException("Error while generating consumer keys from APIM Store", e);
        }
    }


    public static String getSubscriptionInfoString(String apiId, String applicationId, String tier) {
        return "API_Id:" + apiId + " Application_Id:" + applicationId + " Tier:" + tier;
    }


    /**
     * Remove all Subscriptions and Applications accessible via a given Store REST API client
     * @param storeRestClient - an instance of RestAPIStoreImpl
     * @throws CCTestException if an error occurs while removing Subscriptions and Applications from API Manager
     */
    public static void removeAllSubscriptionsAndAppsFromStore(RestAPIStoreImpl storeRestClient) throws CCTestException {
        if (Objects.isNull(storeRestClient)) {
            return;
        }
        try {
            ApplicationListDTO applicationListDTO = storeRestClient.getAllApps();
            if (applicationListDTO.getList() != null) {
                for (ApplicationInfoDTO applicationInfoDTO : applicationListDTO.getList()) {
                    SubscriptionListDTO subsDTO = storeRestClient
                            .getAllSubscriptionsOfApplication(applicationInfoDTO.getApplicationId());
                    if (subsDTO != null && subsDTO.getList() != null) {
                        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
                            storeRestClient.subscriptionIndividualApi.
                                    subscriptionsSubscriptionIdDeleteWithHttpInfo(subscriptionDTO.getSubscriptionId(), null);
                            log.info("Deleted Subscription. API: {} App: {}",
                                    subscriptionDTO.getApiInfo().getName(), subscriptionDTO.getApplicationInfo().getName());
                        }
                    }
                    if (!APIMIntegrationConstants.OAUTH_DEFAULT_APPLICATION_NAME.equals(applicationInfoDTO.getName())) {
                        storeRestClient.deleteApplication(applicationInfoDTO.getApplicationId());
                        log.info("Deleted App {}", applicationInfoDTO.getName());
                    }
                }
            } else {
                throw new CCTestException("Error while removing Apps from Store. Empty ApplicationListDTO");
            }
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            throw new CCTestException("Error while removing Subscriptions and Apps from Store", e);
        }
    }

    public static void removeAllSubscriptionsForAnApp(String appId, RestAPIStoreImpl storeRestClient) throws CCTestException {
        if (Objects.isNull(storeRestClient)) {
            return;
        }
        try {
            SubscriptionListDTO subsDTO = storeRestClient
                    .getAllSubscriptionsOfApplication(appId);
            if (subsDTO != null && subsDTO.getList() != null) {
                for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
                    storeRestClient.subscriptionIndividualApi.
                            subscriptionsSubscriptionIdDeleteWithHttpInfo(subscriptionDTO.getSubscriptionId(), null);
                }
            } else {
                throw new CCTestException(
                        "Error while removing Subscriptions and Apps from Store. Empty SubscriptionListDTO.");
            }
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            throw new CCTestException("Error while removing Subscriptions and Apps from Store", e);
        }
    }
}
