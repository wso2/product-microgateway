package org.wso2.choreo.connect.tests.apim.utils;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class PublisherUtils {
    private static final Logger log = LoggerFactory.getLogger(PublisherUtils.class);

    /**
     * Create and publish an API.
     *
     * @param apiRequest              - Instance of APIRequest
     * @param publisherRestClient     - Instance of RestAPIPublisherImpl
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws CCTestException - Exception throws by API create and publish activities.
     */
    public static String createAndPublishAPI(APIRequest apiRequest,
                                         RestAPIPublisherImpl publisherRestClient,
                                         boolean isRequireReSubscription)
            throws CCTestException, ApiException {
        return createAndPublishAPI(apiRequest, "localhost", publisherRestClient, isRequireReSubscription);
    }

    /**
     * Create API Product Revision and Deploy to gateway using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher - Instance of APIPublisherRestClient
     */
    public static String createAPIProductRevisionAndDeploy(String apiId, RestAPIPublisherImpl restAPIPublisher)
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
        apiRevisionDeployRequest.setName("Default");
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIProductRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Product Revisions:" + apiRevisionsDeployResponse.getData());
        return revisionUUID;
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
     * Undeploy and Delete API Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    public static String undeployAndDeleteAPIRevisions(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException, XPathExpressionException, CCTestException {
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
        apiRevisionUnDeployRequest.setName("Default");
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
        //waitForAPIDeploymentSync(user.getUserName(), apiDto.getName(), apiDto.getVersion(), APIMIntegrationConstants.IS_API_NOT_EXISTS);

        return revisionUUID;
    }

    /**
     * Undeploy and Delete API Product Revisions using REST API.
     *
     * @param apiId            - API UUID
     * @param restAPIPublisher - Instance of APIPublisherRestClient
     */
    public static String undeployAndDeleteAPIProductRevisions(String apiId, RestAPIPublisherImpl restAPIPublisher)
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
        apiRevisionUnDeployRequest.setName("Default");
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
        return revisionUUID;
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

    /**
     * Create and publish an API.
     *
     * @param apiRequest              - Instance of APIRequest
     * @param vhost                   - Vhost to deploy
     * @param publisherRestClient     - Instance of RestAPIPublisherImpl
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws CCTestException - Exception throws by API create and publish activities.
     */
    public static String createAndPublishAPI(APIRequest apiRequest, String vhost,
                                         RestAPIPublisherImpl publisherRestClient,
                                         boolean isRequireReSubscription)
            throws CCTestException, ApiException {
        //Create the API
        HttpResponse createAPIResponse = publisherRestClient.addAPI(apiRequest);
        if (Objects.nonNull(createAPIResponse) && createAPIResponse.getResponseCode() == HttpStatus.SC_CREATED
                && !StringUtils.isEmpty(createAPIResponse.getData())) {
            log.info("API Created :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            // Create Revision and Deploy to Gateway
            try {
                createAPIRevisionAndDeploy(createAPIResponse.getData(), vhost, publisherRestClient);
            } catch (JSONException e) {
                throw new CCTestException("Error in creating and deploying API Revision", e);
            }
            //Publish the API
            HttpResponse publishAPIResponse = changeLCStateAPI(createAPIResponse.getData(),
                    APILifeCycleAction.PUBLISH.getAction(), publisherRestClient, isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HttpStatus.SC_OK && APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()))) {
                throw new CCTestException(
                        "Error in API Publishing" + getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:"
                                + publishAPIResponse.getResponseCode() + " Response Data :" + publishAPIResponse
                                .getData());
            }
            log.info("API Published :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            return createAPIResponse.getData();
        } else {
            throw new CCTestException(
                    "Error in API Creation." + getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:"
                            + createAPIResponse.getResponseCode() + " Response Data :" + createAPIResponse.getData());
        }
    }

    /**
     * Create API Revision and Deploy to gateway using REST API.
     *
     * @param apiId            -  API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    public static String createAPIRevisionAndDeploy(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException {
        return createAPIRevisionAndDeploy(apiId, "localhost", restAPIPublisher);
    }

    /**
     * Create API Revision and Deploy to gateway with provided vhost using REST API.
     *
     * @param apiId            -  API UUID
     * @param vhost            -  VHost to deploy the API
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    public static String createAPIRevisionAndDeploy(String apiId, String vhost, RestAPIPublisherImpl publisherRestClient)
            throws ApiException, JSONException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;
        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = publisherRestClient.addAPIRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Retrieve Revision Info
        HttpResponse apiRevisionsGetResponse = publisherRestClient.getAPIRevisions(apiId, null);
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
        apiRevisionDeployRequest.setName("Default");
        apiRevisionDeployRequest.setVhost(vhost);
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = publisherRestClient.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());
        return revisionUUID;
    }

    /**
     * Publish an API using REST.
     *
     * @param apiId                   - UUID of the API,
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws CCTestException -  Exception throws by the method call of changeAPILifeCycleStatusToPublish() in
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
                throw new CCTestException("Error while publishing the API. API Id : " + apiId);
            }
            return response;
        } catch (ApiException e) {
            throw new CCTestException("Error while publishing the API. API Id : " + apiId);
        }
    }

    public static APIRequest createSampleAPIRequest(String apiName, String apiContext, String apiVersion)
            throws MalformedURLException, APIManagerIntegrationTestException {

        APIRequest apiRequest = new APIRequest(apiName, apiContext,
                new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)));
        apiRequest.setVersion(apiVersion);
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
     * Return a String with combining the value of API Name, API Version, and API Provider Name as key:value format.
     *
     * @param apiRequest - Instance of APIRequest object  that include the  API Name,API Version and API Provider Name
     *                   to create the String
     * @return String - with API Name, API Version, and API Provider Name as key:value format
     */
    public static String getAPIIdentifierStringFromAPIRequest(APIRequest apiRequest) {
        return " API Name:" + apiRequest.getName() + " API Version:" + apiRequest.getVersion() +
                " API Provider Name :" + apiRequest.getProvider() + " ";
    }

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

}
