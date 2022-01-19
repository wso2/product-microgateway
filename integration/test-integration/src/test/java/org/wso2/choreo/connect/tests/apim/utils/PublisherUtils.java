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
package org.wso2.choreo.connect.tests.apim.utils;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.testng.Assert.assertEquals;

public class PublisherUtils {
    private static final Logger log = LoggerFactory.getLogger(PublisherUtils.class);

    /**
     * Create and publish an API
     *
     * @param apiRequest            An APIRequest object
     * @param publisherRestClient   Instance of RestAPIPublisherImpl
     * @return API id
     * @throws CCTestException if an error occurs while creating or publishing the API
     */
    public static String createAndPublishAPI(APIRequest apiRequest,
                                             RestAPIPublisherImpl publisherRestClient) throws CCTestException {
        return createAndPublishAPI(apiRequest, "localhost", publisherRestClient, false);
    }

    /**
     * Creates API using a given OAS file in publisher.
     *
     * @param apiProperties API Properties containing apiName, apiContext, apiVersion,
     *                      userName and other necessary params for the API to be created
     * @param filePath File path for the OAS file
     * @param publisherRestClient An instance of RestAPIPublisherImpl
     * @return API id as a string
     * @throws MalformedURLException
     * @throws ApiException
     */
    public static String createAPIUsingOAS(JSONObject apiProperties,
                                           String filePath, RestAPIPublisherImpl publisherRestClient)
                                           throws MalformedURLException, CCTestException {
        File definition = new File(filePath);
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)).toString());

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        APIDTO apiDTO;
        try {
            apiDTO = publisherRestClient.importOASDefinition(definition, apiProperties.toString());
        } catch (ApiException e) {
            log.error("Error occurred while importing OpenAPI definition to APIM. Response: {}", e.getResponseBody());
            throw new CCTestException("Error while creating an API", e);
        }
        return apiDTO.getId();
    }

    /**
     * Create and publish an API
     *
     * @param apiRequest              Instance of APIRequest
     * @param vhost                   Vhost to deploy
     * @param publisherRestClient     Instance of RestAPIPublisherImpl
     * @param isRequireReSubscription If publish with re-subscription required option true else false.
     * @throws CCTestException - Exception thrown by API create and publish activities.
     */
    public static String createAndPublishAPI(APIRequest apiRequest, String vhost,
                                             RestAPIPublisherImpl publisherRestClient,
                                             boolean isRequireReSubscription) throws CCTestException {
        String apiId = createAPI(apiRequest, publisherRestClient);
        deployAndPublishAPI(apiId, apiRequest.getName(), vhost, publisherRestClient);
        return apiId;
    }

    /**
     * Create an API
     *
     * @param apiRequest            An APIRequest object
     * @param publisherRestClient   Instance of RestAPIPublisherImpl
     * @return API ID
     * @throws CCTestException if an error occurs while creating an API
     */
    public static String createAPI(APIRequest apiRequest, RestAPIPublisherImpl publisherRestClient) throws CCTestException {
        HttpResponse createAPIResponse;
        try {
            createAPIResponse = publisherRestClient.addAPI(apiRequest);
        } catch (ApiException e) {
            log.error(e.getMessage());
            throw new CCTestException("Error while creating an API", e);
        }
        if (Objects.nonNull(createAPIResponse) && createAPIResponse.getResponseCode() == HttpStatus.SC_CREATED
                && !StringUtils.isEmpty(createAPIResponse.getData())) {
            log.info("API Created. " + getAPIIdentifierStringFromAPIRequest(apiRequest));
            return createAPIResponse.getData();
        } else {
            String errorMsg = "Error in API Creation. " + getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:"
                            + createAPIResponse.getResponseCode() + " Response Data :" + createAPIResponse.getData();
            log.error(errorMsg);
            throw new CCTestException(errorMsg);
        }
    }

    /**
     * Deploy and publish API
     *
     * @param apiId                     API ID
     * @param apiName                   API name (for logging purposes)
     * @param vhost                     VHost
     * @param publisherRestClient       Instance of RestAPIPublisherImpl
     * @return revisionUUID
     * @throws CCTestException if an error occurs while deploying or publishing an API
     */
    public static String deployAndPublishAPI(String apiId, String apiName, String vhost,
                                             RestAPIPublisherImpl publisherRestClient)
            throws CCTestException {
        String revisionUUID;
        // Create Revision and Deploy to Gateway
        try {
            revisionUUID = createAPIRevisionAndDeploy(apiId, vhost, publisherRestClient);
        } catch (JSONException | ApiException e) {
            log.error(e.getMessage());
            throw new CCTestException("Error while creating and deploying API Revision", e);
        }
        log.info("API Deployed. Name:" + apiName + " VHost:" + vhost);

        publishAPI(apiId, apiName, publisherRestClient);
        return revisionUUID;
    }



    public static APIRequest createSampleAPIRequest(String apiName, String apiContext, String apiVersion,
                                                    String provider) throws CCTestException {
        APIRequest apiRequest;
        try {
            apiRequest = new APIRequest(apiName, apiContext,
                    new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)));
        } catch (MalformedURLException | APIManagerIntegrationTestException e) {
            throw new CCTestException("Error while creating API Request", e);
        }
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(provider);
        apiRequest.setTiersCollection(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/pet/findByStatus");
        apiOperationsDTO1.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("GET");
        apiOperationsDTO2.setTarget("/store/inventory");
        apiOperationsDTO2.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        operationsDTOS.add(apiOperationsDTO2);
        apiRequest.setOperationsDTOS(operationsDTOS);
        return apiRequest;
    }

    /**
     * Create API Revision and Deploy to gateway using REST API.
     *
     * @param apiId            -  API UUID
     * @param publisherRestClient -  Instance of APIPublisherRestClient
     * @return revisionUUID
     */
    public static String createAPIRevisionAndDeploy(String apiId, RestAPIPublisherImpl publisherRestClient)
            throws ApiException, JSONException {
        return createAPIRevisionAndDeploy(apiId, "localhost", publisherRestClient);
    }

    /**
     * Create API Revision and Deploy to gateway with provided vhost using REST API.
     *
     * @param apiId            -  API UUID
     * @param vhost            -  VHost to deploy the API
     * @param publisherRestClient -  Instance of APIPublisherRestClient
     * @return revisionUUID
     */
    public static String createAPIRevisionAndDeploy(String apiId, String vhost, RestAPIPublisherImpl publisherRestClient)
            throws ApiException, JSONException {

        String revisionUUID = createAPIRevision(apiId, publisherRestClient);

        // Deploy API revision
        List<APIRevisionDeployUndeployRequest> deployRequestList = getDeployRequestList("Default", vhost);
        publisherRestClient.deployAPIRevision(apiId, revisionUUID, deployRequestList);
        return revisionUUID;
    }

    public static String createAPIRevision(String apiId, RestAPIPublisherImpl publisherRestClient)
            throws ApiException, JSONException {

        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = publisherRestClient.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Read revision ID from response
        JSONObject apiRevisionJsonObject = new JSONObject(apiRevisionResponse.getData());
        String revisionUUID = apiRevisionJsonObject.getString("id");
        return revisionUUID;
    }

    /**
     * Create API Product Revision and Deploy to gateway using REST API.
     *
     * @param apiId            - API UUID
     * @param publisherRestClient - Instance of APIPublisherRestClient
     */
    public static String createAPIProductRevisionAndDeploy(String apiId, String vhost,
           RestAPIPublisherImpl publisherRestClient) throws ApiException, JSONException {

        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = publisherRestClient.addAPIProductRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Read revision ID from response
        JSONObject apiRevisionJsonObject = new JSONObject(apiRevisionResponse.getData());
        String revisionUUID = apiRevisionJsonObject.getString("id");

        // Deploy API Product revision
        List<APIRevisionDeployUndeployRequest> deployRequestList = getDeployRequestList("Default", vhost);
        HttpResponse deployResponse = publisherRestClient.deployAPIProductRevision(apiId, revisionUUID,
                deployRequestList);
        assertEquals(deployResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Unable to deploy API Product Revisions:" + deployResponse.getData());
        return revisionUUID;
    }

    private static List<APIRevisionDeployUndeployRequest> getDeployRequestList(String envName, String vhost) {
        // Deploy Revision to gateway
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(envName);
        apiRevisionDeployRequest.setVhost(vhost);
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        return apiRevisionDeployRequestList;
    }

    /**
     * Copy and publish the copied API.
     *
     * @param newAPIVersion           - New API version need to create
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws CCTestException -Exception throws by copyAPI() and publishAPI() method calls
     */
    public static void copyAndPublishCopiedAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient,
                                           boolean isRequireReSubscription) throws CCTestException, ApiException {
        APIDTO apidto = copyAPI(apiID, newAPIVersion, publisherRestClient);
        changeLCStateAPI(apidto.getId(), APILifeCycleAction.PUBLISH.getAction(), publisherRestClient, isRequireReSubscription);
    }

    /**
     * @param apiID               - API id.
     * @param newAPIVersion       - New API version need to create
     * @param publisherRestClient - Instance of RestAPIPublisherImpl
     * @throws ApiException Exception throws by the method call of copyAPIWithReturnDTO() in RestAPIPublisherImpl.java
     */
    public static APIDTO copyAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient)
            throws ApiException {
        //Copy API to version  to newVersion
        return publisherRestClient.copyAPIWithReturnDTO(newAPIVersion, apiID, false);
    }

    public static void deployRevision(String apiId, String revisionUUID, String vhost,
                                      RestAPIPublisherImpl publisherRestClient) throws ApiException {
        List<APIRevisionDeployUndeployRequest> deployRequestList = getDeployRequestList("Default", vhost);
        publisherRestClient.deployAPIRevision(apiId, revisionUUID, deployRequestList);
    }

    public static void publishAPI(String apiId, String apiName,
                                  RestAPIPublisherImpl publisherRestClient) throws CCTestException {
        HttpResponse publishAPIResponse;
        try {
            publishAPIResponse = changeLCStateAPI(apiId,
                    APILifeCycleAction.PUBLISH.getAction(), publisherRestClient, false);
        } catch (CCTestException e) {
            log.error("Error while Publishing API:" + apiName + "API ID:" + apiId);
            throw e;
        }

        if (!(publishAPIResponse.getResponseCode() == HttpStatus.SC_OK && APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()))) {
            String errorMsg = "Error while Publishing API:" + apiName + "Response Code:"
                            + publishAPIResponse.getResponseCode() + " Response Data :" + publishAPIResponse
                            .getData();
            log.error(errorMsg);
            throw new CCTestException(errorMsg);
        }
        log.info("API Published. Name:" + apiName);
    }

    public static void undeployAPI(String apiId, String revisionUUID, RestAPIPublisherImpl publisherRestClient)
            throws ApiException {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Default");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        publisherRestClient.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
    }

    /**
     * Undeploy and Delete API Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param publisherRestClient -  Instance of APIPublisherRestClient
     */
    public static String undeployAndDeleteAPIRevisions(String apiId, RestAPIPublisherImpl publisherRestClient)
            throws JSONException, ApiException {

        // Get Deployed Revisions
        HttpResponse apiRevisionsGetResponse = publisherRestClient.getAPIRevisions(apiId, "deployed:true");
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        JSONObject firstInTheList = arrayList.getJSONObject(0);
        String revisionUUID = firstInTheList.getString("id");

        // Undeploy Revisions
        undeployAPI(apiId, revisionUUID, publisherRestClient);

        publisherRestClient.deleteAPIRevision(apiId, revisionUUID);
        return revisionUUID;
    }

    /**
     * Undeploy and Delete API Product Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param publisherRestClient - Instance of APIPublisherRestClient
     */
    public static String undeployAndDeleteAPIProductRevisions(String apiId, RestAPIPublisherImpl publisherRestClient)
            throws ApiException, JSONException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();

        // Get Deployed Revisions
        HttpResponse apiRevisionsGetResponse = publisherRestClient.getAPIProductRevisions(apiId, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        JSONObject firstInTheList = arrayList.getJSONObject(0);
        String revisionUUID = firstInTheList.getString("id");

        // Un deploy Revisions
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Default");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = publisherRestClient.undeployAPIProductRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Product Revisions:" + apiRevisionsUnDeployResponse.getData());

        // Get Revisions
        publisherRestClient.deleteAPIProductRevision(apiId, revisionUUID);
        return revisionUUID;
    }

    /**
     * Publish an API using REST
     *
     * @param apiId                   - UUID of the API,
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If set to true, users need to re subscribe to the API although they may have subscribed to an older version.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws CCTestException -  Exception thrown by the method call of changeAPILifeCycleStatusToPublish() in
     *                              APIPublisherRestClient.java.
     */
    public static HttpResponse changeLCStateAPI(String apiId, String targetState, RestAPIPublisherImpl publisherRestClient,
                                            boolean isRequireReSubscription) throws CCTestException {
        String lifecycleChecklist = null;
        if (isRequireReSubscription) {
            lifecycleChecklist = "Requires re-subscription when publishing the API:true";
        }
        try {
            HttpResponse response = publisherRestClient
                    .changeAPILifeCycleStatus(apiId, targetState, lifecycleChecklist);
            if (Objects.isNull(response)) {
                log.error("Received a null response when changing lifecycle status to " + targetState);
                throw new CCTestException("Error while changing lifecycle status to " + targetState + ". API Id : " + apiId);
            }
            return response;
        } catch (ApiException e) {
            log.error(e.getResponseBody());
            throw new CCTestException("Error while changing lifecycle status to " + targetState + ". API Id : " + apiId);
        }
    }

    /**
     * Remove all APIs accessible via a given Store REST API client
     * @param publisherRestClient - an instance of RestAPIPublisherImpl
     * @throws CCTestException if an error occurs while removing APIs from API Manager
     */
    public static void removeAllApisFromPublisher(RestAPIPublisherImpl publisherRestClient) throws CCTestException {
        if (Objects.isNull(publisherRestClient)) {
            return;
        }
        try {
            APIListDTO apiListDTO = publisherRestClient.getAllAPIs();
            if (apiListDTO != null && apiListDTO.getList() != null) {
                for (APIInfoDTO apiInfoDTO : apiListDTO.getList()) {
                    publisherRestClient.deleteAPI(apiInfoDTO.getId());
                }
            }
        } catch (APIManagerIntegrationTestException | ApiException e) {
            log.error(e.getMessage());
            throw new CCTestException("Error while removing APIs from Publisher", e);
        }
    }

    public static void removeAllApiProductsFromPublisher(RestAPIPublisherImpl publisherRestClient) throws CCTestException {
        if (Objects.isNull(publisherRestClient)) {
            return;
        }
        try {
            APIProductListDTO allApiProducts = publisherRestClient.getAllApiProducts();
            List<APIProductInfoDTO> apiProductListDTO = allApiProducts.getList();

            if (apiProductListDTO != null) {
                for (APIProductInfoDTO apiProductInfoDTO : apiProductListDTO) {
                    publisherRestClient.deleteApiProduct(apiProductInfoDTO.getId());
                }
            }
        } catch (ApiException e) {
            throw new CCTestException("Error while removing API Products from Publisher", e);
        }
    }

    /**
     * Return a String with combining the value of API Name, API Version, and API Provider Name as key:value format.
     *
     * @param apiRequest - Instance of APIRequest object  that include the  API Name,API Version and API Provider Name
     *                   to create the String
     * @return String - with API Name, API Version, and API Provider Name as key:value format
     */
    public static String getAPIIdentifierStringFromAPIRequest(APIRequest apiRequest) {
        return "Name:" + apiRequest.getName() + " Version:" + apiRequest.getVersion() +
                " Provider:" + apiRequest.getProvider() + " Context:" + apiRequest.getContext();
    }

}
