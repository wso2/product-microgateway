/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.enforcer.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.commons.Filter;
import org.wso2.choreo.connect.commons.model.APIConfig;
import org.wso2.choreo.connect.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.commons.model.EndpointCluster;
import org.wso2.choreo.connect.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.security.jwt.APIKeyAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.InternalAPIKeyAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.JWTAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.UnsecuredAPIAuthenticator;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This is the filter handling the authentication for the requests flowing through the gateway.
 */
public class AuthFilter implements Filter {
    private List<Authenticator> authenticators = new ArrayList<>();
    private static final Logger log = LogManager.getLogger(AuthFilter.class);

    @Override
    public void init(APIConfig apiConfig) {
        initializeAuthenticators(apiConfig);
    }

    private void initializeAuthenticators(APIConfig apiConfig) {
        //TODO: Check security schema and add relevant authenticators.
        boolean isOAuthProtected = true;
        boolean isMutualSSLProtected = false;
        boolean isBasicAuthProtected = false;
        boolean isApiKeyProtected = false;
        boolean isMutualSSLMandatory = false;
        boolean isOAuthBasicAuthMandatory = false;

        // Set security conditions
        if (apiConfig.getSecuritySchemas() == null) {
            isOAuthProtected = true;
        } else {
            for (String apiSecurityLevel : apiConfig.getSecuritySchemas()) {
                if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.DEFAULT_API_SECURITY_OAUTH2)) {
                    isOAuthProtected = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.API_SECURITY_MUTUAL_SSL)) {
                    isMutualSSLProtected = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.API_SECURITY_BASIC_AUTH)) {
                    isBasicAuthProtected = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.API_SECURITY_MUTUAL_SSL_MANDATORY)) {
                    isMutualSSLMandatory = true;
                } else if (apiSecurityLevel.trim().
                        equalsIgnoreCase(APIConstants.API_SECURITY_OAUTH_BASIC_AUTH_API_KEY_MANDATORY)) {
                    isOAuthBasicAuthMandatory = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase((APIConstants.API_SECURITY_API_KEY))) {
                    isApiKeyProtected = true;
                }
            }
        }

        // TODO: Set authenticators for isMutualSSLProtected, isBasicAuthProtected
        if (isOAuthProtected) {
            Authenticator jwtAuthenticator = new JWTAuthenticator();
            authenticators.add(jwtAuthenticator);
        }

        if (isApiKeyProtected) {
            APIKeyAuthenticator apiKeyAuthenticator = new APIKeyAuthenticator();
            authenticators.add(apiKeyAuthenticator);
        }

        Authenticator authenticator = new InternalAPIKeyAuthenticator(
                ConfigHolder.getInstance().getConfig().getAuthHeader().getTestConsoleHeaderName().toLowerCase());
        authenticators.add(authenticator);
    
        Authenticator unsecuredAPIAuthenticator = new UnsecuredAPIAuthenticator();
        authenticators.add(unsecuredAPIAuthenticator);

        authenticators.sort(new Comparator<Authenticator>() {
            @Override
            public int compare(Authenticator o1, Authenticator o2) {
                return (o1.getPriority() - o2.getPriority());
            }
        });
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {

        // It is required to skip the auth Filter if the lifecycle status is prototype
        if (APIConstants.PROTOTYPED_LIFE_CYCLE_STATUS.equals(
                requestContext.getMatchedAPI().getApiLifeCycleState())) {
            return true;
        }

        boolean canAuthenticated = false;
        for (Authenticator authenticator : authenticators) {
            if (authenticator.canAuthenticate(requestContext)) {
                canAuthenticated = true;
                AuthenticationResponse authenticateResponse = authenticate(authenticator, requestContext);
                if (authenticateResponse.isAuthenticated() && !authenticateResponse.isContinueToNextAuthenticator()) {
                    return true;
                }
            }
        }
        if (!canAuthenticated) {
            FilterUtils.setUnauthenticatedErrorToContext(requestContext);
        }
        log.error(
                "None of the authenticators were able to authenticate the request: " + requestContext.getRequestPath());
        //set WWW_AUTHENTICATE header to error response
        requestContext.addOrModifyHeaders(APIConstants.WWW_AUTHENTICATE, getAuthenticatorsChallengeString() +
                ", error=\"invalid_token\"" +
                ", error_description=\"The provided token is invalid\"");
        return false;
    }

    private AuthenticationResponse authenticate(Authenticator authenticator, RequestContext requestContext) {
        try {
            AuthenticationContext  authenticate = authenticator.authenticate(requestContext);
            requestContext.setAuthenticationContext(authenticate);
            if (authenticate.isAuthenticated()) {
                updateClusterHeaderAndCheckEnv(requestContext, authenticate);
                return new AuthenticationResponse(true, false,
                        false);
            }
        } catch (APISecurityException e) {
            //TODO: (VirajSalaka) provide the error code properly based on exception (401, 403, 429 etc)
            FilterUtils.setErrorToContext(requestContext, e);
        }
        return new AuthenticationResponse(false, false, true);
    }



    /**
     * Update the cluster header based on the keyType and authenticate the token against its respective endpoint
     * environment.
     * 
     * @param requestContext request Context 
     * @param authContext authentication context
     * @throws APISecurityException if the environment and 
     */
    private void updateClusterHeaderAndCheckEnv(RequestContext requestContext, AuthenticationContext authContext)
            throws APISecurityException {

        String keyType = authContext.getKeyType();
        if (StringUtils.isEmpty(authContext.getKeyType())) {
            keyType = APIConstants.API_KEY_TYPE_PRODUCTION;
        } 

        // Header needs to be set only if the relevant cluster is available for the resource and the key type is
        // matched.
        if (requestContext.isClusterHeaderEnabled()) {
            if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_PRODUCTION)) {
                requestContext.addOrModifyHeaders(AdapterConstants.CLUSTER_HEADER,
                        requestContext.getProdClusterHeader());
                addRetryConfigHeaders(requestContext, requestContext.getMatchedAPI().getProductionEndpoints());
            } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX)) {
                requestContext.addOrModifyHeaders(AdapterConstants.CLUSTER_HEADER,
                        requestContext.getSandClusterHeader());
                addRetryConfigHeaders(requestContext, requestContext.getMatchedAPI().getSandboxEndpoints());
            } else {
                if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_PRODUCTION)) {
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            "Production key offered to the API with no production endpoint");
                } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX)) {
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            "Sandbox key offered to the API with no sandbox endpoint");
                }
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS, "Invalid key type.");
            }
        } else {
            // Even if the header flag is false, it is required to check if the relevant resource has a defined cluster
            // based on environment. 
            // If not it should provide authentication error.
            // Always at least one of the cluster header values should be set.
            if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_PRODUCTION) && StringUtils
                    .isEmpty(requestContext.getProdClusterHeader())) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Production key offered to the API with no production endpoint");
            } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX) && StringUtils
                    .isEmpty(requestContext.getSandClusterHeader())) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Sandbox key offered to the API with no sandbox endpoint");
            }   
        }
    }

    private String getAuthenticatorsChallengeString() {
        StringBuilder challengeString = new StringBuilder();
        if (authenticators != null) {
            for (Authenticator authenticator : authenticators) {
                challengeString.append(authenticator.getChallengeString()).append(" ");
            }
        }
        return challengeString.toString().trim();
    }

    private void addRetryConfigHeaders(RequestContext requestContext, EndpointCluster endpointCluster) {
        if (endpointCluster != null) {
            if (endpointCluster.getRetryConfig() != null) {
                requestContext.addOrModifyHeaders(HttpConstants.HttpRouterHeaders.RETRY_ON,
                        HttpConstants.HttpRouterHeaderValues.RETRIABLE_STATUS_CODES);
                requestContext.addOrModifyHeaders(HttpConstants.HttpRouterHeaders.MAX_RETRIES,
                        Integer.toString(endpointCluster.getRetryConfig().getCount()));
                requestContext.addOrModifyHeaders(HttpConstants.HttpRouterHeaders.RETRIABLE_STATUS_CODES,
                        String.join(",", endpointCluster.getRetryConfig().getStatusCodes()));
            }
        }
    }
}
