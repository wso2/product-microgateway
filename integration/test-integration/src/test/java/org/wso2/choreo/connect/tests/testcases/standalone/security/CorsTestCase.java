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

package org.wso2.choreo.connect.tests.testcases.standalone.security;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;

public class CorsTestCase extends CorsBaseTest {
    protected String jwtTokenProd;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        HttpsClientRequest.setSSlSystemProperties();
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInPreFlightResponse() throws Exception {
        // AccessControlAllowCredentials set to true
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps("/cors/pet/1"));
        option.addHeader(ORIGIN_HEADER, "http://test1.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);
        validateCORSHeadersInPreFlightResponse(response);
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInSimpleResponse() throws IOException {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest getRequest = new HttpGet(Utils.getServiceURLHttps("/cors/pet/1"));
        getRequest.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        getRequest.addHeader(ORIGIN_HEADER, "http://test2.com");
        HttpResponse response = httpclient.execute(getRequest);
        validateCORSHeadersInSimpleResponse(response);
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInSimpleResponseWithWildcardSubdomain() throws IOException {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest getRequest = new HttpGet(Utils.getServiceURLHttps("/cors/pet/1"));
        getRequest.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        getRequest.addHeader(ORIGIN_HEADER, allowedOriginWildCard);
        HttpResponse response = httpclient.execute(getRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOriginWildCard);
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInPreFlightResponseWildcardSubdomain() throws Exception {
        // AccessControlAllowCredentials set to true
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps("/cors/pet/1"));
        option.addHeader(ORIGIN_HEADER, allowedOriginWildCard);
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOriginWildCard);
    }

    @Test(description = "Invalid Origin, CORS simple request")
    public void testSimpleReqInvalidOrigin() throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest getRequest = new HttpGet(Utils.getServiceURLHttps("/cors/pet/1"));
        getRequest.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        getRequest.addHeader(ORIGIN_HEADER, "http://notAllowedOrigin.com");
        HttpResponse response = httpclient.execute(getRequest);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());
        Assert.assertNull(pickHeader(response.getAllHeaders(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER));
        Assert.assertNull(pickHeader(response.getAllHeaders(), ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER));
    }

    @Test(description = "Invalid Origin, CORS preflight request")
    public void testPreflightReqInvalidOrigin() throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps("/cors/pet/1"));
        option.addHeader(ORIGIN_HEADER, "http://notAllowedOrigin.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT,
                "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString() + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available");
    }

    @Test(description = "CORS preflight request against a resource without CORS")
    public void testPreflightReqResourceWithoutCors() throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps("/v2/standard/pet/1"));
        option.addHeader(ORIGIN_HEADER, "http://notAllowedOrigin.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT,
                "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString() + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is available");
        Assert.assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available");
    }


    @Test(description = "Invalid Origin, CORS preflight request")
    public void testPreflightReqInvalidReqMethod() throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(Utils.getServiceURLHttps("/cors/pet/1"));
        option.addHeader(ORIGIN_HEADER, "http://test1.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "DELETE");
        HttpResponse response = httpclient.execute(option);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,
                "Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());
        Header accessControlAllowMethods = pickHeader(response.getAllHeaders(), ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        Assert.assertTrue(accessControlAllowMethods == null ||
                !accessControlAllowMethods.getValue().contains("DELETE"),
                "AccessControlRequestMethod is not validated properly.");
    }
}
