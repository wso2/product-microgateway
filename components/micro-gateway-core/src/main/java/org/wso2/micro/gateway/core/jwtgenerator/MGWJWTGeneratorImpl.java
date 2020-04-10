package org.wso2.micro.gateway.core.jwtgenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinalang.jvm.values.MapValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *  Class to implement standard claims and custom claims
 */
public class MGWJWTGeneratorImpl extends AbstractMGWJWTGenerator {
    private static final Log logger = LogFactory.getLog(MGWJWTGeneratorImpl.class);
    private static final String AUTH_APPLICATION_USER_LEVEL_TOKEN = "Application_user";

    public MGWJWTGeneratorImpl(String dialectURI,
                               String signatureAlgorithm,
                               String trustStorePath,
                               String trustStorePassword,
                               int jwtExpiryTime,
                               String restrictedClaims,
                               boolean jwtCacheEnabled,
                               int jwtCacheExpiry,
                               String tokenIssuer,
                               String tokenAudience,
                               MapValue apiDetails) {
        super(dialectURI, signatureAlgorithm, trustStorePath, trustStorePassword, jwtExpiryTime, restrictedClaims,
                jwtCacheEnabled, jwtCacheExpiry, tokenIssuer, tokenAudience, apiDetails);
    }

    @Override
    public Map<String, Object> populateStandardClaims(MapValue jwtInfo) {
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL();
        String dialect = this.getDialectURI();
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", getTokenIssuer());
        claims.put("aud", getTokenAudience());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", (int) (currentTime / 1000));
        claims.put("exp", (int) (expireIn / 1000));
        if (StringUtils.isNotEmpty(jwtInfo.getStringValue("sub"))) {
            claims.put("sub", jwtInfo.getStringValue("sub"));
            claims.put(dialect + "/subscriber", jwtInfo.getStringValue("sub"));
            claims.put(dialect + "/endUser", jwtInfo.getStringValue("sub"));
        }
        if (StringUtils.isNotEmpty(jwtInfo.getMapValue("customClaims").getStringValue("scopes"))) {
            claims.put("scopes", jwtInfo.getMapValue("customClaims").getStringValue("scopes"));
        }
        if (StringUtils.isNotEmpty(jwtInfo.getMapValue("customClaims").getMapValue("application").getIntValue("id")
                .toString())) {
            claims.put(dialect + "/applicationid", jwtInfo.getMapValue("customClaims").getMapValue("application")
                    .getIntValue("id").toString());
        }
        if (StringUtils.isNotEmpty(jwtInfo.getMapValue("customClaims").getMapValue("application")
                .getStringValue("name"))) {
            claims.put(dialect + "/applicationname", jwtInfo.getMapValue("customClaims").getMapValue("application")
                    .getStringValue("name"));
        }
        if (StringUtils.isNotEmpty(jwtInfo.getMapValue("customClaims").getMapValue("application")
                .getStringValue("tier"))) {
            claims.put(dialect + "/applicationtier", jwtInfo.getMapValue("customClaims").getMapValue("application")
                    .getStringValue("tier"));
        }
        if (StringUtils.isNotEmpty(getApiDetails().getStringValue("apiContext"))) {
            claims.put(dialect + "/apicontext", getApiDetails().getStringValue("apiContext"));
        }
        if (StringUtils.isNotEmpty(getApiDetails().getStringValue("apiVersion"))) {
            claims.put(dialect + "/version", getApiDetails().getStringValue("apiContext"));
        }
        if (StringUtils.isNotEmpty(getApiDetails().getStringValue("apiTier"))) {
            claims.put(dialect + "/tier", getApiDetails().getStringValue("apiTier"));
        }
        if (StringUtils.isNotEmpty(jwtInfo.getMapValue("customClaims").getStringValue("keytype"))) {
            claims.put(dialect + "/keytype", jwtInfo.getMapValue("customClaims").getStringValue("keytype"));
        } else {
            claims.put(dialect + "/keytype", "PRODUCTION");
        }
        claims.put(dialect + "/usertype", AUTH_APPLICATION_USER_LEVEL_TOKEN);
        return claims;
    }

    @Override
    public Map<String, Object> populateCustomClaims(MapValue jwtInfo, ArrayList<String> restrictedClaims) {
        List<String> defaultRestrictedClaims = new ArrayList<>(Arrays.asList("iss", "sub", "aud", "exp",
                "nbf", "iat", "jti", "application", "tierInfo", "subscribedAPIs", "keytype"));
        restrictedClaims.addAll(defaultRestrictedClaims);
        Map<String, Object> claims = new HashMap<>();
        for (Object key: jwtInfo.getKeys()) {
            if (key.toString().equals("customClaims")) {
                addClaim(jwtInfo.getMapValue(key.toString()), restrictedClaims, claims);
            } else if (!restrictedClaims.contains(key.toString())) {
                claims.put(key.toString(), jwtInfo.getStringValue(key.toString()));
            }
        }
        return claims;
    }
}
