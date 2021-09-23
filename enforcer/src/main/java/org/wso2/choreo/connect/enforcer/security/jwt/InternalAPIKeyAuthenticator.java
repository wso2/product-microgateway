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
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import io.opentelemetry.context.Scope;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.api.RequestContext;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.security.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.RevokedJWTDataHolder;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.text.ParseException;

/**
 * Implements the authenticator interface to authenticate request using a Internal Key.
 */
public class InternalAPIKeyAuthenticator extends APIKeyHandler {

    private static final Log log = LogFactory.getLog(InternalAPIKeyAuthenticator.class);
    private String securityParam;

    public InternalAPIKeyAuthenticator(String securityParam) {
        this.securityParam = securityParam;
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String internalKey = requestContext.getHeaders().get(
                ConfigHolder.getInstance().getConfig().getAuthHeader().getTestConsoleHeaderName().toLowerCase());
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
                // Remove internal key from outbound request
                requestContext.getRemoveHeaders().add(securityParam);

                getKeyNotFoundError(internalKey);

                String[] splitToken = internalKey.split("\\.");
                SignedJWT signedJWT = SignedJWT.parse(internalKey);
                JWSHeader jwsHeader = signedJWT.getHeader();
                JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

                // Check if the decoded header contains type as 'InternalKey'.
                if (!isInternalKey(payload)) {
                    log.error("Invalid Internal Key token type." + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                String tokenIdentifier = payload.getJWTID();
                // check tokenIdentifier contains in revokedMap
                if (RevokedJWTDataHolder.isJWTTokenSignatureExistsInRevokedMap(tokenIdentifier)) {
                    log.error("Invalid Internal Key. " + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                String apiVersion = requestContext.getMatchedAPI().getAPIConfig().getVersion();
                String apiContext = requestContext.getMatchedAPI().getAPIConfig().getBasePath();
                boolean isVerified = false;

                // Verify token when it is found in cache
                JWTTokenPayloadInfo jwtTokenPayloadInfo = (JWTTokenPayloadInfo)
                        CacheProvider.getGatewayInternalKeyDataCache().getIfPresent(tokenIdentifier);
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
                    isVerified = cachedToken.equals(internalKey) && !isJwtTokenExpired(payload);
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
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key not found in the cache.");
                    }

                    String alias = "";
                    if (jwsHeader != null && StringUtils.isNotEmpty(jwsHeader.getKeyID())) {
                        alias = jwsHeader.getKeyID();
                    }

                    try {
                        isVerified = JWTUtil.verifyTokenSignature(signedJWT, alias) &&
                                !isJwtTokenExpired(payload, "InternalKey");
                    } catch (EnforcerException e) {
                        log.error("Internal Key authentication failed. " +
                                FilterUtils.getMaskedToken(splitToken[0]));
                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
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
                    JSONObject api = validateAPISubscription(apiContext, apiVersion, payload, splitToken,
                            false);
                    log.debug("Internal Key authentication successful.");

                    if (Utils.tracingEnabled()) {
                        apiKeyValidateSubscriptionSpanScope.close();
                        Utils.finishSpan(apiKeyValidateSubscriptionSpan);
                    }
                    return FilterUtils.generateAuthenticationContext(tokenIdentifier, payload, api,
                            requestContext.getMatchedAPI().getAPIConfig().getTier(),
                            requestContext.getMatchedAPI().getAPIConfig().getUuid());
                } else {
                    CacheProvider.getGatewayInternalKeyDataCache().invalidate(payload.getJWTID());
                    CacheProvider.getInvalidGatewayInternalKeyCache().put(payload.getJWTID(), internalKey);
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }
            } catch (ParseException e) {
                log.debug("Internal Key authentication failed. ", e);

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

    @Override
    public String getChallengeString() {
        return "";
    }

    private String extractInternalKey(RequestContext requestContext) {
        String internalKey = requestContext.getHeaders().get(securityParam);
        if (internalKey != null) {
            return internalKey.trim();
        }
        return null;
    }

    @Override
    public int getPriority() {

        return -10;
    }
}

