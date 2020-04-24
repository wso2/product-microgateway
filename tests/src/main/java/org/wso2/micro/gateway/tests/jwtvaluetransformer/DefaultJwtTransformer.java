package org.wso2.micro.gateway.tests.jwtvaluetransformer;

import org.wso2.micro.gateway.jwttransformer.JWTValueTransformer;

import java.util.Map;

/**
 * This class is for default Jwt transformer.
 */
public class DefaultJwtTransformer implements JWTValueTransformer {

    @Override
    public Map<String, Object> transformJWT(Map<String, Object> jwtClaims) {
        String scope = "";
        if (jwtClaims.containsKey("scope")) {
            if (jwtClaims.get("scope") instanceof Object[]) {
                for (int i = 0; i < ((Object[]) jwtClaims.get("scope")).length; i++) {
                    scope += ((Object[]) jwtClaims.get("scope"))[i] + " ";
                }
                scope = scope.trim();
            }
            jwtClaims.put("scope", scope);
        }
        return jwtClaims;
    }
}
