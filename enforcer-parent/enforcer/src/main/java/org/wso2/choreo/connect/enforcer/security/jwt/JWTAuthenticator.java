/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.enforcer.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.util.DateUtils;
import io.opentelemetry.context.Scope;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.carbon.apimgt.common.gateway.constants.GraphQLConstants;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.security.Authenticator;
import org.wso2.choreo.connect.enforcer.security.KeyValidator;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTValidator;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.RevokedJWTDataHolder;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the authenticator interface to authenticate request using a JWT token.
 */
public class JWTAuthenticator implements Authenticator {

    private static final Logger log = LogManager.getLogger(JWTAuthenticator.class);
    private final JWTValidator jwtValidator = new JWTValidator();
    private final boolean isGatewayTokenCacheEnabled;
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;

    public JWTAuthenticator() {
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        this.isGatewayTokenCacheEnabled = enforcerConfig.getCacheDto().isEnabled();
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            this.jwtGenerator = BackendJwtUtils.getApiMgtGatewayJWTGenerator();
        }
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        // only getting first operation is enough as all matched resource configs have the same security schemes
        // i.e. graphQL apis do not support resource level security yet
        if (isJWTEnabled(requestContext.getMatchedAPI().getSecuritySchemeDefinitions(),
                requestContext.getMatchedResourcePaths().get(0).getSecuritySchemas())) {
            String authHeaderValue = retrieveAuthHeaderValue(requestContext);

            // Check keyword bearer in header to prevent conflicts with custom authentication
            // (that maybe added with custom filters / interceptors / opa)
            // which also includes a jwt in the auth header yet with a scheme other than 'bearer'.
            //
            // StringUtils.startsWithIgnoreCase(null, "bearer")         = false
            // StringUtils.startsWithIgnoreCase("abc", "bearer")        = false
            // StringUtils.startsWithIgnoreCase("Bearer abc", "bearer") = true
            return StringUtils.startsWithIgnoreCase(authHeaderValue, JWTConstants.BEARER) &&
                    authHeaderValue.trim().split("\\s+").length == 2 &&
                    authHeaderValue.split("\\.").length == 3;
        }
        return false;
    }

    private boolean isJWTEnabled(Map<String, SecuritySchemaConfig> securitySchemeDefinitions,
                                 Map<String, List<String>> resourceSecuritySchemes) {
        if (resourceSecuritySchemes.isEmpty()) {
            // handle default security
            return true;
        }
        for (String securityDefinitionName : resourceSecuritySchemes.keySet()) {
            if (securitySchemeDefinitions.containsKey(securityDefinitionName)) {
                SecuritySchemaConfig config = securitySchemeDefinitions.get(securityDefinitionName);
                if (APIConstants.API_SECURITY_OAUTH2.equals(config.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        TracingTracer tracer = null;
        TracingSpan decodeTokenHeaderSpan = null;
        TracingSpan jwtAuthenticatorInfoSpan = null;
        Scope jwtAuthenticatorInfoSpanScope = null;
        TracingSpan validateSubscriptionSpan = null;
        TracingSpan validateScopesSpan = null;

        try {
            if (Utils.tracingEnabled()) {
                tracer = Utils.getGlobalTracer();
                jwtAuthenticatorInfoSpan = Utils.startSpan(TracingConstants.JWT_AUTHENTICATOR_SPAN, tracer);
                jwtAuthenticatorInfoSpanScope = jwtAuthenticatorInfoSpan.getSpan().makeCurrent();
                Utils.setTag(jwtAuthenticatorInfoSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }
            String jwtToken = retrieveAuthHeaderValue(requestContext);
            String[] splitToken = jwtToken.split("\\s");
            // Extract the token when it is sent as bearer token. i.e Authorization: Bearer <token>
            if (splitToken.length > 1) {
                jwtToken = splitToken[1];
            }
            String context = requestContext.getMatchedAPI().getBasePath();
            String name = requestContext.getMatchedAPI().getName();
            String version = requestContext.getMatchedAPI().getVersion();
            context = context + "/" + version;
            SignedJWTInfo signedJWTInfo;
            Scope decodeTokenHeaderSpanScope = null;
            try {
                if (Utils.tracingEnabled()) {
                    decodeTokenHeaderSpan = Utils.startSpan(TracingConstants.DECODE_TOKEN_HEADER_SPAN, tracer);
                    decodeTokenHeaderSpanScope = decodeTokenHeaderSpan.getSpan().makeCurrent();
                    Utils.setTag(decodeTokenHeaderSpan, APIConstants.LOG_TRACE_ID,
                            ThreadContext.get(APIConstants.LOG_TRACE_ID));
                }
                signedJWTInfo = JWTUtils.getSignedJwt(jwtToken);
            } catch (ParseException | IllegalArgumentException e) {
                log.error("Failed to decode the token header. {}", e.getMessage());
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Not a JWT token. Failed to decode the token header", e);
            } finally {
                if (Utils.tracingEnabled()) {
                    decodeTokenHeaderSpanScope.close();
                    Utils.finishSpan(decodeTokenHeaderSpan);
                }
            }
            JWTClaimsSet claims = signedJWTInfo.getJwtClaimsSet();
            String jwtTokenIdentifier = getJWTTokenIdentifier(signedJWTInfo);

            String jwtHeader = signedJWTInfo.getSignedJWT().getHeader().toString();
            if (StringUtils.isNotEmpty(jwtTokenIdentifier)) {
                if (RevokedJWTDataHolder.isJWTTokenSignatureExistsInRevokedMap(jwtTokenIdentifier)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Token retrieved from the revoked jwt token map. Token: "
                                + FilterUtils.getMaskedToken(jwtHeader));
                    }
                    log.debug("Invalid JWT token. " + FilterUtils.getMaskedToken(jwtHeader));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS, "Invalid JWT token");
                }

            }
            JWTValidationInfo validationInfo = getJwtValidationInfo(signedJWTInfo, jwtTokenIdentifier);
            if (validationInfo != null) {
                if (validationInfo.isValid()) {
                    // Validate subscriptions
                    APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
                    EnforcerConfig configuration = ConfigHolder.getInstance().getConfig();
                    ExtendedTokenIssuerDto issuerDto = configuration.getIssuersMap().get(validationInfo.getIssuer());
                    Scope validateSubscriptionSpanScope = null;
                    try {
                        if (issuerDto.isValidateSubscriptions()) {
                            if (Utils.tracingEnabled()) {
                                validateSubscriptionSpan = Utils
                                        .startSpan(TracingConstants.SUBSCRIPTION_VALIDATION_SPAN, tracer);
                                validateSubscriptionSpanScope = validateSubscriptionSpan.getSpan().makeCurrent();
                                Utils.setTag(validateSubscriptionSpan, APIConstants.LOG_TRACE_ID,
                                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
                            }
                            // if the token is self contained, validation subscription from `subscribedApis` claim
                            JSONObject api = validateSubscriptionFromClaim(name, version, claims, splitToken,
                                    apiKeyValidationInfoDTO, true);
                            if (api == null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Begin subscription validation via Key Manager: "
                                            + validationInfo.getKeyManager());
                                }
                                apiKeyValidationInfoDTO = validateSubscriptionUsingKeyManager(requestContext,
                                        validationInfo);

                                if (log.isDebugEnabled()) {
                                    log.debug("Subscription validation via Key Manager. Status: "
                                            + apiKeyValidationInfoDTO.isAuthorized());
                                }
                                if (!apiKeyValidationInfoDTO.isAuthorized()) {
                                    if (GeneralErrorCodeConstants.API_BLOCKED_CODE == apiKeyValidationInfoDTO
                                            .getValidationStatus()) {
                                        FilterUtils.setErrorToContext(requestContext,
                                                GeneralErrorCodeConstants.API_BLOCKED_CODE,
                                                APIConstants.StatusCodes.SERVICE_UNAVAILABLE.getCode(),
                                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE,
                                                GeneralErrorCodeConstants.API_BLOCKED_DESCRIPTION);
                                        throw new APISecurityException(APIConstants.StatusCodes.SERVICE_UNAVAILABLE
                                                .getCode(), apiKeyValidationInfoDTO.getValidationStatus(),
                                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE);
                                    } else if (APISecurityConstants.API_SUBSCRIPTION_BLOCKED == apiKeyValidationInfoDTO
                                            .getValidationStatus()) {
                                        FilterUtils.setErrorToContext(requestContext,
                                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED,
                                                APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_MESSAGE,
                                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_DESCRIPTION);
                                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED
                                                .getCode(), apiKeyValidationInfoDTO.getValidationStatus(),
                                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_MESSAGE);
                                    }
                                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                                            apiKeyValidationInfoDTO.getValidationStatus(),
                                            "User is NOT authorized to access the Resource. "
                                                    + "API Subscription validation failed.");
                                } else {
                                    /* GraphQL Query Analysis Information */
                                    if (APIConstants.ApiType.GRAPHQL.equals(requestContext.getMatchedAPI()
                                            .getApiType())) {
                                        requestContext.getProperties().put(GraphQLConstants.MAXIMUM_QUERY_DEPTH,
                                                apiKeyValidationInfoDTO.getGraphQLMaxDepth());
                                        requestContext.getProperties().put(
                                                GraphQLConstants.MAXIMUM_QUERY_COMPLEXITY,
                                                apiKeyValidationInfoDTO.getGraphQLMaxComplexity());
                                    }
                                }
                            }
                        } else {
                            // In this case, the application related properties are populated so that analytics
                            // could provide much better insights.
                            // Since application notion becomes less meaningful with subscription validation disabled,
                            // the application name would be populated under the convention "anon:<KM Reference>"
                            JWTUtils.updateApplicationNameForSubscriptionDisabledKM(apiKeyValidationInfoDTO,
                                    issuerDto.getName());
                        }
                    } finally {
                        if (Utils.tracingEnabled()) {
                            if (validateSubscriptionSpan != null) {
                                validateSubscriptionSpanScope.close();
                                Utils.finishSpan(validateSubscriptionSpan);
                            }
                        }
                    }

                    // Validate scopes
                    Scope validateScopesSpanScope = null;
                    try {
                        if (Utils.tracingEnabled()) {
                            validateScopesSpan = Utils.startSpan(TracingConstants.SCOPES_VALIDATION_SPAN, tracer);
                            validateScopesSpanScope = validateScopesSpan.getSpan().makeCurrent();
                            Utils.setTag(validateScopesSpan, APIConstants.LOG_TRACE_ID,
                                    ThreadContext.get(APIConstants.LOG_TRACE_ID));
                        }
                        validateScopes(context, version, requestContext.getMatchedResourcePaths(), validationInfo,
                                signedJWTInfo);
                    } finally {
                        if (Utils.tracingEnabled()) {
                            validateScopesSpanScope.close();
                            Utils.finishSpan(validateScopesSpan);
                        }
                    }
                    log.debug("JWT authentication successful.");

                    // Generate or get backend JWT
                    String endUserToken = null;
                    JWTConfigurationDto backendJwtConfig = ConfigHolder.getInstance().getConfig().
                            getJwtConfigurationDto();
                    if (backendJwtConfig.isEnabled()) {
                        JWTInfoDto jwtInfoDto = FilterUtils.generateJWTInfoDto(null, validationInfo,
                                apiKeyValidationInfoDTO, requestContext);
                        endUserToken = BackendJwtUtils.generateAndRetrieveJWTToken(jwtGenerator, jwtTokenIdentifier,
                                jwtInfoDto, isGatewayTokenCacheEnabled);
                        // Set generated jwt token as a response header
                        requestContext.addOrModifyHeaders(backendJwtConfig.getJwtHeader(), endUserToken);
                    }

                    AuthenticationContext authenticationContext = FilterUtils
                            .generateAuthenticationContext(requestContext, jwtTokenIdentifier, validationInfo,
                                    apiKeyValidationInfoDTO, endUserToken, jwtToken, true);
                    //TODO: (VirajSalaka) Place the keytype population logic properly for self contained token
                    if (claims.getClaim("keytype") != null) {
                        authenticationContext.setKeyType(claims.getClaim("keytype").toString());
                    }
                    return authenticationContext;
                } else {
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            validationInfo.getValidationCode(),
                            APISecurityConstants.getAuthenticationFailureMessage(validationInfo.getValidationCode()));
                }
            } else {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_GENERAL_ERROR,
                        APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
            }
        } finally {
            if (Utils.tracingEnabled()) {
                jwtAuthenticatorInfoSpanScope.close();
                Utils.finishSpan(jwtAuthenticatorInfoSpan);
            }
        }

    }

    @Override
    public String getChallengeString() {
        return "Bearer realm=\"Choreo Connect\"";
    }

    @Override
    public String getName() {
        return "JWT";
    }

    private String retrieveAuthHeaderValue(RequestContext requestContext) {
        Map<String, String> headers = requestContext.getHeaders();
        return headers.get(FilterUtils.getAuthHeaderName(requestContext));
    }

    @Override
    public int getPriority() {
        return 10;
    }


    /**
     * Validate scopes bound to the resource of the API being invoked against the scopes specified
     * in the JWT token payload.
     *
     * @param apiContext        API Context
     * @param apiVersion        API Version
     * @param matchingResources Accessed API resources
     * @param jwtValidationInfo Validated JWT Information
     * @param jwtToken          JWT Token
     * @throws APISecurityException in case of scope validation failure
     */
    private void validateScopes(String apiContext, String apiVersion, ArrayList<ResourceConfig> matchingResources,
                                JWTValidationInfo jwtValidationInfo, SignedJWTInfo jwtToken)
            throws APISecurityException {
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
        Set<String> scopeSet = new HashSet<>(jwtValidationInfo.getScopes());
        apiKeyValidationInfoDTO.setScopes(scopeSet);

        TokenValidationContext tokenValidationContext = new TokenValidationContext();
        tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

        tokenValidationContext.setAccessToken(jwtToken.getToken());
        // since matching resources has same method for all, just get the first element's method is adequate.
        // i.e. graphQL matching resources has same operation type for a request.
        tokenValidationContext.setHttpVerb(matchingResources.get(0).getMethod().toString());
        tokenValidationContext.setMatchingResourceConfigs(matchingResources);
        tokenValidationContext.setContext(apiContext);
        tokenValidationContext.setVersion(apiVersion);

        boolean valid = KeyValidator.validateScopes(tokenValidationContext);
        if (valid) {
            log.debug("Scope validation was successful for the resource.");
        }
    }

    private APIKeyValidationInfoDTO validateSubscriptionUsingKeyManager(RequestContext requestContext,
                                                                        JWTValidationInfo jwtValidationInfo)
            throws APISecurityException {

        String apiContext = requestContext.getMatchedAPI().getBasePath();
        String apiVersion = requestContext.getMatchedAPI().getVersion();
        String uuid = requestContext.getMatchedAPI().getUuid();

        String consumerKey = jwtValidationInfo.getConsumerKey();
        String keyManager = jwtValidationInfo.getKeyManager();

        if (consumerKey != null && keyManager != null) {
            return KeyValidator.validateSubscription(uuid, apiContext, apiVersion, consumerKey, keyManager);
        }
        log.debug("Cannot call Key Manager to validate subscription. "
                + "Payload of the token does not contain the Authorized party - the party to which the ID Token was "
                + "issued");
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
    }


    /**
     * Validate whether the user is subscribed to the invoked API. If subscribed, return a JSON object containing
     * the API information. This validation is done based on the jwt token claims.
     *
     * @param name           API name
     * @param version        API version
     * @param validationInfo token validation related details. this will be populated based on the available data
     *                       during the subscription validation.
     * @param payload        The payload of the JWT token
     * @return an JSON object containing subscribed API information retrieved from token payload.
     * If the subscription information is not found, return a null object.
     * @throws APISecurityException if the user is not subscribed to the API
     */
    private JSONObject validateSubscriptionFromClaim(String name, String version, JWTClaimsSet payload,
                                                     String[] splitToken, APIKeyValidationInfoDTO validationInfo,
                                                     boolean isOauth) throws APISecurityException {
        JSONObject api = null;
        try {
            validationInfo.setEndUserName(payload.getSubject());
            if (payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE) != null) {
                validationInfo.setType(payload.getStringClaim(APIConstants.JwtTokenConstants.KEY_TYPE));
            } else {
                validationInfo.setType(APIConstants.API_KEY_TYPE_PRODUCTION);
            }

            if (payload.getClaim(APIConstants.JwtTokenConstants.CONSUMER_KEY) != null) {
                validationInfo.setConsumerKey(payload.getStringClaim(APIConstants.JwtTokenConstants.CONSUMER_KEY));
            }

            JSONObject app = payload.getJSONObjectClaim(APIConstants.JwtTokenConstants.APPLICATION);
            if (app != null) {
                validationInfo.setApplicationUUID(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_UUID));
                validationInfo.setApplicationId(app.getAsNumber(APIConstants.JwtTokenConstants.APPLICATION_ID)
                        .intValue());
                validationInfo.setApplicationName(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_NAME));
                validationInfo.setApplicationTier(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_TIER));
                validationInfo.setSubscriber(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_OWNER));
                if (app.containsKey(APIConstants.JwtTokenConstants.QUOTA_TYPE)
                        && APIConstants.JwtTokenConstants.QUOTA_TYPE_BANDWIDTH
                        .equals(app.getAsString(APIConstants.JwtTokenConstants.QUOTA_TYPE))) {
                    validationInfo.setContentAware(true);
                }
            }
        } catch (ParseException e) {
            log.error("Error while parsing jwt claims");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                    APISecurityConstants.API_AUTH_FORBIDDEN,
                    APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
        }

        if (payload.getClaim(APIConstants.JwtTokenConstants.SUBSCRIBED_APIS) != null) {
            // Subscription validation
            JSONArray subscribedAPIs =
                    (JSONArray) payload.getClaim(APIConstants.JwtTokenConstants.SUBSCRIBED_APIS);
            for (Object apiObj : subscribedAPIs) {
                JSONObject subApi =
                        (JSONObject) apiObj;
                if (name.equals(subApi.getAsString(APIConstants.JwtTokenConstants.API_NAME)) &&
                        version.equals(subApi.getAsString(APIConstants.JwtTokenConstants.API_VERSION)
                        )) {
                    api = subApi;
                    validationInfo.setAuthorized(true);

                    //set throttling attribs if present
                    String subTier = subApi.getAsString(APIConstants.JwtTokenConstants.SUBSCRIPTION_TIER);
                    String subPublisher = subApi.getAsString(APIConstants.JwtTokenConstants.API_PUBLISHER);
                    String subTenant = subApi.getAsString(APIConstants.JwtTokenConstants.SUBSCRIBER_TENANT_DOMAIN);
                    if (subTier != null) {
                        validationInfo.setTier(subTier);
                        AuthenticatorUtils.populateTierInfo(validationInfo, payload, subTier);
                    }
                    if (subPublisher != null) {
                        validationInfo.setApiPublisher(subPublisher);
                    }
                    if (subTenant != null) {
                        validationInfo.setSubscriberTenantDomain(subTenant);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("User is subscribed to the API: " + name + ", " +
                                "version: " + version + ". Token: " + FilterUtils.getMaskedToken(splitToken[0]));
                    }
                    break;
                }
            }
            if (api == null) {
                if (log.isDebugEnabled()) {
                    log.debug("User is not subscribed to access the API: " + name +
                            ", version: " + version + ". Token: " + FilterUtils.getMaskedToken(splitToken[0]));
                }
                log.error("User is not subscribed to access the API.");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN,
                        APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No subscription information found in the token.");
            }
            // we perform mandatory authentication for Api Keys
            if (!isOauth) {
                log.error("User is not subscribed to access the API.");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN,
                        APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        }
        return api;
    }

    private JWTValidationInfo getJwtValidationInfo(SignedJWTInfo signedJWTInfo, String jti)
            throws APISecurityException {

        String jwtHeader = signedJWTInfo.getSignedJWT().getHeader().toString();
        JWTValidationInfo jwtValidationInfo = null;
        if (isGatewayTokenCacheEnabled &&
                !SignedJWTInfo.ValidationStatus.NOT_VALIDATED.equals(signedJWTInfo.getValidationStatus())) {
            Object cacheToken = CacheProvider.getGatewayTokenCache().getIfPresent(jti);
            if (cacheToken != null && (Boolean) cacheToken &&
                    SignedJWTInfo.ValidationStatus.VALID.equals(signedJWTInfo.getValidationStatus())) {
                if (CacheProvider.getGatewayKeyCache().getIfPresent(jti) != null) {
                    JWTValidationInfo tempJWTValidationInfo =
                            (JWTValidationInfo) CacheProvider.getGatewayKeyCache()
                                    .getIfPresent(jti);
                    checkTokenExpiration(jti, tempJWTValidationInfo);
                    jwtValidationInfo = tempJWTValidationInfo;
                }
            } else if (SignedJWTInfo.ValidationStatus.INVALID.equals(signedJWTInfo.getValidationStatus()) &&
                    CacheProvider.getInvalidTokenCache().getIfPresent(jti) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Token retrieved from the invalid token cache. Token: "
                            + FilterUtils.getMaskedToken(jwtHeader));
                }
                log.debug("Invalid JWT token. " + FilterUtils.getMaskedToken(jwtHeader));
                if (CacheProvider.getGatewayKeyCache().getIfPresent(jti) != null) {
                    jwtValidationInfo = (JWTValidationInfo) CacheProvider.getGatewayKeyCache().getIfPresent(jti);
                } else {
                    log.warn("Token retrieved from the invalid token cache. But the validation info not found "
                            + "in the key cache for the Token: " + FilterUtils.getMaskedToken(jwtHeader));
                    jwtValidationInfo = new JWTValidationInfo();
                    jwtValidationInfo.setValidationCode(APISecurityConstants.API_AUTH_GENERAL_ERROR);
                    jwtValidationInfo.setValid(false);
                }
            }
        }
        if (jwtValidationInfo == null) {

            try {
                jwtValidationInfo = jwtValidator.validateJWTToken(signedJWTInfo);
                signedJWTInfo.setValidationStatus(jwtValidationInfo.isValid() ?
                        SignedJWTInfo.ValidationStatus.VALID : SignedJWTInfo.ValidationStatus.INVALID);
                if (isGatewayTokenCacheEnabled) {
                    // Add token to tenant token cache
                    if (jwtValidationInfo.isValid()) {
                        CacheProvider.getGatewayTokenCache().put(jti, true);
                    } else {
                        CacheProvider.getInvalidTokenCache().put(jti, true);
                    }
                    CacheProvider.getGatewayKeyCache().put(jti, jwtValidationInfo);

                }
                return jwtValidationInfo;
            } catch (EnforcerException e) {
                log.error("JWT Validation failed", e);
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_GENERAL_ERROR,
                        APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
            }
        }
        return jwtValidationInfo;
    }

    /**
     * Check whether the jwt token is expired or not.
     *
     * @param tokenIdentifier The token Identifier of JWT.
     * @param payload         The payload of the JWT token
     * @return
     */
    private JWTValidationInfo checkTokenExpiration(String tokenIdentifier, JWTValidationInfo payload) {

        long timestampSkew = FilterUtils.getTimeStampSkewInSeconds();

        Date now = new Date();
        Date exp = new Date(payload.getExpiryTime());
        if (!DateUtils.isAfter(exp, now, timestampSkew)) {
            if (isGatewayTokenCacheEnabled) {
                CacheProvider.getGatewayTokenCache().invalidate(tokenIdentifier);
                CacheProvider.getGatewayJWTTokenCache().invalidate(tokenIdentifier);
                CacheProvider.getInvalidTokenCache().put(tokenIdentifier, true);
            }
            payload.setValid(false);
            payload.setValidationCode(APISecurityConstants.API_AUTH_INVALID_CREDENTIALS);
            return payload;
        }
        return payload;
    }

    private String getJWTTokenIdentifier(SignedJWTInfo signedJWTInfo) {

        JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
        String jwtid = jwtClaimsSet.getJWTID();
        if (StringUtils.isNotEmpty(jwtid)) {
            return jwtid;
        }
        return signedJWTInfo.getSignedJWT().getSignature().toString();
    }
}
