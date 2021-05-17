/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.testcases.withapim.throttle.ThrottlingBaseTestCase;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StartupDiscoveryTestCase extends ApimBaseTest {
    private Map<String, String> appIdByAppName;

    private Map<String, String> requestHeaders;

    @BeforeClass
    void getAndStoreAppIds() throws ApiException, CCTestException {
        super.initWithSuperTenant();
        appIdByAppName = new HashMap<>();
        ApplicationListDTO applicationListDTO = storeRestClient.getAllApps();
        System.out.println(applicationListDTO.getCount());
        for (ApplicationInfoDTO appInfoDTO : Objects.requireNonNull(applicationListDTO.getList())) {
            appIdByAppName.put(appInfoDTO.getName(), appInfoDTO.getApplicationId());
        }
    }

    @Test
    public void invokeApiWithTestKey() throws CCTestException, IOException {
        String token = HttpsClientRequest.requestTestKey();
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        String endpointURL = Utils.getServiceURLHttps(TestConstant.SRARTUP_TEST.API_CONTEXT
                + "/1.0.0/pet/findByStatus");
        HttpResponse response = HttpsClientRequest.doGet(endpointURL , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test
    public void invokeApiWithKeyManagerAccessToken() throws Exception {
        String appId = appIdByAppName.get(TestConstant.SRARTUP_TEST.APP_NAME);
        ApplicationKeyDTO appKeyDto = StoreUtils.generateKeysForApp(appId, storeRestClient);
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appKeyDto.getConsumerKey(), appKeyDto.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, storeRestClient);

        requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String endpointURL = Utils.getServiceURLHttps(TestConstant.SRARTUP_TEST.API_CONTEXT
                + "/1.0.0/pet/findByStatus");
        Utils.testInvokeAPI(endpointURL, requestHeaders, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);
    }

    @Test(dependsOnMethods = "invokeApiWithKeyManagerAccessToken")
    public void invokeApiToTestThrottling() throws Exception {
        String endpointURL = Utils.getServiceURLHttps(TestConstant.SRARTUP_TEST.API_CONTEXT
                + "/1.0.0/pet/findByStatus");
        Assert.assertTrue(ThrottlingBaseTestCase.isThrottled(endpointURL,
                requestHeaders, null, 5L),
                "Request not throttled by request count condition in api tier");
        Utils.delay(40000, "Could not wait until the throttle decision expired");
    }
}
