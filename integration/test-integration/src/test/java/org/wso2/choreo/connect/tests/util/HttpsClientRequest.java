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

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class can be used to send http request.
 */
public class HttpsClientRequest {
    private static final int maxRetryCount = 10;
    private static final int retryIntervalMillis = 3000;

    private static final Logger log = LoggerFactory.getLogger(HttpsClientRequest.class);

    /**
     * Sends an HTTP Mutual SSL GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @param headers- http request header value for authorization key
     * @param keyStoreName - Keystore name for mutual ssl
     * @return - HttpResponse from the end point
     * @throws CCTestException If an error occurs while sending the GET request
     */
    public static CloseableHttpResponse doMutualSSLGet(String requestUrl, Map<String, String> headers, String keyStoreName)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException,
            KeyManagementException, IOException, CCTestException {

        String keyStorePath = Objects.requireNonNull(HttpsClientRequest.class.getClassLoader()
                .getResource("keystore/" + keyStoreName)).getPath();
        String trustStorePath = Objects.requireNonNull(HttpsClientRequest.class.getClassLoader()
                .getResource("keystore/client-truststore.jks")).getPath();
        CloseableHttpClient httpclient;

        // Get the keystore
        KeyStore keyStore = KeyStore.getInstance("jks");
        InputStream ksIn = new FileInputStream(keyStorePath);
        keyStore.load(ksIn, "password".toCharArray());
        ksIn.close();
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "password".toCharArray());

        // Get the truststore
        KeyStore trustStore = KeyStore.getInstance("jks");
        InputStream tsIn = new FileInputStream(trustStorePath);
        trustStore.load(tsIn, "wso2carbon".toCharArray());
        tsIn.close();
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // Create the SSL configs
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(ssl);

        // Create the request using org.apache.http classes
        try {
            int timeout = 7;
            RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 10000)
                    .setConnectionRequestTimeout(timeout * 10000).setSocketTimeout(timeout * 10000).build();

            httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).disableRedirectHandling().setDefaultRequestConfig(config)
                    .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
            HttpGet request = new HttpGet(requestUrl);
            if (headers != null) {
                for (Map.Entry<String, String> head : headers.entrySet()) {
                    request.addHeader(head.getKey(), head.getValue());
                }
            }
            CloseableHttpResponse responseBuilder =  httpclient.execute(request);
            return responseBuilder;
        } catch (Exception e) {
            throw new CCTestException("Error while sending GET request URL:" + requestUrl, e);
        }
    }
    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "https://github.com/wso2/product-microgateway/releases?q=latest")
     * @param headers - http request header map
     * @return - HttpResponse from the end point
     * @throws CCTestException If an error occurs while sending the GET request
     */
    public static HttpResponse doGet(String requestUrl, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithoutPayload(TestConstant.HTTP_METHOD_GET, requestUrl, headers, true);
    }

    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "https://github.com/wso2/product-microgateway/releases?q=latest")
     * @return - HttpResponse from the end point
     * @throws CCTestException If an error occurs while sending the GET request
     */
    public static HttpResponse doGet(String requestUrl) throws CCTestException {
        return doGet(requestUrl, new HashMap<>());
    }

    public static HttpResponse retryGetRequestUntilDeployed(String requestUrl, Map<String, String> headers)
            throws CCTestException {
        HttpResponse response;
        int retryCount = 0;
        do {
            log.info("Trying request with url : " + requestUrl);
            response = HttpsClientRequest.doGet(requestUrl, headers);
            retryCount++;
        } while (response.getResponseCode() == 404 && response.getResponseMessage().contains("Not Found") &&
                shouldRetry(retryCount));
        return response;
    }

    public static HttpResponse retryUntil404(String requestUrl, Map<String, String> headers)
            throws CCTestException {
        HttpResponse response;
        int retryCount = 0;
        do {
            log.info("Trying request with url : " + requestUrl);
            response = HttpsClientRequest.doGet(requestUrl, headers);
            retryCount++;
        } while (response.getResponseCode() != 404 && shouldRetry(retryCount));
        return response;
    }

    private static boolean shouldRetry(int retryCount) {
        if(retryCount >= maxRetryCount) {
            log.info("Retrying of the request is finished");
            return false;
        }
        Utils.delay(retryIntervalMillis, "Interrupted while waiting for endpoint to be available");
        return true;
    }

    /**
     * Send an HTTP POST request.
     *
     * @param endpoint REST endpoint
     * @param payload  Request payload
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the POST request
     */
    public static HttpResponse doPost(String endpoint, String payload, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithPayload(TestConstant.HTTP_METHOD_POST, payload, headers, endpoint);
    }

    /**
     * Send an HTTP PUT request.
     *
     * @param endpoint REST endpoint
     * @param payload  Request payload
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the PUT request
     */
    public static HttpResponse doPut(String endpoint, String payload, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithPayload(TestConstant.HTTP_METHOD_PUT, payload, headers, endpoint);
    }

    /**
     * Send an HTTP PATCH request.
     *
     * @param endpoint REST endpoint
     * @param payload  Request payload
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the PATCH request
     */
    public static org.apache.http.HttpResponse doPatch(String endpoint, String payload, Map<String, String> headers)
            throws Exception {
        // HttpsURLConnection does not support PATCH. Therefore, org.apache.http classes are used here.
        int timeout = 7;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 10000)
                .setConnectionRequestTimeout(timeout * 10000).setSocketTimeout(timeout * 10000).build();
        CloseableHttpClient httpClient = HttpClients.custom().disableRedirectHandling()
                .setDefaultRequestConfig(config).setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                .build();
        HttpPatch httpPatch = new HttpPatch(endpoint);
        for (String headerType : headers.keySet()) {
            httpPatch.setHeader(headerType, headers.get(headerType));
        }
        if (null != payload) {
            HttpEntity httpEntity = new ByteArrayEntity(payload.getBytes(StandardCharsets.UTF_8));
            if (headers.get("Content-Type") == null) {
                httpPatch.setHeader("Content-Type", "application/json");
            }
            httpPatch.setEntity(httpEntity);
        }
        org.apache.http.HttpResponse httpResponse = httpClient.execute(httpPatch);
        return httpResponse;
    }

    /**
     * Send an HTTP DELETE request.
     *
     * @param endpoint REST endpoint
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the DELETE request
     */
    public static HttpResponse doDelete(String endpoint, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithoutPayload(TestConstant.HTTP_METHOD_DELETE, endpoint, headers, true);
    }

    /**
     * Send an HTTP OPTIONS request.
     *
     * @param endpoint REST endpoint
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the OPTIONS request
     */
    public static HttpResponse doOptions(String endpoint, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithoutPayload(TestConstant.HTTP_METHOD_OPTIONS, endpoint, headers, false);
    }

    /**
     * Send an HTTP HEAD request.
     *
     * @param endpoint REST endpoint
     * @param headers  Request headers map
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs during the HEAD request
     */
    public static HttpResponse doHead(String endpoint, Map<String, String> headers)
            throws CCTestException {
        return sendHttpRequestWithoutPayload(TestConstant.HTTP_METHOD_HEAD, endpoint, headers, false);
    }

    /**
     * Send any HTTP request that has a payload.
     *
     * @param httpVerb The HTTP verb Ex: POST, PUT, PATCH
     * @param payload  Request payload
     * @param headers  Request headers map
     * @param endpoint REST endpoint
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs while sending the POST request
     */
    public static HttpResponse sendHttpRequestWithPayload(String httpVerb, String payload, Map<String, String> headers,
                                                          String endpoint)
            throws CCTestException {
        HttpsURLConnection urlConnection = null;
        OutputStream outputStream = null;
        Writer writer = null;
        try {
            urlConnection = getURLConnection(endpoint, true);
            setHeadersAndMethod(urlConnection, headers, httpVerb);
            outputStream = urlConnection.getOutputStream();

            writer = new OutputStreamWriter(outputStream, TestConstant.CHARSET_NAME);
            writer.write(payload);
            writer.close();
            outputStream.close();
            // Get the response
            return buildResponse(urlConnection);
        } catch (IOException | CCTestException e) {
            throw new CCTestException("Error while sending "+ httpVerb +" request URL:" + endpoint, e);
        }finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(outputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Send any HTTP request that does not have a payload.
     *
     * @param httpVerb The HTTP verb Ex: DELETE, OPTIONS, HEAD
     * @param headers  Request headers map
     * @param endpoint REST endpoint
     * @return - HttpResponse from the endpoint
     * @throws CCTestException If an error occurs while sending the POST request
     */
    public static HttpResponse sendHttpRequestWithoutPayload(String httpVerb, String endpoint,
                                                             Map<String, String> headers, boolean doOutput)
            throws CCTestException {
        HttpsURLConnection conn = null;
        try {
            conn = getURLConnection(endpoint, doOutput);
            setHeadersAndMethod(conn, headers, httpVerb);
            conn.connect();
            return buildResponse(conn);
        } catch (IOException | CCTestException e) {
            throw new CCTestException("Error while sending " + httpVerb + " request URL:" + endpoint, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpsURLConnection getURLConnection(String requestUrl, boolean doOutput)
            throws IOException {

        setSSlSystemProperties();
        URL url = new URL(requestUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(doOutput);
        conn.setReadTimeout(70000);
        conn.setConnectTimeout(15000);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setHostnameVerifier((s, sslSession) -> true);
        conn.setAllowUserInteraction(false);
        return conn;
    }

    private static Map<String, String> readHeaders(URLConnection urlConnection) {
        Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
        Map<String, String> headers = new HashMap();
        while (itr.hasNext()) {
            String key = itr.next();
            if (key != null) {
                headers.put(key, urlConnection.getHeaderField(key));
            }
        }
        return headers;
    }

    /**
     * Helper method to set the SSL context.
     */
    static void setSSlSystemProperties() {
        String certificatesTrustStorePath = Objects.requireNonNull(HttpsClientRequest.class.getClassLoader()
                .getResource("keystore/client-truststore.jks")).getPath();
        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);
    }

    static HttpResponse buildResponse(HttpsURLConnection conn) throws CCTestException {
        HttpResponse httpResponse;
        int responseCode;
        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        try {
            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            throw new CCTestException("Error while connecting to the server", e);
        }

        try {
            if (responseCode < 400) {
                inputStreamReader = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
            } else {
                inputStreamReader = new InputStreamReader(conn.getErrorStream(), Charset.defaultCharset());
            }
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
            inputStreamReader.close();

            httpResponse = new HttpResponse(stringBuilder.toString(), responseCode, readHeaders(conn));
            httpResponse.setResponseMessage(conn.getResponseMessage());
            return httpResponse;
        } catch (IOException e) {
            throw new CCTestException("Error while reading response input stream", e);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
            IOUtils.closeQuietly(inputStreamReader);
        }
    }

    private static void setHeadersAndMethod(HttpsURLConnection connection, Map<String, String> headers, String method) throws ProtocolException {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            connection.setRequestProperty(e.getKey(), e.getValue());
        }
        connection.setRequestMethod(method);
    }

    public static Callable<Boolean> isResponseAvailable(String URL, Map<String, String> requestHeaders) {
        return () -> checkForResponse(URL, requestHeaders);
    }

    public static Callable<Boolean> isResourceURLAvailable(String URL, Map<String, String> requestHeaders) {
        return () -> checkIfResourceIsAvailable(URL, requestHeaders);
    }

    private static Boolean checkIfResourceIsAvailable(String URL, Map<String, String> requestHeaders) {
        HttpResponse response = null;
        try {
            response = HttpsClientRequest.doGet(URL, requestHeaders);
        } catch (CCTestException e) {
            return false;
        }
        int responseCode = response.getResponseCode();
        return responseCode == 200 || responseCode == 201 || responseCode == 401 || responseCode == 403;
    }

    private static Boolean checkForResponse(String URL, Map<String, String> requestHeaders) {
        try {
            HttpsClientRequest.doGet(URL, requestHeaders);
        } catch (CCTestException e) {
            return false;
        }
        //because the response from HttpsClientRequest.doGet only becomes null when an exception gets throws
        return true;
    }

    public static String requestTestKey() throws CCTestException {
        String encodedCredentials = "Basic YWRtaW46YWRtaW4=";
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), encodedCredentials);
        HttpResponse response;
        try {
            response = doPost(Utils.getServiceURLHttps("/testkey") ,"scope=read:pets",  headers);
        } catch (IOException e) {
            throw new CCTestException("Error while retrieving test key", e);
        }
        if (response.getResponseCode() == HttpStatus.SC_OK) {
            return response.getData() ;
        } else throw new CCTestException("Error retrieving test key, either Choreo Connect has not started properly"
            + "or invalid credentials");
    }
}
