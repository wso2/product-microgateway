package org.wso2.micro.gateway.enforcer.keymgt;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.gateway.discovery.keymgt.KeyManagerConfig;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.JWKSConfigurationDTO;
import org.wso2.micro.gateway.enforcer.config.dto.TokenIssuerDto;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.discovery.KeyManagerDiscoveryClient;
import org.wso2.micro.gateway.enforcer.dto.ClaimMappingDto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * KeyManager holder class for Micro gateway.
 */
public class KeyManagerHolder {

    private static final Logger logger = LogManager.getLogger(ConfigHolder.class);

    private static final String X509 = "X.509";
    private static KeyManagerHolder instance;

    private KeyManagerHolder() {}

    public static KeyManagerHolder getInstance() {
        if (instance == null) {
            instance = new KeyManagerHolder();
        }
        return instance;
    }

    public void init() {
        KeyManagerDiscoveryClient keyManagerDs =  KeyManagerDiscoveryClient.getInstance();
        keyManagerDs.watchKeyManagers();
    }

    public void populateKMIssuerConfiguration(List<KeyManagerConfig> kmIssuers) {
        Map<String, TokenIssuerDto> resultIssuerList = new HashMap<>();
        for (KeyManagerConfig keyManagerConfig : kmIssuers) {
            JSONObject configObj = new JSONObject(keyManagerConfig.getConfiguration());
            Map<String, Object> configuration = new HashMap<>();
            Iterator<String> keysItr = configObj.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = configObj.get(key);
                configuration.put(key, value);
            }

            if (keyManagerConfig.getEnabled()) {
                updateTokenIssuerList(configuration, resultIssuerList);
            }
        }
        ArrayList<TokenIssuerDto> configIssuerList = ConfigHolder.getInstance().getConfigIssuerList();
        for (TokenIssuerDto tokenIssuerDto : configIssuerList) {
            if (resultIssuerList.containsKey(tokenIssuerDto.getIssuer())) {
                logger.warn("token issuer " + tokenIssuerDto.getIssuer() + " already exists in config map. " +
                        "Existing configurations will be replaced by KeyManager configuration");
            } else {
                resultIssuerList.put(tokenIssuerDto.getIssuer(), tokenIssuerDto);
            }
        }
        ConfigHolder.getInstance().getConfig().setIssuersMap(resultIssuerList);
    }

    public void updateTokenIssuerList(Map<String, Object> configuration, Map<String, TokenIssuerDto> resultIssuerList) {
        Object selfValidateJWT = configuration.get(APIConstants.KeyManager.SELF_VALIDATE_JWT);
        if (selfValidateJWT != null && (Boolean) selfValidateJWT) {
            Object issuer = configuration.get(APIConstants.KeyManager.ISSUER);
            if (issuer != null) {
                TokenIssuerDto tokenIssuerDto = new TokenIssuerDto((String) issuer);
                Object claimMappings = configuration.get(APIConstants.KeyManager.CLAIM_MAPPING);
                if (claimMappings instanceof JSONArray) {
                    Gson gson = new Gson();
                    ClaimMappingDto[] claimMappingDto = gson.fromJson(claimMappings.toString(),
                            ClaimMappingDto[].class);
                    tokenIssuerDto.addClaimMappings(claimMappingDto);
                }
                Object consumerKeyClaim =
                        configuration.get(APIConstants.KeyManager.CONSUMER_KEY_CLAIM);
                if (consumerKeyClaim instanceof String && StringUtils.isNotEmpty((String) consumerKeyClaim)) {
                    tokenIssuerDto.setConsumerKeyClaim((String) consumerKeyClaim);
                }
                Object scopeClaim =
                        configuration.get(APIConstants.KeyManager.SCOPES_CLAIM);
                if (scopeClaim instanceof String && StringUtils.isNotEmpty((String) scopeClaim)) {
                    tokenIssuerDto.setScopesClaim((String) scopeClaim);
                }
                Object jwksEndpoint = configuration.get(APIConstants.KeyManager.JWKS_ENDPOINT);
                if (jwksEndpoint != null) {
                    if (StringUtils.isNotEmpty((String) jwksEndpoint)) {
                        JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
                        jwksConfigurationDTO.setEnabled(true);
                        jwksConfigurationDTO.setUrl((String) jwksEndpoint);
                        tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
                    }
                }
                Object certificateType = configuration.get(APIConstants.KeyManager.CERTIFICATE_TYPE);
                Object certificateValue = configuration.get(APIConstants.KeyManager.CERTIFICATE_VALUE);
                if (certificateType != null && StringUtils.isNotEmpty((String) certificateType) &&
                        certificateValue != null && StringUtils.isNotEmpty((String) certificateValue)) {
                    if (APIConstants.KeyManager.CERTIFICATE_TYPE_JWKS_ENDPOINT.equals(certificateType)) {
                        JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
                        jwksConfigurationDTO.setEnabled(true);
                        jwksConfigurationDTO.setUrl((String) certificateValue);
                        tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
                    } else {
                        try {
                            byte[] certBytes = Base64.getDecoder().decode(certificateValue.toString());
                            InputStream is = new ByteArrayInputStream(certBytes);
                            Certificate certificate = CertificateFactory.getInstance(X509)
                                    .generateCertificate(is);
                            tokenIssuerDto.setCertificate(certificate);
                        } catch (CertificateException e) {
                            logger.error("Error reading the certificate for issuer " + issuer + ". Error cause: " +
                                    e.getMessage());
                        }
                    }
                }
                resultIssuerList.put(tokenIssuerDto.getIssuer(), tokenIssuerDto);
            }
        }
    }
}
