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
import org.wso2.choreo.connect.enforcer.security.Authenticator;
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
public class InternalAPIKeyAuthenticator implements Authenticator {

    private static final Log log = LogFactory.getLog(InternalAPIKeyAuthenticator.class);
    private String securityParam;

    public InternalAPIKeyAuthenticator(String securityParam) {
        this.securityParam = securityParam;
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String internalKey = requestContext.getHeaders().get(
                ConfigHolder.getInstance().getConfig().getAuthHeader().getTestConsoleHeaderName().toLowerCase());
        if (internalKey != null && internalKey.split("\\.").length == 3) {
            return true;
        }
        return false;
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
            if (log.isDebugEnabled()) {
                log.info("Internal Key Authentication initialized");
            }

            try {
                if (Utils.tracingEnabled()) {
                    tracer = Utils.getGlobalTracer();
                    apiKeyAuthenticatorSpan = Utils.startSpan(TracingConstants.API_KEY_AUTHENTICATOR_SPAN, tracer);
                    apiKeyAuthenticatorSpanScope = apiKeyAuthenticatorSpan.getSpan().makeCurrent();
                    Utils.setTag(apiKeyAuthenticatorSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
                }
                // Extract internal from the request while removing it from the msg context.
                String internalKey = extractInternalKey(requestContext);
                // Remove internal key from outbound request
                requestContext.getRemoveHeaders().add(securityParam);

                if (StringUtils.isEmpty(internalKey)) {
                    log.error("Cannot find Internal key header");
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                String[] splitToken = internalKey.split("\\.");
                SignedJWT signedJWT = SignedJWT.parse(internalKey);
                JWSHeader jwsHeader = signedJWT.getHeader();
                JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

                // Check if the decoded header contains type as 'InternalKey'.
                if (!isInternalKey(payload)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Invalid Internal Key token type. Internal Key: " +
                                FilterUtils.getMaskedToken(splitToken[0]));
                    }
                    log.error("Invalid Internal Key token type." + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                String tokenIdentifier = payload.getJWTID();
                // check tokenIdentifier contains in revokedMap
                if (RevokedJWTDataHolder.isJWTTokenSignatureExistsInRevokedMap(tokenIdentifier)) {
                    if (log.isDebugEnabled()) {
                        log.debug("InternalKey retrieved from the revoked jwt token map. Token: "
                                + FilterUtils.getMaskedToken(splitToken[0]));
                    }
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
                        Utils.setTag(verifyTokenInCacheSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    String cachedToken = jwtTokenPayloadInfo.getAccessToken();
                    isVerified = cachedToken.equals(internalKey) && !isJwtTokenExpired(payload);
                    if(Utils.tracingEnabled()) {
                        verifyTokenInCacheSpanScope.close();
                        Utils.finishSpan(verifyTokenInCacheSpan);
                    }
                } else if (CacheProvider.getInvalidGatewayInternalKeyCache().getIfPresent(tokenIdentifier) != null
                        && internalKey
                        .equals(CacheProvider.getInvalidGatewayInternalKeyCache().getIfPresent(tokenIdentifier))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key retrieved from the invalid internal Key cache. Internal Key: "
                                + FilterUtils.getMaskedToken(splitToken[0]));
                    }
                    log.error("Invalid Internal Key. " + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                Scope verifyTokenWithoutCacheSpanScope = null;
                // Verify token when it is not found in cache
                if (!isVerified) {
                    if (Utils.tracingEnabled()) {
                        verifyTokenWithoutCacheSpan = Utils.startSpan(TracingConstants.VERIFY_TOKEN_SPAN, tracer);
                        verifyTokenWithoutCacheSpanScope = verifyTokenWithoutCacheSpan.getSpan().makeCurrent();
                        Utils.setTag(verifyTokenInCacheSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key not found in the cache.");
                    }

                    String alias = "";
                    if (jwsHeader != null && StringUtils.isNotEmpty(jwsHeader.getKeyID())) {
                        alias = jwsHeader.getKeyID();
                    }

                    try {
                        isVerified = JWTUtil.verifyTokenSignature(signedJWT, alias) && !isJwtTokenExpired(payload);
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
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key signature is verified.");
                    }
                    if (jwtTokenPayloadInfo == null) {
                        // Retrieve payload from InternalKey
                        if (log.isDebugEnabled()) {
                            log.debug("InternalKey payload not found in the cache.");
                        }
                        jwtTokenPayloadInfo = new JWTTokenPayloadInfo();
                        jwtTokenPayloadInfo.setPayload(payload);
                        jwtTokenPayloadInfo.setAccessToken(internalKey);
                        CacheProvider.getGatewayInternalKeyDataCache().put(tokenIdentifier, jwtTokenPayloadInfo);
                    }
                    Scope apiKeyValidateSubscriptionSpanScope = null;
                    if (Utils.tracingEnabled()) {
                        apiKeyValidateSubscriptionSpan = Utils.startSpan(TracingConstants.API_KEY_VALIDATE_SUBSCRIPTION_SPAN, tracer);
                        apiKeyValidateSubscriptionSpanScope = apiKeyValidateSubscriptionSpan.getSpan().makeCurrent();
                        Utils.setTag(apiKeyValidateSubscriptionSpan, APIConstants.LOG_TRACE_ID, ThreadContext.get(APIConstants.LOG_TRACE_ID));
                    }
                    JSONObject api = validateAPISubscription(apiContext, apiVersion, payload, splitToken,
                            false);
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key authentication successful.");
                    }
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
                if (log.isDebugEnabled()) {
                    log.debug("Internal Key authentication failed. ", e);
                }
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

    public static JSONObject validateAPISubscription(String apiContext, String apiVersion, JWTClaimsSet payload,
                                                     String[] splitToken, boolean isOauth)
            throws APISecurityException {

        JSONObject api = null;

        if (payload.getClaim(APIConstants.JwtTokenConstants.SUBSCRIBED_APIS) != null) {
            // Subscription validation
            JSONArray subscribedAPIs =
                    (JSONArray) payload.getClaim(APIConstants.JwtTokenConstants.SUBSCRIBED_APIS);
            for (Object subscribedAPI : subscribedAPIs) {
                JSONObject subscribedAPIsJSONObject = (JSONObject) subscribedAPI;
                if (apiContext
                        .equals(subscribedAPIsJSONObject.getAsString(APIConstants.JwtTokenConstants.API_CONTEXT)) &&
                        apiVersion
                                .equals(subscribedAPIsJSONObject.getAsString(APIConstants.JwtTokenConstants.API_VERSION)
                                )) {
                    api = subscribedAPIsJSONObject;
                    if (log.isDebugEnabled()) {
                        log.debug("User is subscribed to the API: " + apiContext + ", " +
                                "version: " + apiVersion + ". Token: " + FilterUtils.getMaskedToken(splitToken[0]));
                    }
                    break;
                }
            }
            if (api == null) {
                if (log.isDebugEnabled()) {
                    log.debug("User is not subscribed to access the API: " + apiContext +
                            ", version: " + apiVersion + ". Token: " + FilterUtils.getMaskedToken(splitToken[0]));
                }
                log.error("User is not subscribed to access the API.");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No subscription information found in the token.");
            }
            // we perform mandatory authentication for Api Keys
            if (!isOauth) {
                log.error("User is not subscribed to access the API.");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        }
        return api;
    }

    private String extractInternalKey(RequestContext requestContext) {
        String internalKey = requestContext.getHeaders().get(securityParam);
        if (internalKey != null) {
            return internalKey.trim();
        }
        return null;
    }

    private static boolean isInternalKey(JWTClaimsSet jwtClaimsSet) {
        Object tokenTypeClaim = jwtClaimsSet.getClaim(APIConstants.JwtTokenConstants.TOKEN_TYPE);
        if (tokenTypeClaim != null) {
            return APIConstants.JwtTokenConstants.INTERNAL_KEY_TOKEN_TYPE.equals(tokenTypeClaim);
        }
        return false;
    }

    /**
     * Check whether the jwt token is expired or not.
     *
     * @param payload The payload of the JWT token
     * @return returns true if the JWT token is expired
     */
    public boolean isJwtTokenExpired(JWTClaimsSet payload) throws APISecurityException {

        int timestampSkew =  (int) getTimeStampSkewInSeconds();
        DefaultJWTClaimsVerifier jwtClaimsSetVerifier = new DefaultJWTClaimsVerifier();
        jwtClaimsSetVerifier.setMaxClockSkew(timestampSkew);
        try {
            jwtClaimsSetVerifier.verify(payload);
            if (log.isDebugEnabled()) {
                log.debug("Internal-Key is not expired. User: " + payload.getSubject());
            }
        } catch (BadJWTException e) {
            if ("Expired JWT".equals(e.getMessage())) {
                if (log.isDebugEnabled()) {
                    log.debug("Internal Key is expired. Internal Key");
                }
                CacheProvider.getGatewayInternalKeyDataCache().invalidate(payload.getJWTID());
                CacheProvider.getInvalidGatewayInternalKeyCache().put(payload.getJWTID(), "carbon.super");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Internal-Key is not expired. User: " + payload.getSubject());
        }
        return false;
    }

    protected long getTimeStampSkewInSeconds() {
        //TODO : Read from config
        return 5;
    }

    @Override
    public int getPriority() {

        return -10;
    }
}

