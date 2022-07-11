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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.tests.context.CCTestException;

/**
 * This class can be used to send http2 requests
 */
public class Http2ClientRequest {

    private static final int maxRetryCount = 10;
    private static final int retryIntervalMillis = 3000;

    private static final Logger log = LoggerFactory.getLogger(Http2ClientRequest.class);

    /**
     * Send an HTTP GET request.
     *
     * @param requestUrl - The URL of the rest. (Example:
     *                   "http://www.yahoo.com/search?params=value")
     * @param headers    Request headers map
     * @return - HttpResponse from the endpoint
     * @throws Exception If an error occurs during the GET request
     */
    public static java.net.http.HttpResponse<String> doGet(String endpoint, Map<String, String> headers)
            throws IOException, InterruptedException {
        // HttpsURLConnection does not support PATCH. Therefore, java.net.http classes
        // are used here.
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .uri(URI.create(endpoint)).GET();
        headers.forEach(requestBuilder::setHeader);
        return client.send(requestBuilder.build(), BodyHandlers.ofString());
    }

    /**
     * Sends an HTTP GET request to a url.
     *
     * @param requestUrl - The URL of the rest. (Example:
     *                   "http://www.yahoo.com/search?params=value")
     * @return - HttpResponse from the end point
     * @throws IOException If an error occurs while sending the GET request
     */
    public static java.net.http.HttpResponse<String> doGet(String requestUrl) throws Exception {
        return doGet(requestUrl, new HashMap<String, String>());
    }

    public static java.net.http.HttpResponse<String> retryGetRequestUntilDeployed(String requestUrl,
            Map<String, String> headers)
            throws CCTestException, InterruptedException, IOException {
        java.net.http.HttpResponse<String> response;
        int retryCount = 0;
        do {
            log.info("Trying request with url : " + requestUrl);
            response = doGet(requestUrl, headers);
            retryCount++;
        } while (response.statusCode() == 404 &&
                shouldRetry(retryCount));
        return response;
    }

    private static boolean shouldRetry(int retryCount) throws InterruptedException {
        if (retryCount >= maxRetryCount) {
            log.info("Retrying of the request is finished");
            return false;
        }
        Thread.sleep(retryIntervalMillis);
        return true;
    }
}
