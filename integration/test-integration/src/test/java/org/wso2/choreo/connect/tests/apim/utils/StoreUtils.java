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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class StoreUtils {
    private static final Logger log = LoggerFactory.getLogger(StoreUtils.class);

    /**
     * Generate the user access token for the grant type password.
     *
     * @param consumerKey    - consumer key
     * @param consumerSecret - consumer secret
     * @param scopes         - scopes
     * @param user           - user
     * @param restAPIStore   - instance of the RestAPIStoreImpl
     * @return the user access token
     * @throws MalformedURLException if the URL is malformed
     * @throws CCTestException  if an error occurs while generating the user access token
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
     * @return HttpResponse - Response of the API subscribe action
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
        log.info("API Subscribed :" + getSubscriptionInfoString(apiId, applicationId, tier));
        return apiId;
    }

    /**
     * Create the given application using the apimStoreClient. Then, create the client key and the client secret
     * associated to that application.
     *
     * @param app          - definition of the application to be created
     * @param storeRestClient - instance of the RestAPIStoreImpl
     * @return the created application and associated client key and the client secret
     * @throws org.wso2.am.integration.clients.store.api.ApiException if error happens while generating the keys
     * @throws CCTestException                                   if error happens while creating the application
     *                                                                or if the application already exists
     */
    public static AppWithConsumerKey createApplicationWithKeys(Application app, RestAPIStoreImpl storeRestClient)
            throws CCTestException {
        HttpResponse applicationResponse = storeRestClient.createApplication(app.getName(), app.getDescription(),
                app.getThrottleTier(), app.getTokenType());
        if (Objects.isNull(applicationResponse)) {
            throw new CCTestException("Could not create the application: " + app.getName());
        }
        String applicationId = applicationResponse.getData();

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO;
        try {
            applicationKeyDTO = storeRestClient.generateKeys(applicationId,
                    TestConstant.DEFAULT_TOKEN_VALIDITY_TIME, "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                    null, grantTypes);
        } catch (ApiException e) {
            throw new CCTestException("Error while generating consumer keys from APIM Store", e);
        }

        return new AppWithConsumerKey(applicationId, applicationKeyDTO.getConsumerKey(),
                applicationKeyDTO.getConsumerSecret());
    }

    public static String getSubscriptionInfoString(String apiId, String applicationId, String tier) {
        return "API Id : " + apiId + ", Application Id: " + applicationId + " Tier: " + tier;
    }


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
}
