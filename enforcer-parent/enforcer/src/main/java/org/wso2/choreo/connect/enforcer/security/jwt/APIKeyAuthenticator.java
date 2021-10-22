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
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecurityInfo;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.JwtConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Extends the APIKeyHandler to authenticate request using API Key.
 */
public class APIKeyAuthenticator extends APIKeyHandler {

    private static final Log log = LogFactory.getLog(APIKeyAuthenticator.class);
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private boolean isGatewayTokenCacheEnabled;

    private static final int IPV4_ADDRESS_BIT_LENGTH = 32;
    private static final int IPV6_ADDRESS_BIT_LENGTH = 128;

    public APIKeyAuthenticator() {
        log.info("API key authenticator initialized.");
        this.isGatewayTokenCacheEnabled = ConfigHolder.getInstance().getConfig().getCacheDto().isEnabled();
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        boolean isAPIkeyProtected = getIsAPIKeyProtected(requestContext);
        if (!isAPIkeyProtected) {
            return  false;
        }
        String apiKey = getAPIKeyFromRequest(requestContext);
        return isAPIKey(apiKey);
    }

    private String getAPIKeyAllowedIn(RequestContext requestContext, boolean isAppLevelAPIKeyRequest) {
        String apiKeyLocation = "";
        SecuritySchemaConfig securitySchemaConfig = FilterUtils.getAPIKeySchemeConfig(requestContext,
                isAppLevelAPIKeyRequest);
        if (securitySchemaConfig != null) {
            apiKeyLocation = securitySchemaConfig.getIn();
        }
        return apiKeyLocation;
    }

    private boolean getIsAPIKeyProtected(RequestContext requestContext) {
        boolean isAPIKeyProtected = false;
        List<ResourceConfig> resourceConfigList = requestContext.getMatchedAPI().getResources();
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = requestContext.getMatchedAPI()
                .getSecuritySchemeDefinitions();
        for (int a = 0; a < resourceConfigList.size(); a++) {
            ResourceConfig resourceConfig = resourceConfigList.get(a);
            if (resourceConfig.getPath().equalsIgnoreCase(requestContext.getMatchedResourcePath().getPath()) &&
                    resourceConfig.getMethod().name().equalsIgnoreCase(requestContext.getRequestMethod())) {
                Map<String, List<String>> resourceSecuritySchemes = resourceConfig.getSecuritySchemas();
                if (resourceSecuritySchemes.containsKey(APIConstants.API_SECURITY_API_KEY) || resourceSecuritySchemes.
                        containsKey(FilterUtils.getAPIKeyArbitraryName(securitySchemeDefinitions))) {
                    isAPIKeyProtected = true;
                }
            }
        }
        return isAPIKeyProtected;
    }

    // Gets API key from request
    private String getAPIKeyFromRequest(RequestContext requestContext) {
        boolean isAppLevelSecurityRequest = getIsAppLevelSecurityRequest(requestContext);
        String apiKeyName = FilterUtils.getAPIKeyName(requestContext, isAppLevelSecurityRequest);
        String apiKey = "";
        String apiKeyLocation = getAPIKeyAllowedIn(requestContext, isAppLevelSecurityRequest);
        if (apiKeyLocation.equals(APIConstants.SWAGGER_API_KEY_IN_HEADER) || isAppLevelSecurityRequest) {
            Map<String, String> headers = requestContext.getHeaders();
            apiKey = getAPIKeyFromMap(headers, apiKeyName);
        }
        if ((isAppLevelSecurityRequest && StringUtils.isEmpty(apiKey)) || (StringUtils.isEmpty(apiKey) &&
                apiKeyLocation.equals(APIConstants.SWAGGER_API_KEY_IN_QUERY))) {
            Map<String, String> queryParameters = requestContext.getQueryParameters();
            apiKey = getAPIKeyFromMap(queryParameters, apiKeyName);
        }
        return apiKey;
    }

    private boolean getIsAppLevelSecurityRequest(RequestContext requestContext) {
        boolean isApplicationLevelSecurityRequest = false;
        Map<String, String> headers = requestContext.getHeaders();
        Map<String, String> queryParams = requestContext.getQueryParameters();
        if (headers.containsKey(APIConstants.API_SECURITY_API_KEY)) {
            isApplicationLevelSecurityRequest = true;
        } else if (queryParams.containsKey(APIConstants.API_SECURITY_API_KEY)) {
            isApplicationLevelSecurityRequest = true;
        }
        return isApplicationLevelSecurityRequest;

    }

    private String getAPIKeyFromMap(Map<String, String> requestMetaData, String apiKeyName) {
        String apiKey = "";
        if (requestMetaData.containsKey(apiKeyName)) {
            return requestMetaData.get(apiKeyName);
        }
        if (StringUtils.isEmpty(apiKey)) {
            if (requestMetaData.containsKey(APIConstants.API_SECURITY_API_KEY)) {
                return  requestMetaData.get(apiKeyName);
            }
        }
        return apiKey;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        if (requestContext.getMatchedAPI() == null) {
            log.debug("API Key Authentication failed");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    "API key authentication failed.");
        }
        try {
            String apiKey = getAPIKeyFromRequest(requestContext);

            // Gives an error if API key not found
            getKeyNotFoundError(apiKey);

            String[] splitToken = apiKey.split("\\.");
            SignedJWT signedJWT = SignedJWT.parse(apiKey);
            JWSHeader jwsHeader = signedJWT.getHeader();
            JWTClaimsSet payload = signedJWT.getJWTClaimsSet();
            String apiVersion = requestContext.getMatchedAPI().getVersion();
            String apiContext = requestContext.getMatchedAPI().getBasePath();

            // Avoids using internal API keys
            if (isInternalKey(payload)) {
                log.error("Invalid API Key token type." + FilterUtils.getMaskedToken(splitToken[0]));
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                        APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
            }

            // Gives jti (also used to populate authentication context)
            String tokenIdentifier = payload.getJWTID();

            // Checks whether key contains in revoked map.
            checkInRevokedMap(tokenIdentifier, splitToken);

            // Verifies the token if it is found in cache
            JWTTokenPayloadInfo jwtTokenPayloadInfo = (JWTTokenPayloadInfo)
                    CacheProvider.getGatewayAPIKeyDataCache().getIfPresent(tokenIdentifier);
            boolean isVerified = verifyTokenInCache(tokenIdentifier, apiKey, payload, splitToken,
                    "API Key", jwtTokenPayloadInfo);

            // Verifies token when it is not found in cache
            if (!isVerified) {
                isVerified = verifyTokenNotInCache(jwsHeader, signedJWT, splitToken, payload, "API Key");
            }

            if (isVerified) {
                log.debug("API Key signature is verified.");

                if (jwtTokenPayloadInfo == null) {
                    log.debug("API Key payload not found in the cache.");

                    jwtTokenPayloadInfo = new JWTTokenPayloadInfo();
                    jwtTokenPayloadInfo.setPayload(payload);
                    jwtTokenPayloadInfo.setAccessToken(apiKey);
                    CacheProvider.getGatewayAPIKeyDataCache().put(tokenIdentifier, jwtTokenPayloadInfo);
                }

                //Get APIKeyValidationInfoDTO
                APIKeyValidationInfoDTO apiKeyValidationInfoDTO = getAPIKeyValidationDTO(requestContext, payload);

                // Sets endpoint security
                SecurityInfo securityInfo;
                if (apiKeyValidationInfoDTO.getType() != null &&
                        requestContext.getMatchedAPI().getEndpointSecurity() != null) {
                    if (apiKeyValidationInfoDTO.getType().equals(APIConstants.API_KEY_TYPE_PRODUCTION)) {
                        securityInfo = requestContext.getMatchedAPI().getEndpointSecurity().
                                getProductionSecurityInfo();
                    } else {
                        securityInfo = requestContext.getMatchedAPI().getEndpointSecurity().
                                getSandBoxSecurityInfo();
                    }
                    if (securityInfo.isEnabled() &&
                            APIConstants.AUTHORIZATION_HEADER_BASIC.
                                    equalsIgnoreCase(securityInfo.getSecurityType())) {
                        requestContext.getRemoveHeaders().remove(APIConstants.AUTHORIZATION_HEADER_DEFAULT
                                .toLowerCase());
                        requestContext.addOrModifyHeaders(APIConstants.AUTHORIZATION_HEADER_DEFAULT,
                                APIConstants.AUTHORIZATION_HEADER_BASIC + ' ' +
                                        Base64.getEncoder().encodeToString((securityInfo.getUsername() +
                                                ':' + securityInfo.getPassword()).getBytes()));
                    }
                }

                validateAPIKeyRestrictions(payload, requestContext, apiContext, apiVersion);

                validateAPISubscription(apiContext, apiVersion, payload, splitToken, false);

                log.debug("API Key authentication successful.");

                // Begins analytics data processing

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
                    requestContext.addOrModifyHeaders(jwtConfigurationDto.getJwtHeader(), endUserToken);
                }
                JWTClaimsSet claims = signedJWTInfo.getJwtClaimsSet();

                AuthenticationContext authenticationContext = FilterUtils
                        .generateAuthenticationContext(requestContext, tokenIdentifier, validationInfo,
                                apiKeyValidationInfoDTO, endUserToken, false);

                if (claims.getClaim("keytype") != null) {
                    authenticationContext.setKeyType(claims.getClaim("keytype").toString());
                }
                log.debug("Analytics data processing for API Key (jiti) " + tokenIdentifier +
                        " was successful");
                return authenticationContext;

            }
        } catch (ParseException e) {
            log.warn("API Key authentication failed. ", e);
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    "API key authentication failed.");
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
        String name = requestContext.getMatchedAPI().getName();
        String version = requestContext.getMatchedAPI().getVersion();
        JSONObject api = null;

        validationInfoDTO.setApiTier(requestContext.getMatchedAPI().getTier());
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

    private void validateAPIKeyRestrictions(JWTClaimsSet payload, RequestContext requestContext, String apiContext,
                                            String apiVersion) throws APISecurityException {
        String permittedIPList = null;
        if (payload.getClaim(APIConstants.JwtTokenConstants.PERMITTED_IP) != null) {
            permittedIPList = (String) payload.getClaim(APIConstants.JwtTokenConstants.PERMITTED_IP);
        }

        if (StringUtils.isNotEmpty(permittedIPList)) {
            // Validate client IP against permitted IPs
            String clientIP = requestContext.getClientIp();

            if (StringUtils.isNotEmpty(clientIP)) {
                for (String restrictedIP : permittedIPList.split(",")) {
                    if (isIpInNetwork(clientIP, restrictedIP.trim())) {
                        // Client IP is allowed
                        return;
                    }
                }
                if (StringUtils.isNotEmpty(clientIP)) {
                    log.debug("Invocations to API: " + apiContext + ":" + apiVersion +
                            " is not permitted for client with IP: " + clientIP);
                }

                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }

        }

        String permittedRefererList = null;
        if (payload.getClaim(APIConstants.JwtTokenConstants.PERMITTED_REFERER) != null) {
            permittedRefererList = (String) payload.getClaim(APIConstants.JwtTokenConstants.PERMITTED_REFERER);
        }
        if (StringUtils.isNotEmpty(permittedRefererList)) {
            // Validate http referer against the permitted referrers
            Map<String, String> transportHeaderMap = requestContext.getHeaders();
            if (transportHeaderMap != null) {
                String referer = transportHeaderMap.get("referer");
                if (StringUtils.isNotEmpty(referer)) {
                    for (String restrictedReferer : permittedRefererList.split(",")) {
                        String restrictedRefererRegExp = restrictedReferer.trim()
                                .replace("*", "[^ ]*");
                        if (referer.matches(restrictedRefererRegExp)) {
                            // Referer is allowed
                            return;
                        }
                    }
                    if (StringUtils.isNotEmpty(referer)) {
                        log.debug("Invocations to API: " + apiContext + ":" + apiVersion +
                                " is not permitted for referer: " + referer);
                    }
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                            APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
                } else {
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                            APISecurityConstants.API_AUTH_FORBIDDEN, APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
                }
            }
        }
    }

    private boolean isIpInNetwork(String ip, String cidr) {

        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(cidr)) {
            return false;
        }
        ip = ip.trim();
        cidr = cidr.trim();

        if (cidr.contains("/")) {
            String[] cidrArr = cidr.split("/");
            if (cidrArr.length < 2 || (ip.contains(".") && !cidr.contains(".")) ||
                    (ip.contains(":") && !cidr.contains(":"))) {
                return false;
            }

            BigInteger netAddress = ipToBigInteger(cidrArr[0]);
            int netBits = Integer.parseInt(cidrArr[1]);
            BigInteger givenIP = ipToBigInteger(ip);

            if (ip.contains(".")) {
                // IPv4
                if (netAddress.shiftRight(IPV4_ADDRESS_BIT_LENGTH - netBits)
                        .shiftLeft(IPV4_ADDRESS_BIT_LENGTH - netBits).compareTo(
                                givenIP.shiftRight(IPV4_ADDRESS_BIT_LENGTH - netBits)
                                        .shiftLeft(IPV4_ADDRESS_BIT_LENGTH - netBits)) == 0) {
                    return true;
                }
            } else if (ip.contains(":")) {
                // IPv6
                if (netAddress.shiftRight(IPV6_ADDRESS_BIT_LENGTH - netBits)
                        .shiftLeft(IPV6_ADDRESS_BIT_LENGTH - netBits).compareTo(
                                givenIP.shiftRight(IPV6_ADDRESS_BIT_LENGTH - netBits)
                                        .shiftLeft(IPV6_ADDRESS_BIT_LENGTH - netBits)) == 0) {
                    return true;
                }
            }
        } else if (ip.equals(cidr)) {
            return true;
        }
        return false;
    }


    private BigInteger ipToBigInteger(String ipAddress) {

        InetAddress address;
        try {
            address = getAddress(ipAddress);
            byte[] bytes = address.getAddress();
            return new BigInteger(1, bytes);
        } catch (UnknownHostException e) {
            //ignore the error and log it
            log.error("Error while parsing host IP " + ipAddress, e);
        }
        return BigInteger.ZERO;
    }

    private InetAddress getAddress(String ipAddress) throws UnknownHostException {

        return InetAddress.getByName(ipAddress);
    }

    @Override
    public String getChallengeString() {
        return "";
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
