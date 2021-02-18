/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.enforcer.util;

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
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTValidationInfo;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.APISecurityConstants;
import org.wso2.micro.gateway.enforcer.constants.JwtConstants;
import org.wso2.micro.gateway.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.micro.gateway.enforcer.exception.APISecurityException;
import org.wso2.micro.gateway.enforcer.exception.MGWException;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.SSLContext;

/**
 * Common set of utility methods used by the filter core component.
 */
public class FilterUtils {

    private static final Logger log = LogManager.getLogger(FilterUtils.class);
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String STRICT = "Strict";
    public static final String ALLOW_ALL = "AllowAll";

    public static String getMaskedToken(String token) {

        if (token.length() >= 10) {
            return "XXXXX" + token.substring(token.length() - 10);
        } else {
            return "XXXXX" + token.substring(token.length() / 2);
        }
    }

    /**
     * Return a http client instance
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
        } catch (MGWException e) {
            log.error("Error while getting http client connection manager", e);
        }
        pool.setMaxTotal(Integer.parseInt(maxTotal));
        pool.setDefaultMaxPerRoute(Integer.parseInt(defaultMaxPerRoute));

        RequestConfig params = RequestConfig.custom().build();
        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    /**
     * Return a PoolingHttpClientConnectionManager instance
     *
     * @param protocol- service endpoint protocol. It can be http/https
     * @return PoolManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws MGWException {

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

    private static SSLConnectionSocketFactory createSocketFactory() throws MGWException {
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

    public static void handleException(String msg, Throwable t) throws MGWException {

        log.error(msg, t);
        throw new MGWException(msg, t);
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

    public static AuthenticationContext generateAuthenticationContext(String jti, JWTValidationInfo jwtValidationInfo,
            APIKeyValidationInfoDTO apiKeyValidationInfoDTO, String endUserToken, boolean isOauth) {

        AuthenticationContext authContext = new AuthenticationContext();
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
            authContext.setApiPublisher(apiKeyValidationInfoDTO.getApiPublisher());
            authContext.setStopOnQuotaReach(apiKeyValidationInfoDTO.isStopOnQuotaReach());
            authContext.setSpikeArrestLimit(apiKeyValidationInfoDTO.getSpikeArrestLimit());
            authContext.setSpikeArrestUnit(apiKeyValidationInfoDTO.getSpikeArrestUnit());
            authContext.setConsumerKey(apiKeyValidationInfoDTO.getConsumerKey());
            authContext.setIsContentAware(apiKeyValidationInfoDTO.isContentAware());
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

    public static JWTInfoDto generateJWTInfoDto(JSONObject subscribedAPI, JWTValidationInfo jwtValidationInfo,
                                                APIKeyValidationInfoDTO apiKeyValidationInfoDTO,
                                                RequestContext requestContext) {

        JWTInfoDto jwtInfoDto = new JWTInfoDto();
        jwtInfoDto.setJwtValidationInfo(jwtValidationInfo);
        String apiContext = requestContext.getMathedAPI().getAPIConfig().getBasePath();
        String apiVersion = requestContext.getMathedAPI().getAPIConfig().getVersion();
        jwtInfoDto.setApicontext(apiContext);
        jwtInfoDto.setVersion(apiVersion);
        constructJWTContent(subscribedAPI, apiKeyValidationInfoDTO, jwtInfoDto);
        return jwtInfoDto;
    }

    private static void constructJWTContent(JSONObject subscribedAPI,
                                            APIKeyValidationInfoDTO apiKeyValidationInfoDTO, JWTInfoDto jwtInfoDto) {

        if (apiKeyValidationInfoDTO != null) {
            jwtInfoDto.setApplicationid(apiKeyValidationInfoDTO.getApplicationId());
            jwtInfoDto.setApplicationname(apiKeyValidationInfoDTO.getApplicationName());
            jwtInfoDto.setApplicationtier(apiKeyValidationInfoDTO.getApplicationTier());
            jwtInfoDto.setKeytype(apiKeyValidationInfoDTO.getType());
            jwtInfoDto.setSubscriber(apiKeyValidationInfoDTO.getSubscriber());
            jwtInfoDto.setSubscriptionTier(apiKeyValidationInfoDTO.getTier());
            jwtInfoDto.setApiName(apiKeyValidationInfoDTO.getApiName());
            jwtInfoDto.setEndusertenantid(0);
            jwtInfoDto.setApplicationuuid(apiKeyValidationInfoDTO.getApplicationUUID());
            jwtInfoDto.setAppAttributes(apiKeyValidationInfoDTO.getAppAttributes());
        } else if (subscribedAPI != null) {
            // If the user is subscribed to the API
            String apiName = subscribedAPI.getAsString(JwtConstants.API_NAME);
            jwtInfoDto.setApiName(apiName);
            String subscriptionTier = subscribedAPI.getAsString(JwtConstants.SUBSCRIPTION_TIER);
            String subscriptionTenantDomain =
                    subscribedAPI.getAsString(JwtConstants.SUBSCRIBER_TENANT_DOMAIN);
            jwtInfoDto.setSubscriptionTier(subscriptionTier);
            jwtInfoDto.setEndusertenantid(0);

            Map<String, Object> claims = jwtInfoDto.getJwtValidationInfo().getClaims();
            if (claims.get(JwtConstants.APPLICATION) != null) {
                JSONObject
                        applicationObj = (JSONObject) claims.get(JwtConstants.APPLICATION);
                jwtInfoDto.setApplicationid(
                        String.valueOf(applicationObj.getAsNumber(JwtConstants.APPLICATION_ID)));
                jwtInfoDto
                        .setApplicationname(applicationObj.getAsString(JwtConstants.APPLICATION_NAME));
                jwtInfoDto
                        .setApplicationtier(applicationObj.getAsString(JwtConstants.APPLICATION_TIER));
                jwtInfoDto.setSubscriber(applicationObj.getAsString(JwtConstants.APPLICATION_OWNER));
            }
        }
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

}
