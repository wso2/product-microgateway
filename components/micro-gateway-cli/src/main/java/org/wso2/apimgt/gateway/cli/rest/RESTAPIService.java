/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.rest;

import org.wso2.apimgt.gateway.cli.model.rest.ClientCertMetadataDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;

import java.util.List;

/**
 * Interface to communicate with Third party rest API for fetching
 * APIs. As an example usage, Implementations of this interface will
 * be used by import command to communicate with API Provider.
 */
public interface RESTAPIService {

    /**
     * Get APIs of given label.
     *
     * @param labelName   label name
     * @param accessToken access token
     * @return list of APIs belong to the given label
     */
    List<ExtendedAPI> getAPIs(String labelName, String accessToken);

    /**
     * Gets the API specified by name and version.
     *
     * @param apiName     Name of the API
     * @param version     Version of the API
     * @param accessToken access token
     * @return the API specified by name and version by calling the Publisher REST API
     */
    ExtendedAPI getAPI(String apiName, String version, String accessToken);

    /**
     * Get list of application.
     *
     * @param accessToken access token
     * @return list of application policies
     */
    List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String accessToken);

    /**
     * Get list of subsription policies.
     *
     * @param accessToken access token
     * @return list of subscription policies
     */
    List<SubscriptionThrottlePolicyDTO> getSubscriptionPolicies(String accessToken);

    /**
     * Get list of client certificates.
     *
     * @param accessToken access token
     * @return list of  client certificates
     */
    List<ClientCertMetadataDTO> getClientCertificates(String accessToken);
}
