/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.interceptor.opa;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.opa.OPAConstants;
import org.wso2.choreo.connect.enforcer.commons.opa.OPARequestGenerator;
import org.wso2.choreo.connect.enforcer.commons.opa.OPASecurityException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * HTTP Client which send requests to OPA server by selecting the implementation of {@link OPARequestGenerator}
 * which is provided with policy attributes.
 */
public class OPAClient {
    private static final Logger log = LogManager.getLogger(OPAClient.class);
    private static final String DEFAULT_REQUEST_GENERATOR_CLASS =
            "org.wso2.choreo.connect.enforcer.commons.model.RequestContext.OPADefaultRequestGenerator";
    private static final OPAClient opaClient = new OPAClient();

    private final OPARequestGenerator defaultRequestGenerator = new OPADefaultRequestGenerator();
    private final Map<String, OPARequestGenerator> requestGeneratorMap = new HashMap<>();

    private OPAClient() {
    }

    public static void init() {
        getInstance().loadRequestGenerators();
    }

    public static OPAClient getInstance() {
        return opaClient;
    }

    public boolean validateRequest(RequestContext requestContext, Map<String, String> policyAttrib)
            throws OPASecurityException {
        String requestGeneratorClassName = policyAttrib.get("requestGenerator");
        OPARequestGenerator requestGenerator = requestGeneratorMap.get(requestGeneratorClassName);
        if (requestGenerator == null) {
            log.error("OPA Request Generator Implementation is not found in the classPath under the provided name: {}",
                    requestGeneratorClassName, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6103));
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.OPA_REQUEST_FAILURE);
        }

        String serverURL = policyAttrib.get("serverURL");
        String token = policyAttrib.get("accessKey");
        String policyName = policyAttrib.get("policy");
        String ruleName = policyAttrib.get("rule");

        // additionalParameters - we provide this as a Map<String, String> in the interface
        // policyAttrib should support Map<String, MAP<String, String>>
        // and the additionalParameters should come inside this. Since APIM 4.1.0 not supports
        // Map<String, MAP<String, String>> in the UI, this is fine for now.
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put(OPAConstants.AdditionalParameters.ADDITIONAL_PROPERTIES,
                policyAttrib.get("additionalProperties"));
        additionalParameters.put(OPAConstants.AdditionalParameters.SEND_ACCESS_TOKEN,
                policyAttrib.get("sendAccessToken"));

        // client related configs
        Map<String, String> clientOptions = new HashMap<>();
        FilterUtils.putToMapIfNotNull(clientOptions, FilterUtils.HTTPClientOptions.MAX_OPEN_CONNECTIONS,
                policyAttrib.get("maxOpenConnections"));
        FilterUtils.putToMapIfNotNull(clientOptions, FilterUtils.HTTPClientOptions.MAX_PER_ROUTE,
                policyAttrib.get("maxPerRoute"));
        FilterUtils.putToMapIfNotNull(clientOptions, FilterUtils.HTTPClientOptions.CONNECT_TIMEOUT,
                policyAttrib.get("connectionTimeout"));

        // evaluating server policy URL
        serverURL = StringUtils.removeEnd(serverURL, "/");
        String evaluatingPolicyUrl;
        if (StringUtils.isNotEmpty(ruleName)) {
            evaluatingPolicyUrl = String.format("%s/%s/%s", serverURL, policyName, ruleName);
        } else {
            evaluatingPolicyUrl = String.format("%s/%s", serverURL, policyName);
        }

        // calling OPA server and validate response
        String requestBody = requestGenerator.generateRequest(policyName, ruleName, additionalParameters,
                requestContext);
        String opaResponse = callOPAServer(evaluatingPolicyUrl, requestBody, token, clientOptions);
        return requestGenerator.handleResponse(policyName, ruleName, opaResponse, additionalParameters,
                requestContext);
    }

    private void loadRequestGenerators() {
        ServiceLoader<OPARequestGenerator> loader = ServiceLoader.load(OPARequestGenerator.class);
        for (OPARequestGenerator generator : loader) {
            requestGeneratorMap.put(generator.getClass().getName(), generator);
        }
        requestGeneratorMap.put("", defaultRequestGenerator);
        requestGeneratorMap.put(null, defaultRequestGenerator);
        requestGeneratorMap.put(DEFAULT_REQUEST_GENERATOR_CLASS, defaultRequestGenerator);
    }

    private static String callOPAServer(String serverEp, String payload, String token,
                                        Map<String, String> clientOptions) throws OPASecurityException {
        try {
            URL url = new URL(serverEp);
            KeyStore opaKeyStore = ConfigHolder.getInstance().getOpaKeyStore();
            try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol(),
                    opaKeyStore, clientOptions)) {
                HttpPost httpPost = new HttpPost(serverEp);
                HttpEntity reqEntity = new ByteArrayEntity(payload.getBytes(Charset.defaultCharset()));
                httpPost.setEntity(reqEntity);
                httpPost.setHeader(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_JSON);
                if (StringUtils.isNotEmpty(token)) {
                    httpPost.setHeader(APIConstants.AUTHORIZATION_HEADER_DEFAULT,
                            APIConstants.AUTHORIZATION_BEARER + token);
                }
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity entity = response.getEntity();
                        try (InputStream content = entity.getContent()) {
                            return IOUtils.toString(content, Charset.defaultCharset());
                        }
                    } else {
                        log.error("Unexpected HTTP response code responded by the OPA server, HTTP code: {}",
                                statusCode, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6106));
                        throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                                APISecurityConstants.OPA_REQUEST_FAILURE);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error calling the OPA server with server endpoint: {}", serverEp,
                    ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6104), e);
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.OPA_REQUEST_FAILURE, e);
        }
    }
}
