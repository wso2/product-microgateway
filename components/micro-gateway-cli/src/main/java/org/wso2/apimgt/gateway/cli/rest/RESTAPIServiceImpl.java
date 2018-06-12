/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wso2.apimgt.gateway.cli.model.rest.APIListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.Endpoint;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointConfig;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.utils.TokenManagementUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public class RESTAPIServiceImpl implements RESTAPIService {

    /**
     * @see RESTAPIService#getAPIs(String, String)
     */
    public List<ExtendedAPI> getAPIs(String labelName, String accessToken) {

        URL url;
        HttpsURLConnection urlConn = null;
        APIListDTO apiListDTO = null;
        //calling token endpoint
        try {
            Config config = GatewayCmdUtils.getConfig();
            String publisherEp = config.getToken().getPublisherEndpoint();
            publisherEp = publisherEp.endsWith("/") ? publisherEp : publisherEp + "/";
            String urlStr = publisherEp + "apis?query=label:" + labelName + "&expand=true";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("GET");
            urlConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                apiListDTO = mapper.readValue(responseStr, APIListDTO.class);
                for (ExtendedAPI api : apiListDTO.getList()) {
                    String endpointConfig = api.getEndpointConfig();
                    api.setEndpointConfigRepresentation(getEndpointConfig(endpointConfig));
                    // set default values from config if per api cors ins not enabled
                    if (config.getCorsConfiguration().getCorsConfigurationEnabled() && !api.getCorsConfiguration()
                            .getCorsConfigurationEnabled()) {
                        api.setCorsConfiguration(config.getCorsConfiguration());
                    }
                }
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while getting all APIs with label " + labelName;
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return apiListDTO.getList();
    }

    /**
     * @see RESTAPIService#getApplicationPolicies(String)
     */
    public List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String accessToken) {
        URL url;
        HttpsURLConnection urlConn = null;
        ApplicationThrottlePolicyListDTO appsList;
        List<ApplicationThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        Config config = GatewayCmdUtils.getConfig();
        String adminEp = config.getToken().getAdminEndpoint();
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/application";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("GET");
            urlConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                System.out.println(responseStr);
                //convert json string to object
                appsList = mapper.readValue(responseStr, ApplicationThrottlePolicyListDTO.class);
                List<ApplicationThrottlePolicyDTO> policyDTOS = appsList.getList();
                for (ApplicationThrottlePolicyDTO policyDTO : policyDTOS ) {
                    if(!"Unlimited".equalsIgnoreCase(policyDTO.getPolicyName())){
                        filteredPolicyDTOS.add(policyDTO);
                    }
                }
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return filteredPolicyDTOS;
    }

    /**
     * @see RESTAPIService#getSubscriptionPolicies(String)
     */
    public List<SubscriptionThrottlePolicyDTO> getSubscriptionPolicies(String accessToken) {
        URL url;
        HttpsURLConnection urlConn = null;
        SubscriptionThrottlePolicyListDTO subsList;
        List<SubscriptionThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        Config config = GatewayCmdUtils.getConfig();
        String adminEp = config.getToken().getAdminEndpoint();
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/subscription";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("GET");
            urlConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                subsList = mapper.readValue(responseStr, SubscriptionThrottlePolicyListDTO.class);
                List<SubscriptionThrottlePolicyDTO> policyDTOS = subsList.getList();
                for (SubscriptionThrottlePolicyDTO policyDTO : policyDTOS ) {
                    if(!"Unlimited".equalsIgnoreCase(policyDTO.getPolicyName())){
                        filteredPolicyDTOS.add(policyDTO);
                    }
                }
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (Exception e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        return filteredPolicyDTOS;
    }

    private EndpointConfig getEndpointConfig(String endpointConfig) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        EndpointConfig endpointConf = new EndpointConfig();
        rootNode = mapper.readTree(endpointConfig);
        String endpointType = rootNode.path("endpoint_type").asText();
        endpointConf.setEndpointType(endpointType);

        if ("http".equalsIgnoreCase(endpointType) || "failover".equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpointNode = rootNode.get("production_endpoints");
            if (prodEndpointNode != null) {
                Endpoint prod = new Endpoint();
                prod.setEndpointUrl(prodEndpointNode.get("url").asText());
                endpointConf.addProdEndpoint(prod);
            }

            JsonNode sandEndpointNode = rootNode.get("sandbox_endpoints");
            if (sandEndpointNode != null) {
                Endpoint sandbox = new Endpoint();
                sandbox.setEndpointUrl(sandEndpointNode.get("url").asText());
                endpointConf.addSandEndpoint(sandbox);
            }

            if ("failover".equalsIgnoreCase(endpointType)) {
                JsonNode prodFailoverEndpointNode = rootNode.withArray("production_failovers");
                if (prodFailoverEndpointNode != null) {
                    Iterator<JsonNode> prodFailoverEndointIterator = prodFailoverEndpointNode.iterator();
                    while (prodFailoverEndointIterator.hasNext()) {
                        JsonNode node = prodFailoverEndointIterator.next();
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get("url").asText());
                        endpointConf.addProdFailoverEndpoint(endpoint);
                    }
                }

                JsonNode sandFailoverEndpointNode = rootNode.withArray("sandbox_failovers");
                if (sandFailoverEndpointNode != null) {
                    Iterator<JsonNode> sandboxFailoverEndointIterator = sandFailoverEndpointNode.iterator();
                    while (sandboxFailoverEndointIterator.hasNext()) {
                        JsonNode node = sandboxFailoverEndointIterator.next();
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get("url").asText());
                        endpointConf.addSandFailoverEndpoint(endpoint);
                    }
                }
            }
        } else if ("load_balance".equalsIgnoreCase(endpointType)) {
            JsonNode prodEndoints = rootNode.withArray("production_endpoints");
            if (prodEndoints != null) {
                Iterator<JsonNode> prodEndointIterator = prodEndoints.iterator();
                while (prodEndointIterator.hasNext()) {
                    JsonNode node = prodEndointIterator.next();
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get("url").asText());
                    endpointConf.addProdEndpoint(endpoint);
                }
            }

            JsonNode sandboxEndpoints = rootNode.withArray("sandbox_endpoints");
            if (sandboxEndpoints != null) {
                Iterator<JsonNode> sandboxEndointIterator = sandboxEndpoints.iterator();
                while (sandboxEndointIterator.hasNext()) {
                    JsonNode node = sandboxEndointIterator.next();
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get("url").asText());
                    endpointConf.addSandEndpoint(endpoint);
                }
            }
        }
        return endpointConf;
    }
}
