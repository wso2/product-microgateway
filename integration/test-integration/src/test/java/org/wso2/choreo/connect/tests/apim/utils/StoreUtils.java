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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
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
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(applicationId, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for the " +
                "Applications Registration event to be received by the CC");
        return StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, storeRestClient);
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
        HttpResponse response = storeRestClient.createSubscription(apiId, applicationId, tier);
        if (Objects.isNull(response)) {
            throw new CCTestException(
                    "Error while subscribing to the API. API Id : " + apiId + ", Application Id: " + applicationId);
        }
        if (!(response.getResponseCode() == HttpStatus.SC_OK &&
                !StringUtils.isEmpty(response.getData()))) {
            throw new CCTestException("Error in API Subscribe." +
                    getSubscriptionInfoString(apiId, applicationId, tier) +
                    "Response Code:" + response.getResponseCode());
        }
        log.info("API Subscribed. " + getSubscriptionInfoString(apiId, applicationId, tier));
        return response.getData();
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
        ApplicationKeyDTO applicationKeyDTO = generateKeysForApp(applicationId, storeRestClient);
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
        HttpResponse applicationResponse = storeRestClient.createApplication(app.getName(), app.getDescription(),
                app.getThrottleTier(), app.getTokenType());
        if (Objects.isNull(applicationResponse)) {
            throw new CCTestException("Could not create the application: " + app.getName());
        }
        log.info("Application Created. Name:" + app.getName() + " ThrottleTier:" + app.getThrottleTier());
        return applicationResponse.getData();
    }

    /**
     * Generate Consumer key and Secret for an App
     *
     * @param appId             - App id
     * @param storeRestClient   - an instance of RestAPIStoreImpl
     * @return an ApplicationKeyDTO object containing Consumer key and Secret
     * @throws CCTestException if an error occurs while generating keys
     */
    public static ApplicationKeyDTO generateKeysForApp(String appId, RestAPIStoreImpl storeRestClient) throws CCTestException {
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO;
        try {
            applicationKeyDTO = storeRestClient.generateKeys(appId,
                    TestConstant.DEFAULT_TOKEN_VALIDITY_TIME, "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                    null, grantTypes);
        } catch (ApiException e) {
            throw new CCTestException("Error while generating consumer keys from APIM Store", e);
        }
        return applicationKeyDTO;
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
                            storeRestClient.removeSubscription(subscriptionDTO.getSubscriptionId());
                        }
                    }
                    if (!APIMIntegrationConstants.OAUTH_DEFAULT_APPLICATION_NAME.equals(applicationInfoDTO.getName())) {
                        storeRestClient.deleteApplication(applicationInfoDTO.getApplicationId());
                    }
                }
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
                    storeRestClient.removeSubscription(subscriptionDTO.getSubscriptionId());
                }
            }
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            throw new CCTestException("Error while removing Subscriptions and Apps from Store", e);
        }
    }
}
