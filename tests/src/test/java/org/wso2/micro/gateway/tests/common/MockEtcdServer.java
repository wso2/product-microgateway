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

import com.sun.net.httpserver.*;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;

/**
 * Mock http server for etcd
 */
public class MockEtcdServer extends Thread {
    private static final Logger log = LoggerFactory.getLogger(MockHttpServer.class);
    private HttpServer etcdServer;
    private String EtcdServerUrl;
    private int EtcdServerPort = -1;
    private String EtcdKVBasePath = "/v3alpha/kv";
    private String EtcdAuthBasePath = "/v3alpha/auth";
    private String EtcdNodeBasePath = "/v3alpha";
    private String etcdUsername = "etcd";
    private String etcdPassword = "etcd";
    Map<String, String> kvStore = new HashMap<String, String>();
    private int revision = 1;
    private int raft_term = 1;
    private UUID token;
    private UUID clusterID =  UUID.randomUUID();
    private UUID memberID = UUID.randomUUID();

    public static void main(String[] args) {

        MockEtcdServer mockEtcdServer = new MockEtcdServer(3379);
        mockEtcdServer.start();
    }

    public MockEtcdServer(int EtcdServerPort) {

        this.EtcdServerPort = EtcdServerPort;
        this.kvStore.put("pizzashackprod", "https://localhost:9443/echo/prod");
    }

    public void run() {

        if (EtcdServerPort < 0) {
            throw new RuntimeException("Server port is not defined");
        }
        try {
            etcdServer = HttpServer.create(new InetSocketAddress(EtcdServerPort), 0);
            etcdServer.createContext(EtcdAuthBasePath + "/authenticate", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String reqParameters = IOUtils.toString(exchange.getRequestBody());
                    JSONObject parameters = new JSONObject(reqParameters);
                    String reqName = parameters.getString("name");
                    String reqPassword = parameters.getString("password");
                    JSONObject payload = new JSONObject();
                    byte[] response;

                    if(reqName.equals(etcdUsername) && reqPassword.equals(etcdPassword))
                    {
                        JSONObject header = new JSONObject();
                        header.put("cluster_id", clusterID);
                        header.put("member_id", memberID);
                        header.put("revision", revision);
                        header.put("raft_term", raft_term);
                        payload.put("header", header);
                        token = UUID.randomUUID();
                        payload.put("token", token);

                        response = payload.toString().getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                        exchange.getResponseBody().write(response);
                    }
                    else
                    {
                        payload.put("error", "etcdserver: authentication failed, invalid user ID or password");
                        payload.put("code", 3);

                        response = payload.toString().getBytes();
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length);
                        exchange.getResponseBody().write(response);
                    }
                    exchange.close();
                }
            });
            etcdServer.createContext(EtcdKVBasePath + "/range", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String reqParameters = IOUtils.toString(exchange.getRequestBody());
                    JSONObject parameters = new JSONObject(reqParameters);
                    String reqKey = parameters.getString("key");
                    JSONObject payload = new JSONObject();

                    byte[] asBytes = Base64.getDecoder().decode(reqKey);
                    String keyString = new String(asBytes, "utf-8");
                    String value = kvStore.get(keyString);

                    JSONObject header = new JSONObject();
                    header.put("cluster_id", clusterID);
                    header.put("member_id", memberID);
                    header.put("revision", revision);
                    header.put("raft_term", raft_term);
                    payload.put("header", header);

                    if(value != null)
                    {
                        String encodedValue = Base64.getEncoder().encodeToString(value.getBytes("utf-8"));

                        JSONObject kv = new JSONObject();
                        kv.put("key",reqKey);
                        kv.put("create_revision",revision);
                        kv.put("mod_revision",revision);
                        kv.put("version","1");
                        kv.put("value", encodedValue);

                        JSONArray kvs = new JSONArray();
                        kvs.put(kv);

                        payload.put("kvs", kvs);
                        payload.put("count", "1");
                    }
                    byte[] response = payload.toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            etcdServer.createContext(EtcdKVBasePath + "/put", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String requestBody = IOUtils.toString(exchange.getRequestBody());
                    addKv(requestBody);

                    JSONObject payload = new JSONObject();
                    payload.put("message", "new key-value pair added");

                    byte[] response = payload.toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });
            etcdServer.createContext(EtcdKVBasePath + "/delete", new HttpHandler() {
                public void handle(HttpExchange exchange) throws IOException {
                    String requestBody = IOUtils.toString(exchange.getRequestBody());
                    deleteKv(requestBody);

                    JSONObject payload = new JSONObject();
                    payload.put("message", "key deleted");

                    byte[] response = payload.toString().getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                }
            });

            etcdServer.start();
            EtcdServerUrl = "http://localhost:" + EtcdServerPort;
        }
        catch (IOException e) {
            log.error("Error occurred while setting up mock server", e);
        }
        catch (Exception e) {
            log.error("Error occurred while setting up mock server", e);
        }
    }

    private void addKv(String requestBody)
    {
        String key = requestBody.split("=")[0];
        String value = requestBody.split("=")[1];
        kvStore.put(key, value);
    }

    private void deleteKv(String requestBody)
    {
        String key = requestBody;
        kvStore.remove(key);
    }
}