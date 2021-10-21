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
import org.wso2.choreo.connect.mockbackend.MockBackEndServer;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TimeOutTestCase {
    protected String jwtToken;
    private static final Logger logger = Logger.getLogger(TimeOutTestCase.class.getName());


    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtToken = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "Invoke api with timeout of 15s")
    public void invokeAPITimeout15() throws Exception {
        Map<String, String> sandHeaders = new HashMap<String, String>();
        sandHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/timeout/timeout15") , sandHeaders);

        Assert.assertNotNull(response, "API response should not be null");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY,
                "Response message mismatch.");
    }

    @Test(description = "Invoke api with timeout of 70s")
    public void invokeAPITimeout70() throws Exception {
        Map<String, String> prodHeaders = new HashMap<String, String>();
        prodHeaders.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);

        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/v2/timeout/timeout70") , prodHeaders);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_GATEWAY_TIMEOUT,"Response code mismatched");
        Assert.assertTrue(response.getData().toLowerCase().contains("upstream request timeout"), "Response message mismatch.");
    }
}
