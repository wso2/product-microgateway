package org.wso2.micro.gateway.enforcer.keymanager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.gateway.discovery.keyManagerConfig.KeyManagerConfig;
import org.wso2.micro.gateway.enforcer.config.ConfigHolder;
import org.wso2.micro.gateway.enforcer.config.dto.JWKSConfigurationDTO;
import org.wso2.micro.gateway.enforcer.config.dto.TokenIssuerDto;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.discovery.KeyManagerDiscoveryClient;
import org.wso2.micro.gateway.enforcer.dto.ClaimMappingDto;

import java.util.List;
import java.util.Map;

/**
 * KeyManager holder class for Micro gateway.
 */
public class KeyManagerHolder {

    private Map<String, TokenIssuerDto> tokenIssuers = ConfigHolder.getInstance().getConfig().getIssuersMap();
    private static final Logger logger = LogManager.getLogger(ConfigHolder.class);

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

    public void populateKMIssuerConfiguration(List<KeyManagerConfig> kmIssuers)  {

        for (KeyManagerConfig keyManagerConfig : kmIssuers) {
            Map configuration = keyManagerConfig.getConfigurationMap();
            Object selfValidateJWT = configuration.get(APIConstants.KeyManager.SELF_VALIDATE_JWT);
            if (selfValidateJWT != null && (Boolean) selfValidateJWT) {
                Object issuer = configuration.get(APIConstants.KeyManager.ISSUER);
                boolean isIssuerPresent = tokenIssuers.containsKey(issuer);
                if (issuer != null) {
                    TokenIssuerDto tokenIssuerDto = new TokenIssuerDto((String) issuer);
                    Object claimMappings = configuration.get(APIConstants.KeyManager.CLAIM_MAPPING);
                    if (claimMappings instanceof List) {
                        Gson gson = new Gson();
                        JsonElement jsonElement = gson.toJsonTree(claimMappings);
                        ClaimMappingDto[] claimMappingDto = gson.fromJson(jsonElement, ClaimMappingDto[].class);
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
                    Object certificateValue =
                            configuration.get(APIConstants.KeyManager.CERTIFICATE_VALUE);
                    if (certificateType != null && StringUtils.isNotEmpty((String) certificateType) &&
                            certificateValue != null && StringUtils.isNotEmpty((String) certificateValue)) {
                        if (APIConstants.KeyManager.CERTIFICATE_TYPE_JWKS_ENDPOINT.equals(certificateType)) {
                            JWKSConfigurationDTO jwksConfigurationDTO = new JWKSConfigurationDTO();
                            jwksConfigurationDTO.setEnabled(true);
                            jwksConfigurationDTO.setUrl((String) certificateValue);
                            tokenIssuerDto.setJwksConfigurationDTO(jwksConfigurationDTO);
                        }
                    }
                    if (isIssuerPresent) {
                        logger.warn("token issuer is already exist in config map.Existing configurations will be " +
                                "replaced by KeyManager configuration");
                        tokenIssuers.replace(tokenIssuerDto.getIssuer(), tokenIssuerDto);
                    } else {
                        tokenIssuers.put(tokenIssuerDto.getIssuer(), tokenIssuerDto);
                    }
                }
            }
        }
    }
}
