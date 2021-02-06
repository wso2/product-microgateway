/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.jwt.generator;

import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for generate JWT for backend claims.
 */
public abstract class AbstractMGWJWTGenerator {
    private static final Logger logger = LogManager.getLogger(AbstractMGWJWTGenerator.class);
    private String dialectURI;
    private String signatureAlgorithm;
    private String keyStorePath;
    private String keyStorePassword;
    private String certificateAlias;
    private String privateKeyAlias;
    private int jwtExpiryTime;
    private ArrayList<String> restrictedClaims;
    private boolean cacheEnabled;
    private int cacheExpiry;
    private String tokenIssuer;
    private String[] tokenAudience;
    private Map<String, Object> apiDetails;
    private List<String> defaultRestrictedClaims;

    public AbstractMGWJWTGenerator(String dialectURI, String signatureAlgorithm, String keyStorePath,
                                   String keyStorePassword, String certificateAlias, String privateKeyAlias,
                                   int jwtExpiryTime, String[] restrictedClaims, boolean cacheEnabled,
                                   int cacheExpiry, String tokenIssuer, String[] tokenAudience) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.certificateAlias = certificateAlias;
        this.privateKeyAlias = privateKeyAlias;
        this.jwtExpiryTime = jwtExpiryTime;
        this.dialectURI = dialectURI;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cacheEnabled = cacheEnabled;
        this.cacheExpiry = cacheExpiry;
        this.tokenIssuer = tokenIssuer;
        this.tokenAudience = tokenAudience;
        this.restrictedClaims = new ArrayList<>(Arrays.asList(restrictedClaims));
        defaultRestrictedClaims = new ArrayList<>(Arrays.asList(MGWJWTGeneratorConstants.ISSUER_CLAIM,
                MGWJWTGeneratorConstants.SUB_CLAIM, MGWJWTGeneratorConstants.AUDIENCE_CLAIM,
                MGWJWTGeneratorConstants.EXP_CLAIM, MGWJWTGeneratorConstants.NBF_CLAIM,
                MGWJWTGeneratorConstants.IAT_CLAIM, MGWJWTGeneratorConstants.JTI_CLAIM,
                MGWJWTGeneratorConstants.APPLICATION_CLAIM, MGWJWTGeneratorConstants.TIER_INFO_CLAIM,
                MGWJWTGeneratorConstants.SUBSCRIBED_APIS_CLAIM, MGWJWTGeneratorConstants.KEY_TYPE_CLAIM));
        this.restrictedClaims.addAll(defaultRestrictedClaims);
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    public List<String> getDefaultRestrictedClaims() {
        return defaultRestrictedClaims;
    }

    public void setDefaultRestrictedClaims(List<String> defaultRestrictedClaims) {
        this.defaultRestrictedClaims = defaultRestrictedClaims;
    }

    public String getCertificateAlias() {
        return certificateAlias;
    }

    public void setCertificateAlias(String certificateAlias) {
        this.certificateAlias = certificateAlias;
    }

    public Map<String, Object> getApiDetails() {
        return apiDetails;
    }

    public void setApiDetails(Map<String, Object> apiDetails) {
        this.apiDetails = apiDetails;
    }

    public String[] getTokenAudience() {
        return tokenAudience;
    }

    public void setTokenAudience(String[] tokenAudience) {
        this.tokenAudience = tokenAudience;
    }

    public String getTokenIssuer() {
        return tokenIssuer;
    }

    public void setTokenIssuer(String tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheExpiry() {
        return cacheExpiry;
    }

    public void setCacheExpiry(int cacheExpiry) {
        this.cacheExpiry = cacheExpiry;
    }

    public ArrayList<String> getRestrictedClaims() {
        return restrictedClaims;
    }

    public void setRestrictedClaims(ArrayList<String> restrictedClaims) {
        this.restrictedClaims = restrictedClaims;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getDialectURI() {
        return dialectURI;
    }

    public void setDialectURI(String dialectURI) {
        this.dialectURI = dialectURI;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public int getJwtExpiryTime() {
        return jwtExpiryTime;
    }

    public void setJwtExpiryTime(int jwtExpiryTime) {
        this.jwtExpiryTime = jwtExpiryTime;
    }

    /**
     * Used to generate the JWT token.
     * @param jwtInfo - JWT Token payload
     * @return generated JWT token
     * @throws Exception - If an error occurred in building header
     */
    public String generateToken(Map<String, Object> jwtInfo) throws Exception {
        String jwtHeader = buildHeader();
        String jwtBody = buildBody(jwtInfo);
        String base64UrlEncodedHeader = "";
        if (jwtHeader != null) {
            base64UrlEncodedHeader = encode(jwtHeader.getBytes(Charset.defaultCharset()));
        }
        String base64UrlEncodedBody = "";
        if (jwtBody != null) {
            base64UrlEncodedBody = encode(jwtBody.getBytes());
        }
        if (MGWJWTGeneratorConstants.SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            String assertion = base64UrlEncodedHeader + '.' + base64UrlEncodedBody;
            //get the assertion signed
            byte[] signedAssertion = signJWT(assertion);
            String base64UrlEncodedAssertion = encode(signedAssertion);
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;
        } else {
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.';
        }
    }

    /**
     * Used to build the JWT header.
     * @return built JWT header
     * @throws Exception - If an error occurred when adding certificate to header
     */
    public String buildHeader() throws Exception {
        String jwtHeader = null;
        if (MGWJWTGeneratorConstants.NONE.equals(signatureAlgorithm)) {
            Map<String, String> jsonMap = new HashMap<>();
            jsonMap.put(MGWJWTGeneratorConstants.TOKEN_TYPE, MGWJWTGeneratorConstants.TOKEN_TYPE_JWT);
            jsonMap.put(MGWJWTGeneratorConstants.ALGORITHM, MGWJWTGeneratorConstants.NONE.toLowerCase());
            JSONObject jsonHeader = new JSONObject(jsonMap);
            jwtHeader = jsonHeader.toJSONString();
        } else if (MGWJWTGeneratorConstants.SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            jwtHeader = addCertToHeader();
        }
        return jwtHeader;
    }

    /**
     * Used to sign the JWT using the keystore.
     * @param assertion - asserted JWT header and body
     * @return byte array after the assertion is signed
     * @throws Exception - if an error occurred when the JWT token is signed by a key from the keystore
     */
    public byte[] signJWT(String assertion) throws Exception {
        FileInputStream is;
        is = new FileInputStream(keyStorePath);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, keyStorePassword.toCharArray());
        Key key = keystore.getKey(privateKeyAlias, keyStorePassword.toCharArray());
        Key privateKey = null;
        if (key instanceof PrivateKey) {
            privateKey = key;
        }
        //initialize signature with private key and algorithm
        Signature signature = Signature.getInstance(signatureAlgorithm);
        signature.initSign((PrivateKey) privateKey);
        //update signature with data to be signed
        byte[] dataInBytes = assertion.getBytes(Charset.defaultCharset());
        signature.update(dataInBytes);
        // close the file stream
        is.close();
        //sign the assertion and return the signature
        return signature.sign();
    }

    /**
     * Used to get the expiration time of the token.
     * @return cache expiry time or the expiry time from the configuration
     */
    public long getTTL() {
        if (cacheEnabled) {
            return cacheExpiry;
        } else {
            return jwtExpiryTime;
        }
    }

    /**
     * Used to add "ballerina" the certificate from the keystore to the header.
     * @return jwt header after the certificate is added
     * @throws Exception - if an error occurred while reading and adding the certificate into header
     */
    public String addCertToHeader() throws Exception {
        FileInputStream is;
        is = new FileInputStream(keyStorePath);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, keyStorePassword.toCharArray());
        Certificate publicCert = keystore.getCertificate(certificateAlias);

        //generate the SHA-1 thumbprint of the certificate
        MessageDigest digestValue = MessageDigest.getInstance("SHA-1");
        byte[] der = publicCert.getEncoded();
        digestValue.update(der);
        byte[] digestInBytes = digestValue.digest();
        String publicCertThumbprint = hexify(digestInBytes);
        String base64UrlEncodedThumbPrint;
        base64UrlEncodedThumbPrint = java.util.Base64.getUrlEncoder()
                .encodeToString(publicCertThumbprint.getBytes("UTF-8"));
        //Sample header
        //{"typ":"JWT", "alg":"SHA256withRSA", "x5t":"a_jhNus21KVuoFx65LmkW2O_l10"}
        //{"typ":"JWT", "alg":"[2]", "x5t":"[1]"}
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put(MGWJWTGeneratorConstants.TOKEN_TYPE, MGWJWTGeneratorConstants.TOKEN_TYPE_JWT);
        jsonMap.put(MGWJWTGeneratorConstants.ALGORITHM, MGWJWTGeneratorConstants.RS_256);
        jsonMap.put(MGWJWTGeneratorConstants.X5T_HEADER, base64UrlEncodedThumbPrint);
        JSONObject jwtHeader = new JSONObject(jsonMap);
        // close the file stream
        is.close();
        return jwtHeader.toJSONString();
    }

    /**
     * Used to build the body with claims.
     * @param jwtInfo - JWT token payload
     * @return JWT claim set after the claims are populated
     */
    public String buildBody(Map<String, Object> jwtInfo) {
        JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
        Map<String, Object> claims = populateStandardClaims(jwtInfo);
        Map<String, Object> customClaims = populateCustomClaims(jwtInfo, restrictedClaims);
        for (Map.Entry<String, Object> claimEntry : customClaims.entrySet()) {
            if (!claims.containsKey(claimEntry.getKey())) {
                claims.put(claimEntry.getKey(), claimEntry.getValue());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Claim key " + claimEntry.getKey() + " already exist");
                }
            }
        }
        for (Map.Entry<String, Object> claimEntry : claims.entrySet()) {
            jwtClaimSetBuilder.claim(claimEntry.getKey(), claimEntry.getValue());
        }
        JWTClaimsSet jwtClaimsSet = jwtClaimSetBuilder.build();
        return jwtClaimsSet.toJSONObject().toString();
    }

    /**
     * Used for base64 encoding.
     * @param stringToBeEncoded - the string required to be encoded
     * @return the encoded string
     */
    public String encode(byte[] stringToBeEncoded) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(stringToBeEncoded);
    }

    /**
     * Helper method to hexify a byte array.
     * @param bytes - the byte array required to be hexified
     * @return the hexified string
     */
    public String hexify(byte bytes[]) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Method to convert Java array to JSONArray.
     * @param objectArray - java object array to be converted
     * @return converted JSONArray
     */
    public JSONArray arrayToJSONArray(Object[] objectArray) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(Arrays.asList(objectArray));
        return jsonArray;
    }

    public abstract Map<String, Object> populateStandardClaims(Map<String, Object> jwtInfo);

    public abstract Map<String, Object> populateCustomClaims(Map<String, Object> jwtInfo,
                                                             ArrayList<String> restrictedClaims);
}
