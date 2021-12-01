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

package org.wso2.choreo.connect.tests.testcases.standalone.extensions;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.util.*;

import java.util.HashMap;
import java.util.Map;

public class DisableSecurityTestCase {

    @Test(description = "Test to check check API invocation without disabling security and without providing token.")
    public void invokeAPIWithoutDisabledSecurity() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/disable_security/pet/findByStatus") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }

    @Test(description = "Test to check check disable security extensions is working in resource level")
    public void invokeAPIWithDisabledSecurity() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/disable_security/pet/") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.GET_PET_RESPONSE, "Response Body Mismatch.");
    }

    @Test(description = "Test to check check disable security extensions is working in operation level")
    public void invokeAPIWithDisabledSecurityInOperationLevel() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<>();
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/disable_security/store/inventory") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.STORE_INVENTORY_RESPONSE, "Response Body Mismatch.");
    }
}
