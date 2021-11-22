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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.EnforcerConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Extends the APIKeyHandler to authenticate request using API Key.
 */
public class APIKeyAuthenticator extends APIKeyHandler {

    private static final Logger log = LogManager.getLogger(APIKeyAuthenticator.class);
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private final boolean isGatewayTokenCacheEnabled;

    private static final int IPV4_ADDRESS_BIT_LENGTH = 32;
    private static final int IPV6_ADDRESS_BIT_LENGTH = 128;

    public APIKeyAuthenticator() {
        log.info("API key authenticator initialized.");
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        this.isGatewayTokenCacheEnabled = enforcerConfig.getCacheDto().isEnabled();
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            this.jwtGenerator = BackendJwtUtils.getApiMgtGatewayJWTGenerator();
        }
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

    private boolean getIsAPIKeyProtected(RequestContext requestContext) {
        boolean isAPIKeyProtected = false;
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = requestContext.getMatchedAPI()
                .getSecuritySchemeDefinitions();

        ResourceConfig resourceConfig = requestContext.getMatchedResourcePath();
        Map<String, List<String>> resourceSecuritySchemes = resourceConfig.getSecuritySchemas();
        if (resourceSecuritySchemes.containsKey(APIConstants.API_SECURITY_API_KEY) ||
                isResourceSecurityApplicable(securitySchemeDefinitions, resourceSecuritySchemes)) {
            isAPIKeyProtected = true;
        }
        return isAPIKeyProtected;
    }

    private boolean isResourceSecurityApplicable(Map<String, SecuritySchemaConfig> schemeMap,
                                                 Map<String, List<String>> resourceSchemeMap) {
        for (String securityDefinitionName: resourceSchemeMap.keySet()) {
            if (schemeMap.containsKey(securityDefinitionName)) {
                SecuritySchemaConfig config = schemeMap.get(securityDefinitionName);
                if (APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME.equals(config.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Gets API key from request
    private String getAPIKeyFromRequest(RequestContext requestContext) {
        boolean isAppLevelApiKeySecurityEnabled = isAppLevelApiKeySecurityEnabled(requestContext);
        String apiKey = "";
        if (isAppLevelApiKeySecurityEnabled) {
            // If API key enabled at Application level via APIM, key can exist in header or query param
            String apiKeyName = requestContext.getMatchedAPI().
                    getSecuritySchemeDefinitions().get(APIConstants.API_SECURITY_API_KEY).getName();
            if (requestContext.getHeaders().containsKey(apiKeyName)) {
                return requestContext.getHeaders().get(apiKeyName);
            }
            if (requestContext.getQueryParameters().containsKey(apiKeyName)) {
                return requestContext.getQueryParameters().get(apiKeyName);
            }
        } else {
            apiKey = getAPIKey(APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME, requestContext, true);
            if ("".equals(apiKey)) {
                apiKey = getAPIKey(APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME, requestContext, false);
            }
        }
        return apiKey;
    }

    private static String getAPIKey(String securitySchemeType, RequestContext requestContext, boolean isResourceLevel) {
        Iterator<String> securitySchemesToApply;
        if (isResourceLevel) {
            securitySchemesToApply = requestContext.getMatchedResourcePath().getSecuritySchemas().keySet().iterator();
        } else {
            securitySchemesToApply = requestContext.getMatchedAPI().getSecuritySchemas().iterator();
        }

        Map<String, SecuritySchemaConfig> securitySchemaDefinitions = requestContext.getMatchedAPI().
                getSecuritySchemeDefinitions();

        for (Iterator<String> it = securitySchemesToApply; it.hasNext(); ) {
            String securitySchemeName = it.next();
            SecuritySchemaConfig securitySchemaDefinition = securitySchemaDefinitions.get(securitySchemeName);

            // We only need apiKey of the given type
            if (securitySchemaDefinition != null && securitySchemeType.equalsIgnoreCase(
                    securitySchemaDefinition.getType())) {

                // If Defined in openAPI definition (when not enabled at APIM App level),
                // key must exist in specified location
                if (APIConstants.SWAGGER_API_KEY_IN_HEADER.equalsIgnoreCase(
                        securitySchemaDefinition.getIn())) {
                    if (requestContext.getHeaders().containsKey(securitySchemaDefinition.getName())) {
                        return requestContext.getHeaders().get(securitySchemaDefinition.getName());
                    }
                }
                if (APIConstants.SWAGGER_API_KEY_IN_QUERY.equalsIgnoreCase(
                        securitySchemaDefinition.getIn())) {
                    if (requestContext.getQueryParameters().containsKey(securitySchemaDefinition.getName())) {
                        return requestContext.getQueryParameters().get(securitySchemaDefinition.getName());
                    }
                }
            }
        }
        return "";
    }

    private boolean isAppLevelApiKeySecurityEnabled(RequestContext requestContext) {
        // When API-Key security is enabled at app level, all three keys definitionName, type, name
        // in SecuritySchemaConfig are equal to "api_key" and does not have a value for in.
        // When API-Key security is set via definition, the type = "apiKey".
        Map<String, SecuritySchemaConfig> securitySchemaConfigMap = requestContext.getMatchedAPI()
                .getSecuritySchemeDefinitions();
        if (securitySchemaConfigMap.containsKey(APIConstants.API_SECURITY_API_KEY)) {
            return true;
        }
        return false;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        if (requestContext.getMatchedAPI() == null) {
            log.debug("API Key Authentication failed");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR,
                    APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
        }
        try {
            String apiKey = getAPIKeyFromRequest(requestContext);
            String[] splitToken = apiKey.split("\\.");

            SignedJWT signedJWT = SignedJWT.parse(apiKey);
            JWSHeader jwsHeader = signedJWT.getHeader();
            JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

            String apiVersion = requestContext.getMatchedAPI().getVersion();
            String apiContext = requestContext.getMatchedAPI().getBasePath();

            // Avoids using internal API keys, when internal key header or queryParam configured as api_key
            if (isInternalKey(payload)) {
                log.error("Invalid API Key token type. {} ", FilterUtils.getMaskedToken(splitToken[0]));
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
            boolean isVerified = isVerifiedApiKeyInCache(tokenIdentifier, apiKey, payload, splitToken,
                    "API Key", jwtTokenPayloadInfo);

            // Verifies token when it is not found in cache
            if (!isVerified) {
                isVerified = verifyTokenWhenNotInCache(jwsHeader, signedJWT, splitToken, payload, "API Key");
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

                validateAPIKeyRestrictions(payload, requestContext, apiContext, apiVersion);

                validateAPISubscription(apiContext, apiVersion, payload, splitToken, false);

                log.debug("API Key authentication successful.");

                // Get APIKeyValidationInfoDTO
                APIKeyValidationInfoDTO apiKeyValidationInfoDTO = getAPIKeyValidationDTO(requestContext, payload);

                // TODO: Add analytics data processing

                // Get SignedJWTInfo
                SignedJWTInfo signedJWTInfo = JWTUtils.getSignedJwt(apiKey);

                // Get JWTValidationInfo
                JWTValidationInfo validationInfo = new JWTValidationInfo();
                validationInfo.setUser(payload.getSubject());

                // Generate or get backend JWT
                String endUserToken = null;
                JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().
                        getConfig().getJwtConfigurationDto();
                if (jwtConfigurationDto.isEnabled()) {
                    JWTInfoDto jwtInfoDto = FilterUtils
                            .generateJWTInfoDto(null, validationInfo, apiKeyValidationInfoDTO, requestContext);
                    endUserToken = BackendJwtUtils.generateAndRetrieveJWTToken(jwtGenerator, tokenIdentifier,
                            jwtInfoDto, isGatewayTokenCacheEnabled);
                    // Set generated jwt token as a response header
                    requestContext.addOrModifyHeaders(jwtConfigurationDto.getJwtHeader(), endUserToken);
                }

                // Create authentication context
                JWTClaimsSet claims = signedJWTInfo.getJwtClaimsSet();
                AuthenticationContext authenticationContext = FilterUtils
                        .generateAuthenticationContext(requestContext, tokenIdentifier, validationInfo,
                                apiKeyValidationInfoDTO, endUserToken, apiKey, false);
                if (claims.getClaim("keytype") != null) {
                    authenticationContext.setKeyType(claims.getClaim("keytype").toString());
                }
                log.debug("Analytics data processing for API Key (jiti) {} was successful", tokenIdentifier);
                return authenticationContext;

            }
        } catch (ParseException e) {
            log.warn("API Key authentication failed. ", e);
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    "API key authentication failed.");
        }
        log.warn("API Key authentication failed.");
        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                "API key authentication failed.");
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

                    log.debug("APIKeyValidationInfoDTO populated for API: {}, version: {}.", name, version);

                    break;
                }
            }
            if (api == null) {
                log.debug("Subscription data not populated in APIKeyValidationInfoDTO for the API: {}, version: {}.",
                        name, version);
                log.error("User's subscription details cannot obtain for the API : {}", name);
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        APISecurityConstants.API_AUTH_FORBIDDEN,
                        APISecurityConstants.API_AUTH_FORBIDDEN_MESSAGE);
            }
        }

        return validationInfoDTO;
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
                    log.debug("Invocations to API: {}:{} is not permitted for client with IP: {}",
                            apiContext, apiVersion, clientIP);
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
                        log.debug("Invocations to API: {}:{} is not permitted for referer: {}",
                                apiContext, apiVersion, referer);
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
            log.error("Error while parsing host IP {}", ipAddress, e);
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
    public String getName() {
        return "API Key";
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
