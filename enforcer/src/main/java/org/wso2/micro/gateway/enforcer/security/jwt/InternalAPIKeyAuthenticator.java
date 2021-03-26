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
package org.wso2.micro.gateway.enforcer.security.jwt;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.common.CacheProvider;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.APISecurityConstants;
import org.wso2.micro.gateway.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.micro.gateway.enforcer.exception.APISecurityException;
import org.wso2.micro.gateway.enforcer.exception.MGWException;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;
import org.wso2.micro.gateway.enforcer.security.Authenticator;
import org.wso2.micro.gateway.enforcer.security.jwt.validator.RevokedJWTDataHolder;
import org.wso2.micro.gateway.enforcer.util.FilterUtils;

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
        String internalKey = requestContext.getHeaders().get(APIConstants.JwtTokenConstants.INTERNAL_KEY);
        if (internalKey != null && internalKey.split("\\.").length == 3) {
            return true;
        }
        return false;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        if (requestContext.getMathedAPI() != null) {
            if (log.isDebugEnabled()) {
                log.info("Internal Key Authentication initialized");
            }

            try {
                // Extract internal from the request while removing it from the msg context.
                String internalKey = extractInternalKey(requestContext);
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

                String apiVersion = requestContext.getMathedAPI().getAPIConfig().getVersion();
                String apiContext = requestContext.getMathedAPI().getAPIConfig().getBasePath();
                boolean isVerified = false;

                JWTTokenPayloadInfo jwtTokenPayloadInfo = (JWTTokenPayloadInfo)
                        CacheProvider.getGatewayInternalKeyDataCache().getIfPresent(tokenIdentifier);
                if (jwtTokenPayloadInfo != null) {
                    String rawPayload = jwtTokenPayloadInfo.getRawPayload();
                    isVerified = rawPayload.equals(splitToken[1]);
                } else if (CacheProvider.getInvalidGatewayInternalKeyCache().getIfPresent(tokenIdentifier) != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key retrieved from the invalid internal Key cache. Internal Key: "
                                + FilterUtils.getMaskedToken(splitToken[0]));
                    }
                    log.error("Invalid Internal Key. " + FilterUtils.getMaskedToken(splitToken[0]));
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                            APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                }

                // Not found in cache
                if (!isVerified) {
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key not found in the cache.");
                    }

                    String alias = "";
                    if (jwsHeader != null && StringUtils.isNotEmpty(jwsHeader.getKeyID())) {
                        alias = jwsHeader.getKeyID();
                    }

                    try {
                        isVerified = JWTUtil.verifyTokenSignature(signedJWT, alias) && !isJwtTokenExpired(payload);
                    } catch (MGWException e) {
                        log.error("Internal Key authentication failed. " +
                                FilterUtils.getMaskedToken(splitToken[0]));
                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                    }

                    if (!isVerified) {
                        CacheProvider.getGatewayInternalKeyDataCache().invalidate(tokenIdentifier);
                        CacheProvider.getInvalidGatewayInternalKeyCache().put(tokenIdentifier, "carbon.super");
                    }
                }

                if (isVerified) {
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key signature is verified.");
                    }
                    if (jwtTokenPayloadInfo != null && jwtTokenPayloadInfo.getRawPayload() != null) {
                        // Internal Key is found in the key cache
                        if (isJwtTokenExpired(payload)) {
                            CacheProvider.getGatewayInternalKeyDataCache().invalidate(tokenIdentifier);
                            CacheProvider.getInvalidGatewayInternalKeyCache().put(tokenIdentifier, "carbon.super");
                            log.error("Internal Key is expired");
                            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                        }
                    } else {
                        // Retrieve payload from InternalKey
                        if (log.isDebugEnabled()) {
                            log.debug("InternalKey payload not found in the cache.");
                        }
                        jwtTokenPayloadInfo = new JWTTokenPayloadInfo();
                        jwtTokenPayloadInfo.setPayload(payload);
                        jwtTokenPayloadInfo.setRawPayload(splitToken[1]);
                        CacheProvider.getGatewayInternalKeyDataCache().put(tokenIdentifier, jwtTokenPayloadInfo);
                    }

                    JSONObject api = validateAPISubscription(apiContext, apiVersion, payload, splitToken,
                            false);
                    if (log.isDebugEnabled()) {
                        log.debug("Internal Key authentication successful.");
                    }
                    return FilterUtils.generateAuthenticationContext(tokenIdentifier, payload, api,
                            requestContext.getMathedAPI().getAPIConfig().getTier());
                }
            } catch (ParseException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Internal Key authentication failed. ", e);
                }
            }
        }
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                APISecurityConstants.API_AUTH_GENERAL_ERROR, APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
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
    public boolean isJwtTokenExpired(JWTClaimsSet payload) {

        int timestampSkew =  (int) getTimeStampSkewInSeconds();
        DefaultJWTClaimsVerifier jwtClaimsSetVerifier = new DefaultJWTClaimsVerifier();
        jwtClaimsSetVerifier.setMaxClockSkew(timestampSkew);
        try {
            jwtClaimsSetVerifier.verify(payload);
            if (log.isDebugEnabled()) {
                log.debug("Internal-Key is not expired. User: " + payload.getSubject());
            }
        } catch (BadJWTException e) {
            if ("Expired Internal-Key".equals(e.getMessage())) {
                return true;
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

