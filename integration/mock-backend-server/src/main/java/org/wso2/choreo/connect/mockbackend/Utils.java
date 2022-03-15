/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.mockbackend;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.lang3.StringUtils;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    // echo sends request headers in response headers and request body in response body
    public static void echo(HttpExchange exchange) throws IOException {
        byte[] response;
        String requestBody = Utils.requestBodyToString(exchange);
        response = requestBody.getBytes();
        exchange.getResponseHeaders().putAll(exchange.getRequestHeaders());
        int respCode = response.length == 0 ? HttpURLConnection.HTTP_NO_CONTENT : HttpURLConnection.HTTP_OK;
        exchange.sendResponseHeaders(respCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    // echo request body, request headers in echo response payload
    public static void echoFullRequest(HttpExchange exchange) throws IOException {
        EchoResponse echoResponse = new EchoResponse();
        echoResponse.setData(Utils.requestBodyToString(exchange));
        echoResponse.setHeaders(exchange.getRequestHeaders());
        echoResponse.setPath(exchange.getRequestURI().getPath());
        echoResponse.setMethod(exchange.getRequestMethod());

        // queries
        String queries = exchange.getRequestURI().getQuery();
        Map<String, String> queryMap;
        if (StringUtils.isNotEmpty(queries)) {
            queryMap = Arrays.stream(queries.split(Constants.HTTP_QUERY_SEPARATOR))
                    .map(q -> q.split(Constants.HTTP_QUERY_KEY_VAL_SEPARATOR))
                    .collect(Collectors.toMap(q -> q[0], q -> q[1]));
        } else {
            queryMap = Collections.emptyMap();
        }
        echoResponse.setQuery(queryMap);

        Gson gson = new Gson();
        byte[] response = gson.toJson(echoResponse).getBytes();
        int respCode = response.length == 0 ? HttpURLConnection.HTTP_NO_CONTENT : HttpURLConnection.HTTP_OK;
        respondWithBodyAndClose(respCode, response, exchange);
    }

    public static String requestBodyToString(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        InputStreamReader isReader = new InputStreamReader(inputStream);
        //Creating a BufferedReader object
        BufferedReader reader = new BufferedReader(isReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
            sb.append(str);
        }
        return sb.toString();
    }

    public static void respondWithBodyAndClose(int statusCode, byte[] response, HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set(Constants.CONTENT_TYPE,
                Constants.CONTENT_TYPE_APPLICATION_JSON);
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static void send404NotFound(HttpExchange exchange) throws IOException {
        byte[] response = "{\"status\":\"404 Resource Not Found\"}".getBytes();
        exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    public static void send200OK(HttpExchange exchange) throws IOException {
        byte[] response = "{\"status\":\"OK\"}".getBytes();
        exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
        exchange.getResponseBody().write(response);
    }

    public static TrustManager[] getTrustManagers() throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mg.pem");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert = (X509Certificate)cf.generateCertificate(inputStream);

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null); // Don't need the KeyStore instance to come from a file.
        ks.setCertificateEntry("caCert", caCert);

        tmf.init(ks);
        return tmf.getTrustManagers();
    }
    // TODO: close input streams
    public static KeyManager[] getKeyManagers(String keystoreName, String password) throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(keystoreName);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(inputStream, password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, password.toCharArray());
        return kmf.getKeyManagers();
    }
}
