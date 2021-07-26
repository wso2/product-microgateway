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

package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EndpointsWithTrailingSlashTestCase extends ApimBaseTest {
    public static final String API_NAME = "EndpointWithTrailingSlashAPI";
    private static final String API_CONTEXT = "endpointWithTrailingSlash";
    public static final String APPLICATION_NAME = "EndpointWithTrailingSlashApiApp";

    private String apiId;
    private Map<String, String> requestHeaders;
    private String apiEndpointURL;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API IDs
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);

        requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        apiEndpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
    }

    @Test
    public void testInvokeAPI() throws Exception {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(apiEndpointURL, requestHeaders));
        HttpResponse response = HttpsClientRequest.doGet(apiEndpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + apiEndpointURL + " HttpResponse ");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Status code mismatched. Endpoint:" + apiEndpointURL + " HttpResponse ");
    }

}
