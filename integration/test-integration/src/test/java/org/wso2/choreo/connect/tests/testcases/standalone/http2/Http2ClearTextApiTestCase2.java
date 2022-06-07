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

package org.wso2.choreo.connect.tests.testcases.standalone.http2;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.Http2ClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class Http2ClearTextApiTestCase2 {
    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        API api = new API();
        api.setName("Http2ClearTextAPI");
        api.setContext("http2_clear_text");
        api.setProdEndpoint(Utils.getDockerMockServiceURLHttp2ClearText(""));
        api.setVersion("v1");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        jwtTokenProd = TokenUtil.getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, false);
    }

    @Test(description = "Invoke HTTP2 clear text endpoint with prior knowledge")
    public void invokeHttp2ClearTextEndpointSuccess() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/http2_clear_text/hello") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

    @Test(description = "Invoke HTTP2 clear text endpoint with prior knowledge with http2 downstream connection")
    public void invokeHttp2ClearTextEndpointWithHttp2DownsteamSuccess() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        java.net.http.HttpResponse<String> response = Http2ClientRequest.doGet(Utils.getServiceURLHttps(
                "/http2_clear_text/hello") , headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.statusCode(), HttpStatus.SC_OK,"Response code mismatched");
    }

}
