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
package org.wso2.micro.gateway.tests.interceptor;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.interceptor.Caller;
import org.wso2.micro.gateway.interceptor.Interceptor;
import org.wso2.micro.gateway.interceptor.InterceptorException;
import org.wso2.micro.gateway.interceptor.Request;
import org.wso2.micro.gateway.interceptor.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Implements sample interceptor for the integration test cases.
 */
public class TestInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger("ballerina");
    private  String responseString = "";

    @Override
    public boolean interceptRequest(Caller caller, Request request) {
        responseString = "";
        String contentType = request.getHeader("content-type");
        appendResponseString(contentType);
        appendResponseString(getAllHeaders(request));
        appendResponseString(request.getRequestPath());
        appendResponseString(request.getRequestHttpMethod());
        appendResponseString(request.getRequestHttpVersion());
        appendResponseString(request.getQueryParams().toString());
        appendResponseString(request.getQueryParamValue("test"));
        if ("application/json".equals(contentType)) {
            try {
                if (request.hasHeader("X_JWT")) {
                    appendResponseString(request.getJsonArrayPayload().toString());
                } else {
                    appendResponseString(request.getJsonPayload().toString());
                }
            } catch (InterceptorException e) {
                log.error("Error while getting json payload ", e);
            }
        }

        if ("application/xml".equals(contentType) || "text/xml".equals(contentType)) {
            try {
                appendResponseString(request.getXmlPayload().toString());
            } catch (InterceptorException e) {
                log.error("Error while getting xml payload ", e);
            }
        }

        if ("application/x-www-form-urlencoded".equals(contentType)) {
            appendResponseString(getByteChannel(request));
        }

        respondFromRequest(caller);
        return false;
    }

    @Override
    public boolean interceptResponse(Caller caller, Response response) {
        String contentType = response.getHeader("content-type");
        Response response1 = new Response();
        response1.setResponseCode(201);
        response1.addHeader("test", "value1");
        response1.setHeader("content-type", contentType);
        JSONObject responseObject = new JSONObject();
        responseObject.put("name", "jon doe");
        responseObject.put("age", "22");
        responseObject.put("city", "chicago");
        response1.setJsonPayload(responseObject);
        caller.respond(response1);
        return false;
    }

    public String getAllHeaders(Request request) {
        String headerString = "";
        String[] headers = request.getHeaderNames();
        for (String heeader : headers) {
            headerString += heeader + ":";
        }
        return headerString;
    }

    public String getByteChannel(Request request) {
        try {
            ByteChannel byteChannel = request.getByteChannel();
            InputStream in = Channels.newInputStream(byteChannel);
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(
                    new InputStreamReader(in, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
                return textBuilder.toString();
            }
        } catch (InterceptorException | IOException e) {
            log.error("Error while reading the the byte channel", e);
        }
        return "";
    }

    public void respondFromRequest(Caller caller) {
        Response response = new Response();
        response.setTextPayload(responseString);
        caller.respond(response);
    }

    public  void appendResponseString(String response) {
        responseString += ":" + response;
    }
}
