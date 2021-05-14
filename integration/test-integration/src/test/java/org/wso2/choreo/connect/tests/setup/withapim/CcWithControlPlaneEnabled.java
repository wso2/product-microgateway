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
package org.wso2.choreo.connect.tests.setup.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.context.CcInstance;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CcWithControlPlaneEnabled extends ApimBaseTest {
    private static final String API_NAME = "ApiBeforeStartingCC";
    private static final String API_CONTEXT = "before_starting_CC";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "AppBeforeStartingCC";

    CcInstance ccInstance;
    AppWithConsumerKey appCreationResponse;

    @BeforeTest
    public void startChoreoConnect() throws Exception {
        // APIs, Apps, Subs created here will be used to test whether
        // resources that existed in APIM were pulled by CC during startup
        createApiAppSubsEtc();

        ccInstance = new CcInstance.Builder().withNewDockerCompose("cc-in-common-network-docker-compose.yaml")
                .withNewConfig("controlplane-enabled-config.toml")
                .withBackendServiceFile("backend-service-with-tls-and-network.yaml")
                .withAllCustomImpls().build();
        ccInstance.start();
        Awaitility.await().pollDelay(20, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.MINUTES).until(ccInstance.isHealthy());
    }

    @Test
    public void invokeApiWithTestKey() throws CCTestException, IOException {
        String token = HttpsClientRequest.requestTestKey();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps( "/"
                + API_CONTEXT + "/pet/findByStatus?status=available") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test
    public void invokeApiWithKeyManagerAccessToken() throws Exception {
        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appCreationResponse.getConsumerKey(), appCreationResponse.getConsumerSecret(),
                new String[]{"PRODUCTION"}, user, storeRestClient);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/pet/findByStatus");
        Utils.testInvokeAPI(endpointURL, requestHeaders, HttpStatus.SC_SUCCESS, ResponseConstants.RESPONSE_BODY);
    }

    @AfterTest
    public void stop() {
        ccInstance.stop();
    }

    /**
     * Initialize the clients in the super class and create APIs, Apps, Subscriptions etc.
     */
    private void createApiAppSubsEtc() throws Exception {
        super.initWithSuperTenant();

        // Create and publish API
        APIRequest apiRequest = PublisherUtils.createSampleAPIRequest(API_NAME,
                API_CONTEXT, API_VERSION, user.getUserName());
        String apiId = PublisherUtils.createAndPublishAPI(apiRequest, publisherRestClient);
        // TODO: (SuKSW) Following method doesn't seem to work, remove if not necessary
        //waitForAPIDeploymentSync(user.getUserName(), sampleApiName, sampleApiVersion);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for API deployment");

        //Create Application
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        AppWithConsumerKey appCreationResponse = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        String applicationId = appCreationResponse.getApplicationId();

        //Subscribe to application
        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted while waiting for deployment");
    }
}
