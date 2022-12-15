/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.testcases.standalone.ratelimit;

import org.apache.http.client.utils.URIBuilder;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimitUtils {
    private static final Logger log = LoggerFactory.getLogger(RateLimitUtils.class);

    public static HttpResponse sendMultipleRequests(String endpointURL, Map<String, String> headers, long expectedCount)
            throws InterruptedException, IOException, URISyntaxException {
        LocalDateTime date = LocalDateTime.now();
        int secondOfDay = date.toLocalTime().toSecondOfDay();
        // to avoid additional request invocations happening in a new minute
        // (after occurring a new minute old request counts are discarded)
        if (secondOfDay % 60 > 50) {
            Thread.sleep(20000);
        }
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(endpointURL, headers));
        URIBuilder url = new URIBuilder(endpointURL);
        HttpResponse response = null;
        for (int requestCount = 1; requestCount <= expectedCount; requestCount++) {
            response = HTTPSClientUtils.doGet(url.toString(), headers);
            log.info("============== Response {}, {}", response.getResponseCode(), response.getData());
            if (response.getResponseCode() != 200) {
                break;
            }
            Thread.sleep(500);
        }
        return response;
    }

    public static boolean isThrottled(HttpResponse response) {
        if (response.getResponseCode() == 429)
            return true;
        return false;
    }
}
