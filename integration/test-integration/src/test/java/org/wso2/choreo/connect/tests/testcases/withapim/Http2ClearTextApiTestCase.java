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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.Http2ClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Test case to check http2 clear text endpoints
 *
 */
public class Http2ClearTextApiTestCase extends ApimBaseTest {
    private static final String API_NAME = "Http2ClearTextAPI";
    private static final String API_CONTEXT = "http2_clear_text";
    private static final String APPLICATION_NAME = "Http2ClearTextAPIApp";
    private static final String API_VERSION = "1.0.0";
    private final Map<String, String> requestHeaders = new HashMap<>();
  
    String apiId;
    String revisionUUID;
    String applicationId;
    APIRequest apiRequest;
    Map<String, String> headers;
    String endpoint;

    String internalKey;
    private String endpointURL;

    @BeforeClass(alwaysRun = true, description = "initialise the setup")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();

        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        API api = new API();
        api.setContext(API_CONTEXT + "/1.0.0");
        api.setName(API_NAME);
        api.setVersion("1.0.0");
        api.setProvider("admin");

        internalKey = TokenUtil.getJWT(api, null, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/hello");
    }

    @Test(description = "Invoke HTTP2 clear text endpoint with prior knowledge")
    public void invokeHttp2ClearTextEndpointSuccess() throws CCTestException, InterruptedException {
        HttpResponse response = HttpsClientRequest.doGet(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
    }

    @Test(description = "Invoke HTTP2 clear text endpoint with prior knowledge with http2 downstream connection")
    public void invokeHttp2ClearTextEndpointWithHttp2DownsteamSuccess() throws Exception {
        java.net.http.HttpResponse<String>  response = Http2ClientRequest.doGet(endpointURL, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.statusCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
    }
    
}
