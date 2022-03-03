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

package org.wso2.choreo.connect.enforcer.commons.opa;

import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;

import java.util.Map;

/**
 * OPA request generator interface to handle OPA policy validation payload and validation response.
 */
public interface OPARequestGenerator {

    /**
     * Generate the OPA request payload from the provided request context and the additional Properties Map.
     *
     * @param policyName           Name of the policy validated.
     * @param rule                 Rule of the policy.
     * @param additionalParameters Advanced properties that can be used to construct the opa payload.
     * @param requestContext       Request context details to be validated.
     * @return json payload as a string be sent to the OPA server for validation.
     * @throws OPASecurityException If an authentication failure or system error occurs.
     */
    String generateRequest(String policyName, String rule, Map<String, String> additionalParameters,
                           RequestContext requestContext) throws OPASecurityException;

    /**
     * Validate the OPA response and handle request context based on the response.
     *
     * @param policyName           Name of the policy validated.
     * @param rule                 Rule of the policy.
     * @param opaResponse          OPA response to be validated.
     * @param additionalParameters Advanced properties that can be used to construct the opa payload.
     * @param requestContext       Request context details to be validated.
     * @return <code>true</code> if valid, <code>false</code> otherwise.
     * @throws OPASecurityException If an authentication failure or system error occurs.
     */
    boolean handleResponse(String policyName, String rule, String opaResponse, Map<String, String> additionalParameters,
                           RequestContext requestContext) throws OPASecurityException;
}
