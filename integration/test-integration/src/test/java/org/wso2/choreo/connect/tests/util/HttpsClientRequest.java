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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * This class can be used to send http request.
 */
public class HttpsClientRequest {
    private static final int maxRetryCount = 10;
    private static final int retryIntervalMillis = 3000;

    private static final Logger log = LoggerFactory.getLogger(HttpsClientRequest.class);
    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @param headers - http request header map
     * @return - HttpResponse from the end point
     * @throws CCTestException If an error occurs while sending the GET request
     */
    public static HttpResponse doGet(String requestUrl, Map<String, String> headers)
            throws CCTestException {
        HttpsURLConnection conn = null;
        try {
            conn = getURLConnection(requestUrl);
            setHeadersAndMethod(conn, headers, TestConstant.HTTP_METHOD_GET);
            conn.connect();
            return buildResponse(conn);
        } catch (IOException | CCTestException e) {
            throw new CCTestException("Error while sending GET request URL:" + requestUrl, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
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

    private static boolean shouldRetry(int retryCount) {
        if(retryCount >= maxRetryCount) {
            log.info("Retrying of the request is finished");
            return false;
        }
        Utils.delay(retryIntervalMillis, "Interrupted while waiting for endpoint to be available");
        return true;
    }

    /**
     * Send an HTTP POST request to a rest.
     *
     * @param endpoint - rest endpoint
     * @param postBody - message payload
     * @param headers http request headers map
     * @return - HttpResponse from end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static HttpResponse doPost(String endpoint, String postBody, Map<String, String> headers)
            throws CCTestException {
        HttpsURLConnection urlConnection = null;
        OutputStream outputStream = null;
        Writer writer = null;
        try {
            urlConnection = getURLConnection(endpoint);
            setHeadersAndMethod(urlConnection, headers, TestConstant.HTTP_METHOD_POST);
            outputStream = urlConnection.getOutputStream();

            writer = new OutputStreamWriter(outputStream, TestConstant.CHARSET_NAME);
            writer.write(postBody);
            writer.close();
            outputStream.close();
            // Get the response
            return buildResponse(urlConnection);
        } catch (IOException | CCTestException e) {
            throw new CCTestException("Error while sending GET request URL:" + endpoint, e);
        }finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(outputStream);
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }


    private static HttpsURLConnection getURLConnection(String requestUrl)
            throws IOException {
        setSSlSystemProperties();
        URL url = new URL(requestUrl);

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
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
