/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.TokenManagementConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.oauth.builder.DCRRequestBuilder;
import org.wso2.apimgt.gateway.cli.oauth.builder.OAuthTokenRequestBuilder;
import org.wso2.apimgt.gateway.cli.utils.TokenManagementUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;

public class OAuthServiceImpl implements OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthServiceImpl.class);

    /**
     * @see OAuthService#generateAccessToken(String, String, char[], String, String, boolean)
     */
    public String generateAccessToken(String tokenEndpoint, String username, char[] password, String clientId,
                                      String clientSecret, boolean inSecure) {

        URL url;
        HttpsURLConnection urlConn = null;
        try {
            url = new URL(tokenEndpoint);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setRequestMethod(TokenManagementConstants.POST);
            urlConn.setRequestProperty(TokenManagementConstants.CONTENT_TYPE,
                    TokenManagementConstants.CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED);
            String clientEncoded = DatatypeConverter
                    .printBase64Binary((clientId + ':' + clientSecret).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty(TokenManagementConstants.AUTHORIZATION,
                    TokenManagementConstants.BASIC + " " + clientEncoded);
            urlConn.setDoOutput(true);
            String postBody = new OAuthTokenRequestBuilder().setGrantType(TokenManagementConstants.PASSWORD)
                    .setPassword(password).setScopes(new String[] { TokenManagementConstants.POLICY_VIEW_TOKEN_SCOPE,
                            TokenManagementConstants.VIEW_API_SCOPE })
                    .setUsername(username).setValidityPeriod("3600000").
                            requestBody();
            urlConn.getOutputStream().write((postBody).getBytes(TokenManagementConstants.UTF_8));
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                JsonNode rootNode = mapper.readTree(responseStr);
                return rootNode.path(TokenManagementConstants.ACCESS_TOKEN).asText();
            } else {
                throw new CLIInternalException("Error occurred while getting the token. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String serverUrl = getServerUrl(tokenEndpoint);
            throw new CLIRuntimeException(
                    "Error occurred while trying to connect with server. Is the server running at " + serverUrl + "?",
                    "Error occurred while trying to connect with token endpoint: " + tokenEndpoint, 1,
                    e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    /**
     * @see OAuthService#generateClientIdAndSecret(String, String, char[], boolean)
     */
    public String[] generateClientIdAndSecret(String dcrEndpoint, String username, char[] password, boolean inSecure) {

        URL url;
        HttpsURLConnection urlConn = null;
        try {
            String requestBody = new DCRRequestBuilder()
                    .setCallbackUrl(TokenManagementConstants.APPLICATION_CALLBACK_URL)
                    .setClientName(TokenManagementConstants.APPLICATION_NAME).setOwner(username).setSaasApp(true)
                    .setGrantTypes(new String[]{TokenManagementConstants.PASSWORD_GRANT_TYPE})
                    .setTokenScope(TokenManagementConstants.TOKEN_SCOPE_PRODUCTION).requestBody();
            ObjectMapper mapper = new ObjectMapper();
            url = new URL(dcrEndpoint);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setRequestMethod(TokenManagementConstants.POST);
            urlConn.setRequestProperty(TokenManagementConstants.CONTENT_TYPE,
                    TokenManagementConstants.CONTENT_TYPE_APPLICATION_JSON);
            urlConn.setDoOutput(true);
            String clientEncoded = DatatypeConverter
                    .printBase64Binary((username + ':' + new String(password)).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty(TokenManagementConstants.AUTHORIZATION,
                    TokenManagementConstants.BASIC + " " + clientEncoded);
            urlConn.getOutputStream().write(requestBody.getBytes(TokenManagementConstants.UTF_8));
            logger.debug("DCR URL: {}", dcrEndpoint);
            logger.trace("Request body for DCR call: {}", requestBody);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 201 || responseCode == 200) {  //If the DCR call is success
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                logger.debug("Received response status code for DCR call: {}", responseCode);
                logger.trace("Received response body for DCR call: {}", responseStr);
                JsonNode rootNode = mapper.readTree(responseStr);
                JsonNode clientIdNode = rootNode.path("client_id");
                JsonNode clientSecretNode = rootNode.path("client_secret");
                String clientId = clientIdNode.asText();
                String clientSecret = clientSecretNode.asText();
                String[] clientInfo = {clientId, clientSecret};
                logger.debug("Successfully received client id:{} from DCR endpoint", clientId);
                return clientInfo;
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
            } else { //If DCR call fails
                throw new CLIInternalException(
                        "Error occurred while creating oAuth application Status code: " + responseCode);
            }
        } catch (IOException e) {
            String serverUrl = getServerUrl(dcrEndpoint);
            throw new CLIRuntimeException(
                    "Error occurred while trying to connect with server. Is the server running at " + serverUrl + "?",
                    "Error occurred while communicate with DCR endpoint: " + dcrEndpoint, 1,
                    e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    private String getServerUrl(String dcrEndpoint) {

        String[] serverUrlParts = dcrEndpoint.split("/", 4);
        return String.join("/",
                Arrays.copyOfRange(serverUrlParts, 0, serverUrlParts.length >= 3 ? 3 : serverUrlParts.length));
    }
}
