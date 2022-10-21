/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.config;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.dto.ClaimMappingDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWKSConfigurationDTO;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.choreo.connect.discovery.config.enforcer.Analytics;
import org.wso2.choreo.connect.discovery.config.enforcer.AuthHeader;
import org.wso2.choreo.connect.discovery.config.enforcer.BinaryPublisher;
import org.wso2.choreo.connect.discovery.config.enforcer.Cache;
import org.wso2.choreo.connect.discovery.config.enforcer.ClaimMapping;
import org.wso2.choreo.connect.discovery.config.enforcer.Config;
import org.wso2.choreo.connect.discovery.config.enforcer.Filter;
import org.wso2.choreo.connect.discovery.config.enforcer.Issuer;
import org.wso2.choreo.connect.discovery.config.enforcer.JWTGenerator;
import org.wso2.choreo.connect.discovery.config.enforcer.JWTIssuer;
import org.wso2.choreo.connect.discovery.config.enforcer.Management;
import org.wso2.choreo.connect.discovery.config.enforcer.Metrics;
import org.wso2.choreo.connect.discovery.config.enforcer.MutualSSL;
import org.wso2.choreo.connect.discovery.config.enforcer.PublisherPool;
import org.wso2.choreo.connect.discovery.config.enforcer.RestServer;
import org.wso2.choreo.connect.discovery.config.enforcer.Service;
import org.wso2.choreo.connect.discovery.config.enforcer.Soap;
import org.wso2.choreo.connect.discovery.config.enforcer.TMURLGroup;
import org.wso2.choreo.connect.discovery.config.enforcer.ThrottleAgent;
import org.wso2.choreo.connect.discovery.config.enforcer.Throttling;
import org.wso2.choreo.connect.discovery.config.enforcer.Tracing;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.config.dto.AdminRestServerDto;
import org.wso2.choreo.connect.enforcer.config.dto.AnalyticsDTO;
import org.wso2.choreo.connect.enforcer.config.dto.AnalyticsReceiverConfigDTO;
import org.wso2.choreo.connect.enforcer.config.dto.AuthHeaderDto;
import org.wso2.choreo.connect.enforcer.config.dto.AuthServiceConfigurationDto;
import org.wso2.choreo.connect.enforcer.config.dto.CacheDto;
import org.wso2.choreo.connect.enforcer.config.dto.CredentialDto;
import org.wso2.choreo.connect.enforcer.config.dto.ExtendedTokenIssuerDto;
import org.wso2.choreo.connect.enforcer.config.dto.FilterDTO;
import org.wso2.choreo.connect.enforcer.config.dto.JWTIssuerConfigurationDto;
import org.wso2.choreo.connect.enforcer.config.dto.ManagementCredentialsDto;
import org.wso2.choreo.connect.enforcer.config.dto.MetricsDTO;
import org.wso2.choreo.connect.enforcer.config.dto.MutualSSLDto;
import org.wso2.choreo.connect.enforcer.config.dto.SoapErrorResponseConfigDto;
import org.wso2.choreo.connect.enforcer.config.dto.ThreadPoolConfig;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleAgentConfigDto;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottlePublisherConfigDto;
import org.wso2.choreo.connect.enforcer.config.dto.TracingDTO;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.Constants;
import org.wso2.choreo.connect.enforcer.jmx.MBeanRegistrator;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.AgentConfiguration;
import org.wso2.choreo.connect.enforcer.util.BackendJwtUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;
import org.wso2.choreo.connect.enforcer.util.JWTUtils;
import org.wso2.choreo.connect.enforcer.util.TLSUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Configuration holder class for Microgateway.
 */
public class ConfigHolder {

    // TODO: Resolve default configs
    private static final Logger logger = LogManager.getLogger(ConfigHolder.class);

    private static ConfigHolder configHolder;
    private EnvVarConfig envVarConfig = EnvVarConfig.getInstance();
    EnforcerConfig config = new EnforcerConfig();
    private KeyStore trustStore = null;
    private KeyStore trustStoreForJWT = null;
    private KeyStore opaKeyStore = null;
    private TrustManagerFactory trustManagerFactory = null;
    private ArrayList<ExtendedTokenIssuerDto> configIssuerList;
    private boolean controlPlaneEnabled;

    private static final String dtoPackageName = EnforcerConfig.class.getPackageName();
    private static final String apimDTOPackageName = "org.wso2.carbon.apimgt";

    private ConfigHolder() {
        loadTrustStore();
        loadOpaClientKeyStore();
    }

    public static ConfigHolder getInstance() {
        if (configHolder != null) {
            return configHolder;
        }

        configHolder = new ConfigHolder();
        return configHolder;
    }

    /**
     * Initialize the configuration provider class by parsing the cds configuration.
     *
     * @param cdsConfig configuration fetch from CDS
     */
    public static ConfigHolder load(Config cdsConfig) {
        configHolder.parseConfigs(cdsConfig);
        return configHolder;
    }

    /**
     * Parse configurations received from the CDS to internal configuration DTO.
     * This is done inorder to prevent complicated code changes during the initial development
     * of the mgw. Later we can switch to CDS data models directly.
     */
    private void parseConfigs(Config config) {
        // load auth service
        populateAuthService(config.getAuthService());

        // Read jwt token configuration
        populateJWTIssuerConfiguration(config.getSecurity().getTokenServiceList());

        controlPlaneEnabled = config.getControlPlaneEnabled();

        // Read throttle publisher configurations
        populateThrottlingConfig(config.getThrottling());

        // Read backend jwt generation configurations
        populateJWTGeneratorConfigurations(config.getJwtGenerator());

        // Read tracing configurations
        populateTracingConfig(config.getTracing());

        // Read tracing configurations
        populateMetricsConfig(config.getMetrics());

        // Read token caching configs
        populateCacheConfigs(config.getCache());

        // Populate Analytics Configuration Values
        populateAnalyticsConfig(config.getAnalytics());

        // Read jwt issuer configurations
        populateJWTIssuerConfigurations(config.getJwtIssuer());

        populateAuthHeaderConfigurations(config.getSecurity().getAuthHeader());

        populateMTLSConfigurations(config.getSecurity().getMutualSSL());

        populateManagementCredentials(config.getManagement());

        populateRestServer(config.getRestServer());

        // Populates the SOAP error response related configs (SoapErrorInXMLEnabled).
        populateSoapErrorResponseConfigs(config.getSoap());

        // Populates the custom filter configurations applied along with enforcer filters.
        populateCustomFilters(config.getFiltersList());

        // resolve string variables provided as environment variables.
        resolveConfigsWithEnvs(this.config);
    }

    private void populateSoapErrorResponseConfigs(Soap soap) {
        SoapErrorResponseConfigDto soapErrorResponseConfigDto = new SoapErrorResponseConfigDto();
        soapErrorResponseConfigDto.setEnable(soap.getSoapErrorInXMLEnabled());
        config.setSoapErrorResponseConfigDto(soapErrorResponseConfigDto);
    }

    private void populateRestServer(RestServer restServer) {
        AdminRestServerDto adminRestServerDto = new AdminRestServerDto();
        adminRestServerDto.setEnable(restServer.getEnable());
        config.setRestServer(adminRestServerDto);
    }

    private void populateManagementCredentials(Management management) {
        ManagementCredentialsDto managementCredentialsDto = new ManagementCredentialsDto();
        managementCredentialsDto.setPassword(management.getPassword().toCharArray());
        managementCredentialsDto.setUserName(management.getUsername());
        config.setManagement(managementCredentialsDto);
    }

    private void populateAuthHeaderConfigurations(AuthHeader authHeader) {
        AuthHeaderDto authHeaderDto = new AuthHeaderDto();
        authHeaderDto.setAuthorizationHeader(authHeader.getAuthorizationHeader());
        authHeaderDto.setEnableOutboundAuthHeader(authHeader.getEnableOutboundAuthHeader());
        authHeaderDto.setTestConsoleHeaderName(authHeader.getTestConsoleHeaderName());
        config.setAuthHeader(authHeaderDto);
    }

    private void populateMTLSConfigurations(MutualSSL mtlsInfo) {
        MutualSSLDto mutualSSLDto = new MutualSSLDto();
        mutualSSLDto.setCertificateHeader(mtlsInfo.getCertificateHeader());
        mutualSSLDto.setEnableClientValidation(mtlsInfo.getEnableClientValidation());
        mutualSSLDto.setClientCertificateEncode(mtlsInfo.getClientCertificateEncode());
        mutualSSLDto.setEnableOutboundCertificateHeader(mtlsInfo.getEnableOutboundCertificateHeader());
        config.setMtlsInfo(mutualSSLDto);
    }

    private void populateAuthService(Service cdsAuth) {
        AuthServiceConfigurationDto authDto = new AuthServiceConfigurationDto();
        authDto.setKeepAliveTime(cdsAuth.getKeepAliveTime());
        authDto.setPort(cdsAuth.getPort());
        authDto.setMaxHeaderLimit(cdsAuth.getMaxHeaderLimit());
        authDto.setMaxMessageSize(cdsAuth.getMaxMessageSize());

        ThreadPoolConfig threadPool = new ThreadPoolConfig();
        MBeanRegistrator.registerMBean(threadPool);

        threadPool.setCoreSize(cdsAuth.getThreadPool().getCoreSize());
        threadPool.setKeepAliveTime(cdsAuth.getThreadPool().getKeepAliveTime());
        threadPool.setMaxSize(cdsAuth.getThreadPool().getMaxSize());
        threadPool.setQueueSize(cdsAuth.getThreadPool().getQueueSize());
        authDto.setThreadPool(threadPool);

        config.setAuthService(authDto);
    }


    private void populateJWTIssuerConfiguration(List<Issuer> cdsIssuers) {
        configIssuerList = new ArrayList<>();
        try {
            setTrustStoreForJWT(KeyStore.getInstance(KeyStore.getDefaultType()));
            getTrustStoreForJWT().load(null);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.error("Error while initiating the truststore for JWT related public certificates", e);
        }
        for (Issuer jwtIssuer : cdsIssuers) {
            ExtendedTokenIssuerDto issuerDto = new ExtendedTokenIssuerDto(jwtIssuer.getIssuer());

            JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
            jwksConfigurationDTO.setEnabled(StringUtils.isNotEmpty(jwtIssuer.getJwksURL()));
            jwksConfigurationDTO.setUrl(jwtIssuer.getJwksURL());
            issuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
            List<ClaimMapping> claimMaps = jwtIssuer.getClaimMappingList();
            for (ClaimMapping claimMap : claimMaps) {
                ClaimMappingDto map = new ClaimMappingDto(claimMap.getRemoteClaim(), claimMap.getLocalClaim());
                issuerDto.addClaimMapping(map);
            }
            // Load jwt transformers map.
            config.setJwtTransformers(BackendJwtUtils.loadJWTTransformers());
            String certificateAlias = jwtIssuer.getCertificateAlias();
            if (certificateAlias.isBlank()) {
                if (APIConstants.KeyManager.APIM_PUBLISHER_ISSUER.equals(jwtIssuer.getName())) {
                    certificateAlias = APIConstants.PUBLISHER_CERTIFICATE_ALIAS;
                } else if (APIConstants.KeyManager.DEFAULT_KEY_MANAGER.equals(jwtIssuer.getName())) {
                    certificateAlias = APIConstants.WSO2_PUBLIC_CERTIFICATE_ALIAS;
                } else if (APIConstants.KeyManager.APIM_APIKEY_ISSUER.equals(jwtIssuer.getName())) {
                    certificateAlias = APIConstants.APIKEY_CERTIFICATE_ALIAS;
                }
            }
            issuerDto.setCertificateAlias(certificateAlias);
            if (!certificateAlias.isBlank()) {
                try {
                    Certificate cert = TLSUtils.getCertificateFromFile(jwtIssuer.getCertificateFilePath());
                    getTrustStoreForJWT().setCertificateEntry(certificateAlias, cert);
                    TLSUtils.convertCertificate(cert);
                    // Convert the certificate to a javax.security.cert.Certificate and set to issuerDto.
                    issuerDto.setCertificate(TLSUtils.convertCertificate(cert));
                } catch (KeyStoreException | CertificateException | IOException | EnforcerException e) {
                    logger.error("Error while adding certificates to the JWT related Truststore", e);
                    // Continue to avoid making a invalid issuer.
                    continue;
                }
            }

            issuerDto.setName(jwtIssuer.getName());
            issuerDto.setConsumerKeyClaim(jwtIssuer.getConsumerKeyClaim());
            issuerDto.setValidateSubscriptions(jwtIssuer.getValidateSubscription());
            if (APIConstants.KeyManager.APIM_APIKEY_ISSUER.equals(jwtIssuer.getName())) {
                // Both API key and Internal key issuers are referred by issuer "name" instead of "issuer"
                // since the "iss" value present in both are same as oauth tokens. Thus, we override the
                // "issuer" in issuerDto to avoid conflicts (in case a user sets the same "issuer"
                // to Resident Key Manager and any of the other issuers).
                issuerDto.setIssuer(APIConstants.KeyManager.APIM_APIKEY_ISSUER_URL);
                config.getIssuersMap().put(APIConstants.KeyManager.APIM_APIKEY_ISSUER_URL, issuerDto);
            } else {
                config.getIssuersMap().put(jwtIssuer.getIssuer(), issuerDto);
            }
            configIssuerList.add(issuerDto);
        }
    }

    private void populateThrottlingConfig(Throttling throttling) {
        ThrottleConfigDto throttleConfig = new ThrottleConfigDto();
        throttleConfig.setGlobalPublishingEnabled(throttling.getEnableGlobalEventPublishing());
        throttleConfig.setHeaderConditionsEnabled(throttling.getEnableHeaderConditions());
        throttleConfig.setQueryConditionsEnabled(throttling.getEnableQueryParamConditions());
        throttleConfig.setJwtClaimConditionsEnabled(throttling.getEnableJwtClaimConditions());
        throttleConfig.setJmsConnectionInitialContextFactory(throttling.getJmsConnectionInitialContextFactory());
        throttleConfig.setJmsConnectionProviderUrl(throttling.getJmsConnectionProviderUrl());
        config.setThrottleConfig(throttleConfig);
        populateTMBinaryConfig(throttling.getPublisher());
    }

    private void populateTracingConfig(Tracing tracing) {
        TracingDTO tracingConfig = new TracingDTO();
        tracingConfig.setTracingEnabled(tracing.getEnabled());
        tracingConfig.setExporterType(tracing.getType());
        tracingConfig.setConfigProperties(tracing.getConfigPropertiesMap());
        config.setTracingConfig(tracingConfig);
    }

    private void populateMetricsConfig(Metrics metrics) {
        MetricsDTO metricsConfig = new MetricsDTO();
        metricsConfig.setMetricsEnabled(metrics.getEnabled());
        metricsConfig.setMetricsType(metrics.getType());
        config.setMetricsConfig(metricsConfig);
    }

    private void populateTMBinaryConfig(BinaryPublisher binary) {
        ThrottleAgent binaryAgent = binary.getAgent();
        AgentConfiguration agentConf = AgentConfiguration.getInstance();
        agentConf.setBatchSize(binaryAgent.getBatchSize());
        agentConf.setCiphers(binaryAgent.getCiphers());
        agentConf.setCorePoolSize(binaryAgent.getCorePoolSize());
        agentConf.setEvictionTimePeriod(binaryAgent.getEvictionTimePeriod());
        agentConf.setKeepAliveTimeInPool(binaryAgent.getKeepAliveTimeInPool());
        agentConf.setMaxIdleConnections(binaryAgent.getMaxIdleConnections());
        agentConf.setMaxPoolSize(binaryAgent.getMaxPoolSize());
        agentConf.setMaxTransportPoolSize(binaryAgent.getMaxTransportPoolSize());
        agentConf.setMinIdleTimeInPool(binaryAgent.getMinIdleTimeInPool());
        agentConf.setQueueSize(binaryAgent.getQueueSize());
        agentConf.setReconnectionInterval(binaryAgent.getReconnectionInterval());
        agentConf.setSecureEvictionTimePeriod(binaryAgent.getSecureEvictionTimePeriod());
        agentConf.setSecureMaxIdleConnections(binaryAgent.getSecureMaxIdleConnections());
        agentConf.setSecureMaxTransportPoolSize(binaryAgent.getSecureMaxTransportPoolSize());
        agentConf.setSecureMinIdleTimeInPool(binaryAgent.getSecureMinIdleTimeInPool());
        agentConf.setSslEnabledProtocols(binaryAgent.getSslEnabledProtocols());
        agentConf.setSocketTimeoutMS(binaryAgent.getSocketTimeoutMS());
        agentConf.setTrustStore(trustStore);

        PublisherPool pool = binary.getPool();
        ThrottlePublisherConfigDto pubConf = new ThrottlePublisherConfigDto();
        pubConf.setUserName(binary.getUsername());
        pubConf.setPassword(binary.getPassword());
        pubConf.setInitIdleObjectDataPublishingAgents(pool.getInitIdleObjectDataPublishingAgents());
        pubConf.setMaxIdleDataPublishingAgents(pool.getMaxIdleDataPublishingAgents());
        pubConf.setPublisherThreadPoolCoreSize(pool.getPublisherThreadPoolCoreSize());
        pubConf.setPublisherThreadPoolKeepAliveTime(pool.getPublisherThreadPoolKeepAliveTime());
        pubConf.setPublisherThreadPoolMaximumSize(pool.getPublisherThreadPoolMaximumSize());

        processTMPublisherURLGroup(binary.getUrlGroupList(), pubConf);

        ThrottleAgentConfigDto throttleAgent = new ThrottleAgentConfigDto();
        throttleAgent.setAgent(agentConf);
        throttleAgent.setUsername(binary.getUsername());
        throttleAgent.setPassword(binary.getPassword());
        throttleAgent.setPublisher(pubConf);
        config.getThrottleConfig().setThrottleAgent(throttleAgent);
    }

    private void loadTrustStore() {
        try {

            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);

            if (getEnvVarConfig().isTrustDefaultCerts()) {
                loadDefaultCertsToTrustStore();
            }
            loadTrustedCertsToTrustStore();

            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.error("Error in loading certs to the trust store.", e);
        }
    }

    private void loadTrustedCertsToTrustStore() throws IOException {
        String truststoreFilePath = getEnvVarConfig().getTrustedAdapterCertsPath();
        TLSUtils.addCertsToTruststore(trustStore, truststoreFilePath);
    }

    private void loadDefaultCertsToTrustStore() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // Using null here initialises the TMF with the default trust store.
        tmf.init((KeyStore) null);

        // Get hold of the default trust manager
        X509TrustManager defaultTm = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                defaultTm = (X509TrustManager) tm;
                break;
            }
        }

        // Get the certs from defaultTm and add them to our trustStore
        if (defaultTm != null) {
            X509Certificate[] trustedCerts = defaultTm.getAcceptedIssuers();
            Arrays.stream(trustedCerts)
                    .forEach(cert -> {
                        try {
                            trustStore.setCertificateEntry(RandomStringUtils.random(10, true, false),
                                    cert);
                        } catch (KeyStoreException e) {
                            logger.error("Error while adding default trusted ca cert", e);
                        }
                    });
        }
    }

    private void loadOpaClientKeyStore() {
        String certPath = getEnvVarConfig().getOpaClientPublicKeyPath();
        String keyPath = getEnvVarConfig().getOpaClientPrivateKeyPath();
        opaKeyStore = FilterUtils.createClientKeyStore(certPath, keyPath);
    }

    /**
     * The receiverURLGroup and the authURLGroup is preprocessed
     * such that to make them compatible with the binary agent.
     */
    private void processTMPublisherURLGroup(List<TMURLGroup> urlGroups,
                                            ThrottlePublisherConfigDto pubConfiguration) {
        StringBuilder restructuredReceiverURL = new StringBuilder();
        StringBuilder restructuredAuthURL = new StringBuilder();

        for (TMURLGroup urlGroup : urlGroups) {
            List<String> receiverUrls = urlGroup.getReceiverURLsList();
            List<String> authUrls = urlGroup.getAuthURLsList();
            if (receiverUrls.size() == 1 && authUrls.size() == 1) {
                restructuredReceiverURL.append("{").append(receiverUrls.get(0)).append("},");
                restructuredAuthURL.append("{").append(authUrls.get(0)).append("},");
                continue;
            }
            String urlType = urlGroup.getType();
            if (urlType.isBlank() || !(Constants.LOADBALANCE.equalsIgnoreCase(urlType)
                    || Constants.FAILOVER.equalsIgnoreCase(urlType))) {
                logger.warn("Type is not "
                        + Constants.LOADBALANCE + " or " + Constants.FAILOVER + ". Hence proceeding as a "
                        + Constants.FAILOVER + " configuration.");
                urlType = Constants.FAILOVER;
            }
            restructuredReceiverURL.append(processSingleURLGroup(receiverUrls, urlType)).append(",");
            restructuredAuthURL.append(processSingleURLGroup(authUrls, urlType)).append(",");
        }

        //to remove the final ',' in the URLs and set to publisher config
        if (!restructuredReceiverURL.toString().isBlank() && !restructuredAuthURL.toString().isBlank()) {
            pubConfiguration.setReceiverUrlGroup(restructuredReceiverURL.substring(0,
                    restructuredReceiverURL.length() - 1));
            pubConfiguration.setAuthUrlGroup(restructuredAuthURL.substring(0, restructuredAuthURL.length() - 1));
        }
    }

    private String processSingleURLGroup(List<String> urlArray, String urlType) {
        StringBuilder concatenatedURLString = new StringBuilder("{");
        for (String url : urlArray) {
            if (Constants.LOADBALANCE.equalsIgnoreCase(urlType)) {
                concatenatedURLString.append(url).append(Constants.TM_BINARY_LOADBALANCE_SEPARATOR);
            } else if (Constants.FAILOVER.equalsIgnoreCase(urlType)) {
                concatenatedURLString.append(url).append(Constants.TM_BINARY_FAILOVER_SEPARATOR);
            } else {
                concatenatedURLString.append(url).append(Constants.TM_BINARY_FAILOVER_SEPARATOR);
            }
        }
        //to remove the trailing '|' or ','
        concatenatedURLString = new StringBuilder(
                concatenatedURLString.substring(0, concatenatedURLString.length() - 1) + "}");
        return concatenatedURLString.toString();
    }

    private void populateJWTGeneratorConfigurations(JWTGenerator jwtGenerator) {
        JWTConfigurationDto jwtConfigurationDto = new JWTConfigurationDto();
        jwtConfigurationDto.setEnabled(jwtGenerator.getEnable());
        jwtConfigurationDto.setJwtHeader(jwtGenerator.getHeader());
        jwtConfigurationDto.setConsumerDialectUri(jwtGenerator.getClaimDialect());
        jwtConfigurationDto.setSignatureAlgorithm(jwtGenerator.getSigningAlgorithm());
        jwtConfigurationDto.setEnableUserClaims(jwtGenerator.getEnableUserClaims());
        jwtConfigurationDto.setGatewayJWTGeneratorImpl(jwtGenerator.getGatewayGeneratorImpl());
        jwtConfigurationDto.setTtl(jwtGenerator.getTokenTtl());
        try {
            jwtConfigurationDto.setPublicCert(TLSUtils.getCertificate(jwtGenerator.getPublicCertificatePath()));
            jwtConfigurationDto.setPrivateKey(JWTUtils.getPrivateKey(jwtGenerator.getPrivateKeyPath()));
        } catch (EnforcerException | CertificateException | IOException e) {
            logger.error("Error in loading public cert or private key", e);
        }
        config.setJwtConfigurationDto(jwtConfigurationDto);
    }

    private void populateCacheConfigs(Cache cache) {
        CacheDto cacheDto = new CacheDto();
        cacheDto.setEnabled(cache.getEnable());
        cacheDto.setMaximumSize(cache.getMaximumSize());
        cacheDto.setExpiryTime(cache.getExpiryTime());
        config.setCacheDto(cacheDto);
    }

    private void populateAnalyticsConfig(Analytics analyticsConfig) {

        AnalyticsReceiverConfigDTO serverConfig = new AnalyticsReceiverConfigDTO();
        serverConfig.setKeepAliveTime(analyticsConfig.getService().getKeepAliveTime());
        serverConfig.setMaxHeaderLimit(analyticsConfig.getService().getMaxHeaderLimit());
        serverConfig.setMaxMessageSize(analyticsConfig.getService().getMaxMessageSize());
        serverConfig.setPort(analyticsConfig.getService().getPort());

        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
        threadPoolConfig.setCoreSize(analyticsConfig.getService().getThreadPool().getCoreSize());
        threadPoolConfig.setMaxSize(analyticsConfig.getService().getThreadPool().getMaxSize());
        threadPoolConfig.setKeepAliveTime(analyticsConfig.getService().getThreadPool().getKeepAliveTime());
        threadPoolConfig.setQueueSize(analyticsConfig.getService().getThreadPool().getQueueSize());
        serverConfig.setThreadPoolConfig(threadPoolConfig);

        AnalyticsDTO analyticsDTO = new AnalyticsDTO();
        analyticsDTO.setEnabled(analyticsConfig.getEnabled());
        analyticsDTO.setType(analyticsConfig.getType());
        analyticsDTO.setConfigProperties(analyticsConfig.getConfigPropertiesMap());
        analyticsDTO.setServerConfig(serverConfig);
        config.setAnalyticsConfig(analyticsDTO);

    }
    /**
     * This method recursively looks for the string type config values in the {@link EnforcerConfig} object ,
     * which have the prefix `$env{` and reads the respective value from the environment variable and set it to
     * the config object.
     *
     * @param config - Enforcer config object.
     */
    private void resolveConfigsWithEnvs(Object config) {
        List<Field> classFields = Arrays.asList(config.getClass().getDeclaredFields());
        //extended config class env variables should also be resolved
        if (config.getClass().getSuperclass() != null && (
                config.getClass().getSuperclass().getPackageName().contains(dtoPackageName) || config.getClass()
                        .getSuperclass().getPackageName().contains(apimDTOPackageName))) {
            processRecursiveObject(config, config.getClass().getSuperclass().getDeclaredFields());
        }
        processRecursiveObject(config, config.getClass().getDeclaredFields());
    }

    private void processRecursiveObject(Object config, Field[] classFields) {
        for (Field field : classFields) {
            try {
                field.setAccessible(true);
                // handles the string and char array objects
                if (field.getType().isAssignableFrom(String.class) || field.getType().isAssignableFrom(char[].class)) {
                    field.set(config, getEnvValue(field.get(config)));
                } else if (field.getName().contains(Constants.OBJECT_THIS_NOTATION)) {
                    // skip the java internal objects created, inside the recursion to avoid stack overflow.
                    continue;
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    // handles the config objects saved as Maps
                    Map<Object, Object> objectMap = (Map<Object, Object>) field.get(config);
                    for (Map.Entry<Object, Object> entry : objectMap.entrySet()) {
                        if (entry.getValue().getClass().isAssignableFrom(String.class) || entry.getValue().getClass()
                                .isAssignableFrom(char[].class)) {
                            field.set(config, getEnvValue(field.get(config)));
                            continue;
                        } else if (entry.getValue().getClass().getPackageName().contains(dtoPackageName) || entry
                                .getValue().getClass().getPackageName().contains(apimDTOPackageName)) {
                            resolveConfigsWithEnvs(entry.getValue());
                        }
                    }
                } else if (field.getType().isArray() && field.getType().getPackageName().contains(dtoPackageName)) {
                    // handles the config objects saved as arrays
                    Object[] objectArray = (Object[]) field.get(config);
                    for (Object arrayObject : objectArray) {
                        if (arrayObject.getClass().getPackageName().contains(dtoPackageName) || arrayObject.getClass()
                                .getPackageName().contains(apimDTOPackageName)) {
                            resolveConfigsWithEnvs(arrayObject);
                        } else if (arrayObject.getClass().isAssignableFrom(String.class) || arrayObject.getClass()
                                .isAssignableFrom(char[].class)) {
                            field.set(config, getEnvValue(arrayObject));
                        }
                    }
                } else if (field.getType().getPackageName().contains(dtoPackageName) || field.getType().getPackageName()
                        .contains(apimDTOPackageName)) { //recursively call the dto objects in the same package
                    resolveConfigsWithEnvs(field.get(config));
                }
            } catch (IllegalAccessException e) {
                //log and continue
                logger.error("Error while reading the config value : " + field.getName(), e);
            }
        }
    }

    private Object getEnvValue(Object configValue) {
        if (configValue instanceof String) {
            String value = (String) configValue;
            return replaceEnvRegex(value);
        } else if (configValue instanceof char[]) {
            String value = String.valueOf((char[]) configValue);
            return replaceEnvRegex(value).toCharArray();
        }
        return configValue;
    }

    private String replaceEnvRegex(String value) {
        Matcher m = Pattern.compile("\\$env\\{(.*?)\\}").matcher(value);
        if (value.contains(Constants.ENV_PREFIX)) {
            while (m.find()) {
                String envName = value.substring(m.start() + 5, m.end() - 1);
                if (System.getenv(envName) != null) {
                    value = value.replace(value.substring(m.start(), m.end()), System.getenv(envName));
                }
            }
        }
        return value;
    }

    private void populateJWTIssuerConfigurations(JWTIssuer jwtIssuer) {
        JWTIssuerConfigurationDto jwtIssuerConfigurationDto = new JWTIssuerConfigurationDto();
        jwtIssuerConfigurationDto.setEnabled(jwtIssuer.getEnabled());
        jwtIssuerConfigurationDto.setIssuer(jwtIssuer.getIssuer());
        jwtIssuerConfigurationDto.setConsumerDialectUri(jwtIssuer.getClaimDialect());
        jwtIssuerConfigurationDto.setSignatureAlgorithm(jwtIssuer.getSigningAlgorithm());
        try {
            jwtIssuerConfigurationDto.setPrivateKey(JWTUtils.getPrivateKey(jwtIssuer.getPrivateKeyPath()));
            jwtIssuerConfigurationDto.setPublicCert(TLSUtils.getCertificate(jwtIssuer.getPublicCertificatePath()));
        } catch (EnforcerException | CertificateException | IOException e) {
            logger.error("Error in loading public cert or private key", e);
        }
        jwtIssuerConfigurationDto.setTtl(jwtIssuer.getValidityPeriod());
        CredentialDto[] credentialDtos = new CredentialDto[jwtIssuer.getJwtUsersList().size()];
        for (int index = 0; index < jwtIssuer.getJwtUsersList().size(); index++) {
            CredentialDto credentialDto = new CredentialDto(jwtIssuer.getJwtUsers(index).getUsername(),
                    jwtIssuer.getJwtUsers(index).getPassword().toCharArray());
            credentialDtos[index] = credentialDto;
        }
        config.setJwtUsersCredentials(credentialDtos);
        config.setJwtIssuerConfigurationDto(jwtIssuerConfigurationDto);
    }

    private void populateCustomFilters(List<Filter> filterList) {
        FilterDTO[] filterArray = new FilterDTO[filterList.size()];
        int index = 0;
        for (Filter filter : filterList) {
            FilterDTO filterDTO = new FilterDTO();
            filterDTO.setClassName(filter.getClassName());
            filterDTO.setPosition(filter.getPosition());
            filterDTO.setConfigProperties(filter.getConfigPropertiesMap());
            filterArray[index] = filterDTO;
            index++;
        }
        config.setCustomFilters(filterArray);
    }

    public EnforcerConfig getConfig() {
        return config;
    }

    public void setConfig(EnforcerConfig config) {
        this.config = config;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public KeyStore getTrustStoreForJWT() {
        return trustStoreForJWT;
    }

    public KeyStore getOpaKeyStore() {
        return opaKeyStore;
    }

    public void setTrustStoreForJWT(KeyStore trustStoreForJWT) {
        this.trustStoreForJWT = trustStoreForJWT;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
        this.trustManagerFactory = trustManagerFactory;
    }

    public EnvVarConfig getEnvVarConfig() {
        return envVarConfig;
    }

    public ArrayList<ExtendedTokenIssuerDto> getConfigIssuerList() {
        return configIssuerList;
    }

    public void setConfigIssuerList(ArrayList<ExtendedTokenIssuerDto> configIssuerList) {
        this.configIssuerList = configIssuerList;
    }

    public boolean isControlPlaneEnabled() {
        return controlPlaneEnabled;
    }
}
