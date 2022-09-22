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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.withapim.graphql;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLScopeTestCase extends GraphQLBaseTest {
    private static final String API_NAME = "GraphQLScopeAPI";
    private static final String API_CONTEXT = "/gqlScope";
    private static final String API_VERSION = "1.0.0";
    private static final String GQL_QUERY = "{\"variables\":null,\"query\":\"{hero{name}}\"}";
    private static String endpointURL;
    private static String oAuthAppId;
    private static String jwtAppId;
    private static String scopeAppId;
    private static String scopeAPIId;
    private static String tokenWithInvalidScopes;
    private static String jwtToken;

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL for GQL API invocation")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        scopeAPIId = ApimResourceProcessor.apiNameToId.get(API_NAME);
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/" + API_VERSION);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
        jwtAppId = createGraphqlAppAndSubscribeToAPI(scopeAPIId, "GraphQLJWTAPP", ApplicationDTO.TokenTypeEnum.JWT);
        oAuthAppId = createGraphqlAppAndSubscribeToAPI(scopeAPIId, "GraphQLOauthAPP", ApplicationDTO.TokenTypeEnum.OAUTH);
        scopeAppId = ApimResourceProcessor.applicationNameToId.get("GraphQLScopeApp");
        ApplicationKeyDTO appWithConsumerKey = StoreUtils.generateKeysForApp(scopeAppId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);

        jwtToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"subscriber", "resolver"}, user, storeRestClient);
        tokenWithInvalidScopes = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{"inValidScope"}, user, storeRestClient);
    }

    @Test(description = "GraphQL API invocation using JWT App")
    public void testGraphqlAPIInvokeUsingJWTApplication() throws Exception {
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(jwtAppId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response =
                HttpsClientRequest.retryPostUntil200(endpointURL, GQL_QUERY, requestHeaders);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(responseData.contains("name=hero-1"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "API invocation using Oauth App")
    public void testGraphqlAPIInvokeUsingOAuthApplication() throws Exception {
        ApplicationKeyDTO applicationKeyDTO = StoreUtils.generateKeysForApp(oAuthAppId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, storeRestClient);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response =
                HttpsClientRequest.retryPostUntil200(endpointURL, GQL_QUERY, requestHeaders);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(responseData.contains("name=hero-1"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "setup scopes for gql operations",
            dependsOnMethods = {"testGraphqlAPIInvokeUsingJWTApplication", "testGraphqlAPIInvokeUsingOAuthApplication"})
    public void setupScopes() throws Exception {
        ArrayList role = new ArrayList();
        role.add("admin");

        ScopeDTO subscriberScopeObj = new ScopeDTO();
        subscriberScopeObj.setName("subscriber");
        subscriberScopeObj.setBindings(role);
        ScopeDTO resolverScopeObj = new ScopeDTO();
        resolverScopeObj.setName("subscriber");
        resolverScopeObj.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(subscriberScopeObj);
        apiScopeDTO.setScope(resolverScopeObj);

        ArrayList apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);

        HttpResponse apiResponse = publisherRestClient.getAPI(scopeAPIId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(apiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);

        PublisherUtils.createAPIRevisionAndDeploy(scopeAPIId, publisherRestClient);

        ArrayList subscriberScope = new ArrayList();
        subscriberScope.add("subscriber");
        ArrayList resolverScope = new ArrayList();
        resolverScope.add("resolver");
        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equalsIgnoreCase("hero")) {
                        item.setScopes(subscriberScope);
                    } else if (item.getTarget().equalsIgnoreCase("address")) {
                        item.setScopes(resolverScope);
                    }
                }
        );
        apidto.operations(operations);
        publisherRestClient.updateAPI(apidto, scopeAPIId);
        PublisherUtils.createAPIRevisionAndDeploy(scopeAPIId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till API deployment.");
    }

    @Test(description = "Test Oauth scopes with GraphQL API ", dependsOnMethods = "setupScopes")
    public void testOperationalLevelOAuthScopesForGraphql() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + jwtToken);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL,
                GQL_QUERY, requestHeaders);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(responseData.contains("name=hero-1"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(dependsOnMethods = "setupScopes")
    public void testGraphqlWithDifferentScopes() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + jwtToken);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL,
                "{\"query\": \"query MyQuery {hero {name age}\\n  address {planet village}\\n}\\n\", " +
                        "\"variables\": null, \"operationName\": \"MyQuery\" }", requestHeaders);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(responseData.contains("{hero=[{name=hero-1, age=10}], address=[{planet=Earth, village=LA}]}"),
                "Invalid GraphQL response received for " +
                        "query operation");
    }

    @Test(dependsOnMethods = "setupScopes")
    public void testOperationalLevelWithInvalidOAuthScopesForGraphql() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenWithInvalidScopes);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        requestHeaders.put("Content-Type", "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.doPost(endpointURL,
                GQL_QUERY, requestHeaders);
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }

    @Test(dependsOnMethods = "setupScopes")
    public void testMultipleOperationsWithInvalidOAuthScopesForGraphql() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenWithInvalidScopes);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        requestHeaders.put("Content-Type", "application/json");
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.doPost(endpointURL,
                "{\"query\": \"query MyQuery {hero {name age}\\n  address {planet village}\\n}\\n\", " +
                        "\"variables\": null, \"operationName\": \"MyQuery\" }", requestHeaders);
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(oAuthAppId, storeRestClient);
        StoreUtils.removeAllSubscriptionsForAnApp(jwtAppId, storeRestClient);
        StoreUtils.removeAllSubscriptionsForAnApp(scopeAppId, storeRestClient);
        storeRestClient.removeApplicationById(oAuthAppId);
        storeRestClient.removeApplicationById(scopeAppId);
        storeRestClient.removeApplicationById(jwtAppId);
    }
}
