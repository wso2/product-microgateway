package org.wso2.choreo.connect.tests.testCases.throttle;

import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class AdvanceThrottlingTestCase extends APIMLifecycleBaseTest {
    private static final Logger log = LogManager.getLogger(AdvanceThrottlingTestCase.class);
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final String apiPolicyName = "APIPolicyWithDefaultLimit";
    private final long limit5Req = 5L;
    private APIRequest apiRequest;
    private String apiId;
    private String applicationId;
    private String endpointURL;
    private String apiPolicyId;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.init();
        RequestCountLimitDTO threePerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit5Req);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, threePerMin, null);

        // creating the application
        ApplicationDTO app = new ApplicationDTO();
        app.setName("AdvanceThrottlingApp");
        app.setDescription("Test Application for AdvanceThrottling");
        app.setThrottlingPolicy(TestConstant.APPLICATION_TIER.UNLIMITED);
        app.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(app, restAPIStore);
        applicationId = appCreationResponse.getApplicationId();

        // create the advanced throttling policy with no conditions
        AdvancedThrottlePolicyDTO requestCountAdvancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName, "", "", false, defaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> addedApiPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountAdvancedPolicyDTO);

        // assert the status code and policy ID
        Assert.assertEquals(addedApiPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedApiPolicy.getData();
        apiPolicyId = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        // create the request headers after generating the access token
        String accessToken = generateUserAccessToken(appCreationResponse.getConsumerKey(),
                appCreationResponse.getConsumerSecret(), new String[]{}, user, restAPIStore);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        apiRequest = new APIRequest(SAMPLE_API_NAME, SAMPLE_API_CONTEXT,
                new URL(Utils.getDockerMockServiceURLHttp("/v2")));
        String API_VERSION_1_0_0 = "1.0.0";
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setTier(TestConstant.API_TIER.UNLIMITED);
        apiRequest.setApiTier(apiPolicyName);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/pet/findByStatus");
        apiOperationsDTO1.setThrottlingPolicy(TestConstant.API_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        // get a predefined api request
        apiId = createAndPublishAPIWithoutRequireReSubscription(apiRequest, restAPIPublisher);
        endpointURL = Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");

        HttpResponse subscriptionResponse = subscribeToAPI(apiId, applicationId,
                TestConstant.SUBSCRIPTION_TIER.UNLIMITED, restAPIStore);

        assertEquals(subscriptionResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to subscribe to the API " + getAPIIdentifierStringFromAPIRequest(apiRequest));
    }


    @Test(description = "Test API level throttling with default limits")
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(apiPolicyName);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName, "API tier not updated.");

        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);
        Assert.assertTrue(isThrottled(requestHeaders, null, limit5Req),
                "Request not throttled by request count condition in api tier");
    }

    private boolean isThrottled(Map<String, String> requestHeaders, Map<String, String> queryParams,
                                long expectedCount) throws InterruptedException, IOException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                isResponseAvailable(endpointURL, requestHeaders));

        StringBuilder url = new StringBuilder(endpointURL);
        if (queryParams != null) {
            int i = 0;
            if (expectedCount == -1) {
                expectedCount = 21;
            }
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                System.out.println(entry.getKey() + "/" + entry.getValue());
                if (i == 0) {
                    url.append(url).append("?").append(entry.getKey()).append("=").append(entry.getValue());
                } else {
                    url.append(url).append("&").append(entry.getKey()).append("=").append(entry.getValue());
                }
                i++;
            }
        }
        HttpResponse response;
        boolean isThrottled = false;
        for (int j = 0; j < expectedCount; j++) {
            response = HTTPSClientUtils.doGet(url.toString(), requestHeaders);
            log.info("============== Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(500);
        }
        return isThrottled;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId);
    }
}
