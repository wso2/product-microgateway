package org.wso2.micro.gateway.tests.util;

import org.json.JSONObject;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TokenUtil {

    public static String getBasicJWT(ApplicationDTO applicationDTO, JSONObject jwtTokenInfo, String keyType, int validityPeriod)
            throws Exception {

        jwtTokenInfo.put("aud", "http://org.wso2.apimgt/gateway");
        jwtTokenInfo.put("sub", "admin");
        jwtTokenInfo.put("application", new JSONObject(applicationDTO));
        jwtTokenInfo.put("iss", "https://localhost:9443/oauth2/token");
        jwtTokenInfo.put("keytype", keyType);
        jwtTokenInfo.put("iat", System.currentTimeMillis());
        jwtTokenInfo.put("exp", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + validityPeriod);
        jwtTokenInfo.put("jti", UUID.randomUUID());

        String payload = jwtTokenInfo.toString();

        JSONObject head = new JSONObject();
        head.put("typ", "JWT");
        head.put("alg", "RS256");
        head.put("x5t", "UB_BQy2HFV3EMTgq64Q-1VitYbE");
        String header = head.toString();

        String base64UrlEncodedHeader = Base64.getUrlEncoder()
                .encodeToString(header.getBytes(Charset.defaultCharset()));
        String base64UrlEncodedBody = Base64.getUrlEncoder().encodeToString(payload.getBytes(Charset.defaultCharset()));

        Signature signature = Signature.getInstance("SHA256withRSA");
        String jksPath = TokenUtil.class.getClassLoader().getResource("wso2carbon.jks").getPath();
        FileInputStream is = new FileInputStream(jksPath);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, "wso2carbon".toCharArray());
        String alias = "wso2carbon";
        Key key = keystore.getKey(alias, "wso2carbon".toCharArray());
        Key privateKey = null;
        if (key instanceof PrivateKey) {
            privateKey = key;
        }
        signature.initSign((PrivateKey) privateKey);
        String assertion = base64UrlEncodedHeader + "." + base64UrlEncodedBody;
        byte[] dataInBytes = assertion.getBytes(StandardCharsets.UTF_8);
        signature.update(dataInBytes);
        //sign the assertion and return the signature
        byte[] signedAssertion = signature.sign();
        String base64UrlEncodedAssertion = Base64.getUrlEncoder().encodeToString(signedAssertion);
        return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;
    }

    public static String getJwtWithCustomClaims(ApplicationDTO applicationDTO, JSONObject jwtTokenInfo, String keyType, int validityPeriod, Map<String, String > claims)
            throws Exception {
        for(Map.Entry<String, String> entry : claims.entrySet()) {
            jwtTokenInfo.put(entry.getKey(), entry.getValue());
        }
        return getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod);
    }

    public static String getJwtWithCustomClaimsTransformer(ApplicationDTO applicationDTO, JSONObject jwtTokenInfo,
                                                           String keyType, int validityPeriod,
                                                           Map<String, Object > claims)
            throws Exception {
        for(Map.Entry<String, Object> entry : claims.entrySet()) {
            jwtTokenInfo.put(entry.getKey(), entry.getValue());
        }
        return getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod);
    }
}
