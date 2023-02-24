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

import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLCustomComplexityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLQueryComplexityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaTypeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaTypeListDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class GraphQLQueryAnalysisTestCase extends GraphQLBaseTest {
    private static final String APPLICATION_NAME = "GQLComplexityApp";

    private String apiId;
    private String applicationId;
    private String subscriptionId;
    private static String ACCESS_TOKEN;
    private static String endpointURL;

    private static final String API_NAME = "GraphQLQueryAnalysisAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String API_CONTEXT = "gqlQueryAnalysis";
    Map<String, String> headers = new HashMap<>();

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL for GQL API invocation")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        SubscriptionThrottlePolicyDTO subsThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        subsThrottlePolicyDTO.setPolicyId("0c6439fd-9b16-3c2e-be6e-1236e0b9aa92");
        setSubscriptionThrottlePolicyDTO(subsThrottlePolicyDTO, 2, 4, "Platinum1");
        ApiResponse<SubscriptionThrottlePolicyDTO> addedQueryPolicy = adminRestClient.addSubscriptionThrottlingPolicy(
                subsThrottlePolicyDTO);
        Assert.assertEquals(addedQueryPolicy.getStatusCode(), HttpStatus.SC_CREATED);

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT,
                new URL(Utils.getDockerMockGraphQLServiceURLHttp(TestConstant.MOCK_GRAPHQL_BASEPATH)));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setApiTier(TestConstant.API_TIER.UNLIMITED);
        apiId = PublisherUtils.createGraphQLApiFromSchema(apiRequest, publisherRestClient, "Platinum1");
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till GraphQL API creation.");

        // get GraphQL API
        GraphQLSchemaTypeListDTO graphQLSchemaTypeList = publisherRestClient.getGraphQLSchemaTypeList(apiId);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse response =
                publisherRestClient.getGraphQLSchemaTypeListResponse(apiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());

        // add GraphQL Complexity Details
        List<GraphQLSchemaTypeDTO> list = graphQLSchemaTypeList.getTypeList();
        List<GraphQLCustomComplexityInfoDTO> complexityList = new ArrayList<>();
        for (GraphQLSchemaTypeDTO graphQLSchemaTypeDTO : list) {
            List<String> fieldList = graphQLSchemaTypeDTO.getFieldList();
            for (String field : fieldList) {
                GraphQLCustomComplexityInfoDTO graphQLCustomComplexityInfoDTO = new GraphQLCustomComplexityInfoDTO();
                graphQLCustomComplexityInfoDTO.setType(graphQLSchemaTypeDTO.getType());
                if (field.equalsIgnoreCase("hello")) {
                    graphQLCustomComplexityInfoDTO.setComplexityValue(10);
                } else if (field.equalsIgnoreCase("village")) {
                    graphQLCustomComplexityInfoDTO.setComplexityValue(2);
                } else {
                    graphQLCustomComplexityInfoDTO.setComplexityValue(1);
                }
                graphQLCustomComplexityInfoDTO.setField(field);
                complexityList.add(graphQLCustomComplexityInfoDTO);
            }
        }
        GraphQLQueryComplexityInfoDTO graphQLQueryComplexityInfoDTO = new GraphQLQueryComplexityInfoDTO();
        graphQLQueryComplexityInfoDTO.setList(complexityList);
        publisherRestClient.addGraphQLComplexityDetails(graphQLQueryComplexityInfoDTO, apiId);

        PublisherUtils.deployAndPublishAPI(apiId, API_NAME, TestConstant.LOCAL_HOST_NAME, publisherRestClient);

        // creating the application
        Application app = new Application(APPLICATION_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        applicationId = StoreUtils.createApplication(app, storeRestClient);

        subscriptionId = StoreUtils.subscribeToAPI(apiId, applicationId, "Platinum1", storeRestClient);
        // this is to wait until policy deployment is complete in case it didn't complete already
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till the API subscription.");

        ACCESS_TOKEN = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId, user, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for " +
                "API-M access token");
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/" + API_VERSION);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + ACCESS_TOKEN);
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json");
    }

    @Test(description = "Test GraphQL query complexity")
    public void testGraphQLQueryComplexity() throws Exception {
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"variables\":null,\"query\":\"query " +
                "CCGQLQuery {\\n  hello\\n}\\n\"}", headers);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertEquals(response.getResponseCode(),
                com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST,
                "Response code mismatched");
        Assert.assertTrue(responseData.contains("maximum query complexity exceeded"),
                "Invalid response message received for query complexity.");
    }

    @Test(description = "Test GraphQL query depth")
    public void testGraphQLQueryDepth() throws Exception {
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"query\":\"query MyQuery " +
                "{ hero { age name location { village planet} } }\",\"variables\":null,\"operationName\":\"MyQuery\"}", headers);
        String responseData = response.getData();
        Assert.assertNotNull(response, "Empty response received for GraphQL query operation");
        Assert.assertTrue(responseData.contains("maximum query depth exceeded"), "Invalid response message received for query complexity.");
        Assert.assertEquals(response.getResponseCode(),
                com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST,
                "Response code mismatched");
    }

    @Test(description = "Test GraphQL query analysis for successful scenario")
    public void testGraphQLQueryAnalysisForSuccessfulScenario() throws Exception {
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, "{\"query\":\"query MyQuery { address" +
                " { planet } }\",\"variables\":null,\"operationName\":\"MyQuery\"}", headers);
        String responseData = response.getData();
        Assert.assertEquals(response.getResponseCode(),
                com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response, "Empty response received for GraphQL mutation operation");
        Assert.assertTrue(responseData.contains("{address=[{planet=Earth}]}"),
                "Invalid GraphQL response received for query operation");
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
    }
}
