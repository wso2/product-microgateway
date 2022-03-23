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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.opa.OPAConstants;
import org.wso2.choreo.connect.enforcer.commons.opa.OPARequestGenerator;
import org.wso2.choreo.connect.enforcer.commons.opa.OPASecurityException;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;

import java.util.Arrays;
import java.util.Map;

/**
 * Default implementation of the {@link OPARequestGenerator}.
 */
public class OPADefaultRequestGenerator implements OPARequestGenerator {
    private static final Logger log = LogManager.getLogger(OPADefaultRequestGenerator.class);

    @Override
    public String generateRequest(String policyName, String rule, Map<String, String> additionalParameters,
                                  RequestContext requestContext) throws OPASecurityException {
        JSONObject requestPayload = new JSONObject();
        JSONObject inputPayload = new JSONObject();
        requestPayload.put("input", inputPayload);

        // following fields are the same fields sent from the synapse request generator
        JSONObject transportHeaders = new JSONObject(requestContext.getHeaders());
        transportHeaders.remove(StringUtils.lowerCase(requestContext.getMatchedAPI().getAuthHeader()));
        inputPayload.put("transportHeaders", transportHeaders);
        inputPayload.put("requestOrigin", requestContext.getClientIp());
        inputPayload.put("method", requestContext.getRequestMethod());
        inputPayload.put("path", requestContext.getRequestPath());
        inputPayload.put("httpVersion", requestContext.getHttpProtocol()); // TODO (renuka): remove httpVersion

        // API context
        JSONObject apiContext = new JSONObject();
        inputPayload.put("apiContext", apiContext);
        apiContext.put("apiName", requestContext.getMatchedAPI().getName());
        apiContext.put("apiVersion", requestContext.getMatchedAPI().getVersion());
        apiContext.put("orgId", requestContext.getMatchedAPI().getOrganizationId());
        apiContext.put("vhost", requestContext.getMatchedAPI().getVhost());
        apiContext.put("pathTemplate", requestContext.getRequestPathTemplate());
        apiContext.put("prodClusterName", requestContext.getProdClusterHeader());
        apiContext.put("sandClusterName", requestContext.getSandClusterHeader());

        // Authentication Context
        if (Boolean.parseBoolean(additionalParameters.get(OPAConstants.AdditionalParameters.SEND_ACCESS_TOKEN))) {
            AuthenticationContext authContext = requestContext.getAuthenticationContext();
            JSONObject authContextPayload = new JSONObject();
            authContextPayload.put("token", authContext.getRawToken());
            authContextPayload.put("tokenType", authContext.getTokenType());
            authContextPayload.put("keyType", authContext.getKeyType());
            inputPayload.put("authenticationContext", authContextPayload);
        }

        // Additional Properties
        // In APIM additional parameter are appended to the main input payload, handle the same in Choreo Connect
        String addProps = additionalParameters.get(OPAConstants.AdditionalParameters.ADDITIONAL_PROPERTIES);
        if (StringUtils.isNotEmpty(addProps)) {
            Arrays.stream(addProps.split(OPAConstants.AdditionalParameters.PARAM_SEPARATOR))
                    .forEach(key -> inputPayload.put(key, requestContext.getProperties().get(key)));
        }

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
            log.error("Error parsing OPA JSON response, the field \"result\" not found or not a Boolean" +
                    ", response: {}", opaResponse, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6104), e);
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.OPA_RESPONSE_FAILURE, e);
        }
    }
}
