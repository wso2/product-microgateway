/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.withapim.soap;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SOAPApisTestCase extends ApimBaseTest {
    private static final String API_NAME = "SoapAPI";
    private static final String API_CONTEXT = "soap";
    private static final String APPLICATION_NAME = "DefaultAPIApp";
    private static final String API_VERSION = "1.0.0";
    private static final String SOAP_ACTION = "http://mockbackend:2340/phoneverify/query/CheckPhoneNumber";
    private final Map<String, String> requestHeaders_11 = new HashMap<>();
    private final Map<String, String> requestHeaders_12 = new HashMap<>();

    String internalKey;
    String endpointURL11;
    String endpointURL12;

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        String apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders_11.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders_11.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.TEXT_XML);
        requestHeaders_11.put(TestConstant.SOAP_ACTION_HEADER, SOAP_ACTION);
        requestHeaders_12.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders_12.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.SOAP_XML);
        API api = new API();
        api.setContext(API_CONTEXT + TestConstant.URL_SEPARATOR + "1.0.0");
        api.setName(API_NAME);
        api.setVersion("1.0.0");
        api.setProvider("admin");

        internalKey = TokenUtil.getJWT(api, null, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        endpointURL11 = Utils.getServiceURLHttps(API_CONTEXT + TestConstant.URL_SEPARATOR +
                API_VERSION + "/phoneverify11");
        endpointURL12 = Utils.getServiceURLHttps(API_CONTEXT + TestConstant.URL_SEPARATOR +
                API_VERSION + "/phoneverify12");
    }

    @Test(description = "Send a request to the subscribed SOAP API using SOAP 1.1")
    public void testInvokeSoapAPIv11() throws CCTestException {
        HttpResponse response = HttpsClientRequest.doPost(endpointURL11,
                TestConstant.SOAP_ENVELOPES.SOAP11_SAMPLE_REQ_PAYLOAD, requestHeaders_11);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL11);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertTrue(response.getData().contains("<Valid>true</Valid>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.TEXT_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Send a request to the subscribed SOAP API using SOAP 1.2")
    public void testInvokeSoapAPIv12() throws CCTestException {
        HttpResponse response = HttpsClientRequest.doPost(endpointURL12,
                TestConstant.SOAP_ENVELOPES.SOAP12_SAMPLE_REQ_PAYLOAD, requestHeaders_12);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL12);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
        Assert.assertTrue(response.getData().contains("<Valid>true</Valid>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.SOAP_XML,
                "Response content-type mismatch");
    }
}