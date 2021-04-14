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

package org.wso2.choreo.connect.tests.testCases.throttle;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.apim.APIMLifecycleBaseTest;
import org.wso2.choreo.connect.tests.context.MicroGWTestException;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ThrottlingBaseTestCase extends APIMLifecycleBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ThrottlingBaseTestCase.class);

    protected String createThrottleApi(String tiers, String apiTier, String resTier) throws MalformedURLException,
            APIManagerIntegrationTestException, MicroGWTestException, ApiException {
        APIRequest apiRequest = new APIRequest(SAMPLE_API_NAME, SAMPLE_API_CONTEXT,
                new URL(Utils.getDockerMockServiceURLHttp("/v2")));
        String API_VERSION_1_0_0 = "1.0.0";
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion(API_VERSION_1_0_0);
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
        return createAndPublishAPIWithoutRequireReSubscription(apiRequest, restAPIPublisher);
    }

    protected String getThrottleAPIEndpoint() throws MalformedURLException {
        return Utils.getServiceURLHttps(SAMPLE_API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    protected boolean isThrottled(String endpointURL, Map<String, String> headers, Map<String, String> queryParams,
                                  long expectedCount) throws InterruptedException, IOException {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                isResponseAvailable(endpointURL, headers));

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
        for (int j = 0; j <= expectedCount; j++) {
            response = HTTPSClientUtils.doGet(url.toString(), headers);
            log.info("============== Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(500);
        }
        return isThrottled;
    }

}
