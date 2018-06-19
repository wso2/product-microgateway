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
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.rest.APIListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.Endpoint;
import org.wso2.apimgt.gateway.cli.model.rest.EndpointConfig;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.TokenManagementUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RESTAPIServiceImpl implements RESTAPIService {
    private String publisherEp;
    private String adminEp;

    public RESTAPIServiceImpl(String publisherEp, String adminEp) {
        this.publisherEp = publisherEp;
        this.adminEp = adminEp;
    }

    /**
     * @see RESTAPIService#getAPIs(String, String)
     */
    public List<ExtendedAPI> getAPIs(String labelName, String accessToken) {

        URL url;
        HttpURLConnection urlConn = null;
        APIListDTO apiListDTO = null;
        //calling token endpoint
        try {
            publisherEp = publisherEp.endsWith("/") ? publisherEp : publisherEp + "/";
            String urlStr = publisherEp + RESTServiceConstants.APIS_GET_URI
                    .replace(GatewayCliConstants.LABEL_PLACEHOLDER,
                            URLEncoder.encode(labelName, GatewayCliConstants.CHARSET_UTF8));
            url = new URL(urlStr);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                apiListDTO = mapper.readValue(responseStr, APIListDTO.class);
                for (ExtendedAPI api : apiListDTO.getList()) {
                    String endpointConfig = api.getEndpointConfig();
                    api.setEndpointConfigRepresentation(getEndpointConfig(endpointConfig));
                    // set default values from config if per api cors is not enabled
                    Config config = GatewayCmdUtils.getConfig();
                    if (config == null) {
                        if (!api.getCorsConfiguration().getCorsConfigurationEnabled()) {
                            api.setCorsConfiguration(GatewayCmdUtils.getDefaultCorsConfig());
                        }
                    } else {
                        if (config.getCorsConfiguration().getCorsConfigurationEnabled() && !api.getCorsConfiguration()
                                .getCorsConfigurationEnabled()) {
                            api.setCorsConfiguration(config.getCorsConfiguration());
                        }
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
        HttpURLConnection urlConn = null;
        ApplicationThrottlePolicyListDTO appsList;
        List<ApplicationThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/application";
            url = new URL(urlStr);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                appsList = mapper.readValue(responseStr, ApplicationThrottlePolicyListDTO.class);
                List<ApplicationThrottlePolicyDTO> policyDTOS = appsList.getList();
                for (ApplicationThrottlePolicyDTO policyDTO : policyDTOS) {
                    if (!RESTServiceConstants.UNLIMITED.equalsIgnoreCase(policyDTO.getPolicyName())) {
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
        HttpURLConnection urlConn = null;
        SubscriptionThrottlePolicyListDTO subsList;
        List<SubscriptionThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/subscription";
            url = new URL(urlStr);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                subsList = mapper.readValue(responseStr, SubscriptionThrottlePolicyListDTO.class);
                List<SubscriptionThrottlePolicyDTO> policyDTOS = subsList.getList();
                for (SubscriptionThrottlePolicyDTO policyDTO : policyDTOS) {
                    if (!RESTServiceConstants.UNLIMITED.equalsIgnoreCase(policyDTO.getPolicyName())) {
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
        String endpointType = rootNode.path(RESTServiceConstants.ENDPOINT_TYPE).asText();
        endpointConf.setEndpointType(endpointType);

        if (RESTServiceConstants.HTTP.equalsIgnoreCase(endpointType) || RESTServiceConstants.FAILOVER.
                equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpointNode = rootNode.get(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            if (prodEndpointNode != null) {
                Endpoint prod = new Endpoint();
                prod.setEndpointUrl(prodEndpointNode.get(RESTServiceConstants.URL).asText());
                endpointConf.addProdEndpoint(prod);
            }

            JsonNode sandEndpointNode = rootNode.get(RESTServiceConstants.SANDBOX_ENDPOINTS);
            if (sandEndpointNode != null) {
                Endpoint sandbox = new Endpoint();
                sandbox.setEndpointUrl(sandEndpointNode.get(RESTServiceConstants.URL).asText());
                endpointConf.addSandEndpoint(sandbox);
            }

            if (RESTServiceConstants.FAILOVER.equalsIgnoreCase(endpointType)) {
                JsonNode prodFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.PRODUCTION_FAILOVERS);
                if (prodFailoverEndpointNode != null) {
                    Iterator<JsonNode> prodFailoverEndointIterator = prodFailoverEndpointNode.iterator();
                    while (prodFailoverEndointIterator.hasNext()) {
                        JsonNode node = prodFailoverEndointIterator.next();
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                        endpointConf.addProdFailoverEndpoint(endpoint);
                    }
                }

                JsonNode sandFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.SANDBOX_FAILOVERS);
                if (sandFailoverEndpointNode != null) {
                    Iterator<JsonNode> sandboxFailoverEndointIterator = sandFailoverEndpointNode.iterator();
                    while (sandboxFailoverEndointIterator.hasNext()) {
                        JsonNode node = sandboxFailoverEndointIterator.next();
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                        endpointConf.addSandFailoverEndpoint(endpoint);
                    }
                }
            }
        } else if (RESTServiceConstants.LOAD_BALANCE.equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpoints = rootNode.withArray(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            if (prodEndpoints != null) {
                Iterator<JsonNode> prodEndointIterator = prodEndpoints.iterator();
                while (prodEndointIterator.hasNext()) {
                    JsonNode node = prodEndointIterator.next();
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                    endpointConf.addProdEndpoint(endpoint);
                }
            }

            JsonNode sandboxEndpoints = rootNode.withArray(RESTServiceConstants.SANDBOX_ENDPOINTS);
            if (sandboxEndpoints != null) {
                Iterator<JsonNode> sandboxEndpointIterator = sandboxEndpoints.iterator();
                while (sandboxEndpointIterator.hasNext()) {
                    JsonNode node = sandboxEndpointIterator.next();
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                    endpointConf.addSandEndpoint(endpoint);
                }
            }
        }
        return endpointConf;
    }
}
