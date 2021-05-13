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

package org.wso2.choreo.connect.tests.testCases.withAPIM.throttle;

import org.apache.http.client.utils.URIBuilder;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ThrottlingBaseTestCase extends ApimBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ThrottlingBaseTestCase.class);
    public static final String SAMPLE_API_NAME = "Throttling";
    public static final String SAMPLE_API_CONTEXT = "throttling";
    public static final String SAMPLE_API_VERSION = "1.0.0";

    protected String createThrottleApi(String tiers, String apiTier, String resTier) throws MalformedURLException,
            APIManagerIntegrationTestException, CCTestException {
        APIRequest apiRequest = new APIRequest(SAMPLE_API_NAME, SAMPLE_API_CONTEXT,
                new URL(Utils.getDockerMockServiceURLHttp(TestConstant.MOCK_BACKEND_BASEPATH)));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion(SAMPLE_API_VERSION);
        apiRequest.setTiersCollection(tiers);
        apiRequest.setTier(apiTier);
        apiRequest.setApiTier(apiTier);

        APIOperationsDTO findByStatus = new APIOperationsDTO();
        findByStatus.setVerb("GET");
        findByStatus.setTarget("/pet/findByStatus");
        findByStatus.setThrottlingPolicy(resTier);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(findByStatus);
        apiRequest.setOperationsDTOS(operationsDTOS);

        // get a predefined api request
        return PublisherUtils.createAndPublishAPI(apiRequest, publisherRestClient);
    }

    protected String getThrottleAPIEndpoint() throws MalformedURLException {
        return Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    protected boolean isThrottled(String endpointURL, Map<String, String> headers, Map<String, String> queryParams,
                                  long expectedCount) throws InterruptedException, IOException, URISyntaxException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(endpointURL, headers));
        // This buffer is to avoid failures due to delays in evaluating throttle conditions at TM
        // here it sets the final throttle request count twice as the limit set in the policy.
        // it will make sure throttle will happen even if the throttle window passed.
        long throttleBuffer = expectedCount + 2;

        URIBuilder url = new URIBuilder(endpointURL);
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                url.addParameter(entry.getKey(), entry.getValue());
            }
        }
        HttpResponse response;
        boolean isThrottled = false;
        for (int j = 0; j <= expectedCount + throttleBuffer; j++) {

            response = HTTPSClientUtils.doGet(url.toString(), headers);
            log.info("============== Response {}, {}", response.getResponseCode(), response.getData());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }
        return isThrottled;
    }
}
