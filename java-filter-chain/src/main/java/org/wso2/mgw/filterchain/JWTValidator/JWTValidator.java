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

package org.wso2.mgw.filterchain.JWTValidator;

import com.google.rpc.Code;
import com.google.rpc.Status;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.envoyproxy.envoy.service.auth.v2.CheckResponse;
import io.envoyproxy.envoy.service.auth.v2.DeniedHttpResponse;
import io.envoyproxy.envoy.service.auth.v2.OkHttpResponse;

public class JWTValidator{
    private static RSAPublicKey publicKey = readPublicKey();
    private static JWSVerifier jwsVerifier = new RSASSAVerifier(publicKey);
    private static String enableCache = (String) System.getenv("ENVOY_GW_CACHE_ENABLE");
    private static LoadingCache<String, String> GatewayApiKeyCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)                                     // maximum 100 tokens can be cached
                    .expireAfterAccess(30, TimeUnit.MINUTES)      // cache will expire after 30 minutes of access
                    .build(new CacheLoader<String, String>() {            // build the cacheloader
                        @Override
                        public String load(String s) throws Exception {
                            return JWTConstants.UNAVAILABLE ;
                        }

                    });
    private static LoadingCache<String, String> InvalidGatewayApiKeyCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)                                     // maximum 100 tokens can be cached
                    .expireAfterAccess(30, TimeUnit.MINUTES)      // cache will expire after 30 minutes of access
                    .build(new CacheLoader<String, String>() {            // build the cacheloader
                        @Override
                        public String load(String s) throws Exception {
                            return JWTConstants.UNAVAILABLE;
                        }

                    });
    //validate JWT token
    public static CheckResponse validateToken (Map<String, String> headers) {
        boolean valid = false;
        CheckResponse response;

        String token = headers.get(JWTConstants.AUTHORIZATION);
        //Map<String, String> request = new HashMap<String, String>();
        //request.put(JWTConstants.AUTHORIZATION, JWTConstants.JWT_TOKEN);
        valid = HandleJWT(token);

        if(valid) {
            response = CheckResponse.newBuilder()
                    .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
                    .setOkResponse(OkHttpResponse.newBuilder().build())
                    .build();
        } else {
            response = CheckResponse.newBuilder()
                    .setStatus(Status.newBuilder().setCode(Code.UNAUTHENTICATED_VALUE).build())
                    .setDeniedResponse(DeniedHttpResponse.newBuilder().build())
                    .build();
        }

        return response;
    }
    //handle JWT token
    public static boolean HandleJWT(String accessToken){
        //String accessToken = requestAttributes.get(JWTConstants.AUTHORIZATION);
        String[] tokenContent = accessToken.split("\\.");

        if(tokenContent.length != 3){
            //System.out.println("Invalid JWT token received, token must have 3 parts");
        }
        String signedContent = tokenContent[0] + "." + tokenContent[1];
        //System.out.println(signedContent);
        boolean isVerified = validateSignature(accessToken, tokenContent[2]);
        if(isVerified){
            //System.out.println("JWT Token is valid");
        } else {
            //System.out.println("JWT Token is not valid");
        }
        return isVerified;
    }

    // validate the signature
    public static boolean validateSignature(String jwtToken, String signature){
        JWSHeader header;
        JWTClaimsSet payload = null;
        SignedJWT parsedJWTToken;
        boolean isVerified = false;
        try {
            if (enableCache != null && enableCache.equals("true")) {
                if(GatewayApiKeyCache.get(signature) != JWTConstants.UNAVAILABLE){
                    //System.out.println("Api Key retrieved from the Api Key cache.");
                    isVerified = true;
                } else if (InvalidGatewayApiKeyCache.get(signature) != JWTConstants.UNAVAILABLE){
                    //System.out.println("Api Key retrieved from the invalid Api Key cache.");
                    isVerified = false;
                } else {
                    //System.out.println("Token is not available in the cache.");
                    try{
                        parsedJWTToken = (SignedJWT) JWTParser.parse(jwtToken);
                        isVerified = verifyTokenSignature(parsedJWTToken);
                        if (isVerified){
                            GatewayApiKeyCache.put(signature, JWTConstants.VALID);
                        } else {
                            InvalidGatewayApiKeyCache.put(signature, JWTConstants.INVALID);
                        }
                    }catch (ParseException e) {
                        //System.out.println("Invalid JWT token. Failed to decode the token.");
                    }
                }
            } else {
                try{
                    parsedJWTToken = (SignedJWT) JWTParser.parse(jwtToken);
                    isVerified = verifyTokenSignature(parsedJWTToken);
                }catch (ParseException e) {
                    //System.out.println("Invalid JWT token. Failed to decode the token.");
                }
            }
        } catch (Exception e) {
            //System.out.println(e);
        }
        return isVerified;
    }

    public static boolean verifyTokenSignature(SignedJWT parsedJWTToken) {
        boolean state =false;
        if (publicKey == null) {
            publicKey = readPublicKey();
        }
        if (publicKey != null){
            JWSAlgorithm algorithm = parsedJWTToken.getHeader().getAlgorithm();
            if (algorithm != null && (JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm) ||
                    JWSAlgorithm.RS384.equals(algorithm))) {
                try{
                    state = parsedJWTToken.verify(jwsVerifier);
                } catch (JOSEException e) {
                    //System.out.println(e);
                }
            }
        }
        return state;
    }

    public static RSAPublicKey readPublicKey() {
        try {
            String strKeyPEM = "";
            BufferedReader br = new BufferedReader(new FileReader("./src/main/java/org/wso2/mgw/filterchain/JWTValidator/wso2carbon.pem"));
            String line;
            while ((line = br.readLine()) != null) {
                strKeyPEM += line + "\n";
            }
            br.close();
            //System.out.println(strKeyPEM);
            strKeyPEM = strKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
            strKeyPEM = strKeyPEM.replaceAll(System.lineSeparator(), "");
            strKeyPEM = strKeyPEM.replace("-----END PUBLIC KEY-----", "");
            byte[] encoded = Base64.getDecoder().decode(strKeyPEM);
            KeyFactory kf = KeyFactory.getInstance(JWTConstants.RSA);
            publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (IOException | NoSuchAlgorithmException |InvalidKeySpecException e) {
            //System.out.println(e);
        }
        return publicKey;
    }
}
