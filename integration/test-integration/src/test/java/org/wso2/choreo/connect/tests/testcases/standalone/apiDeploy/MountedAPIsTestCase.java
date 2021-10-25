/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.testcases.standalone.apiDeploy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.ResponseConstants;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class MountedAPIsTestCase {
    protected String jwtToken;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        API api = new API();
        api.setName("SwaggerPetstoreStartup");
        api.setContext("startup/v2");
        api.setProdEndpoint(Utils.getMockServiceURLHttp("/echo/prod"));
        api.setVersion("1.0.5");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        jwtToken = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, "write:pets", false);
    }

    @Test(description = "Test if Mounted API Project applied")
    public void MountedAPIProjectDirSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps("/startup/v2/pet/findByStatus") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY, "Response Body Mismatch.");
    }

    @Test(description = "Test if Mounted API Project (zipped_ applied")
    public void MountedAPIProjectZipSuccessTest() throws Exception {
        // Set header
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtToken);
        HttpResponse response = HttpsClientRequest.doGet(
                Utils.getServiceURLHttps("/startup/zipped/v2/pet/findByStatus") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertEquals(response.getData(), ResponseConstants.RESPONSE_BODY, "Response Body Mismatch.");
    }
}
