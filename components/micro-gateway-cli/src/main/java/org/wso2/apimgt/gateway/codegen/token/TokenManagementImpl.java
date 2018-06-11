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
package org.wso2.apimgt.gateway.codegen.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.wso2.apimgt.gateway.codegen.cmd.GatewayCliConstants;
import org.wso2.apimgt.gateway.codegen.cmd.GatewayCmdUtils;
import org.wso2.apimgt.gateway.codegen.config.TOMLConfigParser;
import org.wso2.apimgt.gateway.codegen.config.bean.Config;
import org.wso2.apimgt.gateway.codegen.exception.ConfigParserException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TokenManagementImpl implements TokenManagement {

    @Override
    public String generateAccessToken(String username, char[] password) {
        URL url;
        HttpsURLConnection urlConn = null;
        //calling token endpoint
        try {
            Config config = GatewayCmdUtils.getConfig();
            String clientId = config.getToken().getClientId();
            String clientSecret = GatewayCmdUtils.decrypt(config.getToken().getClientSecret(), new String(password));
            url = new URL(config.getToken().getTokenEndpoint());
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String clientEncoded = DatatypeConverter.printBase64Binary(
                    (clientId + ':' + new String(clientSecret)).getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            String postParams = "grant_type=password&username=" + username + "&password=" + new String(password);
            postParams += "&scope=" + TokenManagementConstants.REQUESTED_TOKEN_SCOPE;
            urlConn.getOutputStream().write((postParams).getBytes("UTF-8"));
            System.out.println(postParams);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = getResponseString(urlConn.getInputStream());
                JsonNode rootNode = mapper.readTree(responseStr);
                String accessToken = rootNode.path("access_token").asText();
                return accessToken;
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
    }

    @Override
    public String generateClientIdAndSecret(Config config, String root, char[] password) {
        URL url;
        HttpURLConnection urlConn = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode application = mapper.createObjectNode();
            application.put(TokenManagementConstants.CALLBACK_URL, TokenManagementConstants.APPLICATION_CALLBACK_URL);
            application.put(TokenManagementConstants.CLIENT_NAME, TokenManagementConstants.APPLICATION_NAME);
            application.put(TokenManagementConstants.TOKEN_SCOPE, TokenManagementConstants.REQUESTED_TOKEN_SCOPE);
            application.put(TokenManagementConstants.OWNER, "admin");
            application.put(TokenManagementConstants.GRANT_TYPE, TokenManagementConstants.PASSWORD_GRANT_TYPE);
            System.out.println(application.toString());

            // Calling DCR endpoint
            String dcrEndpoint = config.getToken().getRegistrationEndpoint();
            url = new URL(dcrEndpoint);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            urlConn.setRequestProperty("Content-Type", "application/json");
            String clientEncoded = DatatypeConverter.printBase64Binary(("admin" + ':' + "admin")
                    .getBytes(StandardCharsets.UTF_8));
            urlConn.setRequestProperty("Authorization", "Basic " + clientEncoded);
            urlConn.getOutputStream().write((application.toString()).getBytes("UTF-8"));
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {  //If the DCR call is success
                String responseStr = getResponseString(urlConn.getInputStream());
                JsonNode rootNode = mapper.readTree(responseStr);
                JsonNode clientIdNode = rootNode.path("clientId");
                JsonNode clientSecretNode = rootNode.path("clientSecret");
                String clientId = clientIdNode.asText();
                String clientSecret = clientSecretNode.asText();
                String encryptedSecret = GatewayCmdUtils.decrypt(clientSecret, new String(password));
                config.getToken().setClientSecret(encryptedSecret);
                config.getToken().setClientId(clientId);
                String configPath = GatewayCmdUtils.getMainConfigLocation(root);
                TOMLConfigParser.write(configPath, config);
                GatewayCmdUtils.setConfig(config);
            } else { //If DCR call fails
                throw new RuntimeException("DCR call failed. Status code: " + responseCode);
            }
        } catch (IOException | ConfigParserException e) {
            String errorMsg = "Can not create OAuth application  : ";
            throw new RuntimeException(errorMsg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return null;
    }

    private static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String file = "";
            String str;
            while ((str = buffer.readLine()) != null) {
                file += str;
            }
            return file;
        }
    }
}
