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

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

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
}
