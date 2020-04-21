package org.wso2.micro.gateway.tests.jwtvaluetransformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.wso2.micro.gateway.jwttransformer.JWTValueTransformer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is for default Jwt transformer.
 */
public class DefaultJwtTransformer implements JWTValueTransformer {

    @Override
    public HashMap<String, Object> transformJWT(HashMap<String, Object> jwtClaims) {
        String scope = "";
        Map<String, Object> claimSet = jwtClaims;
        String data = (String) claimSet.get("scope");
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonArray = (JsonArray) jsonParser.parse(data);
        if (claimSet.containsKey("scope")) {
            if (jsonArray instanceof JsonArray) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    scope += jsonArray.get(i) + " ";
                }
                scope = scope.trim();
                scope = scope.replace("\"", "");
            }
            claimSet.put("scope", scope);
        }
        return (HashMap<String, Object>) claimSet;
    }
}
