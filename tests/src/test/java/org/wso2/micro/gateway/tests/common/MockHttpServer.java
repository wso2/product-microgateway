/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock http server for key-manager and APIM rest api endpoints
 */
public class MockHttpServer extends Thread {
    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpServer httpServer;
    private String KMServerUrl;
    private int KMServerPort = -1;
    private String DCRRestAPIBasePath = "/client-registration/v0.13";
    private String PubRestAPIBasePath = "/api/am/publisher/v0.13";
    private String AdminRestAPIBasePath = "/api/am/admin/v0.13";

    public static void main(String[] args) {
        MockHttpServer mockHttpServer = new MockHttpServer(9443);
        mockHttpServer.start();
    }

    public MockHttpServer(int KMServerPort) {
        this.KMServerPort = KMServerPort;
    }

    public void run() {
        if (KMServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress(KMServerPort), 0);
            httpServer.createContext(DCRRestAPIBasePath + "/register", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    JSONObject payload = new JSONObject();
                    payload.put("clientId", UUID.randomUUID());
                    payload.put("clientSecret", UUID.randomUUID());
                    payload.put("clientName", "admin_Micro Gateway Cli");
                    payload.put("callBackURL", "https://wso2.org");
                    payload.put("isSaasApplication", false);
                    payload.put("jsonString", "{\"grant_types\":\"password\"}");

                    byte[] response = payload.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/echo", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String payload = IOUtils.toString(exchange.getRequestBody());
                    byte[] response = payload.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext("/token", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    JSONObject payload = new JSONObject();
                    payload.put("access_token", UUID.randomUUID());
                    payload.put("refresh_token", UUID.randomUUID());
                    payload.put("scope", "apim:api_view apim:tier_view");
                    payload.put("token_type", "Bearer");
                    payload.put("expires_in", 3600);

                    byte[] response = payload.toString().getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext(PubRestAPIBasePath + "/apis", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String query = parseParas(exchange.getRequestURI()).get("query");
                    String[] paras = URLDecoder.decode(query, GatewayCliConstants.CHARSET_UTF8).split(" ");
                    String label = null;
                    for (String para : paras) {
                        String[] searchQuery = para.split(":");
                        if("label".equalsIgnoreCase(searchQuery[0])){
                            label = searchQuery[1];
                        }
                    }

                    if (!StringUtils.isEmpty(label)) {
                        byte[] response = MockAPIPublisher.getInstance().getAPIResponseForLabel(label).getBytes();
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.close();
                    } else {
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
                        exchange.close();
                    }

                }
            });
            httpServer.createContext("/services/APIKeyValidationService", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String xmlRequest = IOUtils.toString(exchange.getRequestBody());
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    String token = null;

                    try {
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(xmlRequest.toString().getBytes("UTF-8")));
                        token = doc.getElementsByTagName("xsd:accessToken").item(0).getTextContent();

                        byte[] xmlResponse = MockAPIPublisher.getInstance().getKeyValidationResponseForToken(token)
                                .getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "application/soap+xml");
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, xmlResponse.length);
                        exchange.getResponseBody().write(xmlResponse);
                    } catch (ParserConfigurationException e) {
                        log.error("Error occurred while parsing request", e);
                    } catch (SAXException e) {
                        log.error("Error occurred while parsing request", e);
                    }

                    byte[] xmlResponse = MockAPIPublisher.getInstance().getKeyValidationResponseForToken(token)
                            .getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/soap+xml");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, xmlResponse.length);
                    exchange.getResponseBody().write(xmlResponse);
                    exchange.close();

                }
            });
            httpServer.createContext(AdminRestAPIBasePath + "/throttling/policies/application", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    byte[] response = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("application-policies.json").getPath())).getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.createContext(AdminRestAPIBasePath + "/throttling/policies/subscription", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    byte[] response = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("subscription-policies.json").getPath()))
                            .getBytes();
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            httpServer.start();
            KMServerUrl = "http://localhost:" + KMServerPort;
        } catch (IOException e) {
            log.error("Error occurred while setting up mock server", e);
        }
    }

    public void stopIt() {
        httpServer.stop(0);
    }

    public String getKMServerUrl() {
        return KMServerUrl;
    }

    public void setKMServerUrl(String KMServerUrl) {
        this.KMServerUrl = KMServerUrl;
    }

    public int getKMServerPort() {
        return KMServerPort;
    }

    public void setKMServerPort(int KMServerPort) {
        this.KMServerPort = KMServerPort;
    }

    private Map<String, String> parseParas(URI uri) {
        String[] params = uri.getRawQuery().split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
}