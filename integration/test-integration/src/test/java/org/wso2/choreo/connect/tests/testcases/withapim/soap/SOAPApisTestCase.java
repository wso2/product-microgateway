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

public class SOAPApisTestCase extends ApimBaseTest {
    private static final String API_NAME = "SOAPApi";
    private static final String API_CONTEXT = "soap";
    private static final String APPLICATION_NAME = "DefaultAPIApp";
    private static final String API_VERSION = "1.0.0";
    private final Map<String, String> requestHeaders = new HashMap<>();

    String internalKey;
    String endpointURL;

    @BeforeClass(alwaysRun = true, description = "Create access token and define endpoint URL")
    void setEnvironment() throws Exception {
        super.initWithSuperTenant();
        // Get App ID and API ID
        String applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        String apiId = ApimResourceProcessor.apiNameToId.get(API_NAME);

        String accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId,
                user, storeRestClient);
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.TEXT_XML);
        API api = new API();
        api.setContext(API_CONTEXT + TestConstant.URL_SEPARATOR + "1.0.0");
        api.setName(API_NAME);
        api.setVersion("1.0.0");
        api.setProvider("admin");

        internalKey = TokenUtil.getJWT(api, null, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + TestConstant.URL_SEPARATOR +
                API_VERSION + "/phoneverify");
    }

    @Test(description = "Send a request to a subscribed REST API")
    public void testInvokeSoapAPI() throws CCTestException, InterruptedException {
        String payload = "<soap:Envelope\n" +
                "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "\txmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "\t<soap:Body>\n" +
                "\t\t<CheckPhoneNumber\n" +
                "\t\t\txmlns=\"http://ws.cdyne.com/PhoneVerify/query\">\n" +
                "\t\t\t<PhoneNumber>18006785432</PhoneNumber>\n" +
                "\t\t\t<LicenseKey>18006785432</LicenseKey>\n" +
                "\t\t</CheckPhoneNumber>\n" +
                "\t</soap:Body>\n" +
                "</soap:Envelope>";
        HttpResponse response = HttpsClientRequest.doPost(endpointURL, payload, requestHeaders);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " + endpointURL + ". HttpResponse");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_SUCCESS,
                "Valid subscription should be able to invoke the associated API");
    }
}
