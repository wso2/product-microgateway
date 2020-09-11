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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Class to retrieve user claims from Key Manager component of the API Manager.
 */
public class SampleClaimRetriever extends AbstractMGWClaimRetriever {
    private static final Logger logger = LogManager.getLogger(AbstractMGWJWTGenerator.class);
    private SSLSocketFactory sslSocketFactory;
    private String username;
    private String password;
    private String serverUrl;

    public SampleClaimRetriever(String trustStorePath, String trustStoreLocation,
                                 Map<String, String> configurationMap) {
        super(trustStorePath, trustStoreLocation, configurationMap);
        //the following properties are populated by default
        this.username = configurationMap.get("username");
        this.password = configurationMap.get("password");
        this.serverUrl = configurationMap.get("serverUrl");
        sslSocketFactory = createSSLSocketFactory();
    }

    @Override
    public List<ClaimDTO> retrieveClaims(Map<String, Object> authContext) {
        try {
            HttpsURLConnection urlConn = null;
            String userInfoEndpoint = serverUrl + "/keymanager-operations/user-info/claims/generate";
            // Using `java.net.*` libs here to keep the simplicity and avoid the need of third party libs.
            // You can any third party http libs to do the same differently if you prefer
            URL url = new URL(userInfoEndpoint);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setSSLSocketFactory(sslSocketFactory);
            urlConn.setHostnameVerifier((s, sslSession) -> true);
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("POST");
            String credential = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            urlConn.setRequestProperty("Authorization", "Basic " + credential);
            urlConn.setRequestProperty("Content-Type", "application/json");
            String jsonInputString = "{\"username\": \"" +
                    removeTenantFromUserName(authContext.get("username").toString()) + "\"}";
            try (OutputStream os = urlConn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                String responseStr = getResponseString(urlConn.getInputStream());
                ObjectMapper mapper = new ObjectMapper();
                return (List<ClaimDTO>) mapper.readValue(responseStr, Map.class).get("list");
            }
            logger.error("Claim Retrieval request is failed with the response code : " + responseCode);
        } catch (IOException e) {
            logger.error("Error while retrieving user claims from remote endpoint", e);
        }
        return null;
    }


    /**
     * Get inputStream string as string.
     *
     * @param input input stream
     * @return inout stream content as string
     * @throws IOException if read went wrong
     */
    public static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String str;
            while ((str = buffer.readLine()) != null) {
                content.append(str);
            }
            return content.toString();
        }
    }

    private SSLSocketFactory createSSLSocketFactory() {
        SSLContext ctx = createSSLContext();
        if (ctx != null) {
            return ctx.getSocketFactory();
        }
        return null;
    }

    private SSLContext createSSLContext() {
        FileInputStream fileInputStream;
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            fileInputStream = new FileInputStream(this.getTrustStorePath());
            keyStore.load(fileInputStream, this.getTrustStorePassword() != null ?
                    this.getTrustStorePassword().toCharArray() : null);
            trustManagerFactory.init(keyStore);
            ctx.init(null, trustManagerFactory.getTrustManagers(), null);
            return ctx;
        } catch (NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException |
                KeyStoreException e) {
            logger.error("Error while creating the SSL Socket Factory for claim retriever Implementation", e);
            return null;
        }
    }

    private String removeTenantFromUserName(String username) {
        int lastIndexOfAt = username.lastIndexOf("@");
        return username.substring(0, lastIndexOfAt);
    }
}
