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
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.EndpointCluster;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RetryConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.AdapterConstants;
import org.wso2.choreo.connect.enforcer.constants.InterceptorConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.APIKeyAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.InternalAPIKeyAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.JWTAuthenticator;
import org.wso2.choreo.connect.enforcer.security.jwt.UnsecuredAPIAuthenticator;
import org.wso2.choreo.connect.enforcer.security.mtls.MTLSAuthenticator;
import org.wso2.choreo.connect.enforcer.util.EndpointSecurityUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This is the filter handling the authentication for the requests flowing through the gateway.
 */
public class AuthFilter implements Filter {
    private List<Authenticator> authenticators = new ArrayList<>();
    private static final Logger log = LogManager.getLogger(AuthFilter.class);
    private boolean isMutualSSLMandatory;
    private boolean isOAuthBasicAuthMandatory;

    @Override
    public void init(APIConfig apiConfig, Map<String, String> configProperties) {
        initializeAuthenticators(apiConfig);
    }

    private void initializeAuthenticators(APIConfig apiConfig) {
        //TODO: Check security schema and add relevant authenticators.
        boolean isOAuthProtected = true;
        boolean isMutualSSLProtected = false;
        boolean isBasicAuthProtected = false;
        boolean isApiKeyProtected = false;
        isMutualSSLMandatory = false;
        isOAuthBasicAuthMandatory = false;

        // Set security conditions
        if (apiConfig.getApplicationSecurity()) {
            isOAuthBasicAuthMandatory = true;
        }

        if (!Objects.isNull(apiConfig.getMutualSSL())) {
            if (apiConfig.getMutualSSL().equalsIgnoreCase(APIConstants.Optionality.MANDATORY)) {
                isMutualSSLProtected = true;
                isMutualSSLMandatory = true;
            } else if (apiConfig.getMutualSSL().equalsIgnoreCase(APIConstants.Optionality.OPTIONAL)) {
                isMutualSSLProtected = true;
            }
        }

        if (apiConfig.getSecuritySchemeDefinitions() == null) {
            isOAuthProtected = true;
        } else {
            for (Map.Entry<String, SecuritySchemaConfig> securityDefinition :
                    apiConfig.getSecuritySchemeDefinitions().entrySet()) {
                String apiSecurityLevel = securityDefinition.getValue().getType();
                if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.API_SECURITY_OAUTH2)) {
                    isOAuthProtected = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.API_SECURITY_BASIC_AUTH)) {
                    isBasicAuthProtected = true;
                } else if (apiSecurityLevel.trim().equalsIgnoreCase(APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME)) {
                    isApiKeyProtected = true;
                }
            }
        }

        if (!isMutualSSLMandatory) {
            isOAuthBasicAuthMandatory = true;
        }

        // TODO: Set authenticator for isBasicAuthProtected
        if (isMutualSSLProtected) {
            Authenticator mtlsAuthenticator = new MTLSAuthenticator();
            authenticators.add(mtlsAuthenticator);
        }

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
                requestContext.getMatchedAPI().getApiLifeCycleState()) &&
                !requestContext.getMatchedAPI().isMockedApi()) {
            // For prototyped endpoints, only the production endpoints could be available.
            requestContext.addOrModifyHeaders(AdapterConstants.CLUSTER_HEADER,
                    requestContext.getProdClusterHeader());
            requestContext.getRemoveHeaders().remove(AdapterConstants.CLUSTER_HEADER);
            return true;
        }

        // Authentication status of the request
        boolean authenticated = false;
        // Any auth token has been provided for application-level security or not
        boolean canAuthenticated = false;
        for (Authenticator authenticator : authenticators) {
            if (authenticator.canAuthenticate(requestContext)) {
                // For transport level securities (mTLS), canAuthenticated will not be applied
                if (!authenticator.getName().contains(APIConstants.API_SECURITY_MUTUAL_SSL_NAME)) {
                    canAuthenticated = true;
                }
                AuthenticationResponse authenticateResponse = authenticate(authenticator, requestContext);
                // Authentication status will be updated only if the authentication is a mandatory one
                if (authenticateResponse.isMandatoryAuthentication()) {
                    authenticated = authenticateResponse.isAuthenticated();
                    setInterceptorAuthContextMetadata(authenticator, requestContext);
                }
                if (!authenticateResponse.isContinueToNextAuthenticator()) {
                    break;
                }
            } else {
                // Check if the failed authentication is mandatory mTLS
                if (isMutualSSLMandatory && authenticator.getName()
                        .contains(APIConstants.API_SECURITY_MUTUAL_SSL_NAME)) {
                    authenticated = false;
                    log.debug("mTLS authentication was failed for the request: {} , API: {}:{} APIUUID: {} ",
                            requestContext.getMatchedResourcePaths().get(0).getPath(),
                            requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                            requestContext.getMatchedAPI().getUuid());
                    break;
                }
                // Check if the failed authentication is a mandatory application level security
                if (isOAuthBasicAuthMandatory && !authenticator.getName()
                        .contains(APIConstants.API_SECURITY_MUTUAL_SSL_NAME)) {
                    authenticated = false;
                }
            }
        }
        if (authenticated) {
            return true;
        }
        if (!canAuthenticated) {
            FilterUtils.setUnauthenticatedErrorToContext(requestContext);
        }
        log.debug("None of the authenticators were able to authenticate the request: {}",
                requestContext.getRequestPathTemplate(),
                ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6600));
        //set WWW_AUTHENTICATE header to error response
        requestContext.addOrModifyHeaders(APIConstants.WWW_AUTHENTICATE, getAuthenticatorsChallengeString() +
                ", error=\"invalid_token\"" +
                ", error_description=\"The provided token is invalid\"");
        return false;
    }

    private AuthenticationResponse authenticate(Authenticator authenticator, RequestContext requestContext) {
        try {
            AuthenticationContext authenticate = authenticator.authenticate(requestContext);
            requestContext.setAuthenticationContext(authenticate);
            if (authenticator.getName().contains(APIConstants.API_SECURITY_MUTUAL_SSL_NAME)) {
                // This section is for mTLS authentication
                if (authenticate.isAuthenticated()) {
                    updateClusterHeaderAndCheckEnv(requestContext, authenticate);
                    // set backend security
                    EndpointSecurityUtils.addEndpointSecurity(requestContext);
                    log.debug("mTLS authentication was passed for the request: {} , API: {}:{}, APIUUID: {} ",
                            requestContext.getMatchedResourcePaths().get(0).getPath(),
                            requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                            requestContext.getMatchedAPI().getUuid());
                    return new AuthenticationResponse(true, isMutualSSLMandatory, true);
                } else {
                    if (isMutualSSLMandatory) {
                        log.debug("Mandatory mTLS authentication was failed for the request: {} , API: {}:{}, " +
                                        "APIUUID: {} ",
                                requestContext.getMatchedResourcePaths().get(0).getPath(),
                                requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                                requestContext.getMatchedAPI().getUuid());
                        return new AuthenticationResponse(false, true, false);
                    } else {
                        log.debug("Optional mTLS authentication was failed for the request: {} , API: {}:{}, " +
                                        "APIUUID: {} ",
                                requestContext.getMatchedResourcePaths().get(0).getPath(),
                                requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                                requestContext.getMatchedAPI().getUuid());
                        return new AuthenticationResponse(false, false, true);
                    }
                }
            } else if (authenticate.isAuthenticated()) {
                // This section is for application level securities
                if (!requestContext.getMatchedAPI().isMockedApi()) {
                    updateClusterHeaderAndCheckEnv(requestContext, authenticate);
                    // set backend security
                    EndpointSecurityUtils.addEndpointSecurity(requestContext);
                }
                return new AuthenticationResponse(true, isOAuthBasicAuthMandatory, false);
            }
        } catch (APISecurityException e) {
            //TODO: (VirajSalaka) provide the error code properly based on exception (401, 403, 429 etc)
            FilterUtils.setErrorToContext(requestContext, e);
        }
        return new AuthenticationResponse(false, isOAuthBasicAuthMandatory, true);
    }

    /**
     * Update the cluster header based on the keyType and authenticate the token against its respective endpoint
     * environment.
     *
     * @param requestContext request Context
     * @param authContext    authentication context
     * @throws APISecurityException if the environment and
     */
    private void updateClusterHeaderAndCheckEnv(RequestContext requestContext, AuthenticationContext authContext)
            throws APISecurityException {
        String keyType = authContext.getKeyType();
        if (StringUtils.isEmpty(authContext.getKeyType())) {
            keyType = APIConstants.API_KEY_TYPE_PRODUCTION;
        }

        if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_PRODUCTION) &&
                !StringUtils.isEmpty(requestContext.getProdClusterHeader())) {
            requestContext.addOrModifyHeaders(AdapterConstants.CLUSTER_HEADER,
                    requestContext.getProdClusterHeader());
            requestContext.getRemoveHeaders().remove(AdapterConstants.CLUSTER_HEADER);
            addRouterHttpHeaders(requestContext, APIConstants.API_KEY_TYPE_PRODUCTION);
        } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX) &&
                !StringUtils.isEmpty(requestContext.getSandClusterHeader())) {
            requestContext.addOrModifyHeaders(AdapterConstants.CLUSTER_HEADER,
                    requestContext.getSandClusterHeader());
            requestContext.getRemoveHeaders().remove(AdapterConstants.CLUSTER_HEADER);
            addRouterHttpHeaders(requestContext, APIConstants.API_KEY_TYPE_SANDBOX);
        } else {
            if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_PRODUCTION)) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Production key offered to an API with no production endpoint");
            } else if (keyType.equalsIgnoreCase(APIConstants.API_KEY_TYPE_SANDBOX)) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Sandbox key offered to an API with no sandbox endpoint");
            }
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS, "Invalid key type.");
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

    private void addRouterHttpHeaders(RequestContext requestContext, String keyType) {
        // requestContext.getMatchedResourcePaths() will only have one element for non GraphQL APIs.
        // Also, GraphQL APIs doesn't have resource level endpoint configs
        ResourceConfig resourceConfig = requestContext.getMatchedResourcePaths().get(0);
        // In websockets case, the endpoints object becomes null. Hence it would result
        // in a NPE, if it is not checked.
        if (resourceConfig.getEndpoints() != null &&
                resourceConfig.getEndpoints().containsKey(keyType)) {
            EndpointCluster endpointCluster = resourceConfig.getEndpoints().get(keyType);
            addRetryAndTimeoutConfigHeaders(requestContext, endpointCluster);
            handleEmptyPathHeader(requestContext, endpointCluster.getBasePath());
        } else if (requestContext.getMatchedAPI().getEndpoints().containsKey(keyType)) {
            EndpointCluster endpointCluster = requestContext.getMatchedAPI().getEndpoints().get(keyType);
            addRetryAndTimeoutConfigHeaders(requestContext, endpointCluster);
            handleEmptyPathHeader(requestContext, endpointCluster.getBasePath());
        }
    }

    private void addRetryAndTimeoutConfigHeaders(RequestContext requestContext, EndpointCluster endpointCluster) {
        RetryConfig retryConfig = endpointCluster.getRetryConfig();
        if (retryConfig != null) {
            addRetryConfigHeaders(requestContext, retryConfig);
        }
        Integer timeout = endpointCluster.getRouteTimeoutInMillis();
        if (timeout != null) {
            addTimeoutHeaders(requestContext, timeout);
        }
    }

    private void addRetryConfigHeaders(RequestContext requestContext, RetryConfig retryConfig) {
        requestContext.addOrModifyHeaders(AdapterConstants.HttpRouterHeaders.RETRY_ON,
                AdapterConstants.HttpRouterHeaderValues.RETRIABLE_STATUS_CODES);
        requestContext.addOrModifyHeaders(AdapterConstants.HttpRouterHeaders.MAX_RETRIES,
                Integer.toString(retryConfig.getCount()));
        requestContext.addOrModifyHeaders(AdapterConstants.HttpRouterHeaders.RETRIABLE_STATUS_CODES,
                StringUtils.join(retryConfig.getStatusCodes(), ","));
    }

    private void addTimeoutHeaders(RequestContext requestContext, Integer routeTimeoutInMillis) {
        requestContext.addOrModifyHeaders(AdapterConstants.HttpRouterHeaders.UPSTREAM_REQ_TIMEOUT_MS,
                Integer.toString(routeTimeoutInMillis));
    }

    private void setInterceptorAuthContextMetadata(Authenticator authenticator, RequestContext requestContext) {
        // add auth context to metadata, lua script will add it to the auth context of the interceptor
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        String tokenType = authenticator.getName();
        authContext.setTokenType(tokenType);
        requestContext.addMetadataToMap(InterceptorConstants.AuthContextFields.TOKEN_TYPE,
                Objects.toString(tokenType, ""));
        requestContext.addMetadataToMap(InterceptorConstants.AuthContextFields.TOKEN,
                Objects.toString(authContext.getRawToken(), ""));
        requestContext.addMetadataToMap(InterceptorConstants.AuthContextFields.KEY_TYPE,
                Objects.toString(authContext.getKeyType(), ""));
    }

    /**
     * This will fix sending upstream an empty path header issue.
     *
     * @param requestContext request context
     * @param basePath       endpoint basepath
     */
    private void handleEmptyPathHeader(RequestContext requestContext, String basePath) {
        if (StringUtils.isNotBlank(basePath)) {
            return;
        }
        // remaining path after removing the context and the version from the invoked path.
        String remainingPath = StringUtils.removeStartIgnoreCase(requestContext.getHeaders()
                .get(APIConstants.PATH_HEADER).split("\\?")[0], requestContext.getMatchedAPI().getBasePath());
        // if the :path will be empty after applying the route's substitution, then we have to add a "/" forcefully
        // to avoid :path being empty.
        if (StringUtils.isBlank(remainingPath)) {
            String[] splittedPath = requestContext.getHeaders().get(APIConstants.PATH_HEADER).split("\\?");
            String newPath = splittedPath.length > 1 ? splittedPath[0] + "/?" + splittedPath[1] : splittedPath[0] + "/";
            requestContext.addOrModifyHeaders(APIConstants.PATH_HEADER, newPath);
        }
    }
}
