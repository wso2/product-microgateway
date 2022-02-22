package org.wso2.choreo.connect.enforcer.interceptor.opa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the {@link OPARequestGenerator}.
 */
public class OPADefaultRequestGenerator implements OPARequestGenerator {
    private static final Logger log = LogManager.getLogger(OPADefaultRequestGenerator.class);

    @Override
    public String generateRequest(String policyName, String rule, Map<String, Object> advancedProperties,
                                  RequestContext requestContext) throws OPASecurityException {
        JSONObject requestPayload = new JSONObject();
        JSONObject inputPayload = new JSONObject();
        requestPayload.put("input", inputPayload);

        // following fields are the same fields sent from the synapse request generator
        inputPayload.put("transportHeaders", requestContext.getHeaders());
        inputPayload.put("requestOrigin", requestContext.getClientIp());
        inputPayload.put("method", requestContext.getRequestMethod());
        inputPayload.put("path", requestContext.getRequestPath());
        inputPayload.put("httpVersion", requestContext.getHttpProtocol());

        // additional fields from choreo connect
        inputPayload.put("apiName", requestContext.getMatchedAPI().getName());
        inputPayload.put("apiVersion", requestContext.getMatchedAPI().getVersion());
        inputPayload.put("orgId", requestContext.getMatchedAPI().getOrganizationId());
        inputPayload.put("vhost", requestContext.getMatchedAPI().getVhost());
        inputPayload.put("pathTemplate", requestContext.getRequestPathTemplate());
        inputPayload.put("prodClusterName", requestContext.getProdClusterHeader());
        inputPayload.put("sandClusterName", requestContext.getSandClusterHeader());
        inputPayload.put("requestBody", requestContext.getRequestPayload()); // TODO: do we need payload?

        // Authentication Context
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        Map<String, String> authContextPayload = new HashMap<>();
        authContextPayload.put("token", authContext.getRawToken());
        authContextPayload.put("tokenType", authContext.getTokenType());
        authContextPayload.put("keyType", authContext.getKeyType());
        inputPayload.put("authenticationContext", authContextPayload);
        return requestPayload.toString();
    }

    @Override
    public boolean validateResponse(String policyName, String rule, String opaResponse, RequestContext requestContext)
            throws OPASecurityException {
        JSONObject response = new JSONObject(opaResponse);
        try {
            return response.getBoolean("result");
        } catch (JSONException e) { //TODO: catch runtime ex?
            log.error("Error parsing OPA JSON response, the field \"result\" not found or not a Boolean",
                    ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6104), e);
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.REMOTE_AUTHORIZATION_RESPONSE_FAILURE,
                    "Error while evaluating remote authorization response", e);
        }
    }
}
