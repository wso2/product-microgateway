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

package org.wso2.choreo.connect.enforcer.subscription;

import org.wso2.choreo.connect.discovery.subscription.APIs;
import org.wso2.choreo.connect.enforcer.models.API;
import org.wso2.choreo.connect.enforcer.models.ApiPolicy;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicy;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;

import java.util.List;

/**
 * A Facade for obtaining Subscription related Data.
 */
public interface SubscriptionDataStore {

    /**
     * Gets an {@link Application} by Id.
     *
     * @param appUUID Id of the Application
     * @return {@link Application} with the appId
     */
    Application getApplicationById(String appUUID);

    /**
     * Gets the {@link ApplicationKeyMapping} entry by Key.
     *
     * @param key        <ApplicationIs>.<keyType>
     * @param keyManager Keymanager Name
     * @return {@link ApplicationKeyMapping} entry
     */
    ApplicationKeyMapping getKeyMappingByKeyAndKeyManager(String key, String keyManager);

    /**
     * Get API by Context and Version.
     *
     * @param uuid UUID of the API
     * @return {@link API} entry represented by Context and Version.
     */
    API getApiByContextAndVersion(String uuid);

    /**
     * Gets Subscription by ID.
     *
     * @param appUUID Application associated with the Subscription (uuid)
     * @param apiUUID Api associated with the Subscription (uuid)
     * @return {@link Subscription}
     */
    Subscription getSubscriptionById(String appUUID, String apiUUID);

    /**
     * Gets API Throttling Policy by the name and Tenant Id.
     *
     * @param policyName Name of the Throttling Policy
     * @return API Throttling Policy
     */
    ApiPolicy getApiPolicyByName(String policyName);

    /**
     * Gets Subscription Throttling Policy by the name and Tenant Id.
     *
     * @param policyName Name of the Throttling Policy
     * @return Subscription Throttling Policy
     */
    SubscriptionPolicy getSubscriptionPolicyByName(String policyName);

    /**
     * Gets Application Throttling Policy by the name and Tenant Id.
     *
     * @param policyName Name of the Throttling Policy
     * @return Application Throttling Policy
     */
    ApplicationPolicy getApplicationPolicyByName(String policyName);

    void addSubscriptions(List<org.wso2.choreo.connect.discovery.subscription.Subscription> subscriptionList);

    void addApplications(List<org.wso2.choreo.connect.discovery.subscription.Application> applicationList);

    void addApis(List<APIs> apisList);

    void addApplicationPolicies(
            List<org.wso2.choreo.connect.discovery.subscription.ApplicationPolicy> applicationPolicyList);

    void addSubscriptionPolicies(
            List<org.wso2.choreo.connect.discovery.subscription.SubscriptionPolicy> subscriptionPolicyList);

    void addApplicationKeyMappings(
            List<org.wso2.choreo.connect.discovery.subscription.ApplicationKeyMapping> applicationKeyMappingList);

    void addOrUpdateApplication(Application application);

    void addOrUpdateSubscription(Subscription subscription);

    void addOrUpdateAPI(API api);

    void addOrUpdateAPIWithUrlTemplates(API api);

    void addOrUpdateApplicationKeyMapping(ApplicationKeyMapping applicationKeyMapping);

    void addOrUpdateSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy);

    void addOrUpdateApplicationPolicy(ApplicationPolicy applicationPolicy);

    void addOrUpdateApiPolicy(ApiPolicy apiPolicy);

    void removeApplication(Application application);

    void removeAPI(API api);

    void removeSubscription(Subscription subscription);

    void removeApplicationKeyMapping(ApplicationKeyMapping applicationKeyMapping);

    void removeSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy);

    void removeApplicationPolicy(ApplicationPolicy applicationPolicy);

    void removeApiPolicy(ApiPolicy apiPolicy);

    API getDefaultApiByContext(String context);

    /**
     * Filter the applications map based on the criteria.
     * @param name Application Name
     * @param organizationID Application tenant domain/ organization id
     * @param uuid Application uuid.
     * @return List of applications which match the given parameters
     */
    List<Application> getMatchingApplications(String name, String organizationID, String uuid);


    /**
     * Filter the application key mapping map based on provided parameters
     * @param applicationUUID  Application uuid
     * @param consumerKey The application consumer key
     * @return List of key mappings which match the given parameters
     */
    List<ApplicationKeyMapping> getMatchingKeyMapping(String applicationUUID, String consumerKey);


    /**
     * Filter the subscriptions map based on the provided parameters
     * @param applicationUUID UUID of the application
     * @param apiUUID UUID of the api
     * @param state Subscription state
     * @return A List of subscriptions which matches the given parameters
     */
    List<Subscription> getMatchingSubscriptions(String applicationUUID, String apiUUID, String state);

    /**
     * Returns the list of application policies which matches the given policy name
     * @param policyName The name of the policy
     * @return
     */
    List<ApplicationPolicy> getMatchingApplicationPolicies(String policyName);

    /**
     *  Returns the list of subscription policies which matches the given policy name
     * @param policyName The name of the policy
     * @return
     */
    List<SubscriptionPolicy> getMatchingSubscriptionPolicies(String policyName);

}
