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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * API Key authenticator.
 */
public class APIKeyAuthenticator extends JWTAuthenticator {

    private static final Logger log = LogManager.getLogger(APIKeyAuthenticator.class);

    public APIKeyAuthenticator() {
        super();
        log.debug("API key authenticator initialized.");
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {

        String apiKeyValue = getAPIKeyFromRequest(requestContext);
        return apiKeyValue != null && apiKeyValue.startsWith(APIKeyConstants.API_KEY_PREFIX) &&
                apiKeyValue.length() > 10;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {

        AuthenticationContext authCtx = super.authenticate(requestContext);
        // Drop the API key data from the API key header.
        dropAPIKeyDataFromAPIKeyHeader(requestContext);
        return authCtx;
    }

    private void dropAPIKeyDataFromAPIKeyHeader(RequestContext requestContext) throws APISecurityException {

        String apiKeyHeaderValue = getAPIKeyFromRequest(requestContext).trim();
        String checksum = apiKeyHeaderValue.substring(apiKeyHeaderValue.length() - 6);
        JSONObject jsonObject = getDecodedAPIKeyData(apiKeyHeaderValue);
        jsonObject.remove(APIKeyConstants.API_KEY_JSON_KEY);
        // Update the header with the new API key value.
        if (!jsonObject.isEmpty()) {
            String encodedKeyData = Base64.getEncoder().encodeToString(jsonObject.toJSONString().getBytes());
            String newAPIKeyHeaderValue = APIKeyConstants.API_KEY_PREFIX + encodedKeyData + checksum;
            // Add the new header.
            requestContext.addOrModifyHeaders(APIKeyConstants.INTERNAL_API_KEY_HEADER, newAPIKeyHeaderValue);
        }
    }

    private String getAPIKeyFromRequest(RequestContext requestContext) {
        Map<String, String> headers = requestContext.getHeaders();
        return getAPIKeyInternalHeaders(requestContext).stream()
                .map(headers::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<String> getAPIKeyInternalHeaders(RequestContext requestContext) {
        APIConfig apiConfig = requestContext.getMatchedAPI();
        String configuredApiKeyHeader = apiConfig.getApiKeyHeader();
        List<String> apiKeyInternalHeaders
                = ConfigHolder.getInstance().getConfig().getApiKeyConfig().getApiKeyInternalHeaders();
        if (StringUtils.isNotEmpty(configuredApiKeyHeader)) {
            return Stream.concat(apiKeyInternalHeaders.stream(), Stream.of(configuredApiKeyHeader))
                    .distinct()
                    .collect(Collectors.toList());
        }
        return apiKeyInternalHeaders;
    }


    private JSONObject getDecodedAPIKeyData(String apiKeyHeaderValue) throws APISecurityException {
        try {
            // Skipping the prefix(`chk_`) and checksum.
            String apiKeyData = apiKeyHeaderValue.substring(4, apiKeyHeaderValue.length() - 6);
            // Base 64 decode key data.
            String decodedKeyData = new String(Base64.getDecoder().decode(apiKeyData));
            // Convert data into JSON.
            return (JSONObject) JSONValue.parse(decodedKeyData);
        } catch (Exception e) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
    }

    @Override
    protected String retrieveTokenFromRequestCtx(RequestContext requestContext) throws APISecurityException {

        String apiKey = getAPIKeyFromRequest(requestContext).trim();
        if (!APIKeyUtils.isValidAPIKey(apiKey)) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
        String keyHash = APIKeyUtils.generateAPIKeyHash(apiKey);
        String componentId = requestContext.getMatchedAPI().getChoreoComponentInfo().getComponentID();
        if (componentId == null) {
            componentId = "";
        }
        String apiKeyId = keyHash + APIKeyConstants.API_KEY_ID_SEPARATOR + componentId;
        Object cachedJWT = CacheProvider.getGatewayAPIKeyJWTCache().getIfPresent(apiKeyId);
        if (cachedJWT != null && !APIKeyUtils.isJWTExpired((String) cachedJWT)) {
            if (log.isDebugEnabled()) {
                log.debug("Token retrieved from the cache. Token: " + FilterUtils.getMaskedToken(keyHash));
            }
            return (String) cachedJWT;
        }
        // Exchange the API Key to a JWT token.
        Optional<String> jwt = APIKeyUtils.exchangeAPIKeyToJWT(apiKeyId);
        if (jwt.isEmpty()) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
        // Cache the JWT token.
        CacheProvider.getGatewayAPIKeyJWTCache().put(apiKeyId, jwt.get());
        return jwt.get();
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
