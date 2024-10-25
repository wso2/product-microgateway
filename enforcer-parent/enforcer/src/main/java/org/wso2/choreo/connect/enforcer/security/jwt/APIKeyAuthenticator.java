/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.security.jwt;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;

import java.util.Base64;
import java.util.Map;

/**
 * API Key authenticator.
 */
public class APIKeyAuthenticator extends JWTAuthenticator {

    private static final Logger log = LogManager.getLogger(APIKeyAuthenticator.class);

    private static boolean isAPIKeyEnabled = false;

    static {
        if (System.getenv("API_KEY_ENABLED") != null) {
            isAPIKeyEnabled = Boolean.parseBoolean(System.getenv("API_KEY_ENABLED"));
        }
    }

    public APIKeyAuthenticator() {
        super();
        log.debug("API key authenticator initialized.");
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {

        if (!isAPIKeyEnabled) {
            return false;
        }
        String apiKeyValue = getAPIKeyFromRequest(requestContext);
        return apiKeyValue != null && apiKeyValue.startsWith(APIKeyConstants.API_KEY_PREFIX);
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {

        return super.authenticate(requestContext);
    }

    private String getAPIKeyFromRequest(RequestContext requestContext) {
        Map<String, String> headers = requestContext.getHeaders();
        return headers.get(ConfigHolder.getInstance().getConfig().getApiKeyConfig()
                .getApiKeyInternalHeader().toLowerCase());
    }

    @Override
    protected String retrieveTokenFromRequestCtx(RequestContext requestContext) {

        String apiKeyHeaderValue = getAPIKeyFromRequest(requestContext);
        // Skipping the prefix(`chk_`) and checksum.
        String apiKeyData = apiKeyHeaderValue.substring(4, apiKeyHeaderValue.length() - 6);
        // Base 64 decode key data.
        String decodedKeyData = new String(Base64.getDecoder().decode(apiKeyData));
        // Convert data into JSON.
        JSONObject jsonObject = (JSONObject) JSONValue.parse(decodedKeyData);
        // Extracting the jwt token.
        return jsonObject.getAsString(APIKeyConstants.API_KEY_JSON_KEY);
    }

    @Override
    public String getChallengeString() {
        return "";
    }

    @Override
    public String getName() {
        return "Choreo API Key";
    }

    @Override
    public int getPriority() {
        return 15;
    }
}
