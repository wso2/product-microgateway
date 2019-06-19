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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.RESTServiceConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIInternalException;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.config.Config;
import org.wso2.apimgt.gateway.cli.model.config.MutualSSL;
import org.wso2.apimgt.gateway.cli.model.rest.APIListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ClientCertMetadataDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ClientCertificatesDTO;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyListDTO;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.apimgt.gateway.cli.utils.TokenManagementUtil;

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

    /**
     * @see RESTAPIService#getAPIs(String, String)
     */
    public List<ExtendedAPI> getAPIs(String labelName, String accessToken) {
        logger.debug("Retrieving APIs with label {}", labelName);
        URL url;
        HttpsURLConnection urlConn = null;
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
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            logger.debug("Response code: {}", responseCode);
            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                logger.trace("Response body: {}", responseStr);
                //convert json string to object
                apiListDTO = mapper.readValue(responseStr, APIListDTO.class);
                for (ExtendedAPI api : apiListDTO.getList()) {

                    setAdditionalConfigs(api);
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
        return apiListDTO.getList();
    }


    /**
     * @see RESTAPIService#getAPI(String, String, String)
     */
    public ExtendedAPI getAPI(String apiName, String version, String accessToken) {
        logger.debug("Retrieving API with name {}, version {}", apiName, version);
        URL url;
        HttpsURLConnection urlConn = null;
        ExtendedAPI matchedAPI = null;
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
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
            int responseCode = urlConn.getResponseCode();
            logger.debug("Response code: {}", responseCode);

            if (responseCode == 200) {
                ObjectMapper mapper = new ObjectMapper();
                String responseStr = TokenManagementUtil.getResponseString(urlConn.getInputStream());
                logger.trace("Response body: {}", responseStr);
                //convert json string to object
                APIListDTO apiList = mapper.readValue(responseStr, APIListDTO.class);
                if (apiList != null) {
                    for (ExtendedAPI api : apiList.getList()) {
                        if (apiName.equals(api.getName()) && version.equals(api.getVersion())) {
                            matchedAPI = api;
                            break;
                        }
                    }
                    if (matchedAPI == null) {
                        return null;
                    }
                    //set additional configs such as CORS configs from the toolkit configuration
                    setAdditionalConfigs(matchedAPI);
                }else {
                    throw new CLIInternalException("No proper response received for get API request.");
                }
            } else if (responseCode == 401) {
                throw new CLIRuntimeException(
                        "Invalid user credentials or the user does not have required permissions");
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
        return matchedAPI;
    }

    private void setAdditionalConfigs(ExtendedAPI api) {
        //todo: remove the comment
        //api.setEndpointConfigRepresentation((endpointConfig));
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
            String urlStr = adminEp + "throttling/policies/application";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
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
            String urlStr = adminEp + "throttling/policies/subscription";
            url = new URL(urlStr);
            urlConn = (HttpsURLConnection) url.openConnection();
            if (inSecure) {
                urlConn.setHostnameVerifier((s, sslSession) -> true);
            }
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
            urlConn.setRequestProperty(RESTServiceConstants.AUTHORIZATION,
                    RESTServiceConstants.BEARER + " " + accessToken);
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
