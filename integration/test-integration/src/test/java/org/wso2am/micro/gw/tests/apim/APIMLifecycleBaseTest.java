/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2am.micro.gw.tests.apim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.context.MicroGWTestException;
import org.wso2am.micro.gw.tests.util.TestConstant;
import org.wso2am.micro.gw.tests.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

public class APIMLifecycleBaseTest extends APIMWithMgwBaseTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(APIMLifecycleBaseTest.class);

    /**
     * Map of the predefined API requests. Key represents the api name and the value represents the APIRequest.
     */
    private static Map<String, APIRequest> apiRequestListByAPIName = new LinkedHashMap<>();

    /**
     * Map that binds the index of the api request to the api name.
     */
    private static Map<Integer, String> apiRequestListByIndex = new LinkedHashMap<>();

    /**
     * Map of the predefined application info. Key represents the application name and the value represents the
     * Application instance.
     */
    private static Map<String, Application> applicationListByName = new LinkedHashMap<>();

    /**
     * Map that binds the index of the application to the application name.
     */
    private static Map<Integer, String> applicationListByIndex = new LinkedHashMap<>();

    static {
        // list of predefined applications.
        addToApplicationList("SubscriptionValidationTestApp",
                             new Application("SubscriptionValidationTestApp",
                                             "Test Application for SubscriptionValidationTestCase",
                                             TestConstant.APPLICATION_TIER.UNLIMITED,
                                             ApplicationDTO.TokenTypeEnum.JWT));

        // list of predefined api requests
        String apiName = "SubscriptionValidationTestAPI";
        String apiContext = "subscriptionValidationTestAPI";
        String apiEndPointPostfixUrl = "/v2";

        try {
            APIRequest apiRequest = new APIRequest(apiName, apiContext,
                                                   new URL(Utils.getDockerMockServiceURLHttp(apiEndPointPostfixUrl)));
            String API_VERSION_1_0_0 = "1.0.0";
            apiRequest.setVersion(API_VERSION_1_0_0);
            apiRequest.setTiersCollection(TestConstant.API_TIER.UNLIMITED);
            apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);

            APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
            apiOperationsDTO1.setVerb("GET");
            apiOperationsDTO1.setTarget("/pet/findByStatus");
            apiOperationsDTO1.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

            List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
            operationsDTOS.add(apiOperationsDTO1);
            apiRequest.setOperationsDTOS(operationsDTOS);

            addToAPIList(apiName, apiRequest);
        } catch (APIManagerIntegrationTestException | MalformedURLException e) {
            LOGGER.error("Error creating the APIRequest instance for API name: " + apiName);
        }
    }

    /**
     * Publish an API using REST.
     *
     * @param apiId                   - UUID of the API,
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws MicroGWTestException -  Exception throws by the method call of changeAPILifeCycleStatusToPublish() in
     *                              APIPublisherRestClient.java.
     */
    protected HttpResponse publishAPI(String apiId, RestAPIPublisherImpl publisherRestClient,
                                      boolean isRequireReSubscription) throws MicroGWTestException {
        String lifecycleChecklist = null;
        if (isRequireReSubscription) {
            lifecycleChecklist = "Requires re-subscription when publishing the API:true";
        }
        try {
            HttpResponse response = publisherRestClient.changeAPILifeCycleStatus(apiId,
                                                                                 APILifeCycleAction.PUBLISH.getAction(),
                                                                                 lifecycleChecklist);
            if (Objects.isNull(response)) {
                throw new MicroGWTestException("Error while publishing the API. API Id : " + apiId);
            }
            return response;
        } catch (ApiException e) {
            throw new MicroGWTestException("Error while publishing the API. API Id : " + apiId);
        }
    }

    /**
     * Subscribe to an API.
     *
     * @param apiId           - UUID of the API
     * @param applicationId   - UUID of the application
     * @param storeRestClient - Instance of APIPublisherRestClient
     * @return HttpResponse - Response of the API subscribe action
     * @throws MicroGWTestException if the response of the create subscription is null. This may null when there is an
     *                              error while subscribing to the API or when the subscription already exists.
     */
    protected HttpResponse subscribeToAPI(String apiId, String applicationId, String tier,
                                          RestAPIStoreImpl storeRestClient) throws MicroGWTestException {
        HttpResponse response = storeRestClient.createSubscription(apiId, applicationId, tier);
        if (Objects.isNull(response)) {
            throw new MicroGWTestException(
                    "Error while subscribing to the API. API Id : " + apiId + ", Application Id: " + applicationId);
        }
        return response;
    }

    /**
     * Create and publish an API.
     *
     * @param apiRequest              - Instance of APIRequest
     * @param publisherRestClient     - Instance of RestAPIPublisherImpl
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws MicroGWTestException - Exception throws by API create and publish activities.
     */
    protected String createAndPublishAPI(APIRequest apiRequest,
                                         RestAPIPublisherImpl publisherRestClient,
                                         boolean isRequireReSubscription)
            throws MicroGWTestException, ApiException {
        //Create the API
        HttpResponse createAPIResponse = publisherRestClient.addAPI(apiRequest);
        if (Objects.nonNull(createAPIResponse) && createAPIResponse.getResponseCode() == HttpStatus.SC_CREATED
                && !StringUtils.isEmpty(createAPIResponse.getData())) {
            LOGGER.info("API Created :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            // Create Revision and Deploy to Gateway
            try {
                createAPIRevisionAndDeploy(createAPIResponse.getData(), publisherRestClient);
            } catch (JSONException e) {
                throw new MicroGWTestException("Error in creating and deploying API Revision", e);
            }
            //Publish the API
            HttpResponse publishAPIResponse = publishAPI(createAPIResponse.getData(), publisherRestClient,
                                                         isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HttpStatus.SC_OK &&
                    APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()))) {
                throw new MicroGWTestException(
                        "Error in API Publishing" + getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:"
                                + publishAPIResponse.getResponseCode() + " Response Data :" + publishAPIResponse
                                .getData());
            }
            LOGGER.info("API Published :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            return createAPIResponse.getData();
        } else {
            throw new MicroGWTestException(
                    "Error in API Creation." + getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:"
                            + createAPIResponse.getResponseCode() + " Response Data :" + createAPIResponse.getData());
        }
    }

    /**
     * Create and publish a API with re-subscription not required.
     *
     * @param apiRequest          - Instance of APIRequest
     * @param publisherRestClient - Instance of RestAPIPublisherImpl
     * @throws MicroGWTestException - Exception throws by API create and publish activities.
     */
    protected String createAndPublishAPIWithoutRequireReSubscription(APIRequest apiRequest,
                                                                     RestAPIPublisherImpl publisherRestClient)
            throws MicroGWTestException, ApiException {
        return createAndPublishAPI(apiRequest, publisherRestClient, false);
    }

    /**
     * @param apiID               - API id.
     * @param newAPIVersion       - New API version need to create
     * @param publisherRestClient - Instance of RestAPIPublisherImpl
     * @throws ApiException Exception throws by the method call of copyAPIWithReturnDTO() in RestAPIPublisherImpl.java
     */
    protected APIDTO copyAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient)
            throws ApiException {
        //Copy API to version  to newVersion
        return publisherRestClient.copyAPIWithReturnDTO(newAPIVersion, apiID, false);
    }

    /**
     * Copy and publish the copied API.
     *
     * @param newAPIVersion           - New API version need to create
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws MicroGWTestException -Exception throws by copyAPI() and publishAPI() method calls
     */
    protected void copyAndPublishCopiedAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient,
                                           boolean isRequireReSubscription) throws MicroGWTestException, ApiException {
        APIDTO apidto = copyAPI(apiID, newAPIVersion, publisherRestClient);
        publishAPI(apidto.getId(), publisherRestClient, isRequireReSubscription);
    }

    /**
     * Create publish and subscribe a API using REST API.
     *
     * @param apiRequest          - Instance of APIRequest with all needed API information
     * @param publisherRestClient -  Instance of APIPublisherRestClient
     * @param storeRestClient     - Instance of APIStoreRestClient
     * @param applicationId       - UUID of the Application that the API need to subscribe.
     * @param tier                - Tier that needs to be subscribed.
     * @throws MicroGWTestException - Exception throws by API create publish and subscribe a API activities.
     */
    protected String createPublishAndSubscribeToAPI(APIRequest apiRequest,
                                                    RestAPIPublisherImpl publisherRestClient,
                                                    RestAPIStoreImpl storeRestClient, String applicationId,
                                                    String tier)
            throws MicroGWTestException, ApiException, XPathExpressionException {
        String apiId = createAndPublishAPI(apiRequest, publisherRestClient, false);
        waitForAPIDeploymentSync(user.getUserName(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseSubscribeAPI = subscribeToAPI(apiId, applicationId, tier, storeRestClient);
        if (!(httpResponseSubscribeAPI.getResponseCode() == HttpStatus.SC_OK &&
                !StringUtils.isEmpty(httpResponseSubscribeAPI.getData()))) {
            throw new MicroGWTestException("Error in API Subscribe." +
                                                   getAPIIdentifierStringFromAPIRequest(apiRequest) +
                                                   "Response Code:" + httpResponseSubscribeAPI
                    .getResponseCode());
        }
        LOGGER.info("API Subscribed :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
        return apiId;
    }

    /**
     * Create API Revision and Deploy to gateway using REST API.
     *
     * @param apiId            -  API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    protected String createAPIRevisionAndDeploy(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;
        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Retrieve Revision Info
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId, null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision : revisionList) {
            revisionUUID = revision.getString("id");
        }

        // Deploy Revision to gateway
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("Production and Sandbox");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                                                                                     apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());
        return revisionUUID;
    }

    /**
     * Undeploy and Delete API Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    protected String undeployAndDeleteAPIRevisions(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException, XPathExpressionException, MicroGWTestException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;

        // Get Deployed Revisions
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision : revisionList) {
            revisionUUID = revision.getString("id");
        }

        if (revisionUUID == null) {
            return null;
        }

        // Un deploy Revisions
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Production and Sandbox");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                                                                                         apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());

        // Get Revisions
        HttpResponse apiRevisionsFullGetResponse = restAPIPublisher.getAPIRevisions(apiId, null);
        assertEquals(apiRevisionsFullGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsFullGetResponse.getData());
        List<JSONObject> revisionFullList = new ArrayList<>();
        JSONObject jsonFullObject = new JSONObject(apiRevisionsFullGetResponse.getData());

        JSONArray arrayFullList = jsonFullObject.getJSONArray("list");
        for (int i = 0, l = arrayFullList.length(); i < l; i++) {
            revisionFullList.add(arrayFullList.getJSONObject(i));
        }
        for (JSONObject revision : revisionFullList) {
            revisionUUID = revision.getString("id");
            HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
            assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                         "Unable to delete API Revisions:" + apiRevisionsDeleteResponse.getData());
        }

        //Waiting for API un-deployment
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);
        waitForAPIDeploymentSync(user.getUserName(), apiDto.getName(), apiDto.getVersion(),
                                 APIMIntegrationConstants.IS_API_NOT_EXISTS);

        return revisionUUID;
    }

    /**
     * Create API Product Revision and Deploy to gateway using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher - Instance of APIPublisherRestClient
     */
    protected String createAPIProductRevisionAndDeploy(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;
        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIProductRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Retrieve Revision Info
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId, null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision : revisionList) {
            revisionUUID = revision.getString("id");
        }

        // Deploy Revision to gateway
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("Production and Sandbox");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIProductRevision(apiId, revisionUUID,
                                                                                            apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Unable to deploy API Product Revisions:" + apiRevisionsDeployResponse.getData());
        //Waiting for API deployment
        waitForAPIDeployment();
        return revisionUUID;
    }

    /**
     * Undeploy and Delete API Product Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher - Instance of APIPublisherRestClient
     */
    protected String undeployAndDeleteAPIProductRevisions(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;

        // Get Deployed Revisions
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIProductRevisions(apiId, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision : revisionList) {
            revisionUUID = revision.getString("id");
        }

        if (revisionUUID == null) {
            return null;
        }

        // Un deploy Revisions
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Production and Sandbox");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIProductRevision(apiId, revisionUUID,
                                                                                                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                     "Unable to Undeploy API Product Revisions:" + apiRevisionsUnDeployResponse.getData());

        // Get Revisions
        HttpResponse apiRevisionsFullGetResponse = restAPIPublisher.getAPIProductRevisions(apiId, null);
        assertEquals(apiRevisionsFullGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Unable to retrieve revisions" + apiRevisionsFullGetResponse.getData());
        List<JSONObject> revisionFullList = new ArrayList<>();
        JSONObject jsonFullObject = new JSONObject(apiRevisionsFullGetResponse.getData());

        JSONArray arrayFullList = jsonFullObject.getJSONArray("list");
        for (int i = 0, l = arrayFullList.length(); i < l; i++) {
            revisionFullList.add(arrayFullList.getJSONObject(i));
        }
        for (JSONObject revision : revisionFullList) {
            revisionUUID = revision.getString("id");
            HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIProductRevision(apiId, revisionUUID);
            assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                         "Unable to delete API Product Revisions:" + apiRevisionsDeleteResponse.getData());
        }

        //Waiting for API un-deployment
        waitForAPIDeployment();
        return revisionUUID;
    }

    /**
     * Return a String with combining the value of API Name, API Version, and API Provider Name as key:value format
     *
     * @param api - Instance of API object that includes the API Name, API Version, and API Provider Name to create the
     *            String
     * @return String - with API Name,API Version and API Provider Name as key:value format
     */
    private String getAPIIdentifierString(API api) {
        return " API Name:" + api.getName() + ", API Version:" + api.getVersion() +
                ", API Provider Name :" + api.getProvider() + " ";

    }

    /**
     * Return a String with combining the value of API Name, API Version, and API Provider Name as key:value format.
     *
     * @param apiRequest - Instance of APIRequest object  that include the  API Name,API Version and API Provider Name
     *                   to create the String
     * @return String - with API Name, API Version, and API Provider Name as key:value format
     */
    protected String getAPIIdentifierStringFromAPIRequest(APIRequest apiRequest) {
        return " API Name:" + apiRequest.getName() + " API Version:" + apiRequest.getVersion() +
                " API Provider Name :" + apiRequest.getProvider() + " ";
    }

    /**
     * Retrieve  the value from JSON object bu using the key.
     *
     * @param httpResponse - Response that containing the JSON object in it response data.
     * @param key          - key of the JSON value the need to retrieve.
     * @return String - The value of provided key as a String
     * @throws MicroGWTestException - Exception throws when resolving the JSON object in the HTTP response
     */
    protected String getValueFromJSON(HttpResponse httpResponse, String key) throws MicroGWTestException {
        try {
            JSONObject jsonObject = new JSONObject(httpResponse.getData());
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            throw new MicroGWTestException("Exception thrown when resolving the JSON object in the HTTP response ", e);
        }
    }

    /**
     * Create application for the given name using the restAPIStore. Then, create the client key and the client secret
     * associated to that application.
     *
     * @param appIndex     - index of the relevant application in the predefined list 'applicationListByIndex'
     * @param restAPIStore - instance of the RestAPIStoreImpl
     * @return the created application and associated client key and the client secret
     * @throws org.wso2.am.integration.clients.store.api.ApiException if error happens while generating the keys
     * @throws MicroGWTestException                                   if error happens while creating the application or
     *                                                                if the application already exists
     */
    protected ApplicationCreationResponse createApplicationAndClientKeyClientSecret(int appIndex,
                                                                                    RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException, MicroGWTestException {
        return createApplicationAndClientKeyClientSecret(applicationListByIndex.get(appIndex), restAPIStore);
    }

    /**
     * Create application for the given name using the restAPIStore. Then, create the client key and the client secret
     * associated to that application.
     *
     * @param appName      - name of the application to be created as mentioned in the predefined list
     *                     'applicationListByName'
     * @param restAPIStore - instance of the RestAPIStoreImpl
     * @return the created application and associated client key and the client secret
     * @throws org.wso2.am.integration.clients.store.api.ApiException if error happens while generating the keys
     * @throws MicroGWTestException                                   if error happens while creating the application or
     *                                                                if the application already exists
     */
    protected ApplicationCreationResponse createApplicationAndClientKeyClientSecret(String appName,
                                                                                    RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException, MicroGWTestException {
        Application newApp = applicationListByName.get(appName);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationResponse =
                restAPIStore.createApplication(newApp.getAppName(), newApp.getDescription(), newApp.getThrottleTier(),
                                               newApp.getTokenType());
        if (Objects.isNull(applicationResponse)) {
            throw new MicroGWTestException("Could not create the application: " + appName);
        }
        String applicationId = applicationResponse.getData();

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId,
                                                                        TestConstant.DEFAULT_TOKEN_VALIDITY_TIME, "",
                                                                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                                                                        null, grantTypes);

        return new ApplicationCreationResponse(applicationId, applicationKeyDTO.getConsumerKey(),
                                               applicationKeyDTO.getConsumerSecret());
    }

    /**
     * Create list of applications for the given list of names using the restAPIStore. Then, create the client key and
     * the client secret associated to those applications.
     *
     * @param appNameList  - list of application names to be created as mentioned in the predefined list
     *                     'applicationListByName'
     * @param restAPIStore - instance of the RestAPIStoreImpl
     * @return map of created application and associated client key and the client secret
     * @throws org.wso2.am.integration.clients.store.api.ApiException if error happens while generating the keys
     * @throws MicroGWTestException                                   if error happens while creating the application or
     *                                                                if the application already exists
     */
    public Map<String, ApplicationCreationResponse> createApplicationAndClientKeyClientSecretbyListOfAppNames(
            List<String> appNameList, RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException, MicroGWTestException {
        Map<String, ApplicationCreationResponse> response = new HashMap<>();
        for (String appName : appNameList) {
            response.put(appName, createApplicationAndClientKeyClientSecret(appName, restAPIStore));
        }
        return response;
    }

    /**
     * Create list of applications for the given list of indices in the predefined app list 'applicationListByIndex'.
     * Then, create the client key and the client secret associated to those applications.
     *
     * @param appIndexList - list of application indices to be created as mentioned in the predefined list
     *                     'applicationListByIndex'
     * @param restAPIStore - instance of the RestAPIStoreImpl
     * @return map of created application and associated client key and the client secret
     * @throws org.wso2.am.integration.clients.store.api.ApiException if error happens while generating the keys
     * @throws MicroGWTestException                                   if error happens while creating the application or
     *                                                                if the application already exists
     */
    public Map<String, ApplicationCreationResponse> createApplicationAndClientKeyClientSecretByListOfAppIndices(
            List<Integer> appIndexList, RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException, MicroGWTestException {
        Map<String, ApplicationCreationResponse> response = new HashMap<>();
        for (int appIndex : appIndexList) {
            String appName = applicationListByIndex.get(appIndex);
            response.put(appName, createApplicationAndClientKeyClientSecret(appName, restAPIStore));
        }
        return response;
    }

    /**
     * Generate the user access token for the grant type password.
     *
     * @param consumerKey    - consumer key
     * @param consumerSecret - consumer secret
     * @param scopes         - scopes
     * @param user           - user
     * @param restAPIStore   - instance of the RestAPIStoreImpl
     * @return the user access token
     * @throws MalformedURLException if the URL is malformed
     * @throws MicroGWTestException  if an error occurs while generating the user access token
     */
    protected String generateUserAccessToken(String consumerKey, String consumerSecret, String[] scopes,
                                             User user, RestAPIStoreImpl restAPIStore)
            throws MalformedURLException, MicroGWTestException {
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() +
                "&scope=" + String.join(" ", scopes);
        URL tokenEndpointURL = new URL(apimServiceURLHttps + "oauth2/token");

        JSONObject accessTokenGenerationResponse;
        try {
            accessTokenGenerationResponse = new JSONObject(
                    restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                            .getData());
        } catch (APIManagerIntegrationTestException e) {
            throw new MicroGWTestException("Error occurred while generating the user access token.", e);
        }
        return accessTokenGenerationResponse.getString("access_token");
    }

    private static void addToApplicationList(String appName, Application application) {
        applicationListByName.put(appName, application);
        applicationListByIndex.put(applicationListByIndex.size(), appName);
    }

    private static void addToAPIList(String apiName, APIRequest apiRequest) {
        apiRequestListByAPIName.put(apiName, apiRequest);
        apiRequestListByIndex.put(apiRequestListByIndex.size(), apiName);
    }

    protected static APIRequest getAPIRequest(String apiName) {
        return apiRequestListByAPIName.get(apiName);
    }

    protected static APIRequest getAPIRequest(int apiIndex) {
        String apiName = apiRequestListByIndex.get(apiIndex);
        return apiRequestListByAPIName.get(apiName);
    }


    /**
     * This class represents the application details required to create an application using the RestAPIStoreImpl.
     */
    private static class Application {
        private String appName;
        private String description;
        private String throttleTier;
        private ApplicationDTO.TokenTypeEnum tokenType;

        Application(String appName, String description, String throttleTier,
                    ApplicationDTO.TokenTypeEnum tokenType) {
            this.appName = appName;
            this.description = description;
            this.throttleTier = throttleTier;
            this.tokenType = tokenType;
        }

        String getAppName() {
            return appName;
        }

        String getDescription() {
            return description;
        }

        String getThrottleTier() {
            return throttleTier;
        }

        org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum getTokenType() {
            return tokenType;
        }
    }

    /**
     * This class represents the application creation response that wraps the application id, and the associated
     * consumer key and the consume secret.
     */
    protected static class ApplicationCreationResponse {
        private String applicationId;
        private String consumerKey;
        private String consumerSecret;

        ApplicationCreationResponse(String applicationId, String consumerKey, String consumerSecret) {
            this.applicationId = applicationId;
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getConsumerKey() {
            return consumerKey;
        }

        public String getConsumerSecret() {
            return consumerSecret;
        }
    }

}
