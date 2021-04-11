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

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


/**
 * This class can be used to send http post multipart request.
 */
public class HttpPostMultipart {
    private final String boundary;
    private HttpURLConnection conn;
    private OutputStream outputStream;
    private PrintWriter writer;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param headers
     * @throws IOException
     */
    public HttpPostMultipart(String requestURL, Map<String, String> headers) throws IOException {

        boundary = UUID.randomUUID().toString();
        URL url = new URL(requestURL);
        conn = (HttpsURLConnection) url.openConnection();

        conn.setUseCaches(false);
        conn.setDoOutput(true);    // indicates POST method
        conn.setDoInput(true);
        conn.setReadTimeout(30000);
        conn.setConnectTimeout(15000);
        conn.setDoInput(true);

        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (headers != null && headers.size() > 0) {
            Iterator<String> it = headers.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = headers.get(key);
                conn.setRequestProperty(key, value);
            }
        }
        outputStream = conn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, TestConstant.CHARSET_NAME), true);
    }


    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(TestConstant.LINE);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(TestConstant.LINE);
        writer.append("Content-Type: text/plain; charset=" + TestConstant.CHARSET_NAME).append(TestConstant.LINE);
        writer.append(TestConstant.LINE);
        writer.append(value).append(TestConstant.LINE);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName
     * @param uploadFile
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(TestConstant.LINE);
        writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").
                append(TestConstant.LINE);
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(TestConstant.LINE);
        writer.append("Content-Transfer-Encoding: binary").append(TestConstant.LINE);
        writer.append(TestConstant.LINE);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
        writer.append(TestConstant.LINE);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @throws IOException
     */
    public HttpResponse getResponse() throws IOException {
        writer.flush();
        writer.append("--" + boundary + "--").append(TestConstant.LINE);
        writer.close();

        return HttpClientRequest.buildResponse(conn);
    }
}
