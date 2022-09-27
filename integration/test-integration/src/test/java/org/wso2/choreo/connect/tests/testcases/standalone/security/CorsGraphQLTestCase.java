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

package org.wso2.choreo.connect.tests.testcases.standalone.security;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;

public class CorsGraphQLTestCase extends CorsBaseTest {
    private final String basePath = "/gql/";
    private final String version = "1.0.0";
    private static String testKey;
    private static HttpPost postRequest;

    @BeforeClass
    public void deployApi() throws Exception {
        API api = new API();
        api.setName("DefaultVersion");
        api.setContext(basePath + version);
        api.setVersion(version);
        api.setProvider("admin");
        ApplicationDTO app = new ApplicationDTO();
        app.setName("DefaultAPIApp");
        app.setTier("Unlimited");
        app.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, app, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);

        HttpsClientRequest.setSSlSystemProperties();
        postRequest = new HttpPost(Utils.getServiceURLHttps("/gql/1.0.0"));
        StringEntity postingString = new StringEntity("{\"query\":\"query " +
                "MyQuery{ hero { name age } } \",\"variables\":null,\"operationName\":\"GetHeroName\"}");
        postRequest.setEntity(postingString);
        postRequest.addHeader("Content-type", "application/json");
        postRequest.addHeader(TestConstant.INTERNAL_KEY_HEADER, testKey);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Couldn't wait till deployment completion.");
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInPreFlightResponseForGraphQL() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps(basePath + version));
        option.addHeader(ORIGIN_HEADER, "http://test1.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);
        validateCORSHeadersInPreFlightResponse(response);
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInSimpleResponseForGraphQL() throws IOException, InterruptedException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        postRequest.addHeader(ORIGIN_HEADER, "http://test2.com");
        HttpResponse response = httpclient.execute(postRequest);
        validateCORSHeadersInSimpleResponse(response);
    }

    @Test(description = "Invalid Origin, CORS simple request for GraphQL")
    public void testSimpleReqInvalidOriginForGraphQL() throws IOException, InterruptedException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        postRequest.addHeader(ORIGIN_HEADER, "http://notAllowedOrigin.com");
        HttpResponse response = httpclient.execute(postRequest);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());
        Assert.assertNull(pickHeader(response.getAllHeaders(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER));
        Assert.assertNull(pickHeader(response.getAllHeaders(), ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER));
    }
}
