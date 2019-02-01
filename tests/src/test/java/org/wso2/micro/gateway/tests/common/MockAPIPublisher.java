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

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.micro.gateway.tests.common.model.ApplicationPolicy;
import org.wso2.micro.gateway.tests.common.model.SubscriptionPolicy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * APIM Publisher mock class
 */
public class MockAPIPublisher {
    private static final Logger log = LoggerFactory.getLogger(MockAPIPublisher.class);
    private static MockAPIPublisher instance;
    private static List<SubscriptionPolicy> subscriptionPolicies;
    private static List<ApplicationPolicy> applicationPolicies;
    private Map<String, List<APIDTO>> apis;
    private Map<String, KeyValidationInfo> tokenInfo;
    private Map<String, IntrospectInfo> introspectInfo;

    public MockAPIPublisher() {
        apis = new HashMap<>();
        tokenInfo = new HashMap<>();
        introspectInfo = new HashMap<>();
        subscriptionPolicies = new ArrayList<>();
        applicationPolicies = new ArrayList<>();
    }

    public static MockAPIPublisher getInstance() {
        if (instance == null) {
            instance = new MockAPIPublisher();
        }
        return instance;
    }

    public static List<SubscriptionPolicy> getSubscriptionPolicies() {
        return subscriptionPolicies;
    }

    public static List<ApplicationPolicy> getApplicationPolicies() {
        return applicationPolicies;
    }

    public void addApi(String label, APIDTO api) {

        try {
            api = populateJson(api);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (apis.containsKey(label)) {
            apis.get(label).add(api);
        } else {
            apis.put(label, new ArrayList<>(Arrays.asList(api)));
        }
    }

    private APIDTO populateJson(APIDTO api) throws IOException {
        String apiJson = IOUtils
                .toString(new FileInputStream(getClass().getClassLoader().getResource("api-json.json").getPath()));
        String apiDefinition = IOUtils.toString(
                new FileInputStream(getClass().getClassLoader().getResource("api-definition.json").getPath()));
        String endpointJson = IOUtils
                .toString(new FileInputStream(getClass().getClassLoader().getResource("endpoint.json").getPath()));

        JSONArray endpoint = new JSONArray(endpointJson);
        JSONObject apiJsonObj = new JSONObject(apiJson);
        apiJsonObj.put("endpoint", endpoint);
        apiJsonObj.put("apiDefinition", apiDefinition);
        apiJsonObj.put("name", api.getName());
        apiJsonObj.put("version", api.getVersion());
        apiJsonObj.put("context", api.getContext());
        apiJsonObj.put("provider", api.getProvider());

        //todo: set tiers and swagger
        return api;
    }

    public String getAPIResponseForLabel(String label) {
        List<APIDTO> filterd = apis.get(label);
        try {
            String apiJson = IOUtils
                    .toString(new FileInputStream(getClass().getClassLoader().getResource("api-json.json").getPath()));
            String restResponse = IOUtils.toString(
                    new FileInputStream(getClass().getClassLoader().getResource("api-response.json").getPath()));
            String endpointJson = IOUtils
                    .toString(new FileInputStream(getClass().getClassLoader().getResource("endpoint.json").getPath()));
            JSONObject response = new JSONObject(restResponse);
            response.put("count", filterd.size());
            JSONArray arr = new JSONArray();
            for (APIDTO api : filterd) {
                JSONObject jsonObject = new JSONObject(apiJson);

                if (api.getEndpoint().size() > 0) {
                    String url = api.getEndpoint().get(0).getInline().getEndpointConfig().getList().get(0).getUrl();
                    String endpointConfig = endpointJson.replace("serviceUrl", url);
                    jsonObject.put("endpoint", new JSONArray(endpointConfig));
                }
                arr.put(jsonObject);
            }
            response.put("list", arr);
            return response.toString();
        } catch (IOException e) {
            log.error("Error occurred when generating response", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getSwaggerResponseForAPI() {
        try {
            return IOUtils.toString(new FileInputStream(getClass().getClassLoader().getResource("api-definition.json").getPath()));
        } catch (IOException e) {
            log.error("Error occurred when generating response", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public String getAndRegisterAccessToken(KeyValidationInfo info) {
        String token = UUID.randomUUID().toString();
        tokenInfo.put(token, info);
        return token;
    }

    public String getAndRegisterAccessToken(IntrospectInfo info) {
        String token = UUID.randomUUID().toString();
        introspectInfo.put(token, info);
        return token;
    }

    public String getKeyValidationResponseForToken(String token) {
        KeyValidationInfo info = tokenInfo.get(token);
        try {
            if (info == null) {
                log.debug("Token not registered");
                String xmlResponse = IOUtils.toString(new FileInputStream(
                        getClass().getClassLoader().getResource("key-validation-error-response.xml").getPath()));
                return xmlResponse;
            } else {
                if (info.isResponsePresent()) {
                    return info.getStringResponse();
                } else {
                    String xmlResponse = IOUtils.toString(new FileInputStream(
                            getClass().getClassLoader().getResource("key-validation-response.xml").getPath()));
                    xmlResponse = xmlResponse.replace("$KEY_TYPE", info.getKeyType());
                    xmlResponse = xmlResponse.replace("$APINAME", info.getApi().getName());
                    xmlResponse = xmlResponse.replace("$APPLICATION_ID", String.valueOf(info.getApplication().getId()));
                    xmlResponse = xmlResponse.replace("$APPLICATION_NAME", info.getApplication().getName());
                    xmlResponse = xmlResponse.replace("$APPLICATION_TIER", info.getApplication().getTier());
                    xmlResponse = xmlResponse.replace("$TIER", info.getSubscriptionTier());
                    return xmlResponse;
                }
            }
        } catch (IOException e) {
            log.error("Error occurred when generating response", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void clear() {
        tokenInfo.clear();
        apis.clear();
        subscriptionPolicies.clear();
        applicationPolicies.clear();
    }

    public void addSubscriptionPolicy(SubscriptionPolicy subscriptionPolicy) {
        subscriptionPolicies.add(subscriptionPolicy);
    }

    public void addApplicationPolicy(ApplicationPolicy applicationPolicy) {
        applicationPolicies.add(applicationPolicy);
    }

    public Map<String, IntrospectInfo> getIntrospectInfo() {
        return introspectInfo;
    }

}
