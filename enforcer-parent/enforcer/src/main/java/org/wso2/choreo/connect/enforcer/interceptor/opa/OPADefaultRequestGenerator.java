package org.wso2.choreo.connect.enforcer.interceptor.opa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;

import java.util.Map;

public class OPADefaultRequestGenerator implements OPARequestGenerator {
    private static final Logger log = LogManager.getLogger(OPADefaultRequestGenerator.class);

    @Override
    public String generateRequest(String policyName, String rule, Map<String, Object> advancedProperties, RequestContext requestContext) throws OPASecurityException {
        JSONObject requestPayload = new JSONObject();
        JSONObject inputPayload = new JSONObject();
        requestPayload.put("input", inputPayload);

        // following fields are the same fields sent from the synapse request generator
        inputPayload.put("requestOrigin", requestContext.getClientIp());
        inputPayload.put("method", requestContext.getRequestMethod());
        inputPayload.put("path", requestContext.getRequestPath());
        inputPayload.put("httpVersion", "");
        inputPayload.put("transportHeaders", requestContext.getHeaders());
        inputPayload.put("requestBody", requestContext.getRequestPayload());
        inputPayload.put("authenticationContext", requestContext.getAuthenticationContext());

        System.out.println("reqToOPA: " + requestPayload);
        return requestPayload.toString();
//        return "{\"input\":{\"servers\":[{\"id\":\"app\",\"protocols\":[\"https\",\"ssh\"],\"ports\":[\"p1\",\"p2\",\"p3\"]},{\"id\":\"db\",\"protocols\":[\"mysql\"],\"ports\":[\"p3\"]},{\"id\":\"cache\",\"protocols\":[\"memcache\"],\"ports\":[\"p3\"]},{\"id\":\"ci\",\"protocols\":[\"http\"],\"ports\":[\"p1\",\"p2\"]},{\"id\":\"busybox\",\"protocols\":[\"telnet\"],\"ports\":[\"p1\"]}],\"networks\":[{\"id\":\"net1\",\"public\":false},{\"id\":\"net2\",\"public\":false},{\"id\":\"net3\",\"public\":true},{\"id\":\"net4\",\"public\":true}],\"ports\":[{\"id\":\"p1\",\"network\":\"net1\"},{\"id\":\"p2\",\"network\":\"net3\"},{\"id\":\"p3\",\"network\":\"net2\"}]}}";
    }

    @Override
    public boolean validateResponse(String policyName, String rule, String opaResponse, RequestContext requestContext) throws OPASecurityException {
        System.out.println(opaResponse);
        JSONObject response = new JSONObject(opaResponse);
        try {
            return response.getBoolean("result");
        } catch (JSONException e) { //TODO: (renuka) runtime exc
            log.error("Error parsing OPA JSON response, the field \"result\" not found or not a Boolean", e);
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR,
                    APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE, e);
        }
    }
}
