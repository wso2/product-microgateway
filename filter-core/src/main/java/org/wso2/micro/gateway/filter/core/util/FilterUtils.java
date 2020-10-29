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

package org.wso2.micro.gateway.filter.core.util;

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
import org.wso2.micro.gateway.filter.core.dto.APIKeyValidationInfoDTO;
import org.wso2.micro.gateway.filter.core.security.AuthenticationContext;
import org.wso2.micro.gateway.filter.core.security.jwt.JWTValidationInfo;
import org.wso2.micro.gateway.filter.core.common.ReferenceHolder;
import org.wso2.micro.gateway.filter.core.constants.APIConstants;
import org.wso2.micro.gateway.filter.core.exception.MGWException;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

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

    public static Certificate getCertificateFromTrustStore(String certAlias) throws MGWException {

        Certificate publicCert = null;
        //Read the client-truststore.jks into a KeyStore
        try {
            KeyStore trustStore = ReferenceHolder.getInstance().getMGWConfiguration().getTrustStore();
            if (trustStore != null) {
                // Read public certificate from trust store
                publicCert = trustStore.getCertificate(certAlias);
            }
        } catch (KeyStoreException e) {
            String msg = "Error in retrieving public certificate from the trust store with alias : " + certAlias;
            log.error(msg, e);
            throw new MGWException(msg, e);
        }
        return publicCert;
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

//        String keyStorePath = "/home/ubuntu/security"; ///TODO : Read from config
        String keyStorePath = "/Users/menakajayawardena/WSO2/git/microgateway/product-microgateway/filter-core/src/main/resources/client-truststore.jks"; ///TODO : Read from config
        String keyStorePassword = "wso2carbon"; //TODO : Read from config
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(keyStorePath), keyStorePassword.toCharArray());
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
        } catch (IOException e) {
            handleException("Key Store not found in " + keyStorePath, e);
        } catch (CertificateException e) {
            handleException("Failed to read Certificate", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Failed to load Key Store from " + keyStorePath, e);
        } catch (KeyManagementException e) {
            handleException("Failed to load key from" + keyStorePath, e);
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

}
