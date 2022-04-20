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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
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
    private Map<String, String> headers = new HashMap<>();
    private static final String API_CONTEXT = "/trailing-slash";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProd);
    }

    @Test(description = "test path with trailing slash but without path parameters")
    public void testTrailingSlashWithoutPathParam() throws CCTestException, MalformedURLException {
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/pet/findByStatus");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY, "Response Body Mismatch.");

        String endpoint_with_slash = Utils.getServiceURLHttps(API_CONTEXT + "/pet/findByStatus/");
        HttpResponse response_with_slash = HttpsClientRequest.doGet(endpoint_with_slash, headers);
        Assert.assertNotNull(response_with_slash);
        Assert.assertEquals(response_with_slash.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response_with_slash.getData(), ResponseConstants.RESPONSE_BODY, "Response Body Mismatch.");
    }

    @Test(description = "test path with trailing slash with path parameter")
    public void testTrailingSlashWithPathParam() throws CCTestException, MalformedURLException {
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/pet/1");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.PET_BY_ID_RESPONSE, "Response Body Mismatch.");

        String endpoint_with_slash = Utils.getServiceURLHttps(API_CONTEXT + "/pet/1/");
        HttpResponse response_with_slash = HttpsClientRequest.doGet(endpoint_with_slash, headers);
        Assert.assertNotNull(response_with_slash);
        Assert.assertEquals(response_with_slash.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response_with_slash.getData(), ResponseConstants.PET_BY_ID_RESPONSE, "Response Body Mismatch.");
    }

    @Test(description = "test path with trailing slash with multiple path parameters")
    public void testTrailingSlashWithMultiplePathParams() throws CCTestException, MalformedURLException {
        String endpoint = Utils.getServiceURLHttps(API_CONTEXT + "/store/1/pet/123");
        HttpResponse response = HttpsClientRequest.doGet(endpoint, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.STORE_INVENTORY_RESPONSE, "Response Body Mismatch.");

        String endpoint_with_slash = Utils.getServiceURLHttps(API_CONTEXT + "/store/1/pet/123");
        HttpResponse response_with_slash = HttpsClientRequest.doGet(endpoint_with_slash, headers);
        Assert.assertNotNull(response_with_slash);
        Assert.assertEquals(response_with_slash.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response_with_slash.getData(), ResponseConstants.STORE_INVENTORY_RESPONSE, "Response Body Mismatch.");
    }
}
