package sample.jwt.generator;

import org.apache.commons.lang3.StringUtils;
import org.wso2.micro.gateway.jwt.generator.AbstractMGWJWTGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SampleJWTGenerator extends AbstractMGWJWTGenerator {
    public SampleJWTGenerator(String dialectURI,
                                String signatureAlgorithm,
                                String keyStorePath,
                                String keyStorePassword,
                                String certificateAlias,
                                String privateKeyAlias,
                                int jwtExpiryTime,
                                String[] restrictedClaims,
                                boolean cacheEnabled,
                                int cacheExpiry,
                                String tokenIssuer,
                                String[] tokenAudience) {
        super(dialectURI,
                signatureAlgorithm,
                keyStorePath,
                keyStorePassword,
                certificateAlias,
                privateKeyAlias,
                jwtExpiryTime,
                restrictedClaims,
                cacheEnabled,
                cacheExpiry,
                tokenIssuer,
                tokenAudience);
    }

    @Override
    public Map<String, Object> populateStandardClaims(Map<String, Object> jwtInfo) {
        long currentTime = System.currentTimeMillis();
        long expireIn = currentTime + getTTL();
        String dialect = this.getDialectURI();
        Map<String, Object> claims = new HashMap();
        HashMap<String, Object> customClaims = (HashMap<String, Object>) jwtInfo.get("customClaims");
        claims.put("iss", getTokenIssuer());
        if (getTokenAudience().length == 1) {
            claims.put("aud", getTokenAudience()[0]);
        } else if (getTokenAudience().length != 0) {
            claims.put("aud", arrayToJSONArray(getTokenAudience()));
        }
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", (int) (currentTime / 1000));
        claims.put("exp", (int) (expireIn / 1000));
        if (StringUtils.isNotEmpty((CharSequence) jwtInfo.get("sub"))) {
            claims.put("sub", jwtInfo.get("sub"));
            claims.put(dialect + "/endUser", jwtInfo.get("sub"));
        }
        if (StringUtils.isNotEmpty((CharSequence) customClaims.get("scopes"))) {
            claims.put("scopes", (customClaims.get("scopes")));
        }
        return claims;
    }

    @Override
    public Map<String, Object> populateCustomClaims(Map<String, Object> jwtInfo, ArrayList<String> restrictedClaims) {
        Map<String, Object> claims = new HashMap();
        for (String key: jwtInfo.keySet()) {
            if (key.equals("customClaims")) {
                Map<String, Object> customClaims = (Map<String, Object>) jwtInfo.get(key);
                for (String subKey: customClaims.keySet()) {
                    if (!restrictedClaims.contains(subKey)) {
                        claims.put(subKey, customClaims.get(subKey));
                    }
                }
            } else {
                if (!restrictedClaims.contains(key)) {
                    claims.put(key, jwtInfo.get(key));
                }
            }
        }
        claims.put("custom", "claim");
        return claims;
    }
}
