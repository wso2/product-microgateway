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

package org.wso2am.micro.gw.tests.testCases.security;

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
import org.wso2am.micro.gw.tests.util.*;

import java.io.IOException;

@Test(groups = { TestGroup.MGW_WITH_BACKEND_TLS_AND_API })
public class CorsTestCase {
    protected String jwtTokenProd;
    private String allowedOrigin1 = "http://test1.com";
    private String allowedOrigin2 = "http://test2.com";
    private String allowedMethods = "GET,PUT,POST";
    private String allowedHeaders = "Authorization,X-PINGOTHER";

    private static final String ORIGIN_HEADER = "Origin";
    private static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "access-control-request-method";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "access-control-allow-origin";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "access-control-allow-methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "access-control-allow-headers";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "access-control-allow-credentials";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        String apiZipfile = ApiProjectGenerator.createApictlProjZip("cors/api.yaml", "cors/swagger.yaml");
        ApiDeployment.deployAPI(apiZipfile);

        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null);
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInPreFlightResponse() throws Exception {
        // AccessControlAllowCredentials set to true
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(URLs.getServiceURLHttps("/cors/pet/1"));
        option.addHeader(ORIGIN_HEADER, "http://test1.com");
        option.addHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST");
        HttpResponse response = httpclient.execute(option);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOrigin1);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString() + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString())
                        .getValue().replaceAll(" ", "")
                , allowedMethods);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString())
                        .getValue().replaceAll(" ", ""), allowedHeaders);
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is unavailable");
        Assert.assertTrue(Boolean.parseBoolean(pickHeader(responseHeaders,
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER).getValue()));
    }

    @Test(description = "Success Scenario, with allow credentials is set to true.")
    public void testCORSHeadersInSimpleResponse() throws IOException {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest getRequest = new HttpGet(URLs.getServiceURLHttps("/cors/pet/1"));
        getRequest.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        getRequest.addHeader(ORIGIN_HEADER, "http://test2.com");
        HttpResponse response = httpclient.execute(getRequest);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,"Response code mismatched");
        Assert.assertNotNull(response.getAllHeaders());

        Header[] responseHeaders = response.getAllHeaders();
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is unavailable");
        Assert.assertEquals(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).getValue(),
                allowedOrigin2);

        // TODO: (VirajSalaka) Check the validity
        Assert.assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is unavailable");
        Assert.assertTrue(Boolean.parseBoolean(pickHeader(responseHeaders,
                ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER).getValue()));
    }

    @Test(description = "Invalid Origin, CORS simple request")
    public void testSimpleReqInvalidOrigin() throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest getRequest = new HttpGet(URLs.getServiceURLHttps("/cors/pet/1"));
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
        HttpUriRequest option = new HttpOptions(URLs.getServiceURLHttps("/cors/pet/1"));
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
        HttpUriRequest option = new HttpOptions(URLs.getServiceURLHttps("/v2/pet/1"));
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
        HttpUriRequest option = new HttpOptions(URLs.getServiceURLHttps("/cors/pet/1"));
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

    private Header pickHeader(Header[] headers, String requiredHeader){
        if (requiredHeader == null){
            return null;
        }
        for (Header header : headers) {
            if(requiredHeader.equals(header.getName())){
                return header;
            }
        }
        return null;
    }
}
