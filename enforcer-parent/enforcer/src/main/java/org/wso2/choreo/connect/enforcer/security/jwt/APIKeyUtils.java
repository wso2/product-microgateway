/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.enforcer.security.jwt;

import com.google.gson.Gson;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * This class holds the utility methods related to API key functionalities.
 */
public class APIKeyUtils {

    private static final Logger log = LogManager.getLogger(APIKeyUtils.class);

    private static final Gson gson = new Gson();

    /**
     * Check if the provided API key is valid.
     *
     * @param apiKey    API Key
     * @return          True if valid.
     */
    public static boolean isValidAPIKey(String apiKey) {

        // Getting the API Key checksum.
        String checksum = apiKey.substring(apiKey.length() - 6);
        // Skipping the prefix(`chp_`) and checksum.
        String keyData = apiKey.substring(4, apiKey.length() - 6);
        String generatedChecksum = generateCRC32Checksum(keyData);
        return StringUtils.equals(checksum, generatedChecksum);
    }

    /**
     * Generate API Key Hash.
     *
     * @param apiKey    API Key
     * @return key hash
     */
    public static String generateAPIKeyHash(String apiKey) throws APISecurityException {

        try {
            // Skipping the prefix(`chp_`) and checksum.
            String keyData = apiKey.substring(4, apiKey.length() - 6);
            // Base 64 decode key data.
            String decodedKeyData = new String(Base64.getDecoder().decode(keyData));
            // Convert data into JSON.
            JSONObject jsonObject = (JSONObject) JSONValue.parse(decodedKeyData);
            // Extracting the key.
            String key = jsonObject.getAsString(APIKeyConstants.API_KEY_JSON_KEY);
            // Return SHA256 hash of the key.
            return DigestUtils.sha256Hex(key);
        } catch (Exception e) {
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS,
                    APISecurityConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
        }
    }

    /**
     * This function exchanges a given PAT hash to an JWT token.
     *
     * @param patHash    Key Hash
     * @return JWT corresponding to given PAT.
     */
    public static Optional<String> exchangePATToJWT(String patHash) {

        URL url = null;
        try {
            String patExchangeURL = String.format("%s%s", ConfigHolder.getInstance().getConfig()
                    .getApiKeyConfig().getOauthAgentURL(), APIKeyConstants.PAT_EXCHANGE_ENDPOINT);
            url = new URL(patExchangeURL);
        } catch (MalformedURLException e) {
            log.error("Error occurred while parsing OAuth agent URL", e);
            return Optional.empty();
        }
        try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol())) {
            // Create a request to exchange API key to JWT.
            HttpPost exchangeRequest = new HttpPost(url.toURI());
            exchangeRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
            exchangeRequest.setEntity(new StringEntity(createKeyHashExchangeRequest(patHash)));
            try (CloseableHttpResponse response = httpClient.execute(exchangeRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        OAuthAgentResponse resp = gson.fromJson(IOUtils.toString(content),
                                OAuthAgentResponse.class);
                        return Optional.of(resp.getAccessToken());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while exchanging API key to JWT", e);
        }
        return Optional.empty();
    }

    /**
     * Exchange a given API key ID to a JWT token.
     *
     * @param apiKeyId    API Key Hash + "#" + Target component ID.
     * @return JWT corresponding to given API Key.
     */
    public static Optional<String> exchangeAPIKeyToJWT(String apiKeyId) {

        URL url = null;
        try {
            String apiKeyExchangeURL = String.format("%s%s", ConfigHolder.getInstance().getConfig()
                    .getApiKeyConfig().getOauthAgentURL(), APIKeyConstants.API_KEY_EXCHANGE_ENDPOINT);
            url = new URL(apiKeyExchangeURL);
        } catch (MalformedURLException e) {
            log.error("Error occurred while parsing OAuth agent URL", e);
            return Optional.empty();
        }
        try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol())) {
            // Create a request to exchange API key to JWT.
            HttpPost exchangeRequest = new HttpPost(url.toURI());
            exchangeRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
            exchangeRequest.setEntity(new StringEntity(createKeyHashExchangeRequest(apiKeyId)));
            try (CloseableHttpResponse response = httpClient.execute(exchangeRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        OAuthAgentResponse resp = gson.fromJson(IOUtils.toString(content),
                                OAuthAgentResponse.class);
                        return Optional.of(resp.getAccessToken());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while exchanging API key to JWT", e);
        }
        return Optional.empty();
    }

    /**
     * Validate JWT expiry.
     *
     * @param jwt   JWT
     * @return      True if expired.
     */
    public static boolean isJWTExpired(String jwt) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            Date now = new Date();
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            return DateUtils.isBefore(exp, now, ConfigHolder.getInstance().getConfig()
                    .getPatConfig().getTokenExpirySkewSeconds());
        } catch (ParseException e) {
            log.debug("JWT token parsing failed");
        }
        return true;
    }

    private static String generateCRC32Checksum(String data) {

        CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes());
        long crcValue = crc32.getValue();
        byte[] checksumBytes = ByteBuffer.allocate(4).putInt((int) crcValue).array();
        return Base64.getEncoder().withoutPadding().encodeToString(checksumBytes);
    }

    private static String createKeyHashExchangeRequest(String keyHash) {
        Map<String, Object> patRequest = new HashMap<>();
        patRequest.put("apiKeyHash", keyHash);
        return gson.toJson(patRequest);
    }
}
