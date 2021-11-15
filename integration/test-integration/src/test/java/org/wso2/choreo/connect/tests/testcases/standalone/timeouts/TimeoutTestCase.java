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
package org.wso2.choreo.connect.tests.testcases.standalone.timeouts;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class TimeoutTestCase {
    String prodToken;
    String sandboxToken;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        prodToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        sandboxToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_SANDBOX, null, false);
    }

    @Test(description = "Invoke endpoint of delay 8s when global timeout is 15s")
    public void testGlobalTimeout_notTriggerTimeout_8s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/global-timeout/delay-8"), prodToken,
                HttpStatus.SC_OK, ResponseConstants.RESPONSE_BODY);
    }

    @Test(description = "Invoke endpoint of delay 17s when global timeout is 15s")
    public void testGlobalTimeout_triggerTimeout_17s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/global-timeout/delay-17"), prodToken,
                HttpStatus.SC_GATEWAY_TIMEOUT, ResponseConstants.UPSTREAM_TIMEOUT_ERROR);
    }

    @Test(description = "Invoke endpoint of delay 5s when API level timeout is 7s")
    public void testAPILevelTimeout_notTriggerTimeout_4s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-5"), prodToken,
                HttpStatus.SC_OK, ResponseConstants.RESPONSE_BODY);

        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-5"), sandboxToken,
                HttpStatus.SC_OK, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test(description = "Invoke endpoint of delay 8s when API level timeout is 7s")
    public void testAPILevelTimeout_triggerTimeout_8s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-8"), prodToken,
                HttpStatus.SC_GATEWAY_TIMEOUT, ResponseConstants.UPSTREAM_TIMEOUT_ERROR);

        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-8"), sandboxToken,
                HttpStatus.SC_GATEWAY_TIMEOUT, ResponseConstants.UPSTREAM_TIMEOUT_ERROR);
    }

    @Test(description = "Invoke endpoint without delay when Resource level timeout is 3s")
    public void testResourceLevelTimeout_notTriggerTimeout_0s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/pet/findByStatus"), prodToken,
                HttpStatus.SC_OK, ResponseConstants.RESPONSE_BODY);

        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/pet/findByStatus"), sandboxToken,
                HttpStatus.SC_OK, ResponseConstants.API_SANDBOX_RESPONSE);
    }

    @Test(description = "Invoke endpoint of delay 4s when Resource level timeout is 3s")
    public void testResourceLevelTimeout_triggerTimeout_4s() throws Exception {
        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-4"), prodToken,
                HttpStatus.SC_GATEWAY_TIMEOUT, ResponseConstants.UPSTREAM_TIMEOUT_ERROR);

        invokeEndpointAndCheckStatusAndPayload(
                Utils.getServiceURLHttps("/endpoint-timeout/delay-4"), sandboxToken,
                HttpStatus.SC_GATEWAY_TIMEOUT, ResponseConstants.UPSTREAM_TIMEOUT_ERROR);
    }

    void invokeEndpointAndCheckStatusAndPayload(String endpoint, String token,
                                                int expectedStatus, String expectedPayload) throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token);
        HttpResponse response = HttpsClientRequest.doGet(endpoint , sandHeaders);

        Assert.assertNotNull(response, "API response should not be null");
        Assert.assertEquals(response.getResponseCode(), expectedStatus,"Response code mismatched");
        Assert.assertTrue(response.getData().contains(expectedPayload), "Response message mismatch.");
    }
}
