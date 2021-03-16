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
package org.wso2.micro.gateway.enforcer.security;

import org.apache.commons.lang3.StringUtils;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.APISecurityConstants;
import org.wso2.micro.gateway.enforcer.constants.AdapterConstants;
import org.wso2.micro.gateway.enforcer.exception.APISecurityException;
import org.wso2.micro.gateway.enforcer.security.jwt.InternalAPIKeyAuthenticator;
import org.wso2.micro.gateway.enforcer.security.jwt.JWTAuthenticator;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the filter handling the authentication for the requests flowing through the gateway.
 */
public class AuthFilter implements Filter {
    private List<Authenticator> authenticators = new ArrayList<>();

    @Override
    public void init(APIConfig apiConfig) {
        initializeAuthenticators(apiConfig);
    }

    private void initializeAuthenticators(APIConfig apiConfig) {
        //TODO: Check security schema and add relevant authenticators.
        if (APIConstants.PUBLISHED_LIFE_CYCLE_STATUS.equals(apiConfig.getApiLifeCycleState())) {
            Authenticator jwtAuthenticator = new JWTAuthenticator();
            authenticators.add(jwtAuthenticator);
        }
        Authenticator internalAPIKeyAuthenticator = new
                InternalAPIKeyAuthenticator(APIConstants.JwtTokenConstants.INTERNAL_KEY);
        authenticators.add(internalAPIKeyAuthenticator);
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        for (Authenticator authenticator : authenticators) {
            if (authenticator.canAuthenticate(requestContext)) {
                AuthenticationResponse authenticate = authenticate(authenticator, requestContext);
                if (authenticate.isAuthenticated()) {
                    return true;
                }

                if (authenticate.getException() != null) {
                    FilterUtils.setErrorToContext(requestContext, authenticate.getException());
                    return false;
                }
            }
        }
        FilterUtils.setUnauthenticatedErrorToContext(requestContext);
        return false;
    }

    private AuthenticationResponse authenticate(Authenticator authenticator, RequestContext requestContext) {
        try {
            AuthenticationContext  authenticate = authenticator.authenticate(requestContext);
            if (authenticate.isAuthenticated()) {
                updateClusterHeaderAndCheckEnv(requestContext, authenticate);
                return new AuthenticationResponse(true, false,
                        false, null);
            }
        } catch (APISecurityException e) {
            //TODO: (VirajSalaka) provide the error code properly based on exception (401, 403, 429 etc)
            return new AuthenticationResponse(false, false,
                    true, e);
        }
        return new AuthenticationResponse(false, false, true,
                null);
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
                requestContext.addResponseHeaders(AdapterConstants.CLUSTER_HEADER,
                        requestContext.getProdClusterHeader());
            } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX)) {
                requestContext.addResponseHeaders(AdapterConstants.CLUSTER_HEADER,
                        requestContext.getSandClusterHeader());
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
}
