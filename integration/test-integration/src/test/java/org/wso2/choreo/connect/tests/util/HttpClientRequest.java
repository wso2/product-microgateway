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

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class can be used to send http request.
 */
public class HttpClientRequest {
    private static final int maxRetryCount = 10;
    private static final int retryIntervalMillis = 3000;

    private static final Logger log = LoggerFactory.getLogger(HttpClientRequest.class);

    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @param headers    - http request header map
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static HttpResponse doGet(String requestUrl, Map<String, String> headers)
            throws IOException {
        return executeRequestWithoutRequestBody(TestConstant.HTTP_METHOD_GET, requestUrl, headers);
    }

    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static HttpResponse doGet(String requestUrl) throws IOException {
        return doGet(requestUrl, new HashMap<String, String>());
    }

    /**
     * Send an HTTP POST request to a rest.
     *
     * @param endpoint - rest endpoint
     * @param postBody - message payload
     * @param headers  http request headers map
     * @return - HttpResponse from end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static HttpResponse doPost(String endpoint, String postBody, Map<String, String> headers)
            throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = getURLConnection(endpoint);
            setHeadersAndMethod(urlConnection, headers, TestConstant.HTTP_METHOD_POST);
            try (OutputStream out = urlConnection.getOutputStream()) {
                Writer writer = new OutputStreamWriter(out, TestConstant.CHARSET_NAME);
                writer.write(postBody);
                writer.close();
            }
            return buildResponse(urlConnection);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Send an HTTP PUT request to a rest.
     *
     * @param endpoint - rest endpoint
     * @param putBody - message payload
     * @param headers  http request headers map
     * @return - HttpResponse from end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static HttpResponse doPut(String endpoint, String putBody, Map<String, String> headers)
            throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = getURLConnection(endpoint);
            setHeadersAndMethod(urlConnection, headers, TestConstant.HTTP_METHOD_PUT);
            try (OutputStream out = urlConnection.getOutputStream()) {
                Writer writer = new OutputStreamWriter(out, TestConstant.CHARSET_NAME);
                writer.write(putBody);
                writer.close();
            }
            return buildResponse(urlConnection);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Sends an HTTP DELETE request to a url.
     * @param endpoint - The endpoint of the resource
     * @param queryParams - Query parameters to be included in the url
     * @param headers - HTTP request headers map
     * @return HttpResponse
     * @throws IOException If an error occurs while sending the DELETE request
     */
    public static HttpResponse doDelete(String endpoint, Map<String, String> queryParams, Map<String, String> headers)
            throws IOException {
        headers.put("Accept", "application/json");

        List<BasicNameValuePair> nameValuePairs = queryParams.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        String params = URLEncodedUtils.format(nameValuePairs, TestConstant.CHARSET_NAME);
        String requestUrl = endpoint + "?" + params;
        return executeRequestWithoutRequestBody(TestConstant.HTTP_METHOD_DELETE, requestUrl, headers);
    }

    /**
     * Sends an HTTP OPTIONS request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @param headers    http request headers map
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the OPTIONS request
     */
    public static HttpResponse doOptions(String requestUrl, Map<String, String> headers) throws IOException {
        return executeRequestWithoutRequestBody(TestConstant.HTTP_METHOD_OPTIONS, requestUrl, headers);
    }

    /**
     * Sends an HTTP HEAD request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the HEAD request
     */
    public static HttpResponse doHead(String requestUrl) throws IOException {
        return doHead(requestUrl, new HashMap<String, String>());
    }

    /**
     * Sends an HTTP HEAD request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example: "http://www.yahoo.com/search?params=value")
     * @param headers    - http request header map
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the HEAD request
     */
    public static HttpResponse doHead(String requestUrl, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = null;
        HttpResponse httpResponse;
        try {
            conn = getURLConnection(requestUrl);
            setHeadersAndMethod(conn, headers, TestConstant.HTTP_METHOD_HEAD);
            conn.connect();
            httpResponse = new HttpResponse(null, conn.getResponseCode());
            httpResponse.setHeaders(readHeaders(conn));
            httpResponse.setResponseMessage(conn.getResponseMessage());
            return httpResponse;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static HttpResponse executeRequestWithoutRequestBody(String method, String requestUrl, Map<String
            , String> headers) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = getURLConnection(requestUrl);
            setHeadersAndMethod(conn, headers, method);
            conn.connect();
            return buildResponse(conn);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpURLConnection getURLConnection(String requestUrl) throws IOException {
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setReadTimeout(30000);
        conn.setConnectTimeout(15000);
        conn.setDoInput(true);
        conn.setUseCaches(false);
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

    private static void setHeadersAndMethod(HttpURLConnection conn, Map<String, String> headers, String method)
            throws ProtocolException {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.setRequestMethod(method);
    }

    static HttpResponse buildResponse(HttpURLConnection conn) throws IOException {
        HttpResponse httpResponse;
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()
                    , Charset.defaultCharset()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ex) {
            if (conn.getErrorStream() == null) {
                //log.error("An IOException occurred", ex);
                return null;
            }
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()
                    , Charset.defaultCharset()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (rd != null) {
                rd.close();
            }
        }
        Map<String, String> responseHeaders = readHeaders(conn);
        httpResponse = new HttpResponse(sb.toString(), conn.getResponseCode(), responseHeaders);
        httpResponse.setResponseMessage(conn.getResponseMessage());
        return httpResponse;
    }

    public static HttpResponse retryPostRequestUntilDeployed(String requestUrl, Map<String, String> headers,
                                                             String body) throws CCTestException, InterruptedException {
        HttpResponse response;
        int retryCount = 0;
        do {
            log.info("Trying request with url : " + requestUrl);
            response = HttpsClientRequest.doPost(requestUrl, body, headers);
            retryCount++;
        } while (response != null && response.getResponseCode() == 404 && response.getResponseMessage()
                .contains("Not Found") && shouldRetry(retryCount));
        return response;
    }

    public static HttpResponse retryGetRequestUntilDeployed(String requestUrl, Map<String, String> headers)
            throws CCTestException, InterruptedException {
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

    private static boolean shouldRetry(int retryCount) throws InterruptedException {
        if(retryCount >= maxRetryCount) {
            log.info("Retrying of the request is finished");
            return false;
        }
        Thread.sleep(retryIntervalMillis);
        return true;
    }
}
