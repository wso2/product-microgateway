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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.constants.GraphQLConstants;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
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
import org.wso2.choreo.connect.enforcer.dto.JWTTokenPayloadInfo;
import org.wso2.choreo.connect.enforcer.security.KeyValidator;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Map;

/**
 * Extends the APIKeyHandler to authenticate request using API Key.
 */
public class APIKeyAuthenticator extends APIKeyHandler {

    private static final Logger log = LogManager.getLogger(APIKeyAuthenticator.class);

    private static String certAlias;
    private static boolean apiKeySubValidationEnabled;
    private AbstractAPIMgtGatewayJWTGenerator jwtGenerator;
    private final boolean isGatewayTokenCacheEnabled;
    private static final int IPV4_ADDRESS_BIT_LENGTH = 32;
    private static final int IPV6_ADDRESS_BIT_LENGTH = 128;

    public APIKeyAuthenticator() {
        log.debug("API key authenticator initialized.");
        EnforcerConfig enforcerConfig = ConfigHolder.getInstance().getConfig();
        this.isGatewayTokenCacheEnabled = enforcerConfig.getCacheDto().isEnabled();
        if (enforcerConfig.getJwtConfigurationDto().isEnabled()) {
            this.jwtGenerator = BackendJwtUtils.getApiMgtGatewayJWTGenerator();
        }
        for (ExtendedTokenIssuerDto tokenIssuer : enforcerConfig.getIssuersMap().values()) {
            if (APIConstants.KeyManager.APIM_APIKEY_ISSUER.equals(tokenIssuer.getName())) {
                certAlias = tokenIssuer.getCertificateAlias();
                apiKeySubValidationEnabled = tokenIssuer.isValidateSubscriptions();
                break;
            }
        }

        // For backward compatibility
        if (StringUtils.isBlank(certAlias)) {
            for (ExtendedTokenIssuerDto tokenIssuer : enforcerConfig.getIssuersMap().values()) {
                if (APIConstants.KeyManager.APIM_PUBLISHER_ISSUER.equals(tokenIssuer.getName())) {
                    certAlias = tokenIssuer.getCertificateAlias();
                    apiKeySubValidationEnabled = tokenIssuer.isValidateSubscriptions();
                    break;
                }
            }
        }
        if (StringUtils.isBlank(certAlias)) {
            log.error("Could not properly initialize APIKeyAuthenticator. Empty certificate alias. {}",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 6604));
        }
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        // only getting first operation is enough as all matched resource configs have the same security schemes
        // i.e. graphQL apis do not support resource level security yet
        return isAPIKey(getAPIKeyFromRequest(requestContext, requestContext.getMatchedResourcePaths().get(0)));
    }

    // Gets API key from request
    private static String getAPIKeyFromRequest(RequestContext requestContext, ResourceConfig resourceConfig) {
        Map<String, SecuritySchemaConfig> securitySchemaDefinitions = requestContext.getMatchedAPI().
                getSecuritySchemeDefinitions();
        // loop over resource security and get definition for the matching security definition name
        for (String securityDefinitionName : resourceConfig.getSecuritySchemas().keySet()) {
            if (securitySchemaDefinitions.containsKey(securityDefinitionName)) {
                SecuritySchemaConfig securitySchemaDefinition =
                        securitySchemaDefinitions.get(securityDefinitionName);
                if (APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME.equalsIgnoreCase(
                        securitySchemaDefinition.getType())) {
                    // If Defined in openAPI definition (when not enabled at APIM App level),
                    // key must exist in specified location
                    if (APIConstants.SWAGGER_API_KEY_IN_HEADER.equalsIgnoreCase(securitySchemaDefinition.getIn())) {
                        if (requestContext.getHeaders().containsKey(securitySchemaDefinition.getName())) {
                            return requestContext.getHeaders().get(securitySchemaDefinition.getName());
                        }
                    }
                    if (APIConstants.SWAGGER_API_KEY_IN_QUERY.equalsIgnoreCase(securitySchemaDefinition.getIn())) {
                        if (requestContext.getQueryParameters().containsKey(securitySchemaDefinition.getName())) {
                            return requestContext.getQueryParameters().get(securitySchemaDefinition.getName());
                        }
                    }
                }
            }
        }
        return "";
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        if (StringUtils.isBlank(certAlias)) {
            log.error("APIKeyAuthenticator has not been properly initialized. Empty certificate alias.",
                    ErrorDetails.errorLog(LoggingConstants.Severity.CRITICAL, 6604));
            throw new APISecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR,
                    APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
        }
        if (requestContext.getMatchedAPI() == null) {
            log.debug("API Key Authentication failed");
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR,
                    APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
        }
        String apiKey = getAPIKeyFromRequest(requestContext, requestContext.getMatchedResourcePaths().get(0));
        return processAPIKey(requestContext, apiKey);
    }

    private AuthenticationContext processAPIKey(RequestContext requestContext, String apiKey)
            throws APISecurityException {
        try {
            String[] splitToken = apiKey.split("\\.");

            SignedJWT signedJWT = SignedJWT.parse(apiKey);
            JWTClaimsSet payload = signedJWT.getJWTClaimsSet();

            String apiVersion = requestContext.getMatchedAPI().getVersion();
            String apiContext = requestContext.getMatchedAPI().getBasePath();
            String apiUuid = requestContext.getMatchedAPI().getUuid();

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
                isVerified = verifyTokenWhenNotInCache(certAlias, signedJWT, splitToken, payload, "API Key");
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
                APIKeyValidationInfoDTO validationInfoDto;
                if (ConfigHolder.getInstance().isControlPlaneEnabled()) {
                    log.debug("Validating subscription for API Key against subscription store."
                            + " context: {} version: {}", apiContext, apiVersion);
                    validationInfoDto = KeyValidator.validateSubscription(apiUuid, apiContext, payload);
                } else if (apiKeySubValidationEnabled) {
                    log.debug("Validating subscription for API Key using JWT claims against invoked API info."
                            + " context: {} version: {}", apiContext, apiVersion);
                    validationInfoDto = getAPIKeyValidationDTO(requestContext, payload);
                } else {
                    log.debug("Creating API Key info DTO for unknown API and Application."
                            + " context: {} version: {}", apiContext, apiVersion);
                    validationInfoDto = new APIKeyValidationInfoDTO();
                    JWTUtils.updateApplicationNameForSubscriptionDisabledKM(validationInfoDto,
                            APIConstants.KeyManager.APIM_APIKEY_ISSUER);
                    validationInfoDto.setAuthorized(true);
                }

                if (!validationInfoDto.isAuthorized()) {
                    if (GeneralErrorCodeConstants.API_BLOCKED_CODE == validationInfoDto
                            .getValidationStatus()) {
                        FilterUtils.setErrorToContext(requestContext,
                                GeneralErrorCodeConstants.API_BLOCKED_CODE,
                                APIConstants.StatusCodes.SERVICE_UNAVAILABLE.getCode(),
                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE,
                                GeneralErrorCodeConstants.API_BLOCKED_DESCRIPTION);
                        throw new APISecurityException(APIConstants.StatusCodes.SERVICE_UNAVAILABLE
                                .getCode(), validationInfoDto.getValidationStatus(),
                                GeneralErrorCodeConstants.API_BLOCKED_MESSAGE);
                    } else if (APISecurityConstants.API_SUBSCRIPTION_BLOCKED == validationInfoDto
                            .getValidationStatus()) {
                        FilterUtils.setErrorToContext(requestContext,
                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED,
                                APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_MESSAGE,
                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_DESCRIPTION);
                        throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED
                                .getCode(), validationInfoDto.getValidationStatus(),
                                APISecurityConstants.API_SUBSCRIPTION_BLOCKED_MESSAGE);
                    }
                    throw new APISecurityException(APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                            validationInfoDto.getValidationStatus(),
                            "User is NOT authorized to access the Resource. "
                                    + "API Subscription validation failed.");
                }

                log.debug("API Key authentication successful.");

                /* GraphQL Query Analysis Information */
                if (APIConstants.ApiType.GRAPHQL.equals(requestContext.getMatchedAPI()
                        .getApiType())) {
                    requestContext.getProperties().put(GraphQLConstants.MAXIMUM_QUERY_DEPTH,
                            validationInfoDto.getGraphQLMaxDepth());
                    requestContext.getProperties().put(GraphQLConstants.MAXIMUM_QUERY_COMPLEXITY,
                            validationInfoDto.getGraphQLMaxComplexity());
                }

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
                            .generateJWTInfoDto(null, validationInfo, validationInfoDto, requestContext);
                    endUserToken = BackendJwtUtils.generateAndRetrieveJWTToken(jwtGenerator, tokenIdentifier,
                            jwtInfoDto, isGatewayTokenCacheEnabled);
                    // Set generated jwt token as a response header
                    requestContext.addOrModifyHeaders(jwtConfigurationDto.getJwtHeader(), endUserToken);
                }

                // Create authentication context
                JWTClaimsSet claims = signedJWTInfo.getJwtClaimsSet();
                AuthenticationContext authenticationContext = FilterUtils
                        .generateAuthenticationContext(requestContext, tokenIdentifier, validationInfo,
                                validationInfoDto, endUserToken, apiKey, false);
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
        JSONObject api = null;

        if (payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE) != null) {
            validationInfoDTO.setType(payload.getStringClaim(APIConstants.JwtTokenConstants.KEY_TYPE));
        } else {
            validationInfoDTO.setType(APIConstants.API_KEY_TYPE_PRODUCTION);
        }
        if (app != null) {
            validationInfoDTO.setApplicationId(app.getAsNumber(APIConstants.JwtTokenConstants.APPLICATION_ID)
                    .intValue());
            validationInfoDTO.setApplicationUUID(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_UUID));
            validationInfoDTO.setApplicationName(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_NAME));
            validationInfoDTO.setApplicationTier(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_TIER));
            validationInfoDTO.setSubscriber(app.getAsString(APIConstants.JwtTokenConstants.APPLICATION_OWNER));
        }

        //check whether name is assigned correctly (This was not populated in JWTAuthenticator)
        String name = requestContext.getMatchedAPI().getName();
        String version = requestContext.getMatchedAPI().getVersion();
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
                        AuthenticatorUtils.populateTierInfo(validationInfoDTO, payload, subTier);
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
