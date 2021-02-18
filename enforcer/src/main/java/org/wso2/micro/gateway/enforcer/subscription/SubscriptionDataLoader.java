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

import org.wso2.micro.gateway.enforcer.exception.DataLoadingException;
import org.wso2.micro.gateway.enforcer.models.API;
import org.wso2.micro.gateway.enforcer.models.ApiPolicy;
import org.wso2.micro.gateway.enforcer.models.Application;
import org.wso2.micro.gateway.enforcer.models.ApplicationKeyMapping;
import org.wso2.micro.gateway.enforcer.models.ApplicationPolicy;
import org.wso2.micro.gateway.enforcer.models.Subscription;
import org.wso2.micro.gateway.enforcer.models.SubscriptionPolicy;

import java.util.List;

/**
 * This interface abstracts Data Loading operations. Interface will be consumed by
 * {@link SubscriptionDataStore} while populating in memory storage. The entries can be
 * fetched directly by the Database or by calling a service.
 */
public interface SubscriptionDataLoader {

    /**
     * Loads all subscribers from underlying Storage.
     *
     * @return A list of all {@link Subscription} objects at the time of calling.
     * @throws DataLoadingException If any error
     */
    public List<Subscription> loadAllSubscriptions(String tenantDomain) throws DataLoadingException;

    /**
     * Load all Applications from the Database belonging to all Tenants.
     *
     * @return A list of all {@link Application}s.
     * @throws DataLoadingException If any error
     */
    public List<Application> loadAllApplications(String tenantDomain) throws DataLoadingException;

    /**
     * Load all Key Mappings (Mapping between the Consumer Key and Application) from the Database.
     * owned by all tenants.
     *
     * @return A list of {@link ApplicationKeyMapping}s
     * @throws DataLoadingException If any error
     */
    public List<ApplicationKeyMapping> loadAllKeyMappings(String tenantDomain) throws DataLoadingException;

    /**
     * Load all {@link API} objects owned by all Tenants.
     *
     * @return A list of {@link API}
     * @throws DataLoadingException If any error
     */
    public List<API> loadAllApis(String tenantDomain) throws DataLoadingException;

    /**
     * Load All Subscription Throttling Policies.
     *
     * @return A list of Subscription Throttling Policies.
     * @throws DataLoadingException If any error
     */
    public List<SubscriptionPolicy> loadAllSubscriptionPolicies(String tenantDomain) throws DataLoadingException;

    /**
     * Load All API Throttling Policies.
     *
     * @return A list of API Throttling Policies.
     * @throws DataLoadingException If any error
     */
    public List<ApiPolicy> loadAllAPIPolicies(String tenantDomain) throws DataLoadingException;

    /**
     * Loads All Application Throttling Policies.
     *
     * @return A list of Api Throttling Policies.
     * @throws DataLoadingException If any error
     */
    public List<ApplicationPolicy> loadAllAppPolicies(String tenantDomain) throws DataLoadingException;

    /**
     * Retrieve Subscription from db.
     *
     * @return A {@link Subscription}.
     * @throws DataLoadingException If any error
     */
    public Subscription getSubscriptionById(String apiId, String appId) throws DataLoadingException;

    /**
     * Retrieve Application from db.
     *
     * @return An {@link Application}s.
     * @throws DataLoadingException If any error
     */
    public Application getApplicationById(int appId) throws DataLoadingException;

    /**
     * Retrieve Key Mapping (Mapping between the Consumer Key and Application) from the Database.
     *
     * @return A list of {@link ApplicationKeyMapping}s
     * @throws DataLoadingException If any error
     */
    public ApplicationKeyMapping getKeyMapping(String consumerKey) throws DataLoadingException;

    /**
     * Retrieve {@link API} object.
     *
     * @param context context of the API
     * @param version Version of the API
     * @return An {@link API}
     * @throws DataLoadingException If any error
     */
    public API getApi(String context, String version) throws DataLoadingException;

    /**
     * Retrieve Subscription Throttling Policy.
     *
     * @return A {@link SubscriptionPolicy}.
     * @throws DataLoadingException If any error
     */
    public SubscriptionPolicy getSubscriptionPolicy(String policyName, String tenantDomain) throws DataLoadingException;

    /**
     * Retrieve Application Throttling Policy.
     *
     * @return A {@link ApplicationPolicy}.
     * @throws DataLoadingException If any error
     */
    public ApplicationPolicy getApplicationPolicy(String policyName, String tenantDomain) throws DataLoadingException;

    /**
     * Retrieve API Throttling Policy.
     * @param policyName name
     * @param tenantDomain tenant
     * @return A {@link ApiPolicy}.
     * @throws DataLoadingException If any error
     */
    public ApiPolicy getAPIPolicy(String policyName, String tenantDomain) throws DataLoadingException;

}

