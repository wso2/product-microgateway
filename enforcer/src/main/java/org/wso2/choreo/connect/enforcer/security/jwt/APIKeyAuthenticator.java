/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.discovery.api.SecurityInfo;
import org.wso2.choreo.connect.enforcer.api.RequestContext;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.JwtConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.security.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTValidator;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

/**
 * Extends the APIKeyHandler to authenticate request using API Key.
 */
public class APIKeyAuthenticator extends APIKeyHandler {

    private static final Log log = LogFactory.getLog(APIKeyAuthenticator.class);
    private JWTValidator jwtValidator = new JWTValidator();
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private boolean isGatewayTokenCacheEnabled;

    public APIKeyAuthenticator() {
        log.info("API key authenticator initialized.");
        this.isGatewayTokenCacheEnabled = ConfigHolder.getInstance().getConfig().getCacheDto().isEnabled();
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String apiKey = retrieveAPIKeyHeaderValue(requestContext);
        return isAPIKey(apiKey);
    }

    private String retrieveAPIKeyHeaderValue(RequestContext requestContext) {
        Map<String, String> headers = requestContext.getHeaders();
        return headers.get(FilterUtils.getAPIKeyHeaderName(requestContext));
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        if (requestContext.getMatchedAPI() != null) {
            log.debug("API Key Authentication initialized");

            try {
                String apiKey = retrieveAPIKeyHeaderValue(requestContext);

                // gives an error if API key not found
                getKeyNotFoundError(apiKey);

                String[] splitToken = apiKey.split("\\.");
                SignedJWT signedJWT = SignedJWT.parse(apiKey);
                JWSHeader jwsHeader = signedJWT.getHeader();
                JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

                //gives jti (also used to populate authentication context)
                String tokenIdentifier = payload.getJWTID();

                //check whether key contains in revoked map.
                checkInRevokedMap(tokenIdentifier, splitToken);

                // Verify the token if it is found in cache
                JWTTokenPayloadInfo jwtTokenPayloadInfo = (JWTTokenPayloadInfo)
                        CacheProvider.getGatewayAPIKeyDataCache().getIfPresent(tokenIdentifier);
                boolean isVerified = verifyTokenInCache(tokenIdentifier, apiKey, payload, splitToken,
                        "API Key", jwtTokenPayloadInfo);

                // Verify token when it is not found in cache
                if (!isVerified) {
                    isVerified = verifyTokenNotInCache(jwsHeader, signedJWT, splitToken, payload, "API Key");
                }

                if (isVerified) {
                    log.debug("API Key signature is verified.");

                    if (jwtTokenPayloadInfo == null) {
                        log.debug("InternalKey payload not found in the cache.");

                        jwtTokenPayloadInfo = new JWTTokenPayloadInfo();
                        jwtTokenPayloadInfo.setPayload(payload);
                        jwtTokenPayloadInfo.setAccessToken(apiKey);
                        CacheProvider.getGatewayAPIKeyDataCache().put(tokenIdentifier, jwtTokenPayloadInfo);
                    }

                    //Get APIKeyValidationInfoDTO
                    APIKeyValidationInfoDTO apiKeyValidationInfoDTO = getAPIKeyValidationDTO(requestContext, payload);

                    // set endpoint security
                    SecurityInfo securityInfo;
                    if (apiKeyValidationInfoDTO.getType() != null &&
                            requestContext.getMatchedAPI().getAPIConfig().getEndpointSecurity() != null) {
                        if (apiKeyValidationInfoDTO.getType().equals(APIConstants.API_KEY_TYPE_PRODUCTION)) {
                            securityInfo = requestContext.getMatchedAPI().getAPIConfig().getEndpointSecurity().
                                    getProductionSecurityInfo();
                        } else {
                            securityInfo = requestContext.getMatchedAPI().getAPIConfig().getEndpointSecurity().
                                    getSandBoxSecurityInfo();
                        }
                        if (securityInfo.getEnabled() &&
                                APIConstants.AUTHORIZATION_HEADER_BASIC.
                                        equalsIgnoreCase(securityInfo.getSecurityType())) {
                            requestContext.getRemoveHeaders().remove(APIConstants.AUTHORIZATION_HEADER_DEFAULT
                                    .toLowerCase());
                            requestContext.addResponseHeaders(APIConstants.AUTHORIZATION_HEADER_DEFAULT,
                                    APIConstants.AUTHORIZATION_HEADER_BASIC + ' ' +
                                            Base64.getEncoder().encodeToString((securityInfo.getUsername() +
                                                    ':' + securityInfo.getPassword()).getBytes()));
                        }
                    }

//                    JSONObject api = validateAPISubscription(apiContext, apiVersion, payload, splitToken, false);

                    log.debug("API Key authentication successful.");

                    //======= Analytics data processing begins

                    //Get SignedJWTInfo
                    SignedJWTInfo signedJWTInfo = getSignedJwt(apiKey);

                    //Get JWTValidationInfo
                    JWTValidationInfo validationInfo = new JWTValidationInfo();
                    validationInfo.setUser(payload.getSubject());

                    String endUserToken = null;

                    // Get jwtConfigurationDto
                    JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().
                            getConfig().getJwtConfigurationDto();
                    if (jwtConfigurationDto.isEnabled()) {
                        // Set ttl
                        jwtConfigurationDto.setTtl(JWTUtil.getTTL());

                        JWTInfoDto jwtInfoDto = FilterUtils
                                .generateJWTInfoDto(null, validationInfo, apiKeyValidationInfoDTO, requestContext);
                        endUserToken = generateAndRetrieveJWTToken(tokenIdentifier, jwtInfoDto);
                        // Set generated jwt token as a response header
                        requestContext.addResponseHeaders(jwtConfigurationDto.getJwtHeader(), endUserToken);
                    }
                    JWTClaimsSet claims = signedJWTInfo.getJwtClaimsSet();

                    AuthenticationContext authenticationContext = FilterUtils
                            .generateAuthenticationContext(requestContext, tokenIdentifier, validationInfo,
                                    apiKeyValidationInfoDTO, endUserToken, false);

                    if (claims.getClaim("keytype") != null) {
                        authenticationContext.setKeyType(claims.getClaim("keytype").toString());
                    }

                    return authenticationContext;

                }
            } catch (ParseException e) {
                log.debug("API Key authentication failed. ", e);
            }

        }

        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                APISecurityConstants.API_AUTH_GENERAL_ERROR, APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
    }

    private SignedJWTInfo getSignedJwt(String accessToken) throws ParseException {
        String signature = accessToken.split("\\.")[2];
        SignedJWTInfo signedJWTInfo = null;
        //Check whether GatewaySignedJWTParseCache is correct
        LoadingCache gatewaySignedJWTParseCache = CacheProvider.getGatewaySignedJWTParseCache();
        if (gatewaySignedJWTParseCache != null) {
            Object cachedEntry = gatewaySignedJWTParseCache.getIfPresent(signature);
            if (cachedEntry != null) {
                signedJWTInfo = (SignedJWTInfo) cachedEntry;
            }
            if (signedJWTInfo == null  || !signedJWTInfo.getToken().equals(accessToken)) {
                SignedJWT signedJWT = SignedJWT.parse(accessToken);
                JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
                gatewaySignedJWTParseCache.put(signature, signedJWTInfo);
            }
        } else {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
        }
        return signedJWTInfo;
    }

    private APIKeyValidationInfoDTO getAPIKeyValidationDTO(RequestContext requestContext, JWTClaimsSet payload)
            throws ParseException, APISecurityException {

        APIKeyValidationInfoDTO validationInfoDTO = new APIKeyValidationInfoDTO();
        JSONObject app = payload.getJSONObjectClaim(APIConstants.JwtTokenConstants.APPLICATION);
        String name = requestContext.getMatchedAPI().getAPIConfig().getName();
        String version = requestContext.getMatchedAPI().getAPIConfig().getVersion();
        JSONObject api = null;

        validationInfoDTO.setApiTier(requestContext.getMatchedAPI().getAPIConfig().getTier());
        if (payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE) != null) {
            validationInfoDTO.setType(payload.getStringClaim(APIConstants.JwtTokenConstants.KEY_TYPE));
        } else {
            validationInfoDTO.setType(APIConstants.API_KEY_TYPE_PRODUCTION);
        }
        if (app != null) {
            validationInfoDTO.setApplicationId(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_ID));
            validationInfoDTO.setApplicationName(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_NAME));
            validationInfoDTO.setApplicationTier(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_TIER));
            validationInfoDTO.setSubscriber(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_OWNER));
        }

        //check whether name is assigned correctly (This was not populated in JWTAuthenticator)
        validationInfoDTO.setApiName(name);
        validationInfoDTO.setApiVersion(version);

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
                    validationInfoDTO.setAuthorized(true);

                    //set throttling attributes if present
                    String subTier = subApi.getAsString(APIConstants.JwtTokenConstants.SUBSCRIPTION_TIER);
                    String subPublisher = subApi.getAsString(APIConstants.JwtTokenConstants.API_PUBLISHER);
                    String subTenant = subApi.getAsString(APIConstants.JwtTokenConstants.SUBSCRIBER_TENANT_DOMAIN);
                    if (subTier != null) {
                        validationInfoDTO.setTier(subTier);
                    }
                    if (subPublisher != null) {
                        validationInfoDTO.setApiPublisher(subPublisher);
                    }
                    if (subTenant != null) {
                        validationInfoDTO.setSubscriberTenantDomain(subTenant);
                    }

                    log.debug("APIKeyValidationInfoDTO populated for API: " + name + ", " +
                            "version: " + version + ".");

                    break;
                }
            }
            if (api == null) {
                log.debug("Subscription data not populated in APIKeyValidationInfoDTO for the API: " + name +
                            ", version: " + version + ".");
                log.error("User's subscription details cannot obtain for the API : " + name + ".");
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN,
                        APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        }

        return validationInfoDTO;
    }

    private String generateAndRetrieveJWTToken(String tokenSignature, JWTInfoDto jwtInfoDto)
            throws APISecurityException {
        log.debug("Inside generateAndRetrieveJWTToken");

        String endUserToken = null;
        boolean valid = false;
        String jwtTokenCacheKey = jwtInfoDto.getApiContext().concat(":").concat(jwtInfoDto.getVersion()).concat(":")
                .concat(tokenSignature);
        JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().getConfig().getJwtConfigurationDto();
        // Get the jwt generator class (Default jwt generator class)
        //Todo: load the class from the configuration
        jwtGenerator = JWTUtil.getApiMgtGatewayJWTGenerator();
        if (jwtGenerator != null) {
            jwtGenerator.setJWTConfigurationDto(jwtConfigurationDto);
            if (isGatewayTokenCacheEnabled) {
                try {
                    Object token = CacheProvider.getGatewayJWTTokenCache().get(jwtTokenCacheKey);
                    if (token != null && !JWTConstants.UNAVAILABLE.equals(token)) {
                        endUserToken = (String) token;
                        String[] splitToken = ((String) token).split("\\.");
                        org.json.JSONObject payload = new org.json.JSONObject(new String(Base64.getUrlDecoder().
                                decode(splitToken[1])));
                        long exp = payload.getLong(JwtConstants.EXP);
                        long timestampSkew = getTimeStampSkewInSeconds() * 1000;
                        valid = (exp - System.currentTimeMillis() > timestampSkew);
                    }
                } catch (Exception e) {
                    log.error("Error while getting token from the cache", e);
                }

                if (StringUtils.isEmpty(endUserToken) || !valid) {
                    try {
                        endUserToken = jwtGenerator.generateToken(jwtInfoDto);
                        CacheProvider.getGatewayJWTTokenCache().put(jwtTokenCacheKey, endUserToken);
                    } catch (JWTGeneratorException e) {
                        log.error("Error while Generating Backend JWT", e);
                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                APISecurityConstants.API_AUTH_GENERAL_ERROR,
                                APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE, e);
                    }
                }
            } else {
                try {
                    endUserToken = jwtGenerator.generateToken(jwtInfoDto);
                } catch (JWTGeneratorException e) {
                    log.error("Error while Generating Backend JWT", e);
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                            APISecurityConstants.API_AUTH_GENERAL_ERROR,
                            APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE, e);
                }
            }
        } else {
            log.debug("Error while loading JWTGenerator");
        }
        return endUserToken;
    }

    @Override
    public String getChallengeString() {
        return "";
        //check again
    }

    @Override
    public int getPriority() {
        return 10;
        //check again
    }
}
