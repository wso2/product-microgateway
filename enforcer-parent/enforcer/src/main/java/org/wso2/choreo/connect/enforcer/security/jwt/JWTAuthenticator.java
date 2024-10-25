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
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.features.FeatureFlags;
import org.wso2.choreo.connect.enforcer.keymgt.KeyManagerHolder;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;
import org.wso2.choreo.connect.enforcer.security.Authenticator;
import org.wso2.choreo.connect.enforcer.security.KeyValidator;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTValidator;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.RevokedJWTDataHolder;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataHolder;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataStore;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the authenticator interface to authenticate request using a JWT token.
 */
public class JWTAuthenticator implements Authenticator {

    private static final Logger log = LogManager.getLogger(JWTAuthenticator.class);
    private final JWTValidator jwtValidator = new JWTValidator();
    private final boolean isGatewayTokenCacheEnabled;
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private static final Set<String> prodTokenNonProdAllowedOrgs = new HashSet<>();
    private static boolean isPATEnabled;

    static {
        if (System.getenv("PROD_TOKEN_NONPROD_ALLOWED_ORGS") != null) {
            Collections.addAll(prodTokenNonProdAllowedOrgs,
                    System.getenv("PROD_TOKEN_NONPROD_ALLOWED_ORGS").split("\\s+"));
        }
        if (System.getenv("PAT_ENABLED") != null) {
            if (System.getenv("PAT_ENABLED").equalsIgnoreCase("true")) {
                isPATEnabled = true;
            }
        }
    }

    public JWTAuthenticator() {
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        this.isGatewayTokenCacheEnabled = enforcerConfig.getCacheDto().isEnabled();
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            this.jwtGenerator = BackendJwtUtils.getApiMgtGatewayJWTGenerator();
        }
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String apiType = requestContext.getMatchedAPI().getApiType();
        if (isJWTEnabled(requestContext)) {
            String token = retrieveAuthHeaderValue(requestContext);

            if (apiType.equalsIgnoreCase(APIConstants.ApiType.WEB_SOCKET)) {
                if (token == null) {
                    token = extractJWTInWSProtocolHeader(requestContext);
                }
                AuthenticatorUtils.addWSProtocolResponseHeaderIfRequired(requestContext,
                        Constants.WS_OAUTH2_KEY_IDENTIFIED);
            }
            if (token != null) {
                // Extract token in case header value is in Bearer <token> format.
                if (token.split("\\s").length > 1) {
                    token = token.split("\\s")[1];
                }
                // Check whether the token is a JWT or a PAT.
                return (token.split("\\.").length == 3 || token.startsWith(APIKeyConstants.PAT_PREFIX));
            }
        }
        return false;
    }

    private boolean isJWTEnabled(RequestContext requestContext) {
        Map<String, List<String>> resourceSecuritySchemes = requestContext.getMatchedResourcePath()
                .getSecuritySchemas();
        if (resourceSecuritySchemes.isEmpty()) {
            // handle default security
            return true;
        }
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = requestContext.getMatchedAPI()
                .getSecuritySchemeDefinitions();
        for (String securityDefinitionName: resourceSecuritySchemes.keySet()) {
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

            String token = retrieveTokenFromRequestCtx(requestContext);
            String context = requestContext.getMatchedAPI().getBasePath();
            String name = requestContext.getMatchedAPI().getName();
            String version = requestContext.getMatchedAPI().getVersion();
            context = context + "/" + version;
            ResourceConfig matchingResource = requestContext.getMatchedResourcePath();
            SignedJWTInfo signedJWTInfo;
            Scope decodeTokenHeaderSpanScope = null;
            try {
                if (Utils.tracingEnabled()) {
                    decodeTokenHeaderSpan = Utils.startSpan(TracingConstants.DECODE_TOKEN_HEADER_SPAN, tracer);
                    decodeTokenHeaderSpanScope = decodeTokenHeaderSpan.getSpan().makeCurrent();
                    Utils.setTag(decodeTokenHeaderSpan, APIConstants.LOG_TRACE_ID,
                            ThreadContext.get(APIConstants.LOG_TRACE_ID));
                }
                signedJWTInfo = JWTUtils.getSignedJwt(token);
            } catch (ParseException | IllegalArgumentException e) {
                log.error("Failed to decode the token header", e);
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
                    log.error("Invalid JWT token. " + FilterUtils.getMaskedToken(jwtHeader));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS, "Invalid JWT token");
                }

            }
            JWTValidationInfo validationInfo = getJwtValidationInfo(signedJWTInfo, jwtTokenIdentifier,
                    requestContext.getMatchedAPI().getOrganizationId());
            if (validationInfo != null) {
                if (validationInfo.isValid()) {
                    // Check if the token has access to the gateway configured environment.
                    checkTokenEnv(claims, requestContext.getMatchedAPI().getEnvironmentName());
                    // Validate subscriptions
                    APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
                    ExtendedTokenIssuerDto issuerDto = KeyManagerHolder.getInstance()
                            .getTokenIssuerDTO(requestContext.getMatchedAPI().getOrganizationId(),
                                    validationInfo.getIssuer());
                    if (issuerDto == null) {
                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                APISecurityConstants.API_AUTH_GENERAL_ERROR,
                                APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
                    }
                    isAllowedEnvironmentForIDP(requestContext.getMatchedAPI().getEnvironmentName(),
                            issuerDto.getEnvironments());
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
                            JSONObject api = validateSubscriptionFromClaim(name, version, claims, token,
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
                                        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE,
                                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE);
                                        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                                                GeneralErrorCodeConstants.API_BLOCKED_DESCRIPTION);
                                        throw new APISecurityException(APIConstants.StatusCodes.SERVICE_UNAVAILABLE
                                                .getCode(), apiKeyValidationInfoDTO.getValidationStatus(),
                                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE);
                                    }
                                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                                            apiKeyValidationInfoDTO.getValidationStatus(),
                                            "User is NOT authorized to access the Resource. "
                                                    + "API Subscription validation failed.");
                                }
                                // Check if the token has access to the gateway configured environment.
                                checkTokenEnvAgainstDeploymentType(apiKeyValidationInfoDTO.getType(),
                                        requestContext.getMatchedAPI());
                            }
                        } else {
                            // In this case, the application related properties are populated so that analytics
                            // could provide much better insights.
                            // Since application notion becomes less meaningful with subscription validation disabled,
                            // the application name would be populated under the convention "anon:<KM Reference>"
                            updateApplicationNameForSubscriptionDisabledKM(apiKeyValidationInfoDTO,
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
                        validateScopes(context, version, matchingResource, validationInfo, signedJWTInfo);
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
                    if (backendJwtConfig.isEnabled() && requestContext.getMatchedAPI().isEnableBackendJWT()) {
                        JWTInfoDto jwtInfoDto = FilterUtils.generateJWTInfoDto(null, validationInfo,
                                        apiKeyValidationInfoDTO, requestContext);
                        endUserToken = BackendJwtUtils.generateAndRetrieveJWTToken(jwtGenerator, jwtTokenIdentifier,
                                jwtInfoDto, isGatewayTokenCacheEnabled);
                        // Set generated jwt token as a response header
                        requestContext.addOrModifyHeaders(backendJwtConfig.getJwtHeader(), endUserToken);
                    }

                    AuthenticationContext authenticationContext = FilterUtils
                            .generateAuthenticationContext(requestContext, jwtTokenIdentifier, validationInfo,
                                    apiKeyValidationInfoDTO, endUserToken, token, true);
                    //TODO: (VirajSalaka) Place the keytype population logic properly for self contained token
                    if (claims.getClaim("keytype") != null) {
                        authenticationContext.setKeyType(claims.getClaim("keytype").toString());
                    }
                    if (!"Unlimited".equals(authenticationContext.getTier())) {
                        // For subscription rate limiting, it is required to populate dynamic metadata
                        String apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
                        SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                                .getTenantSubscriptionStore(apiTenantDomain);
                        String subscriptionId = authenticationContext.getApiUUID() + ":" +
                                authenticationContext.getApplicationUUID();
                        String subPolicyName = authenticationContext.getTier();
                        requestContext.addMetadataToMap("ratelimit:subscription", subscriptionId);
                        requestContext.addMetadataToMap("ratelimit:usage-policy", subPolicyName);

                        String matchedApiOrganizationId = requestContext.getMatchedAPI().getOrganizationId();
                        // Denotes datastore contains a subscription policy for the given name and organization
                        SubscriptionPolicy subPolicy = datastore.getSubscriptionPolicyByOrgIdAndName(
                                matchedApiOrganizationId, subPolicyName);
                        String metaDataOrgId = APIConstants.SUPER_TENANT_DOMAIN_NAME;
                        if (subPolicy != null) {
                            metaDataOrgId =
                                    FeatureFlags.getCustomSubscriptionPolicyHandlingOrg(subPolicy.getOrganization());
                            requestContext.addMetadataToMap("ratelimit:organization", metaDataOrgId);
                        } else {
                            // Datastore does not contain a subscription policy for the given name and
                            // organization. Hence, subscription rate-limiting should be performed using the default org
                            requestContext.addMetadataToMap("ratelimit:organization", metaDataOrgId);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Organization ID: " + metaDataOrgId + ", SubscriptionId: "
                                    + subscriptionId + ", SubscriptionPolicy: " + subPolicyName +
                                    " will be evaluated for subscription rate-limiting");
                        }
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

    private void checkTokenEnv(JWTClaimsSet claims, String matchedEnv) throws APISecurityException {
        // If the claim "aud" does not exist, getAudience() returns an empty list.
        // If the value for "aud" is a String, getAudience() appends the value to the list that is returned.
        List<String> aud = claims.getAudience();
        for (String item: aud) {
            // If value assigned to choreoGatewayEnv is present in the audience array, the token is valid.
            // The token is invalid if there are at least one list item that starts with the ENV_NAME_PREFIX but
            // the exact value for the gateway's environment (choreoGatewayEnv) is not present.

            if (item.startsWith(APIConstants.JwtTokenConstants.ENV_NAME_PREFIX)) {
                // At least one list item that starts with the ENV_NAME_PREFIX has been found.
                if (aud.contains(APIConstants.JwtTokenConstants.ENV_NAME_PREFIX + matchedEnv)) {
                    // The expected value was one of the elements in the array.
                    log.debug("Environment validation for the access token was successful.");
                    break;
                } else {
                    // None of the elements were equal to choreoGatewayEnv
                    log.info("The access token does not have access to the environment {}.", matchedEnv);
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT,
                            APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT_ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * isAllowedEnvironmentForIDP checks if the token is valid for the environment that the API is deployed.
     *
     * @param apiDeployedEnv            The environment that the API is deployed.
     * @param allowedEnvsForTokenIssuer The environments that the token is valid for.
     * @throws APISecurityException If the token is not valid for the environment that the API is deployed.
     */
    void isAllowedEnvironmentForIDP(String apiDeployedEnv,
                                            Set<String> allowedEnvsForTokenIssuer) throws APISecurityException {
        if (allowedEnvsForTokenIssuer == null) {
            // If the allowedEnvsForTokenIssuer is null, the token is valid for all environments.
            return;
        }
        // If the allowedEnvsForTokenIssuer is not null, but the length is 0,
        // the token is invalid for all environments.
        if (allowedEnvsForTokenIssuer.size() == 0) {
            log.debug("The access token does not have access to any environment.");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT,
                    APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT_ERROR_MESSAGE);
        }
        if (allowedEnvsForTokenIssuer.contains(apiDeployedEnv)) {
            return;
        }
        log.debug("The access token does not have access to the environment {}.", apiDeployedEnv);
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT,
                APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT_ERROR_MESSAGE);
    }

    /**
     * checkTokenEnvAgainstDeploymentType checks if the keyType claim is matched with the deploymentType of the
     * requestContext's matchedAPIConfig.
     *
     * @param keyType    keyType resolved for the access token
     * @param matchedAPI API config matched for the request
     * @throws APISecurityException if the keyType is not matched with the deploymentType
     */
    private void checkTokenEnvAgainstDeploymentType(String keyType, APIConfig matchedAPI)
            throws APISecurityException {
        if (keyType == null) {
            keyType = APIConstants.JwtTokenConstants.SANDBOX_KEY_TYPE;
        }

        if (keyType.equalsIgnoreCase(matchedAPI.getDeploymentType())) {
            return;
        }

        if (System.getenv("DEPLOYMENT_TYPE_ENFORCED") != null
                && System.getenv("DEPLOYMENT_TYPE_ENFORCED").equalsIgnoreCase("false")
                && keyType.equalsIgnoreCase(APIConstants.JwtTokenConstants.PRODUCTION_KEY_TYPE)) {
            if (!prodTokenNonProdAllowedOrgs.isEmpty() &&
                    !prodTokenNonProdAllowedOrgs.contains(matchedAPI.getOrganizationId())) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT,
                        APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT_ERROR_MESSAGE);
            }
            log.info("Deprecated: Production access token is used to access sandbox API deployment in " +
                    "organization : " +  matchedAPI.getOrganizationId());
            return;
        }

        log.info("The access token does not have access to the {} type API deployment",
                matchedAPI.getDeploymentType());
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT,
                APISecurityConstants.API_AUTH_INVALID_ENVIRONMENT_ERROR_MESSAGE);
    }

    private void updateApplicationNameForSubscriptionDisabledKM(APIKeyValidationInfoDTO apiKeyValidationInfoDTO,
                                                                String kmReference) {
        String applicationRef = APIConstants.ANONYMOUS_PREFIX + kmReference;
        apiKeyValidationInfoDTO.setApplicationName(applicationRef);
        apiKeyValidationInfoDTO.setApplicationId(-1);
        apiKeyValidationInfoDTO.setApplicationUUID(
                UUID.nameUUIDFromBytes(
                        applicationRef.getBytes(StandardCharsets.UTF_8)).toString());
        apiKeyValidationInfoDTO.setApplicationTier(APIConstants.UNLIMITED_TIER);
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

    /**
     * Extract the JWT token from the request context.
     *
     * @param requestContext    Request context
     * @return JWT token
     * @throws APISecurityException If an error occurs while extracting the JWT token
     */
    protected String retrieveTokenFromRequestCtx(RequestContext requestContext) throws APISecurityException {

        String authHeaderVal = retrieveAuthHeaderValue(requestContext);
        if (authHeaderVal == null
                && requestContext.getMatchedAPI().getApiType().equalsIgnoreCase(APIConstants.ApiType.WEB_SOCKET)) {
            String tokenValue = extractJWTInWSProtocolHeader(requestContext);
            if (StringUtils.isNotEmpty(tokenValue)) {
                authHeaderVal = JWTConstants.BEARER + " " + tokenValue;
            }
        }
        if (authHeaderVal == null || !authHeaderVal.toLowerCase().contains(JWTConstants.BEARER)) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_MISSING_CREDENTIALS, "Missing Credentials");
        }
        String token = authHeaderVal.trim();
        String[] splitToken = token.split("\\s");
        // Extract the token when it is sent as bearer token. i.e Authorization: Bearer <token>
        if (splitToken.length > 1) {
            token = splitToken[1];
        }
        // Handle PAT logic
        if (isPATEnabled && token.startsWith(APIKeyConstants.PAT_PREFIX)) {
            token = exchangeJWTForPAT(requestContext, token);
        }
        return token;
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
     * @param matchingResource  Accessed API resource
     * @param jwtValidationInfo Validated JWT Information
     * @param jwtToken          JWT Token
     * @throws APISecurityException in case of scope validation failure
     */
    protected void validateScopes(String apiContext, String apiVersion, ResourceConfig matchingResource,
            JWTValidationInfo jwtValidationInfo, SignedJWTInfo jwtToken) throws APISecurityException {
        try {
            APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
            Set<String> scopeSet = new HashSet<>();
            scopeSet.addAll(jwtValidationInfo.getScopes());
            apiKeyValidationInfoDTO.setScopes(scopeSet);

            TokenValidationContext tokenValidationContext = new TokenValidationContext();
            tokenValidationContext.setValidationInfoDTO(apiKeyValidationInfoDTO);

            tokenValidationContext.setAccessToken(jwtToken.getToken());
            tokenValidationContext.setHttpVerb(matchingResource.getPath().toUpperCase());
            tokenValidationContext.setMatchingResourceConfig(matchingResource);
            tokenValidationContext.setContext(apiContext);
            tokenValidationContext.setVersion(apiVersion);

            boolean valid = KeyValidator.validateScopes(tokenValidationContext);
            if (valid) {
                if (log.isDebugEnabled()) {
                    log.debug("Scope validation successful for the resource: " + matchingResource.getPath());
                }
            } else {
                String message = "User is NOT authorized to access the Resource: " + matchingResource.getPath()
                        + ". Scope validation failed.";
                log.debug(message);
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.INVALID_SCOPE, message);
            }
        } catch (EnforcerException e) {
            String message = "Error while accessing backend services for token scope validation";
            log.error(message, e);
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR, message, e);
        }
    }

    private APIKeyValidationInfoDTO validateSubscriptionUsingKeyManager(RequestContext requestContext,
            JWTValidationInfo jwtValidationInfo) throws APISecurityException {

        String consumerKey = jwtValidationInfo.getConsumerKey();
        String keyManager = jwtValidationInfo.getKeyManager();

        if (consumerKey != null && keyManager != null) {
            return KeyValidator.validateSubscription(requestContext.getMatchedAPI(), consumerKey, keyManager);
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
    private JSONObject validateSubscriptionFromClaim(String name, String version, JWTClaimsSet payload, String token,
                                                     APIKeyValidationInfoDTO validationInfo, boolean isOauth)
            throws APISecurityException {
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
                                "version: " + version + ". Token: " + FilterUtils.getMaskedToken(token));
                    }
                    break;
                }
            }
            if (api == null) {
                if (log.isDebugEnabled()) {
                    log.debug("User is not subscribed to access the API: " + name +
                            ", version: " + version + ". Token: " + FilterUtils.getMaskedToken(token));
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

    private JWTValidationInfo getJwtValidationInfo(SignedJWTInfo signedJWTInfo, String jti, String organizationUUID)
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
                log.error("Invalid JWT token. " + FilterUtils.getMaskedToken(jwtHeader));
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
                jwtValidationInfo = jwtValidator.validateJWTToken(signedJWTInfo, organizationUUID);
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
                log.debug("JWT Validation failed:" + e.getMessage());
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

    private String exchangeJWTForPAT(RequestContext requestContext, String pat) throws APISecurityException {
        if (!APIKeyUtils.isValidAPIKey(pat)) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
        String keyHash = APIKeyUtils.generateAPIKeyHash(pat);
        Object cachedJWT = CacheProvider.getGatewayAPIKeyJWTCache().getIfPresent(keyHash);
        if (cachedJWT != null && !APIKeyUtils.isJWTExpired((String) cachedJWT)) {
            if (log.isDebugEnabled()) {
                log.debug("Token retrieved from the cache. Token: " + FilterUtils.getMaskedToken(pat));
            }
            setXForwardedAuthorizationHeader(requestContext, (String) cachedJWT);
            return (String) cachedJWT;
        }
        Optional<String> jwt = APIKeyUtils.exchangePATToJWT(keyHash);
        if (jwt.isEmpty()) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
        CacheProvider.getGatewayAPIKeyJWTCache().put(keyHash, jwt.get());
        // Add jwt to x-forwarded-authorization header.
        setXForwardedAuthorizationHeader(requestContext, jwt.get());
        return jwt.get();
    }

    private void setXForwardedAuthorizationHeader(RequestContext requestContext, String jwt) {
        requestContext.addOrModifyHeaders("x-forwarded-authorization", String.format("Bearer %s", jwt));
    }

    public String extractJWTInWSProtocolHeader(RequestContext requestContext) {
        String protocolHeader = requestContext.getHeaders().get(
                HttpConstants.WEBSOCKET_PROTOCOL_HEADER);
        if (protocolHeader != null) {
            String[] secProtocolHeaderValues = protocolHeader.split(",");
            if (secProtocolHeaderValues.length > 1 && secProtocolHeaderValues[0].equals(
                    Constants.WS_OAUTH2_KEY_IDENTIFIED)) {
                return secProtocolHeaderValues[1].trim();
            }
        }
        return "";
    }
}
