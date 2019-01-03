/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * This class performs required function related to Etcd.
 */
public class EtcdClient {
    private String etcdUrl;
    private String EtcdKVBasePath = "/v3alpha/kv";
    private String EtcdAuthBasePath = "/v3alpha/auth";
    private String EtcdUserBasePath = EtcdAuthBasePath + "/user";
    private String EtcdRoleBasePath = EtcdAuthBasePath + "/role";

    public EtcdClient(String host, String port){
        etcdUrl = "http://" + host + ":" + port;
    }

    public String getEtcdUrl() {
        return etcdUrl;
    }


    public void createUser(String username, String password) throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("name", username);
        payload.put("password", password);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest.
                doPost(etcdUrl + EtcdUserBasePath + "/add", payload.toString(), headers);
    }

    public void createRole(String rolename) throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("name", rolename);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdRoleBasePath + "/add", payload.toString(), headers);
    }

    public void addRoleToUser(String user, String role) throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("user", user);
        payload.put("role", role);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdUserBasePath + "/grant", payload.toString(), headers);
    }

    public String authenticate() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("name", "root");
        payload.put("password", "root");
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdAuthBasePath + "/authenticate", payload.toString(), headers);
        JSONObject responsePayload = new JSONObject(response.getData());
        String token = responsePayload.get("token").toString();
        return token;
    }

    public void addKeyValuePair(String key, String value) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("key", key);
        payload.put("value", value);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdKVBasePath + "/put", payload.toString(), headers);
    }

    public void addKeyValuePair(String token, String key, String value) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("key", key);
        payload.put("value", value);
        headers.put("Authorization", token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdKVBasePath + "/put", payload.toString(), headers);
    }

    public void deleteKeyValuePair(String token, String key) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        payload.put("key", key);
        headers.put("Authorization", token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdKVBasePath + "/delete", payload.toString(), headers);
    }

    public void enableAuthentication() throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);

        JSONObject payload = new JSONObject();
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdAuthBasePath + "/enable", payload.toString(), headers);
    }

    public void disableAuthentication(String token) throws Exception{
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_TEXT_PLAIN);
        JSONObject payload = new JSONObject();
        headers.put("Authorization", token);
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doPost(etcdUrl + EtcdAuthBasePath + "/disable", payload.toString(), headers);
    }
}