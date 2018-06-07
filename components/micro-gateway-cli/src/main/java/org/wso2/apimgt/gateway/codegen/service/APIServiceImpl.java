package org.wso2.apimgt.gateway.codegen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wso2.apimgt.gateway.codegen.cmd.GatewayCmdUtils;
import org.wso2.apimgt.gateway.codegen.config.bean.Config;
import org.wso2.apimgt.gateway.codegen.service.bean.APIListDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.Endpoint;
import org.wso2.apimgt.gateway.codegen.service.bean.EndpointConfig;
import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.ApplicationThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.codegen.service.bean.policy.SubscriptionThrottlePolicyListDTO;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class APIServiceImpl implements APIService {
    
    @Override
    public List<ExtendedAPI> getAPIs(String labelName, String accessToken) {

        URL url;
        HttpsURLConnection urlConn = null;
        APIListDTO apiListDTO = null;
        //calling token endpoint
        try {
            Config config = GatewayCmdUtils.getConfig();
            String publisherEp = config.getTokenConfig().getPublisherEndpoint();
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
                String responseStr = getResponseString(urlConn.getInputStream());
                System.out.println(responseStr);
                //convert json string to object
                apiListDTO = mapper.readValue(responseStr, APIListDTO.class);
                for (ExtendedAPI api : apiListDTO.getList()) {
                    String endpointConfig = api.getEndpointConfig();
                    api.setEndpointConfigRepresentation(getEndpointConfig(endpointConfig)); 
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

    @Override
    public List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String token) {
        URL url;
        HttpsURLConnection urlConn = null;
        ApplicationThrottlePolicyListDTO appsList;
        List<ApplicationThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        Config config = GatewayCmdUtils.getConfig();
        String adminEp = config.getTokenConfig().getAdminEndpoint();
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/application";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("GET");
            urlConn.setRequestProperty("Authorization", "Bearer " + token);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = getResponseString(urlConn.getInputStream());
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

    @Override
    public List<SubscriptionThrottlePolicyDTO> getSubscriptionPolicies(String token) {
        URL url;
        HttpsURLConnection urlConn = null;
        SubscriptionThrottlePolicyListDTO subsList;
        List<SubscriptionThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        Config config = GatewayCmdUtils.getConfig();
        String adminEp = config.getTokenConfig().getAdminEndpoint();
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "throttling/policies/subscription";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod("GET");
            urlConn.setRequestProperty("Authorization", "Bearer " + token);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = getResponseString(urlConn.getInputStream());
                System.out.println(responseStr);
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

    private static String getResponseString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String file = "";
            String str;
            while ((str = buffer.readLine()) != null) {
                file += str;
            }
            return file;
        }
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
