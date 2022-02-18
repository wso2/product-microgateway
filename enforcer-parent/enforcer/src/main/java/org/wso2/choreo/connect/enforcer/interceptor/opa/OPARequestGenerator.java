/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.interceptor.opa;

import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;

import java.util.Map;

/**
 * OPA request generator interface to handle OPA policy validation payload and validation response.
 */
public interface OPARequestGenerator {

    /**
     * Generate the OPA request payload from the provided request context and the additional Properties Map.
     *
     * @param policyName         Name of the policy validated
     * @param rule               Rule of the policy
     * @param advancedProperties Advanced properties that can be used to construct the opa payload
     * @param requestContext     Request context details to be validated
     * @return json payload as an array of bytes be sent to the OPA server for validation
     * @throws OPASecurityException If an authentication failure or some other error occurs
     */
    String generateRequest(String policyName, String rule, Map<String, Object> advancedProperties,
                           RequestContext requestContext) throws OPASecurityException;
    // TODO: (renuka) returns byte[]? string in https://github.com/wso2/wso2-synapse/pull/1899/files
    // TODO: (renuka) method name

    /**
     * Authenticates the given request using the authenticators which have been initialized.
     *
     * @param policyName     Name of the policy validated
     * @param rule           Rule of the policy
     * @param opaResponse    OPA response to be validated
     * @param requestContext Request context details to be validated
     * @throws OPASecurityException If an authentication failure or some other error occurs
     */
    boolean validateResponse(String policyName, String rule, String opaResponse, RequestContext requestContext)
            throws OPASecurityException;
    // TODO: (renuka) method name in https://github.com/wso2/wso2-synapse/pull/1899/files
    // TODO: (renuka) return exception for auth error, without bool (but we can log minor level, if bool is returned)
}
