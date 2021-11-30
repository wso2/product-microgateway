package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.gson.Gson;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PrototypedAPITestCase extends ApimBaseTest {
    private static final String SAMPLE_API_NAME = "PrototypedAPI";
    private static final String SAMPLE_API_CONTEXT = "prototypedApi";
    private static final String SAMPLE_API_VERSION = "1.0.0";
    private static final String APP_NAME = "APIKeyTestApp";
    private String apiId;
    private String endPoint;

    @BeforeClass(description = "Initialise the setup for API key tests")
    void start() throws Exception {
        super.initWithSuperTenant();

        String apiName = "APIMPrototypedEndpointAPI1";
        String apiContext = "pizzashack-prototype";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

//        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
        String apiEndPointUrl = "http://run.mocky.io/v2/5185415ba171ea3a00704eed";
        String apiProvider = "admin";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(SAMPLE_API_VERSION);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTags(apiTags);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest.setProvider(apiProvider);

        apiId = PublisherUtils.createAPI(apiRequest, publisherRestClient);

        WorkflowResponseDTO lcChangeResponse = publisherRestClient.changeAPILifeCycleStatus(
                apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = publisherRestClient.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + apiEndPointUrl + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apiDto.setEndpointConfig(endpoint);
        publisherRestClient.updateAPI(apiDto);

        Assert.assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");

        Thread.sleep(10000);
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
    }

    // When invoke with original token even though the tampered key is in the invalid key cache,
    // original token should pass.
    @Test(description = "Test to check the InternalKey is working")
    public void invokeInternalKeyHeaderSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        String internalKey = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, true);
        headers.put("Internal-Key", internalKey);
        org.wso2.choreo.connect.tests.util.HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/v2/pet/2") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

}
