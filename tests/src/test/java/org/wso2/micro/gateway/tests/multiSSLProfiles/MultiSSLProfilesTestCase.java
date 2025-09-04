/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.multiSSLProfiles;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.micro.gateway.tests.common.BaseTestCase;
import org.wso2.micro.gateway.tests.common.model.ApplicationDTO;
import org.wso2.micro.gateway.tests.util.HttpClientRequest;
import org.wso2.micro.gateway.tests.util.TestConstant;
import org.wso2.micro.gateway.tests.util.TokenUtil;

import java.util.HashMap;
import java.util.Map;

public class MultiSSLProfilesTestCase extends BaseTestCase {
    protected String jwtTokenProd;
    protected String context = "v1";
    @BeforeClass()
    public void start() throws Exception {

        String project = "MultiSSLProfileProject";
        //Define application info

        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));
        jwtTokenProd = TokenUtil.getBasicJWT(application, new JSONObject(), TestConstant.KEY_TYPE_PRODUCTION, 3600);

        //generate apis with CLI and start the micro gateway server
        super.init(project, new String[] { "multiSSLProfiles/multi_ssl_profiles.yaml"}, null,
                "confs/multi-ssl-profiles.conf");
    }

    @Test(description = "Test invoking a backend with a correct truststore and keystore")
    public void testCorrectTrustStoreAndKeyStore() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // invokes the 2381, which contains the correct truststore and keystore
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("petstore/v1/pet/findByStatus"), headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @Test(description = "Test invoking a backend with an incorrect truststore and keystore")
    public void testIncorrectTrustStoreAndKeyStore() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        // invokes the backend at port 2382, which contains an incorrect truststore and keystore
        org.wso2.micro.gateway.tests.util.HttpResponse response = HttpClientRequest
                .doGet(getServiceURLHttp("/petstore/v1/pet/10"), headers);
        Assert.assertNull(response);
    }

    @AfterClass
    public void stop() throws Exception {
        //Stop all the mock servers
        super.finalize();
    }

}
