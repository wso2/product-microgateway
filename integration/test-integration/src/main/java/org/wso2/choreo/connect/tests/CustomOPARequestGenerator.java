/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests;

import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.opa.OPARequestGenerator;
import org.wso2.choreo.connect.enforcer.commons.opa.OPASecurityException;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;

import java.util.Map;

public class CustomOPARequestGenerator implements OPARequestGenerator {
    @Override
    public String generateRequest(String policyName, String rule, Map<String, String> additionalParameters,
                                  RequestContext requestContext) throws OPASecurityException {
        JSONObject requestPayload = new JSONObject();
        JSONObject inputPayload = new JSONObject();
        requestPayload.put("input", inputPayload);
        JSONObject transportHeaders = new JSONObject(requestContext.getHeaders());
        inputPayload.put("headers", transportHeaders);
        return requestPayload.toString();
    }

    @Override
    public boolean handleResponse(String policyName, String rule, String opaResponse,
                                  Map<String, String> additionalParameters, RequestContext requestContext)
            throws OPASecurityException {
        try {
            JSONObject response = new JSONObject(opaResponse);
            return response.getBoolean("result");
        } catch (JSONException e) {
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.OPA_RESPONSE_FAILURE, e);
        }
    }
}
