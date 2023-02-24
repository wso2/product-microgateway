/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLBasicTestCase extends ApimBaseTest {
    private static final String APPLICATION_NAME = "GraphQLBasicApp";
    private static final String API_NAME = "GraphQLBasicAPI";
    private static final String API_CONTEXT = "/gql";
    private static final String API_VERSION = "1.0.0";
    private static Map<String, String> requestHeaders = new HashMap<>();
    private static String ACCESS_TOKEN;
    private static String endpointURL;
    private static String apiId;
    private static String apiKey;

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL for GQL API invocation")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);
        ACCESS_TOKEN = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId, user, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for " +
                "API-M access token");
        // Obtain API keys
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        apiKey = apiKeyDTO.getApikey();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + ACCESS_TOKEN);
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/" + API_VERSION);
    }

    @Test(description = "Test GraphQL query operation")
    public void testGraphQLQueryOperation() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"variables\":null,\"" +
                "query\":\"{hero{name, age}}\"}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("name=hero-1, age=10"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "Test GraphQL query operation with name")
    public void testGraphQLQueryWithOperationName() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "{\"query\":\"query " +
                "MyQuery{\\n hero {\\n name\\n age\\n  }\\n}\\n\",\"variables\":null,\"operationName\":\"GetHeroName\"}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("name=hero-1, age=10"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "Test GraphQL mutation operation")
    public void testGraphQLMutationOperation() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"query\":\"mutation " +
                "{createHero(name: \\\"Hero1\\\", age: 14){name,age}}\",\"variables\":null}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL mutation operation");
        Assert.assertTrue(responseData.contains("{createHero={name=Hero1, age=14}}"), "Invalid GraphQL response " +
                "received for mutation operation");
    }

    @Test(description = "Test GraphQL mutation operation with name")
    public void testGraphQLMutationWithOperationName() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "{\"query\":\"\\n\\n " +
                "mutation MyMutation {\\n  createHero(name: \\\"Hero1\\\", age: 14){name,age}}\\n\\n\",\"variables\"" +
                ":null,\"operationName\":\"MyMutation\"}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL mutation operation");
        Assert.assertTrue(responseData.contains("{createHero={name=Hero1, age=14}}"), "Invalid GraphQL response " +
                "received for mutation operation");
    }

    @Test(description = "Test GraphQL nested query")
    public void testGraphQLWithNestedQuery() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "{\"query\":\"query MyQuery " +
                "{ hero { age name location { village planet} } }\",\"variables\":null,\"operationName\":\"MyQuery\"}",
                requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL mutation operation");
        Assert.assertTrue(responseData.contains("{age=10, name=hero-1, location={village=LA, planet=Earth}}]}"),
                "Invalid GraphQL response received for nested query operation");
    }

    @Test(description = "Test GraphQL query operation with application/graphql content type")
    public void testGraphQLQueryOperationWithGraphQLContentType() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/graphql");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{hero{name, age}}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("name=hero-1, age=10"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "Test GraphQL query operation with invalid token", dependsOnMethods = {"testGraphQLQueryOperation",
            "testGraphQLQueryWithOperationName", "testGraphQLMutationOperation","testGraphQLMutationWithOperationName",
            "testGraphQLQueryOperationWithGraphQLContentType"})
    public void testGraphQLQueryOperationWithInvalidToken() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        requestHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + ACCESS_TOKEN + "0000");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"variables\":null,\"" +
                "query\":\"{hero{name, age}}\"}", requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Invoke GraphQL API resource with disabled security", dependsOnMethods =
            "testGraphQLQueryOperationWithInvalidToken")
    public void testGraphQLAPIResourceWithDisabledSecurity() throws Exception {
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse apiResponse = publisherRestClient.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(apiResponse.getData(), APIDTO.class);
        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equalsIgnoreCase("hero")) {
                        item.setAuthType("None");
                    }
                }
        );
        apidto.operations(operations);
        publisherRestClient.updateAPI(apidto, apiId);
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait until the API was deployed in Choreo Connect");

        requestHeaders.remove(HttpHeaderNames.AUTHORIZATION.toString());
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"variables\":null,\"" +
                "query\":\"{hero{name, age}}\"}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("name=hero-1, age=10"), "Invalid GraphQL response received for " +
                "query operation");
    }

    @Test(description = "Invoke multiple GraphQL API resources with disabled security. Since the request includes a " +
            "secured resource it should not allow to proceed the invocation without a valid authentication.",
            dependsOnMethods = "testGraphQLAPIResourceWithDisabledSecurity")
    public void testGraphQLAPIMultipleResourceWithDisabledSecurity() throws Exception {
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"query\":\"query MyQuery {\\n  " +
                "hello\\n  hero {\\n    age\\n  }\\n}\\n\",\"variables\":null,\"operationName\":\"MyQuery\"}", requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test GraphQL query operation with API key",
            dependsOnMethods = {"testGraphQLAPIMultipleResourceWithDisabledSecurity"})
    public void testGraphQLQueryOperationWithAPIKey() throws Exception {
        requestHeaders.remove(TestConstant.AUTHORIZATION_HEADER);
        requestHeaders.put("apikey", apiKey);
        requestHeaders.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"variables\":null,\"" +
                "query\":\"{address{planet}}\"}", requestHeaders);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("{planet=Earth}]}"), "Invalid GraphQL response received for " +
                "query operation");
    }

}
