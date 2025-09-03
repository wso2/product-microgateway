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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.opentelemetry.context.Scope;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements the authenticator interface to authenticate request using an Internal Key.
 */
public class InternalAPIKeyAuthenticator extends APIKeyHandler {

    public static final String INTERNAL_KEY_AUTHENTICATOR_NAME = "Internal Key";

    private static final Log log = LogFactory.getLog(InternalAPIKeyAuthenticator.class);
    private static final String DEV_PORTAL_TEST_ISSUER_PREFIX = "/api/am/devportal/v2/apis/test-key";
    private String securityParam;
    private List<String> tempConsoleTestHeaders;
    private String tempTestConsoleHeadersMode;
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private final boolean isGatewayTokenCacheEnabled;

    public InternalAPIKeyAuthenticator(String securityParam) {
        this.securityParam = securityParam;
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        this.isGatewayTokenCacheEnabled = enforcerConfig.getCacheDto().isEnabled();
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            this.jwtGenerator = BackendJwtUtils.getApiMgtGatewayJWTGenerator();
        }
        this.tempConsoleTestHeaders = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTempTestConsoleHeaderNames();
        this.tempTestConsoleHeadersMode = ConfigHolder.getInstance().getConfig().getAuthHeader()
                .getTempTestConsoleTestHeadersMode();
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String apiType = requestContext.getMatchedAPI().getApiType();
        String internalKey = requestContext.getHeaders().get(
                ConfigHolder.getInstance().getConfig().getAuthHeader().getTestConsoleHeaderName().toLowerCase());
        if (internalKey == null &&
                Constants.TEMP_CONSOLE_TEST_HEADERS_ACTIVE_MODE.equals(tempTestConsoleHeadersMode)) {
            internalKey = tempConsoleTestHeaders.stream().map(header -> requestContext.getHeaders().get(header))
                    .filter(this::isAPIKey)
                    .findFirst().orElse(null);
        }
        if (apiType.equalsIgnoreCase(APIConstants.ApiType.WEB_SOCKET)) {
            if (internalKey == null) {
                internalKey = extractInternalKeyInWSProtocolHeader(requestContext);
            }
            String protocolHeader = requestContext.getHeaders().get(HttpConstants.WEBSOCKET_PROTOCOL_HEADER);
            if (protocolHeader != null) {
                String[] secProtocolHeaderValues = protocolHeader.split(",");
                AuthenticatorUtils.addWSProtocolResponseHeaderIfRequired(requestContext, secProtocolHeaderValues[0]);
            }
        }

        return isAPIKey(internalKey);
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        TracingTracer tracer = null;
        TracingSpan apiKeyAuthenticatorSpan = null;
        Scope apiKeyAuthenticatorSpanScope = null;
        TracingSpan apiKeyValidateSubscriptionSpan = null;
        TracingSpan verifyTokenInCacheSpan = null;
        TracingSpan verifyTokenWithoutCacheSpan = null;

        if (requestContext.getMatchedAPI() != null) {
            log.debug("Internal Key Authentication initialized");

            try {
                if (Utils.tracingEnabled()) {
                    tracer = Utils.getGlobalTracer();
                    apiKeyAuthenticatorSpan = Utils.startSpan(TracingConstants.API_KEY_AUTHENTICATOR_SPAN, tracer);
                    apiKeyAuthenticatorSpanScope = apiKeyAuthenticatorSpan.getSpan().makeCurrent();
                    Utils.setTag(apiKeyAuthenticatorSpan, APIConstants.LOG_TRACE_ID,
                            ThreadContext.get(APIConstants.LOG_TRACE_ID));
                }
                // Extract internal from the request while removing it from the msg context.
                String internalKey = extractInternalKey(requestContext);

                String[] splitToken = internalKey.split("\\.");
                SignedJWT signedJWT = SignedJWT.parse(internalKey);
                JWSHeader jwsHeader = signedJWT.getHeader();
                JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

                // Check if the decoded header contains type as 'InternalKey'.
                if (!isInternalKey(payload)) {
                    log.error("Invalid Internal Key token type. " + FilterUtils.getMaskedToken(splitToken[0]));
                    // To provide support for API keys. If internal key name's header name value changed similar
                    // to the API key header name this will enable that support.
                    AuthenticationContext authenticationContext = new AuthenticationContext();
                    authenticationContext.setAuthenticated(false);

                    // We check the type before verifying the signature. In case the type was incorrect but also not an
                    // API key, this will throw a NPE at RestAPI class setStatusCode method. This prevents it.
                    FilterUtils.setUnauthenticatedErrorToContext(requestContext);
                    return authenticationContext;
                }

                String tokenIdentifier = payload.getJWTID();

                checkInRevokedMap(tokenIdentifier, splitToken);
                String deploymentType = requestContext.getMatchedAPI().getDeploymentType();
                compareAudience(payload.getAudience(), deploymentType);

                String apiVersion = requestContext.getMatchedAPI().getVersion();
                String apiContext = requestContext.getMatchedAPI().getBasePath();

                // Verify token when it is found in cache
                JWTTokenPayloadInfo jwtTokenPayloadInfo = (JWTTokenPayloadInfo)
                        CacheProvider.getGatewayInternalKeyDataCache().getIfPresent(tokenIdentifier);

                boolean isVerified = isVerifiedApiKeyInCache(tokenIdentifier, internalKey, payload, splitToken,
                        "InternalKey", jwtTokenPayloadInfo);
                Scope verifyTokenInCacheSpanScope = null;
                if (jwtTokenPayloadInfo != null) {
                    if (Utils.tracingEnabled()) {
                        verifyTokenInCacheSpan = Utils.startSpan(TracingConstants.VERIFY_TOKEN_IN_CACHE_SPAN, tracer);
                        verifyTokenInCacheSpanScope = verifyTokenInCacheSpan.getSpan().makeCurrent();
                        Utils.setTag(verifyTokenInCacheSpan, APIConstants.LOG_TRACE_ID,
                                ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    String cachedToken = jwtTokenPayloadInfo.getAccessToken();
                    isVerified = cachedToken.equals(internalKey) && !isJwtTokenExpired(payload, "InternalKey");
                    if (Utils.tracingEnabled()) {
                        verifyTokenInCacheSpanScope.close();
                        Utils.finishSpan(verifyTokenInCacheSpan);
                    }
                } else if (CacheProvider.getInvalidGatewayInternalKeyCache().getIfPresent(tokenIdentifier) != null
                        && internalKey
                        .equals(CacheProvider.getInvalidGatewayInternalKeyCache().getIfPresent(tokenIdentifier))) {

                    log.debug("Internal Key retrieved from the invalid internal Key cache. Internal Key: "
                            + FilterUtils.getMaskedToken(splitToken[0]));

                    log.error("Invalid Internal Key. " + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                Scope verifyTokenWithoutCacheSpanScope = null;
                // Verify token when it is not found in cache
                if (!isVerified) {
                    log.debug("Internal Key not found in the cache.");
                    if (Utils.tracingEnabled()) {
                        verifyTokenWithoutCacheSpan = Utils.startSpan(TracingConstants.VERIFY_TOKEN_SPAN, tracer);
                        verifyTokenWithoutCacheSpanScope = verifyTokenWithoutCacheSpan.getSpan().makeCurrent();
                        Utils.setTag(verifyTokenWithoutCacheSpan, APIConstants.LOG_TRACE_ID,
                                ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    try {
                        isVerified = verifyTokenWhenNotInCache(jwsHeader, signedJWT, splitToken, payload,
                                "InternalKey");
                    } finally {
                        if (Utils.tracingEnabled()) {
                            verifyTokenWithoutCacheSpanScope.close();
                            Utils.finishSpan(verifyTokenWithoutCacheSpan);
                        }
                    }
                }

                if (isVerified) {
                    log.debug("Internal Key signature is verified.");

                    if (jwtTokenPayloadInfo == null) {
                        // Retrieve payload from InternalKey
                        log.debug("InternalKey payload not found in the cache.");

                        jwtTokenPayloadInfo = new JWTTokenPayloadInfo();
                        jwtTokenPayloadInfo.setPayload(payload);
                        jwtTokenPayloadInfo.setAccessToken(internalKey);
                        CacheProvider.getGatewayInternalKeyDataCache().put(tokenIdentifier, jwtTokenPayloadInfo);
                    }
                    Scope apiKeyValidateSubscriptionSpanScope = null;
                    if (Utils.tracingEnabled()) {
                        apiKeyValidateSubscriptionSpan = Utils
                                .startSpan(TracingConstants.API_KEY_VALIDATE_SUBSCRIPTION_SPAN, tracer);
                        apiKeyValidateSubscriptionSpanScope = apiKeyValidateSubscriptionSpan.getSpan().makeCurrent();
                        Utils.setTag(apiKeyValidateSubscriptionSpan, APIConstants.LOG_TRACE_ID,
                                ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    JSONObject api; // kept outside to make this reachable for methods outside the try block
                    try {
                        api = validateAPISubscription(apiContext, apiVersion, payload, splitToken,
                                false);
                        validateScopes(requestContext, payload);
                    } finally {
                        log.debug("Internal Key authentication successful.");
                        if (Utils.tracingEnabled()) {
                            apiKeyValidateSubscriptionSpanScope.close();
                            Utils.finishSpan(apiKeyValidateSubscriptionSpan);
                        }
                    }
                    //Get APIKeyValidationInfoDTO for internal key with limited info
                    APIKeyValidationInfoDTO apiKeyValidationInfoDTO = getAPIKeyValidationDTO(requestContext, payload);

                    // Generate or get backend JWT
                    JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().
                            getConfig().getJwtConfigurationDto();
                    if (jwtConfigurationDto.isEnabled() && requestContext.getMatchedAPI().isEnableBackendJWT()) {
                        JWTValidationInfo validationInfo = new JWTValidationInfo();
                        validationInfo.setUser(payload.getSubject());
                        JWTInfoDto jwtInfoDto = FilterUtils
                                .generateJWTInfoDto(null, validationInfo, apiKeyValidationInfoDTO, requestContext);
                        String endUserToken = BackendJwtUtils.generateAndRetrieveJWTToken(jwtGenerator, tokenIdentifier,
                                jwtInfoDto, isGatewayTokenCacheEnabled);
                        // Set generated jwt token as a response header
                        requestContext.addOrModifyHeaders(jwtConfigurationDto.getJwtHeader(), endUserToken);
                    }

                    return FilterUtils.generateAuthenticationContext(tokenIdentifier, payload, api,
                            requestContext.getMatchedAPI().getUuid(), internalKey);
                } else {
                    log.error("Internal Key authentication failed. " + FilterUtils.getMaskedToken(splitToken[0]));
                    CacheProvider.getGatewayInternalKeyDataCache().invalidate(payload.getJWTID());
                    CacheProvider.getInvalidGatewayInternalKeyCache().put(payload.getJWTID(), internalKey);
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }
            } catch (ParseException e) {
                log.warn("Internal Key authentication failed. ", e);
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Internal key authentication failed.");

            } finally {
                if (Utils.tracingEnabled()) {
                    apiKeyAuthenticatorSpanScope.close();
                    Utils.finishSpan(apiKeyAuthenticatorSpan);
                }
            }
        }
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                APISecurityConstants.API_AUTH_GENERAL_ERROR, APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
    }

    private APIKeyValidationInfoDTO getAPIKeyValidationDTO(RequestContext requestContext, JWTClaimsSet payload)
            throws ParseException {

        APIKeyValidationInfoDTO validationInfoDTO = new APIKeyValidationInfoDTO();
        if (payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE) != null) {
            validationInfoDTO.setType(payload.getStringClaim(APIConstants.JwtTokenConstants.KEY_TYPE));
        } else {
            validationInfoDTO.setType(APIConstants.API_KEY_TYPE_PRODUCTION);
        }

        //check whether name is assigned correctly (This was not populated in JWTAuthenticator)
        validationInfoDTO.setApiName(requestContext.getMatchedAPI().getName());
        validationInfoDTO.setApiVersion(requestContext.getMatchedAPI().getVersion());
        return validationInfoDTO;
    }

    private void validateScopes(RequestContext requestContext, JWTClaimsSet payload) throws APISecurityException {

        // If the test key is dev portal and required scopes from any security scheme,
        // then the scope validation should fail.
        ResourceConfig matchingResource = requestContext.getMatchedResourcePath();
        if (!isDevPortalTestKey(payload) || matchingResource.getSecuritySchemas() == null
                || matchingResource.getSecuritySchemas().values() == null) {
            return;
        }
        List<String> requiredScopes = matchingResource.getSecuritySchemas().values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (!requiredScopes.isEmpty()) {
            log.error("Scope validation failed for the token");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.INVALID_SCOPE, APISecurityConstants.INVALID_SCOPE_MESSAGE);
        }
    }

    private boolean isDevPortalTestKey(JWTClaimsSet payload) {

        String issuer = payload.getIssuer();
        return StringUtils.isEmpty(issuer) || issuer.endsWith(DEV_PORTAL_TEST_ISSUER_PREFIX);
    }

    @Override
    public String getChallengeString() {
        return "";
    }

    @Override
    public String getName() {
        return INTERNAL_KEY_AUTHENTICATOR_NAME;
    }

    private String extractInternalKey(RequestContext requestContext) {
        String internalKey;
        internalKey = requestContext.getHeaders().get(securityParam);
        if (internalKey == null &&
                Constants.TEMP_CONSOLE_TEST_HEADERS_ACTIVE_MODE.equals(tempTestConsoleHeadersMode)) {
            internalKey = tempConsoleTestHeaders.stream().map(header -> requestContext.getHeaders().get(header))
                    .filter(this::isAPIKey)
                    .findFirst().orElse(null);
        }
        if (internalKey != null) {
            return internalKey.trim();
        }
        if (requestContext.getMatchedAPI().getApiType().equalsIgnoreCase(APIConstants.ApiType.WEB_SOCKET)) {
            internalKey = extractInternalKeyInWSProtocolHeader(requestContext);
            if (internalKey != null && !internalKey.isEmpty()) {
                String protocols = getProtocolsToSetInRequestHeaders(requestContext);
                if (protocols != null) {
                    requestContext.addOrModifyHeaders(HttpConstants.WEBSOCKET_PROTOCOL_HEADER, protocols);
                }
                return internalKey.trim();
            }
        }
        return null;
    }

    public String extractInternalKeyInWSProtocolHeader(RequestContext requestContext) {
        String protocolHeader = requestContext.getHeaders().get(
                HttpConstants.WEBSOCKET_PROTOCOL_HEADER);
        if (protocolHeader != null) {
            String[] secProtocolHeaderValues = protocolHeader.split(",");
            if (secProtocolHeaderValues.length > 1 && (secProtocolHeaderValues[0].equals(
                    Constants.WS_TEST_KEY_IDENTIFIER)
                    || secProtocolHeaderValues[0].equals(Constants.WS_API_KEY_IDENTIFIER))) {
                return secProtocolHeaderValues[1].trim();
            }
        }
        return "";
    }

    public String getProtocolsToSetInRequestHeaders(RequestContext requestContext) {
        String[] secProtocolHeaderValues = requestContext.getHeaders().get(
            HttpConstants.WEBSOCKET_PROTOCOL_HEADER).split(",");
        if (secProtocolHeaderValues.length > 2) {
            return Arrays.stream(secProtocolHeaderValues, 2, secProtocolHeaderValues.length)
                    .collect(Collectors.joining(",")).trim();
        }
        return null;
    }

    @Override
    public int getPriority() {
        return -10;
    }
}

