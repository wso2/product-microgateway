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

import org.apache.commons.lang3.StringUtils;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Util class for the token generation.
 */
public class TokenUtil {

    public static String getBasicJWT(ApplicationDTO applicationDTO, String keyType,
                                     int validityPeriod, JSONObject specificClaims) throws Exception {
        JSONObject jwtTokenInfo = populateBasicPayloadForToken(applicationDTO, keyType, validityPeriod,
                specificClaims);

        return createOauthToken(jwtTokenInfo);
    }

    private static JSONObject populateBasicPayloadForToken(ApplicationDTO applicationDTO, String keyType,
                                                           int validityPeriod, JSONObject specificClaims) {
        JSONObject jwtTokenInfo = new JSONObject();
        jwtTokenInfo.put("aud", "http://org.wso2.apimgt/gateway");
        jwtTokenInfo.put("sub", "admin");
        jwtTokenInfo.put("application", new JSONObject(applicationDTO));
        jwtTokenInfo.put("iss", "https://localhost:9443/oauth2/token");
        jwtTokenInfo.put("keytype", keyType);
        jwtTokenInfo.put("iat", System.currentTimeMillis());
        jwtTokenInfo.put("exp", (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + validityPeriod);
        jwtTokenInfo.put("jti", UUID.randomUUID());

        for (Iterator it = specificClaims.keys(); it.hasNext(); ) {
            String overriddenKey = (String) it.next();
            jwtTokenInfo.put(overriddenKey, specificClaims.get(overriddenKey));
        }
        return jwtTokenInfo;
    }

    private static String createOauthToken(JSONObject payloadJson) throws Exception {
        JSONObject head = new JSONObject();
        head.put("typ", "JWT");
        head.put("alg", "RS256");
        head.put("x5t", "UB_BQy2HFV3EMTgq64Q-1VitYbE");
        String header = head.toString();
        String payload = payloadJson.toString();

        return createJWT(header, payload);
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
        return createJWT(header, payload);
    }

    private static String createJWT(String header, String payload) throws Exception {
        Key privateKey = loadPrivateKey("keystore/wso2carbon.jks", "wso2carbon", "wso2carbon");

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign((PrivateKey) privateKey);

        String base64UrlEncodedHeader = Base64.getUrlEncoder()
                .encodeToString(header.getBytes(Charset.defaultCharset()));
        String base64UrlEncodedBody = Base64.getUrlEncoder().encodeToString(payload.getBytes(Charset.defaultCharset()));
        String assertion = base64UrlEncodedHeader + "." + base64UrlEncodedBody;
        byte[] dataInBytes = assertion.getBytes(StandardCharsets.UTF_8);
        signature.update(dataInBytes);
        //sign the assertion and return the signature
        byte[] signedAssertion = signature.sign();
        String base64UrlEncodedAssertion = Base64.getUrlEncoder().encodeToString(signedAssertion);
        return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;
    }

    private static Key loadPrivateKey(String keystorePath, String alias, String password) throws Exception {
        String jksPath = TokenUtil.class.getClassLoader().getResource(keystorePath).getPath();
        FileInputStream is = new FileInputStream(jksPath);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, password.toCharArray());
        Key key = keystore.getKey(alias, password.toCharArray());
        Key privateKey = null;
        if (key instanceof PrivateKey) {
            privateKey = key;
        }
        return privateKey;
    }

    /**
     * Get an OAuth JWT token.
     *
     * @param api            API info to include in subscribed API section
     * @param applicationDTO Application info
     * @param tier           Subscription tier to include in tierInfo
     * @param keyType        key type as PRODUCTION or SANDBOX
     * @param validityPeriod validity period
     * @param scopes         one or more scopes as space separated single string
     * @param isInternalKey  whether to create an InternalKey
     * @throws Exception     if an error occurs while loading keystore
     * @return               OAuth JWT token
     */
    public static String getJWT(API api, ApplicationDTO applicationDTO, String tier, String keyType,
                                int validityPeriod, String scopes, boolean isInternalKey) throws Exception {

        JSONObject specificClaims = new JSONObject();
        specificClaims.put("subscribedAPIs", new JSONArray(Arrays.asList(getSubscribedApiDTO(api, tier))));
        specificClaims.put("tierInfo", tierInfoJSONObject(tier));
        if (scopes != null) {
            specificClaims.put("scope", scopes);
        }

        if (isInternalKey) {
            return TokenUtil.getInternalKey(specificClaims, keyType, validityPeriod);
        }
        return TokenUtil.getBasicJWT(applicationDTO, keyType, validityPeriod, specificClaims);
    }

    /**
     * Get an OAuth JWT token with scopes as an array of strings.
     *
     * @param api            API info to include in subscribed API section
     * @param applicationDTO Application info
     * @param tier           Subscription tier to include in tierInfo
     * @param keyType        key type as PRODUCTION or SANDBOX
     * @param validityPeriod validity period
     * @param specificClaims non default or updated claims to include in the final JWT
     * @throws Exception     if an error occurs while loading keystore
     * @return               OAuth JWT token
     */
    public static String getJWT(API api, ApplicationDTO applicationDTO, String tier, String keyType,
                                int validityPeriod, JSONObject specificClaims) throws Exception {
        JSONObject jwtClaims;
        jwtClaims = Objects.requireNonNullElseGet(specificClaims, JSONObject::new);
        jwtClaims.put("subscribedAPIs", new JSONArray(Arrays.asList(getSubscribedApiDTO(api, tier))));
        jwtClaims.put("tierInfo", tierInfoJSONObject(tier));

        return TokenUtil.getBasicJWT(applicationDTO, keyType, validityPeriod, jwtClaims);
    }

    private static SubscribedApiDTO getSubscribedApiDTO(API api, String tier) {
        SubscribedApiDTO subscribedApiDTO = new SubscribedApiDTO();
        if (!StringUtils.startsWith(api.getContext(), "/")) {
            api.setContext("/" + api.getContext());
        }
        subscribedApiDTO.setContext(api.getContext());
        subscribedApiDTO.setName(api.getName());
        subscribedApiDTO.setVersion(api.getVersion());
        subscribedApiDTO.setPublisher("admin");

        subscribedApiDTO.setSubscriptionTier(tier);
        subscribedApiDTO.setSubscriberTenantDomain("carbon.super");
        return subscribedApiDTO;
    }

    private static JSONObject tierInfoJSONObject(String tier) {
        JSONObject tierInfoDTO = new JSONObject();
        JSONObject tierDTO = new JSONObject();
        tierDTO.put("stopOnQuotaReach", true);
        tierInfoDTO.put(tier, tierDTO);
        return tierInfoDTO;
    }

    public static String getJwtForPetstore(String keyType, String scopes, boolean isInternalKey) throws Exception {
        return getJwtForPetstoreWithDifferentContext(keyType, scopes, isInternalKey, "v2");
    }

    public static String getJwtForPetstoreWithDifferentContext(String keyType, String scopes, boolean isInternalKey,
                                                               String apiContext) throws Exception {
        API api = new API();
        api.setName("PetStoreAPI");
        api.setContext(apiContext);
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
