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

package org.wso2.choreo.connect.tests.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpClient {

    /**
     * Send an HTTP POST request to a rest.
     *
     * @param endpoint - rest endpoint
     * @param postBody - message payload
     * @param headers  http request headers map
     * @return - status code
     * @throws IOException If an error occurs while sending the GET request
     */
    public static int doPost(String endpoint, String postBody, Map<String, String> headers)
            throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = getURLConnection(endpoint);
            setHeadersAndMethod(urlConnection, headers, "POST");
            try (OutputStream out = urlConnection.getOutputStream()) {
                Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                writer.write(postBody);
                writer.close();
            }
            return  urlConnection.getResponseCode();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
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

    private static void setHeadersAndMethod(HttpURLConnection conn, Map<String, String> headers, String method)
            throws ProtocolException {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.setRequestMethod(method);
    }
}
