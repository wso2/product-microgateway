package org.wso2.choreo.connect.tests.testcases.withapim.apikey;

import com.google.common.net.HttpHeaders;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.testcases.withapim.throttle.ThrottlingBaseTestCase;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIKeySubLevelThrottleTestCase extends ApimBaseTest {

    private static final String API_NAME= "APIKeySubLevelThrottleApi";
    private static final String API_CONTEXT = "apikey_sub_level_throttling";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "apiKeySubThrottleApp";

    private static final String POLICY_NAME = "sub5PerMin";
    private static final String POLICY_TIME_UNIT = "min";
    private static final Integer POLICY_UNIT_TIME = 1;
    private static final long REQUEST_COUNT = 5L;
    private static final String POLICY_DESC = "This is a subscription level throttle policy";

    private SubscriptionThrottlePolicyDTO subThrottlePolicyDTO;
    String apiId;
    String applicationId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // create the throttling policy DTO with request count limit
        RequestCountLimitDTO reqCountLimit = DtoFactory.createRequestCountLimitDTO(POLICY_TIME_UNIT, POLICY_UNIT_TIME,
                REQUEST_COUNT);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                reqCountLimit, null);

        // Add the subscription throttling policy
        subThrottlePolicyDTO = DtoFactory.createSubscriptionThrottlePolicyDTO(POLICY_NAME, POLICY_NAME, POLICY_DESC,
                false, defaultLimit,-1, -1, 100,
                "min", new ArrayList<>(), true, "", 0);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedSubPolicy = adminRestClient.addSubscriptionThrottlingPolicy(
                        subThrottlePolicyDTO);
        subThrottlePolicyDTO = addedSubPolicy.getData();

        // Create API
        APIRequest apiRequest = PublisherUtils.createSampleAPIRequest(API_NAME, API_CONTEXT, API_VERSION, user.getUserName());
        apiRequest.setTiersCollection(POLICY_NAME);

        List<String> securityScheme = new ArrayList<>();
        securityScheme.add("oauth_basic_auth_api_key_mandatory");
        securityScheme.add("api_key");
        apiRequest.setSecurityScheme(securityScheme);

        apiId = PublisherUtils.createAndPublishAPI(apiRequest, publisherRestClient);

        // Create the app and subscribe
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        applicationId = StoreUtils.createApplication(app, storeRestClient);
        StoreUtils.subscribeToAPI(apiId, applicationId, POLICY_NAME, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Could not wait till initial setup completion.");
    }

    @Test(description = "Test subscription level throttling for API Key")
    public void testSubscriptionLevelThrottlingForAPIKey() throws Exception {
        // Create API Key
        APIKeyDTO apiKeyDTO = StoreUtils.generateAPIKey(applicationId, TestConstant.KEY_TYPE_PRODUCTION,
                storeRestClient);
        String apiKey = apiKeyDTO.getApikey();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("apikey", apiKey);

        String endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        Assert.assertTrue(ThrottlingBaseTestCase.isThrottled(endpointURL, requestHeaders, null, REQUEST_COUNT),
                "Request not throttled by request count condition in application tier");
    }

    @AfterClass
    public void destroy() throws Exception {
        StoreUtils.removeAllSubscriptionsForAnApp(applicationId, storeRestClient);
        storeRestClient.removeApplicationById(applicationId);
        publisherRestClient.deleteAPI(apiId);
        adminRestClient.deleteSubscriptionThrottlingPolicy(subThrottlePolicyDTO.getPolicyId());
    }
}
