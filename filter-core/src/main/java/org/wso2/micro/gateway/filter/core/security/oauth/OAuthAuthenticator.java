/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.filter.core.security.oauth;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.micro.gateway.filter.core.api.RequestContext;
import org.wso2.micro.gateway.filter.core.security.AccessTokenInfo;
import org.wso2.micro.gateway.filter.core.security.AuthenticationContext;
import org.wso2.micro.gateway.filter.core.security.Authenticator;
import org.wso2.micro.gateway.filter.core.security.jwt.validator.JWTValidator;
import org.wso2.micro.gateway.filter.core.util.FilterUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * An API consumer authenticator which authenticates user requests using
 * the OAuth protocol. This implementation uses some default token/delimiter
 * values to parse OAuth headers, but if needed these settings can be overridden
 * through the APIManagerConfiguration.
 *
 * //TODO: Complete the implementation.
 */
public class OAuthAuthenticator implements Authenticator {
    private static final Log log = LogFactory.getLog(OAuthAuthenticator.class);
    private List<String> keyManagerList;

//    protected APIKeyValidator keyValidator;
    protected JWTValidator jwtValidator;

    private String KM_ENDPOINT = "https://localhost:9443/oauth2";
    private String securityHeader = HttpHeaders.AUTHORIZATION;
    private String defaultAPIHeader="WSO2_AM_API_DEFAULT_VERSION";
    private String consumerKeyHeaderSegment = "Bearer";
    private String oauthHeaderSplitter = ",";
    private String consumerKeySegmentDelimiter = " ";
    private String securityContextHeader;
    private boolean removeOAuthHeadersFromOutMessage=true;
    private boolean removeDefaultAPIHeaderFromOutMessage=true;
    private String clientDomainHeader = "referer";
    private String requestOrigin;
    private String remainingAuthHeader;
    private boolean isMandatory;

    public OAuthAuthenticator() {
    }

    public OAuthAuthenticator(String authorizationHeader, boolean isMandatory, boolean removeOAuthHeader,
                              List<String> keyManagerList) {
        this.securityHeader = authorizationHeader;
        this.removeOAuthHeadersFromOutMessage = removeOAuthHeader;
        this.isMandatory = isMandatory;
        this.keyManagerList = keyManagerList;
    }

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String token = requestContext.getHeaders().get("authorization");
        return !token.contains("\\.");
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) {
        String token = requestContext.getHeaders().get("authorization");
        AccessTokenInfo accessTokenInfo = new AccessTokenInfo();

        if (token.toLowerCase().contains("bearer")) {
            token = token.split("\\s")[1];
        }

        try {
            IntrospectInfo introspectInfo = validateToken(token);
            accessTokenInfo.setAccessToken(token);
            accessTokenInfo.setConsumerKey(introspectInfo.getClientId());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new AuthenticationContext();
    }

    /**
     * Extracts the customer API key from the OAuth Authentication header. If the required
     * security header is present in the provided map, it will be removed from the map
     * after processing.
     *
     * @param headersMap Map of HTTP headers
     * @return extracted customer key value or null if the required header is not present
     */
    public String extractCustomerKeyFromAuthHeader(Map headersMap) {

        //From 1.0.7 version of this component onwards remove the OAuth authorization header from
        // the message is configurable. So we dont need to remove headers at this point.
        String authHeader = (String) headersMap.get(securityHeader);
        if (authHeader == null) {
            if (log.isDebugEnabled()) {
                log.debug("OAuth2 Authentication: Expected authorization header with the name '"
                        .concat(securityHeader).concat("' was not found."));
            }
            return null;
        }

        ArrayList<String> remainingAuthHeaders = new ArrayList<>();
        String consumerKey = null;
        boolean consumerkeyFound = false;
        String[] headers = authHeader.split(oauthHeaderSplitter);
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                String[] elements = headers[i].split(consumerKeySegmentDelimiter);
                if (elements != null && elements.length > 1) {
                    int j = 0;
                    boolean isConsumerKeyHeaderAvailable = false;
                    for (String element : elements) {
                        if (!"".equals(element.trim())) {
                            if (consumerKeyHeaderSegment.equals(elements[j].trim())) {
                                isConsumerKeyHeaderAvailable = true;
                            } else if (isConsumerKeyHeaderAvailable) {
                                consumerKey = removeLeadingAndTrailing(elements[j].trim());
                                consumerkeyFound = true;
                            }
                        }
                        j++;
                    }
                }
                if (!consumerkeyFound) {
                    remainingAuthHeaders.add(headers[i]);
                } else {
                    consumerkeyFound = false;
                }
            }
        }
        remainingAuthHeader = String.join(oauthHeaderSplitter, remainingAuthHeaders);
        return consumerKey;
    }

    private String removeLeadingAndTrailing(String base) {
        String result = base;

        if (base.startsWith("\"") || base.endsWith("\"")) {
            result = base.replace("\"", "");
        }
        return result.trim();
    }

    public String getChallengeString() {
        return "Bearer realm=\"WSO2 API Manager\"";
    }


    /**
     * Validate the token via the token introspection.
     *
     * @param accessToken : The access token which needs to be validated
     * @return The IntrospectInfo object
     * @throws IOException : If any error occurred during invoking the introspect endpoint.
     */
    private IntrospectInfo validateToken(String accessToken) throws IOException {
        URL url = new URL(KM_ENDPOINT + "/introspect");
        try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol())) {
            HttpPost introspectRequest = new HttpPost(KM_ENDPOINT + "/introspect");
            List<NameValuePair> params = new ArrayList<>();
            NameValuePair token = new BasicNameValuePair("token", accessToken);
            params.add(token);
            introspectRequest.setEntity(new UrlEncodedFormEntity(params));
            introspectRequest.setHeader("Content-type", "application/x-www-form-urlencoded");
            introspectRequest.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes()));
            try (CloseableHttpResponse response = httpClient.execute(introspectRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        return new Gson().fromJson(IOUtils.toString(content), IntrospectInfo.class);
                    }
                } else {
                    return null;
                }
            }
        }
    }
}
