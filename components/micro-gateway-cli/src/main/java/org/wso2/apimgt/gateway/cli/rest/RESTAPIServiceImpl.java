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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.MutualSSL;
import org.wso2.apimgt.gateway.cli.model.rest.*;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.TokenManagementUtil;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyListDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class RESTAPIServiceImpl implements RESTAPIService {

    private static final Logger logger = LoggerFactory.getLogger(RESTAPIServiceImpl.class);

    private String publisherEp;
    private String adminEp;
    private boolean inSecure;

    public RESTAPIServiceImpl(String publisherEp, String adminEp, boolean inSecure) {
        this.publisherEp = publisherEp;
        this.adminEp = adminEp;
        this.inSecure = inSecure;
    }

    public String getAuthHeader() {
        String authHeader;
        String exportHeader = System.getProperty(RESTServiceConstants.AUTH_HEADER);
        if (exportHeader != null) {
            authHeader = exportHeader;
        } else {
            authHeader = RESTServiceConstants.AUTHORIZATION;
        }
        return authHeader;
    }

    /**
     * @see RESTAPIService#getAPIs(String, String)
     */
    public List<APIInfoDTO> getAPIs(String labelName, String accessToken) {
        logger.debug("Retrieving APIs with label {}", labelName);
        URL url;
        HttpsURLConnection urlConn = null;
        List<APIInfoDTO> apisList = new ArrayList<>();
        APIListDTO apiListDTO;
        //calling token endpoint
        try {
            publisherEp = publisherEp.endsWith("/") ? publisherEp : publisherEp + "/";
            String urlStr = publisherEp + RESTServiceConstants.APIS_GET_URI
                    .replace(GatewayCliConstants.LABEL_PLACEHOLDER,
                            URLEncoder.encode(labelName, GatewayCliConstants.CHARSET_UTF8));
            logger.debug("GET API URL: {}", urlStr);
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            logger.debug("Response code: {}", responseCode);
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                logger.trace("Response body: {}", responseStr);
                //convert json string to object
                apiListDTO = mapper.readValue(responseStr, APIListDTO.class);
                for (APIDTO api : apiListDTO.getList()) {
                    apisList.add(api);
                }
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String msg = "Error while getting all APIs with label " + labelName;
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        logger.debug("Retrieving APIs with label {} was successful.", labelName);
        return apisList;
    }

    /**
     * @see RESTAPIService#getAPI(String, String, String)
     */
    public APIDTO getAPI(String apiName, String version, String accessToken) {
        logger.debug("Retrieving API with name {}, version {}", apiName, version);
        URL url;
        HttpsURLConnection urlConn = null;
        APIDTO apidto = null;
        //calling token endpoint
        try {
            publisherEp = publisherEp.endsWith("/") ? publisherEp : publisherEp + "/";
            String urlStr = publisherEp + RESTServiceConstants.API_GET_BY_NAME_VERSION_URI
                    .replace(GatewayCliConstants.API_NAME_PLACEHOLDER,
                            URLEncoder.encode(apiName, GatewayCliConstants.CHARSET_UTF8))
                    .replace(GatewayCliConstants.VERSION_PLACEHOLDER,
                            URLEncoder.encode(version, GatewayCliConstants.CHARSET_UTF8));
            logger.debug("GET API URL: {}", urlStr);
            url = new URL(urlStr);

            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            ;
            int responseCode = urlConn.getResponseCode();
            logger.debug("Response code: {}", responseCode);
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                logger.trace("Response body: {}", responseStr);
                //convert json string to object
                org.wso2.carbon.apimgt.rest.api.publisher.dto.APIListDTO apiList = mapper
                        .readValue(responseStr, org.wso2.carbon.apimgt.rest.api.publisher.dto.APIListDTO.class);
                if (apiList != null) {
                    for (APIInfoDTO api : apiList.getList()) {
                        if (apiName.equals(api.getName()) && version.equals(api.getVersion())) {
                            HttpsURLConnection urlConn1 = null;
                            try {
                                String id = api.getId();
                                String urlOfAPIget = publisherEp + "apis/" + id;
                                logger.debug("GET API URL: {}", urlOfAPIget);
                                url = new URL(urlOfAPIget);

                                urlConn1 = (HttpsURLConnection) url.openConnection();
                                if (inSecure) {
                                    urlConn.setHostnameVerifier((s, sslSession) -> true);
                                }
                                urlConn1.setDoOutput(true);
                                urlConn1.setRequestMethod(RESTServiceConstants.GET);
                                urlConn1.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                                        RESTServiceConstants.BEARER + " " + accessToken);
                                int responseCodeForGet = urlConn1.getResponseCode();
                                if (responseCodeForGet == 200) {
                                    ObjectMapper mapper1 = new ObjectMapper();
                                    String responseStr1 = TokenManagementUtil
                                            .getResponseString(urlConn1.getInputStream());
                                    //convert json string to object
                                    apidto = mapper1.readValue(responseStr1, APIDTO.class);
                                }
                            } finally {
                                if (urlConn1 != null) {
                                    urlConn1.disconnect();
                                }
                            }
                        }
                    }

                    //set additional configs such as CORS configs from the toolkit configuration
                    //setAdditionalConfigs(matchedAPI);
                } else if (responseCode == 401) {
                    throw new CLIRuntimeException(
                            "Invalid user credentials or the user does not have required permissions");
                } else {
                    throw new CLIInternalException("No proper response received for get API request.");
                }
            } else {
                throw new CLIInternalException("Error occurred while getting the token. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String msg = "Error while getting the API with name:" + apiName + ", version: " + version;
            throw new CLIInternalException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        logger.debug("Retrieving API with name {}, version {} was successful.", apiName, version);
        return apidto;
    }

    public String getAPISwaggerDefinition(String apiId, String accessToken) {
        HttpsURLConnection urlConn1 = null;
        String swagger = null;
        URL url;
        try {
            String urlOfAPIget = publisherEp + "apis/" + apiId + "/swagger";
            url = new URL(urlOfAPIget);

            urlConn1 = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn1.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn1.setDoOutput(true);
            urlConn1.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn1.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            ;
            int responseCodeForGet = urlConn1.getResponseCode();
            if (responseCodeForGet == 200) {
                ObjectMapper mapper1 = new ObjectMapper();
                String responseStr1 = TokenManagementUtil.getResponseString(urlConn1.getInputStream());
                //convert json string to object
                swagger = responseStr1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConn1 != null) {
                urlConn1.disconnect();
            }
        }
        return swagger;
    }

    /*private void setAdditionalConfigs(APIDTO api) throws IOException {
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
    }*/

    /**
     * @see RESTAPIService#getApplicationPolicies(String)
     */
    public List<ApplicationThrottlePolicyDTO> getApplicationPolicies(String accessToken) {
        URL url;
        HttpsURLConnection urlConn = null;
        ApplicationThrottlePolicyListDTO appsList;
        List<ApplicationThrottlePolicyDTO> filteredPolicyDTOS = new ArrayList<>();
        //calling token endpoint
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "policies/throttling/application";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                appsList = mapper.readValue(responseStr, ApplicationThrottlePolicyListDTO.class);
                List<ApplicationThrottlePolicyDTO> policyDTOS = appsList.getList();
                for (ApplicationThrottlePolicyDTO policyDTO : policyDTOS) {
                    if (!RESTServiceConstants.UNLIMITED.equalsIgnoreCase(policyDTO.getPolicyName())) {
                        filteredPolicyDTOS.add(policyDTO);
                    }
                }
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (IOException e) {
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
        adminEp = adminEp.endsWith("/") ? adminEp : adminEp + "/";
        try {
            String urlStr = adminEp + "policies/throttling/subscription";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                subsList = mapper.readValue(responseStr, SubscriptionThrottlePolicyListDTO.class);
                List<SubscriptionThrottlePolicyDTO> policyDTOS = subsList.getList();
                for (SubscriptionThrottlePolicyDTO policyDTO : policyDTOS) {
                    if (!RESTServiceConstants.UNLIMITED.equalsIgnoreCase(policyDTO.getPolicyName())) {
                        filteredPolicyDTOS.add(policyDTO);
                    }
                }
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (IOException e) {
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
        JsonNode rootNode;
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
                //ballerina does not treat primary/failover endpoint separately. Hence, primary production/sandbox
                //  eps (if any) will be added into failover list.
                if (endpointConf.getProdEndpoints() != null
                        && endpointConf.getProdEndpoints().getEndpoints().size() > 0) {
                    endpointConf.addProdFailoverEndpoint(endpointConf.getProdEndpoints().getEndpoints().get(0));
                }
                if (endpointConf.getSandEndpoints() != null
                        && endpointConf.getSandEndpoints().getEndpoints().size() > 0) {
                    endpointConf.addSandFailoverEndpoint(endpointConf.getSandEndpoints().getEndpoints().get(0));
                }

                //Adding additional production/sandbox failover endpoints
                JsonNode prodFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.PRODUCTION_FAILOVERS);
                if (prodFailoverEndpointNode != null) {
                    for (JsonNode node : prodFailoverEndpointNode) {
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                        endpointConf.addProdFailoverEndpoint(endpoint);
                    }
                }

                JsonNode sandFailoverEndpointNode = rootNode.withArray(RESTServiceConstants.SANDBOX_FAILOVERS);
                if (sandFailoverEndpointNode != null) {
                    for (JsonNode node : sandFailoverEndpointNode) {
                        Endpoint endpoint = new Endpoint();
                        endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                        endpointConf.addSandFailoverEndpoint(endpoint);
                    }
                }
            }
        } else if (RESTServiceConstants.LOAD_BALANCE.equalsIgnoreCase(endpointType)) {
            JsonNode prodEndpoints = rootNode.withArray(RESTServiceConstants.PRODUCTION_ENDPOINTS);
            if (prodEndpoints != null) {
                for (JsonNode node : prodEndpoints) {
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                    endpointConf.addProdEndpoint(endpoint);
                }
            }

            JsonNode sandboxEndpoints = rootNode.withArray(RESTServiceConstants.SANDBOX_ENDPOINTS);
            if (sandboxEndpoints != null) {
                for (JsonNode node : sandboxEndpoints) {
                    Endpoint endpoint = new Endpoint();
                    endpoint.setEndpointUrl(node.get(RESTServiceConstants.URL).asText());
                    endpointConf.addSandEndpoint(endpoint);
                }
            }
        }
        return endpointConf;
    }

    /**
     * @see RESTAPIService#getClientCertificates(String)
     */
    public List<ClientCertMetadataDTO> getClientCertificates(String accessToken) {
        Config config = GatewayCmdUtils.getConfig();
        URL url;
        HttpsURLConnection urlConn = null;
        ClientCertificatesDTO certList;
        List<ClientCertMetadataDTO> selectedCertificates = new ArrayList<>();
        //calling token endpoint
        publisherEp = publisherEp.endsWith("/") ? publisherEp : publisherEp + "/";
        try {
            String urlStr = publisherEp + "clientCertificates";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
            urlConn.setDoOutput(true);
            urlConn.setRequestMethod(RESTServiceConstants.GET);
            String authHeader = getAuthHeader();
            urlConn.setRequestProperty(authHeader, RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                //convert json string to object
                certList = mapper.readValue(responseStr, ClientCertificatesDTO.class);
                List<ClientCertMetadataDTO> certDTOS = certList.getCertificates();
                for (ClientCertMetadataDTO certDTO : certDTOS) {
                    if (!RESTServiceConstants.UNLIMITED.equalsIgnoreCase(certDTO.getTier())) {
                        selectedCertificates.add(certDTO);
                    }
                }
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
            } else if (responseCode == 404 || responseCode == 400) {
                selectedCertificates = null;

            } else {
                throw new RuntimeException("Error occurred while getting token. Status code: " + responseCode);
            }
        } catch (IOException e) {
            String msg = "Error while creating the new token for token regeneration.";
            throw new RuntimeException(msg, e);
        } finally {
            if (urlConn != null) {
                urlConn.disconnect();
            }
        }
        if (selectedCertificates != null) {
            MutualSSL clientDetails = new MutualSSL();
            clientDetails.setClientCertificates(selectedCertificates);
            config.setMutualSSL(clientDetails);
        }
        return selectedCertificates;
    }
}
