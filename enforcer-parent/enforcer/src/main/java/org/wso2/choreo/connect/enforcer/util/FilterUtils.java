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

package org.wso2.choreo.connect.enforcer.util;

import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.constants.JWTConstants;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.SecuritySchemaConfig;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.constants.JwtConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleConstants;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLContext;

/**
 * Common set of utility methods used by the filter core component.
 */
public class FilterUtils {

    private static final Logger log = LogManager.getLogger(FilterUtils.class);
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String STRICT = "Strict";
    public static final String ALLOW_ALL = "AllowAll";
    public static final List<String> SKIPPED_FAULT_CODES = new ArrayList<>(Arrays.asList("700700"));

    public static String getMaskedToken(String token) {

        if (token.length() >= 10) {
            return "XXXXX" + token.substring(token.length() - 10);
        } else {
            return "XXXXX" + token.substring(token.length() / 2);
        }
    }

    /**
     * Return a http client instance.
     *
     * @param protocol - service endpoint protocol http/https
     * @return
     */
    public static HttpClient getHttpClient(String protocol) {

        //        APIManagerConfiguration configuration = ServiceReferenceHolder.getInstance().
        //                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        String maxTotal = "100"; //TODO : Read from config
        String defaultMaxPerRoute = "10"; //TODO : Read from config

        PoolingHttpClientConnectionManager pool = null;
        try {
            pool = getPoolingHttpClientConnectionManager(protocol);
        } catch (EnforcerException e) {
            log.error("Error while getting http client connection manager", e);
        }
        pool.setMaxTotal(Integer.parseInt(maxTotal));
        pool.setDefaultMaxPerRoute(Integer.parseInt(defaultMaxPerRoute));

        RequestConfig params = RequestConfig.custom().build();
        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    /**
     * Return a PoolingHttpClientConnectionManager instance.
     *
     * @param protocol- service endpoint protocol. It can be http/https
     * @return PoolManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws EnforcerException {

        PoolingHttpClientConnectionManager poolManager;
        if (APIConstants.HTTPS_PROTOCOL.equals(protocol)) {
            SSLConnectionSocketFactory socketFactory = createSocketFactory();
            org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                    .register(APIConstants.HTTPS_PROTOCOL, socketFactory).build();
            poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            poolManager = new PoolingHttpClientConnectionManager();
        }
        return poolManager;
    }

    private static SSLConnectionSocketFactory createSocketFactory() throws EnforcerException {
        SSLContext sslContext;
        try {
            KeyStore trustStore = ConfigHolder.getInstance().getTrustStore();
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();

            X509HostnameVerifier hostnameVerifier;
            String hostnameVerifierOption = System.getProperty(HOST_NAME_VERIFIER);

            if (ALLOW_ALL.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            } else if (STRICT.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
            } else {
                hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
            }

            return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (KeyStoreException e) {
            handleException("Failed to read from Key Store", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Failed to initialize sslContext. ", e);
        } catch (KeyManagementException e) {
            handleException("Failed to initialize sslContext ", e);
        }

        return null;
    }

    public static void handleException(String msg, Throwable t) throws EnforcerException {

        log.error(msg, t);
        throw new EnforcerException(msg, t);
    }

    public static String getTenantDomainFromRequestURL(String requestURI) {
        String domain = null;
        if (requestURI.contains("/t/")) {
            int index = requestURI.indexOf("/t/");
            int endIndex = requestURI.indexOf("/", index + 3);
            domain = endIndex != -1 ? requestURI.substring(index + 3, endIndex) : requestURI.substring(index + 3);
        }

        return domain;
    }

    public static AuthenticationContext generateAuthenticationContext(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        String clientIP = requestContext.getClientIp();

        // Create a dummy AuthenticationContext object with hard coded values for
        // Tier and KeyType. This is because we cannot determine the Tier nor Key
        // Type without subscription information.
        authContext.setAuthenticated(true);
        authContext.setTier(APIConstants.UNAUTHENTICATED_TIER);
        authContext.setApiKey(clientIP);
        authContext.setKeyType(APIConstants.API_KEY_TYPE_PRODUCTION);
        // Setting end user as anonymous
        authContext.setUsername(APIConstants.END_USER_ANONYMOUS);
        authContext.setApiTier(getAPILevelTier(requestContext));
        authContext.setApplicationId(clientIP);
        authContext.setApplicationName(null);
        authContext.setApplicationTier(APIConstants.UNLIMITED_TIER);
        authContext.setSubscriber(APIConstants.END_USER_ANONYMOUS);
        authContext.setApiName(requestContext.getMatchedAPI().getName());
        authContext.setStopOnQuotaReach(true);
        authContext.setConsumerKey(null);
        authContext.setCallerToken(null);
        String apiUUID = requestContext.getMatchedAPI().getUuid();
        if (!StringUtils.isEmpty(apiUUID)) {
            authContext.setApiUUID(apiUUID);
        }
        return authContext;
    }

    public static AuthenticationContext generateAuthenticationContext(RequestContext requestContext, String jti,
                                                                      JWTValidationInfo jwtValidationInfo,
                                                                      APIKeyValidationInfoDTO apiKeyValidationInfoDTO,
                                                                      String endUserToken, boolean isOauth) {

        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        authContext.setAuthenticated(true);
        authContext.setApiKey(jti);
        authContext.setUsername(jwtValidationInfo.getUser());

        if (apiKeyValidationInfoDTO != null) {
            authContext.setApiTier(apiKeyValidationInfoDTO.getApiTier());
            authContext.setKeyType(apiKeyValidationInfoDTO.getType());
            authContext.setApplicationId(apiKeyValidationInfoDTO.getApplicationId());
            authContext.setApplicationName(apiKeyValidationInfoDTO.getApplicationName());
            authContext.setApplicationTier(apiKeyValidationInfoDTO.getApplicationTier());
            authContext.setSubscriber(apiKeyValidationInfoDTO.getSubscriber());
            authContext.setTier(apiKeyValidationInfoDTO.getTier());
            authContext.setSubscriberTenantDomain(apiKeyValidationInfoDTO.getSubscriberTenantDomain());
            authContext.setApiName(apiKeyValidationInfoDTO.getApiName());
            authContext.setApiVersion(apiKeyValidationInfoDTO.getApiVersion());
            authContext.setApiPublisher(apiKeyValidationInfoDTO.getApiPublisher());
            authContext.setStopOnQuotaReach(apiKeyValidationInfoDTO.isStopOnQuotaReach());
            authContext.setSpikeArrestLimit(apiKeyValidationInfoDTO.getSpikeArrestLimit());
            authContext.setSpikeArrestUnit(apiKeyValidationInfoDTO.getSpikeArrestUnit());
            authContext.setConsumerKey(apiKeyValidationInfoDTO.getConsumerKey());
            authContext.setIsContentAware(apiKeyValidationInfoDTO.isContentAware());
            authContext.setApiUUID(apiKeyValidationInfoDTO.getApiUUID());
        }
        if (isOauth) {
            authContext.setConsumerKey(jwtValidationInfo.getConsumerKey());
        }
        // Set JWT token sent to the backend
        if (StringUtils.isNotEmpty(endUserToken)) {
            authContext.setCallerToken(endUserToken);
        }

        return authContext;
    }

    public static long ipToLong(String ipAddress) {

        long result = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            //left shifting 24,16,8,0 and bitwise OR
            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);

        }
        return result;
    }

    /**
     * This method provides the BigInteger value for the given IP address. This supports both IPv4 and IPv6 address
     *
     * @param ipAddress ip address
     * @return BigInteger value for the given ip address. returns 0 for unknown host
     */
    public static BigInteger ipToBigInteger(String ipAddress) {

        InetAddress address;
        try {
            address = InetAddress.getByName(ipAddress);
            byte[] bytes = address.getAddress();
            return new BigInteger(1, bytes);
        } catch (UnknownHostException e) {
            //ignore the error and log it
            log.error("Error while parsing host IP " + ipAddress, e);
        }
        return BigInteger.ZERO;
    }

    /**
     * Generates Authentication Context for the Internal Key Authenticator.
     * @param tokenIdentifier
     * @param payload
     * @param api
     * @param apiLevelPolicy
     * @return
     * @throws java.text.ParseException
     */
    public static AuthenticationContext generateAuthenticationContext(String tokenIdentifier, JWTClaimsSet payload,
                                                                      JSONObject api, String apiLevelPolicy,
                                                                      String apiUUID)
            throws java.text.ParseException {

        AuthenticationContext authContext = new AuthenticationContext();
        authContext.setAuthenticated(true);
        authContext.setApiKey(tokenIdentifier);
        authContext.setUsername(payload.getSubject());
        if (payload.getClaim(APIConstants.JwtTokenConstants.KEY_TYPE) != null) {
            authContext.setKeyType(payload.getStringClaim(APIConstants.JwtTokenConstants.KEY_TYPE));
        } else {
            authContext.setKeyType(APIConstants.API_KEY_TYPE_PRODUCTION);
        }
        authContext.setApiTier(apiLevelPolicy);
        if (api != null) {
            authContext.setTier(APIConstants.UNLIMITED_TIER);
            authContext.setApiName(api.getAsString(APIConstants.JwtTokenConstants.API_NAME));
            authContext.setApiPublisher(api.getAsString(APIConstants.JwtTokenConstants.API_PUBLISHER));

        }
        if (!StringUtils.isEmpty(apiUUID)) {
            authContext.setApiUUID(apiUUID);
        }
        authContext.setApplicationName(APIConstants.JwtTokenConstants.INTERNAL_KEY_APP_NAME);
        authContext.setApplicationId(UUID.nameUUIDFromBytes(APIConstants.JwtTokenConstants.INTERNAL_KEY_APP_NAME.
                getBytes(StandardCharsets.UTF_8)).toString());
        authContext.setApplicationTier(APIConstants.UNLIMITED_TIER);
        authContext.setSubscriber(APIConstants.JwtTokenConstants.INTERNAL_KEY_APP_NAME);
        return authContext;
    }

    public static JWTInfoDto generateJWTInfoDto(JSONObject subscribedAPI, JWTValidationInfo jwtValidationInfo,
                                                APIKeyValidationInfoDTO apiKeyValidationInfoDTO,
                                                RequestContext requestContext) {

        JWTInfoDto jwtInfoDto = new JWTInfoDto();
        jwtInfoDto.setJwtValidationInfo(jwtValidationInfo);
        String apiContext = requestContext.getMatchedAPI().getBasePath();
        String apiVersion = requestContext.getMatchedAPI().getVersion();
        jwtInfoDto.setApiContext(apiContext);
        jwtInfoDto.setVersion(apiVersion);
        constructJWTContent(subscribedAPI, apiKeyValidationInfoDTO, jwtInfoDto);
        return jwtInfoDto;
    }

    private static void constructJWTContent(JSONObject subscribedAPI,
                                            APIKeyValidationInfoDTO apiKeyValidationInfoDTO, JWTInfoDto jwtInfoDto) {

        Map<String, Object> claims = getClaimsFromJWTValidationInfo(jwtInfoDto);
        if (claims != null) {
            if (claims.get(JWTConstants.SUB) != null) {
                String sub = (String) claims.get(JWTConstants.SUB);
                jwtInfoDto.setSub(sub);
            }
            if (claims.get(JWTConstants.ORGANIZATIONS) != null) {
                String[] organizations = (String[]) claims.get(JWTConstants.ORGANIZATIONS);
                jwtInfoDto.setOrganizations(organizations);
            }
        }
        if (apiKeyValidationInfoDTO != null) {
            jwtInfoDto.setApplicationId(apiKeyValidationInfoDTO.getApplicationId());
            jwtInfoDto.setApplicationName(apiKeyValidationInfoDTO.getApplicationName());
            jwtInfoDto.setApplicationTier(apiKeyValidationInfoDTO.getApplicationTier());
            jwtInfoDto.setKeyType(apiKeyValidationInfoDTO.getType());
            jwtInfoDto.setSubscriber(apiKeyValidationInfoDTO.getSubscriber());
            jwtInfoDto.setSubscriptionTier(apiKeyValidationInfoDTO.getTier());
            jwtInfoDto.setApiName(apiKeyValidationInfoDTO.getApiName());
            jwtInfoDto.setEndUserTenantId(0);
            jwtInfoDto.setApplicationUUId(apiKeyValidationInfoDTO.getApplicationUUID());
            jwtInfoDto.setAppAttributes(apiKeyValidationInfoDTO.getAppAttributes());
        } else if (subscribedAPI != null) {
            // If the user is subscribed to the API
            String apiName = subscribedAPI.getAsString(JwtConstants.API_NAME);
            jwtInfoDto.setApiName(apiName);
            String subscriptionTier = subscribedAPI.getAsString(JwtConstants.SUBSCRIPTION_TIER);
            String subscriptionTenantDomain =
                    subscribedAPI.getAsString(JwtConstants.SUBSCRIBER_TENANT_DOMAIN);
            jwtInfoDto.setSubscriptionTier(subscriptionTier);
            jwtInfoDto.setEndUserTenantId(0);

            if (claims != null && claims.get(JwtConstants.APPLICATION) != null) {
                JSONObject
                        applicationObj = (JSONObject) claims.get(JwtConstants.APPLICATION);
                jwtInfoDto.setApplicationId(
                        String.valueOf(applicationObj.getAsNumber(JwtConstants.APPLICATION_ID)));
                jwtInfoDto
                        .setApplicationName(applicationObj.getAsString(JwtConstants.APPLICATION_NAME));
                jwtInfoDto
                        .setApplicationTier(applicationObj.getAsString(JwtConstants.APPLICATION_TIER));
                jwtInfoDto.setSubscriber(applicationObj.getAsString(JwtConstants.APPLICATION_OWNER));
            }
        }
    }

    private static Map<String, Object> getClaimsFromJWTValidationInfo(JWTInfoDto jwtInfoDto) {

        if (jwtInfoDto.getJwtValidationInfo() != null) {
            return jwtInfoDto.getJwtValidationInfo().getClaims();
        }
        return null;
    }

    /**
     * Set the error code, message and description to the request context. The enforcer response will
     * retrieve this error details from the request context. Make sure to call this method and set the proper error
     * details when enforcer filters returns an error.
     *
     * @param requestContext - The context object holds details about the specific request.
     * @param e - APISecurityException thrown when validation failure happens at filter level.
     */
    public static void setErrorToContext(RequestContext requestContext, APISecurityException e) {
        Map<String, Object> requestContextProperties = requestContext.getProperties();
        if (!requestContextProperties.containsKey(APIConstants.MessageFormat.STATUS_CODE)) {
            requestContext.getProperties().put(APIConstants.MessageFormat.STATUS_CODE, e.getStatusCode());
        }
        if (!requestContextProperties.containsKey(APIConstants.MessageFormat.ERROR_CODE)) {
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_CODE, e.getErrorCode());
        }
        if (!requestContextProperties.containsKey(APIConstants.MessageFormat.ERROR_MESSAGE)) {
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE,
                    APISecurityConstants.getAuthenticationFailureMessage(e.getErrorCode()));
        }
        if (!requestContextProperties.containsKey(APIConstants.MessageFormat.ERROR_DESCRIPTION)) {
            requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                    APISecurityConstants.getFailureMessageDetailDescription(e.getErrorCode(), e.getMessage()));
        }
    }

    /**
     * Set the unauthenticated status code(401), error code(900901), message and description to the request context.
     * The enforcer response will retrieve this error details from the request context. Make sure to call
     * this method and set the proper error details when enforcer filters returns an error.
     *
     * @param requestContext - The context object holds details about the specific request.
     */
    public static void setUnauthenticatedErrorToContext(RequestContext requestContext) {
        requestContext.getProperties()
                .put(APIConstants.MessageFormat.STATUS_CODE, APIConstants.StatusCodes.UNAUTHENTICATED.getCode());
        requestContext.getProperties()
                .put(APIConstants.MessageFormat.ERROR_CODE, APISecurityConstants.API_AUTH_INVALID_CREDENTIALS);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE, APISecurityConstants
                .getAuthenticationFailureMessage(APISecurityConstants.API_AUTH_INVALID_CREDENTIALS));
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_DESCRIPTION);
    }

    /**
     * Set the throttle error related details to the {@code RequestContext}.
     *
     * @param context   request context object to set the details.
     * @param errorCode internal wso2 throttle error code.
     * @param msg       wso2 throttle error message.
     * @param desc      description of throttle decision.
     */
    public static void setThrottleErrorToContext(RequestContext context, int errorCode, String msg, String desc) {
        context.getProperties().put(APIConstants.MessageFormat.ERROR_CODE, errorCode);
        if (ThrottleConstants.BLOCKED_ERROR_CODE == errorCode) {
            context.getProperties().put(APIConstants.MessageFormat.STATUS_CODE,
                    APIConstants.StatusCodes.UNAUTHORIZED.getCode());
        } else {
            context.getProperties().put(APIConstants.MessageFormat.STATUS_CODE,
                    APIConstants.StatusCodes.THROTTLED.getCode());
        }
        context.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE, msg);
        context.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION, desc);
    }

    /**
     * Generates a map out of the {@code list} provided. Key will be the {@code toString}
     * value of the list item. Value will be the list item.
     *
     * @param list list to be converted in to a map
     * @param <T>  List Item type
     * @return A map of type {@code <String, T>}
     */
    public static <T> Map<String, T> generateMap(Collection<T> list) {
        if (list == null) {
            return new HashMap<>();
        }
        Map<String, T> map = new HashMap<String, T>();

        for (T el : list) {
            map.put(el.toString(), el);
        }
        return map;
    }

    /**
     * Append the username with tenant domain if not appended already.
     * @param username username
     * @param tenantDomain tenant domain
     * @return tenant domain appended username
     */
    public static String buildUsernameWithTenant(String username, String tenantDomain) {
        if (StringUtils.isEmpty(tenantDomain)) {
            tenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }

        // Check if the tenant domain is appended with userName and append if it is not there
        if (!StringUtils.contains(username, tenantDomain)) {
            return username + '@' + tenantDomain;
        }
        return username;
    }

    /**
     * Get the API related throttling tier for auth context.
     * If API level present, it will be returned. If not resource level
     * would be returned. If both not present, UNLIMITED tier would be returned.
     * @param requestContext Request context
     * @return string format API tier.
     */
    public static String getAPILevelTier(RequestContext requestContext) {
        String apiTier = requestContext.getMatchedAPI().getTier();
        String resourceTier = requestContext.getMatchedResourcePath().getTier();

        if (!apiTier.isEmpty() && !ThrottleConstants.UNLIMITED_TIER.equalsIgnoreCase(apiTier)) {
            return apiTier;
        }
        if (!resourceTier.isBlank()) {
            return resourceTier;
        }
        return ThrottleConstants.UNLIMITED_TIER;
    }

    public static String getClientIp(Map<String, String> headers, String knownIp) {
        String clientIp = knownIp;
        String xForwardFor = headers.get(APIConstants.X_FORWARDED_FOR);
        if (!StringUtils.isEmpty(xForwardFor)) {
            clientIp = xForwardFor;
            int idx = xForwardFor.indexOf(',');
            if (idx > -1) {
                clientIp = clientIp.substring(0, idx);
            }
        }

        return clientIp;
    }

    public static String getAuthHeaderName(RequestContext requestContext) {
        AuthHeaderDto authHeader = ConfigHolder.getInstance().getConfig().getAuthHeader();
        String authHeaderName = requestContext.getMatchedAPI().getAuthHeader();
        if (StringUtils.isEmpty(authHeaderName)) {
            authHeaderName = authHeader.getAuthorizationHeader();
        }
        if (StringUtils.isEmpty(authHeaderName)) {
            authHeaderName = APIConstants.AUTHORIZATION_HEADER_DEFAULT;
        }
        return authHeaderName.toLowerCase();
    }

    /**
     * Gives the name used to define API key.
     * @param requestContext Request context instance
     * @return String to identify API key
     */
    public static String getAPIKeyName(RequestContext requestContext, boolean isAppLevelAPIKey) {
        SecuritySchemaConfig apiKeySecurityScheme = getAPIKeySchemeConfig(requestContext, isAppLevelAPIKey);
        String apiKeyNameInDefinition = "";
        if (apiKeySecurityScheme != null) {
            apiKeyNameInDefinition = apiKeySecurityScheme.getName();
        }
        return apiKeyNameInDefinition.toLowerCase();
    }

    /**
     * Gives security scheme config relevant to API key.
     * @param requestContext Request context instance
     * @param isAppLevelAPIKey boolean value to denote request is for api_key (Application level API key request)
     * @return Instance relevant to the security scheme config
     */
    public static SecuritySchemaConfig getAPIKeySchemeConfig(RequestContext requestContext, boolean isAppLevelAPIKey) {
        Map<String, SecuritySchemaConfig> securitySchemeDefinitions = requestContext.getMatchedAPI().
                getSecuritySchemeDefinitions();
        if (isAppLevelAPIKey) {
            return securitySchemeDefinitions.get(APIConstants.API_SECURITY_API_KEY);
        }
        return  securitySchemeDefinitions.get(APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME);
    }

    /**
     * Gives arbitrary name used to define API key.
     * @param securitySchemeDefinitions Map of security scheme definitions
     * @return Arbitrary name used to define API key security scheme
     */
    public static String getAPIKeyArbitraryName(Map<String, SecuritySchemaConfig> securitySchemeDefinitions) {
        String apiKeyArbitraryName = "";
        SecuritySchemaConfig apiKeySecurityScheme = securitySchemeDefinitions.
                get(APIConstants.SWAGGER_API_KEY_AUTH_TYPE_NAME);
        if (apiKeySecurityScheme != null) {
            return apiKeySecurityScheme.getDefinitionName();
        }
        return apiKeyArbitraryName;
    }

    /**
     * Check whether the fault event is a one that should be published to analytics server.
     * @param errorCode The error code returned during the filter process
     * @return whether the fault scenario should be skipped from publishing to analytics server.
     */
    public static boolean isSkippedAnalyticsFaultEvent(String errorCode) {
        return SKIPPED_FAULT_CODES.contains(errorCode);
    }
}
