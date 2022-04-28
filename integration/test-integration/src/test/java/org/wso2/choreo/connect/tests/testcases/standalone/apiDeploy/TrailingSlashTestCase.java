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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

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
    public void testPathWithoutSlashWithoutPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/findByStatus",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/no-slash/findByStatus","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/findByStatus/",
                headers, jwtTokenProd);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/no-slash/findByStatus","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with path parameter")
    public void testPathWithoutSlashWithPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/1",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/no-slash/1","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/1/",
                headers, jwtTokenProd);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/no-slash/1","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with multiple path parameters")
    public void testPathWithoutSlashWithMultiplePathParams() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/1/pet/123",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/no-slash/1/pet/123","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/no-slash/1/pet/123/",
                headers, jwtTokenProd);
        // Asserting without slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/no-slash/1/pet/123","Request path mismatched");
    }

    /*
     * In the test upto now, the paths were given in the OpenAPI without a trailing slash.
     * The remaining tests in this class are for the scenario where the path in the OpenAPI contains a trailing slash.
     */

    @Test(description = "test path with trailing slash but without path parameters")
    public void testPathWithSlashWithoutPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/findByStatus",
                headers, jwtTokenProd);
        // Asserting with slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/with-slash/findByStatus/","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/findByStatus/",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/with-slash/findByStatus/","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with path parameter")
    public void testPathWithSlashWithPathParam() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/1",
                headers, jwtTokenProd);
        // Asserting with slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/with-slash/1/","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/1/",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/with-slash/1/","Request path mismatched");
    }

    @Test(description = "test path with trailing slash with multiple path parameters")
    public void testPathWithSlashWithMultiplePathParams() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/1/pet/123",
                headers, jwtTokenProd);
        // Asserting with slash, which is the resource given in the API definition.
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/with-slash/1/pet/123/","Request path mismatched");

        EchoResponse echoResponse2 = Utils.invokeEchoGet(API_CONTEXT, "/echo-full/with-slash/1/pet/123/",
                headers, jwtTokenProd);
        Assert.assertEquals(echoResponse2.getPath(), "/v2/echo-full/with-slash/1/pet/123/","Request path mismatched");
    }
}
