/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VersionRangeInvocationTestCase extends ApimBaseTest {

    private String endpointURLVersion10;
    private String endpointURLVersion11;
    private String endpointURLVersionRange;

    public static final String API_NAME = "VersionRangeSubscriptionValidationApi";
    private static final String API_CONTEXT = "int_routing";
    public static final String APPLICATION_NAME = "VersionRangeSubscriptionValidationApp";
    public static final String APPLICATION_THROTTLING_TIER = "Unlimited";
    public static final String X_ENVOY_UPSTREAM_SERVICE_TIME_HEADER= "x-envoy-upstream-service-time";

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Version range endpoint URL
        endpointURLVersion10 = Utils.getServiceURLHttps(API_CONTEXT + "/v1.0/pet/findByStatus");
        endpointURLVersion11 = Utils.getServiceURLHttps(API_CONTEXT + "/v1.1/pet/findByStatus");
        endpointURLVersionRange = Utils.getServiceURLHttps(API_CONTEXT + "/v1/pet/findByStatus");
    }

    private Map<String, String> getRequestHeaders(String apiVersion) throws Exception {
        String apiId = ApimResourceProcessor.apiNameVersionToId.get(
                PublisherUtils.getAPINameVersionIdentifier(API_NAME, apiVersion));
        API api = new API();
        api.setId(apiId);
        api.setName(API_NAME);
        api.setContext("/" + API_CONTEXT + "/" + apiVersion);
        api.setVersion(apiVersion);
        api.setProvider("admin");

        ApplicationDTO application = new ApplicationDTO();
        application.setName(APPLICATION_NAME);
        application.setTier(APPLICATION_THROTTLING_TIER);
        application.setId((int) (Math.random() * 1000));

        String token = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, "write:pets", false);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + token);
        return requestHeaders;
    }

    private void invokeAPI(String endpointURL, String apiVersion) throws Exception {
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(60, TimeUnit.SECONDS).until(
                HttpsClientRequest.isResponseAvailable(endpointURL, getRequestHeaders(apiVersion)));
        HttpResponse response = HttpsClientRequest.doGet(endpointURL, getRequestHeaders(apiVersion));
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatched. Response Data: " + response.getData());
        Assert.assertFalse(response.getHeaders().containsKey(X_ENVOY_UPSTREAM_SERVICE_TIME_HEADER),
                "Response contains the " + X_ENVOY_UPSTREAM_SERVICE_TIME_HEADER + " header.");
    }

    @Test(description = "Invoke an API using major version range and exact API version and" +
            " check if status code is 200 and check if the expected result is received")
    public void testAPIVersionAndVersionRangeInvocations() throws Exception {
        // Test invocation with endpoint urls with exact versions and version range
        invokeAPI(endpointURLVersion10, "v1.0");
        invokeAPI(endpointURLVersion11, "v1.1");

        invokeAPI(endpointURLVersionRange, "v1.1");
    }
}
