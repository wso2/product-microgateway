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
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;

import java.util.List;

public interface RESTAPIService {

    /**
     * Check whether user has set the AUTH_HEADER as System property and if not set default value
     * @return Authorization Header value
     */

     String getAuthHeader();
    /**
     * Get APIs of given label
     *
     * @param labelName   label name
     * @param accessToken access token
     * @return list of APIs belong to the given label
     */
    List<APIInfoDTO> getAPIs(String labelName, String accessToken);

    /**
     * Gets the API specified by name and version
     *
     * @param apiName     Name of the API
     * @param version     Version of the API
     * @param accessToken access token
     * @return the API specified by name and version by calling the Publisher REST API
     */
    APIDTO getAPI(String apiName, String version, String accessToken);

    /**
     * Get list of application
     *
     * @param accessToken access token
     * @return list of application policies
     */
    List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String accessToken);

    /**
     * Get list of subsription policies
     *
     * @param accessToken access token
     * @return list of subscription policies
     */
    List<SubscriptionThrottlePolicyDTO> getSubscriptionPolicies(String accessToken);

    String getAPISwaggerDefinition(String apiId, String accessToken);

    /**
     * Get list of client certificates
     *
     * @param accessToken access token
     * @return list of  client certificates
     */
    List<ClientCertMetadataDTO> getClientCertificates(String accessToken);
}
