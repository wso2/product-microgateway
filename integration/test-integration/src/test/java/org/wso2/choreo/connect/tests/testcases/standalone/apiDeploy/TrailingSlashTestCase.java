/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import com.google.common.net.HttpHeaders;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class TrailingSlashTestCase {
    private String jwtTokenProd;
    private final Map<String, String> headers = new HashMap<>();
    private static final String API_CONTEXT = "/trailing-slash";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @Test(description = "test path with trailing slash but without path parameters")
    public void testTrailingSlashWithoutPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/findByStatus", jwtTokenProd, headers);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/findByStatus","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/findByStatus/", jwtTokenProd, headers);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/findByStatus","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with path parameter")
    public void testTrailingSlashWithPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/1", jwtTokenProd, headers);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/1","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/1/", jwtTokenProd, headers);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/1","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with multiple path parameters")
    public void testTrailingSlashWithMultiplePathParams() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/1/pet/123", jwtTokenProd, headers);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/1/pet/123","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/1/pet/123/", jwtTokenProd, headers);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/1/pet/123","Request path mismatched");
    }
}
