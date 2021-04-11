package org.wso2.choreo.connect.tests.testCases.throttle;

import com.google.common.net.HttpHeaders;
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
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
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
    private final String THROTTLED_IP = "10.100.1.22";
    private final String THROTTLED_HEADER = "10.100.7.77";
    private final String THROTTLED_QUERY_PARAM = "name";
    private final String THROTTLED_QUERY_PARAM_VALUE = "admin";
    private final Map<String, String> requestHeaders = new HashMap<>();
    private final String apiPolicyName = "APIPolicyWithDefaultLimit";
    private final String conditionalPolicyName = "APIPolicyWithIPLimit";
    private final long limit5Req = 5L;
    private final long limit10Req = 10L;
    private final long limit1000Req = 1000L;
    private APIRequest apiRequest;
    private String apiId;
    private String applicationId;
    private String endpointURL;
    private String apiPolicyId;
    private String conditionalPolicyId;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.init();
        RequestCountLimitDTO threePerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit5Req);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, threePerMin, null);
        RequestCountLimitDTO thousandPerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit1000Req);
        ThrottleLimitDTO defaultLimitForConditions =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, thousandPerMin, null);
        RequestCountLimitDTO tenPerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, limit10Req);
        ThrottleLimitDTO limitForConditions =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, tenPerMin, null);

        // creating the application
        ApplicationDTO app = new ApplicationDTO();
        app.setName("AdvanceThrottlingApp");
        app.setDescription("Test Application for AdvanceThrottling");
        app.setThrottlingPolicy(TestConstant.APPLICATION_TIER.UNLIMITED);
        app.setTokenType(ApplicationDTO.TokenTypeEnum.JWT);
        ApplicationCreationResponse appCreationResponse = createApplicationWithKeys(app, restAPIStore);
        applicationId = appCreationResponse.getApplicationId();

        // create the advanced throttling policy with no conditions
        AdvancedThrottlePolicyDTO apiPolicyDto = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName, "", "", false, defaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> addedApiPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(apiPolicyDto);

        // assert the status code and policy ID
        Assert.assertEquals(addedApiPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedApiPolicyDto = addedApiPolicy.getData();
        apiPolicyId = addedApiPolicyDto.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        // create the advanced throttling policy with conditions
        AdvancedThrottlePolicyDTO conditionPolicyDto = DtoFactory
                .createAdvancedThrottlePolicyDTO(conditionalPolicyName, "", "", false, defaultLimitForConditions,
                        createConditionalGroups(limitForConditions));
        ApiResponse<AdvancedThrottlePolicyDTO> addedConditionalPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(conditionPolicyDto);

        // assert the status code and policy ID
        Assert.assertEquals(addedConditionalPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedConditionPolicyDto = addedConditionalPolicy.getData();
        conditionalPolicyId = addedConditionPolicyDto.getPolicyId();
        Assert.assertNotNull(conditionalPolicyId, "The policy ID cannot be null or empty");

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


    @Test(description = "Test Advance throttling with IP Condition", dependsOnMethods = {"testAPILevelThrottling"})
    public void testAPILevelThrottlingWithIpCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(APIMIntegrationConstants.API_TIER.UNLIMITED);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), APIMIntegrationConstants.API_TIER.UNLIMITED,
                "API tier not updated.");
        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);
        Assert.assertFalse(isThrottled(requestHeaders, null, -1),
                "Request was throttled unexpectedly in Unlimited API tier");

        apidto.setApiThrottlingPolicy(conditionalPolicyName);
        updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");
        Assert.assertFalse(isThrottled(requestHeaders, null, -1),
                "Request not need to throttle since policy was Unlimited");
        // create Revision and Deploy to Gateway
        createAPIRevisionAndDeploy(apiId, restAPIPublisher);

        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, "192.100.1.24");
        Assert.assertFalse(isThrottled(requestHeaders, null, limit10Req),
                "Request shouldn't throttle for an IP not in a condition");

        requestHeaders.put(HttpHeaders.X_FORWARDED_FOR, THROTTLED_IP);
        Assert.assertTrue(isThrottled(requestHeaders, null, limit10Req),
                "Request need to throttle since policy was updated");
        requestHeaders.remove(HttpHeaders.X_FORWARDED_FOR);
    }

    @Test(description = "Test Advance throttling with Header Condition",
            dependsOnMethods = {"testAPILevelThrottlingWithIpCondition"})
    public void testAPILevelThrottlingWithHeaderCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");

        requestHeaders.put(HttpHeaders.HOST, "19.2.1.2");
        Assert.assertFalse(isThrottled(requestHeaders, null, limit10Req),
                "Request shouldn't throttle for a host not in a condition");

        requestHeaders.put(HttpHeaders.HOST, THROTTLED_HEADER);
        Assert.assertTrue(isThrottled(requestHeaders, null, limit10Req),
                "Request not throttled by request count header condition in API tier");
        requestHeaders.remove(HttpHeaders.HOST);
    }

    @Test(description = "Test Advance throttling with query param Condition",
            dependsOnMethods = {"testAPILevelThrottlingWithHeaderCondition"})
    public void testAPILevelThrottlingWithQueryCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), conditionalPolicyName,
                "API tier not updated.");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(THROTTLED_QUERY_PARAM, "foo");
        Assert.assertFalse(isThrottled(requestHeaders, queryParams, limit10Req),
                "Request shouldn't throttle for a query param not in a condition");
        queryParams.put(THROTTLED_QUERY_PARAM, THROTTLED_QUERY_PARAM_VALUE);
        Assert.assertTrue(isThrottled(requestHeaders, queryParams, limit10Req),
                "Request not throttled by request count query parameter condition in API tier");
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

    /**
     * Creates a set of conditional groups with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created list of conditional group DTO
     */
    private List<ConditionalGroupDTO> createConditionalGroups(ThrottleLimitDTO limit) {
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();

        // create an IP condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> ipGrp = new ArrayList<>();
        IPConditionDTO ipConditionDTO =
                DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC, THROTTLED_IP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        ipGrp.add(ipCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "IP conditional group", ipGrp, limit));

        // create a header condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> headerGrp = new ArrayList<>();
        HeaderConditionDTO headerConditionDTO = DtoFactory.createHeaderConditionDTO(HttpHeaders.HOST, THROTTLED_HEADER);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        headerGrp.add(headerCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Header conditional group", headerGrp, limit));

        // create a query parameter condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> queryGrp = new ArrayList<>();
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(THROTTLED_QUERY_PARAM, THROTTLED_QUERY_PARAM_VALUE);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        queryGrp.add(queryParameterCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Query param conditional group", queryGrp, limit));

        return conditionalGroups;
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(conditionalPolicyId);
    }
}
