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

package org.wso2.choreo.connect.tests.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.common.model.SubscribedApiDTO;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Util class for the token generation.
 */
public class TokenUtil {

    public static String getBasicJWT(ApplicationDTO applicationDTO, JSONObject jwtTokenInfo, String keyType,
            int validityPeriod, String scopes) throws Exception {

        jwtTokenInfo.put("aud", "http://org.wso2.apimgt/gateway");
        jwtTokenInfo.put("sub", "admin");
        if (scopes != null) {
            jwtTokenInfo.put("scope", scopes);
        }
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
        String jksPath = TokenUtil.class.getClassLoader().getResource("keystore/wso2carbon.jks").getPath();
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

    public static String getInternalKey(JSONObject jwtTokenInfo, String keyType, int validityPeriod) throws Exception {
        jwtTokenInfo.put("sub", "admin");
        jwtTokenInfo.put("iss", "https://localhost:9443/oauth2/token");
        jwtTokenInfo.put("keytype", keyType);
        jwtTokenInfo.put("iat", System.currentTimeMillis());
        jwtTokenInfo.put("exp", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + validityPeriod);
        jwtTokenInfo.put("jti", UUID.randomUUID());
        jwtTokenInfo.put("token_type", "InternalKey");
        String payload = jwtTokenInfo.toString();

        JSONObject head = new JSONObject();
        head.put("alg", "RS256");
        head.put("kid", "gateway_certificate_alias");

        String header = head.toString();
        String base64UrlEncodedHeader = Base64.getUrlEncoder()
                .encodeToString(header.getBytes(Charset.defaultCharset()));
        String base64UrlEncodedBody = Base64.getUrlEncoder().encodeToString(payload.getBytes(Charset.defaultCharset()));

        Signature signature = Signature.getInstance("SHA256withRSA");
        String jksPath = TokenUtil.class.getClassLoader().getResource("keystore/wso2carbon.jks").getPath();
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
        return getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod, null);
    }

    public static String getJwtWithCustomClaimsTransformer(ApplicationDTO applicationDTO, JSONObject jwtTokenInfo,
                                                           String keyType, int validityPeriod,
                                                           Map<String, Object > claims)
            throws Exception {
        for(Map.Entry<String, Object> entry : claims.entrySet()) {
            jwtTokenInfo.put(entry.getKey(), entry.getValue());
        }
        return getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod, null);
    }

    /**
     * get a jwt token.
     *
     * @param api            api
     * @param applicationDTO application dto
     * @param tier           tier
     * @param keyType        keytype
     * @param validityPeriod validityPeriod
     * @throws Exception
     * @return JWT
     */
    public static String getJWT(API api, ApplicationDTO applicationDTO, String tier, String keyType,
                                int validityPeriod, String scopes, boolean isInternalKey) throws Exception {
        SubscribedApiDTO subscribedApiDTO = new SubscribedApiDTO();
        if (!api.getContext().startsWith("/")) {
            api.setContext("/" + api.getContext());
        }
        subscribedApiDTO.setContext(api.getContext());
        subscribedApiDTO.setName(api.getName());
        subscribedApiDTO.setVersion(api.getVersion());
        subscribedApiDTO.setPublisher("admin");

        subscribedApiDTO.setSubscriptionTier(tier);
        subscribedApiDTO.setSubscriberTenantDomain("carbon.super");

        JSONObject jwtTokenInfo = new JSONObject();
        jwtTokenInfo.put("subscribedAPIs", new JSONArray(Arrays.asList(subscribedApiDTO)));
        if (isInternalKey) {
            return TokenUtil.getInternalKey(jwtTokenInfo, keyType, validityPeriod);
        }
        return TokenUtil.getBasicJWT(applicationDTO, jwtTokenInfo, keyType, validityPeriod, scopes);
    }

    public static String getJwtForPetstore(String keyType, String scopes, boolean isInternalKey) throws Exception {
        API api = new API();
        api.setName("PetStoreAPI");
        api.setContext("v2");
        api.setProdEndpoint(Utils.getMockServiceURLHttp("/echo/prod"));
        api.setVersion("1.0.5");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        return getJWT(api, application, "Unlimited", keyType, 3600, scopes, isInternalKey);
    }
}
