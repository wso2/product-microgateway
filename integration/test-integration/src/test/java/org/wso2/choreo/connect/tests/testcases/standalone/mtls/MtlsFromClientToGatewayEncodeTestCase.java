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

package org.wso2.choreo.connect.tests.testcases.standalone.mtls;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MtlsFromClientToGatewayEncodeTestCase {
    protected String jwtTokenProd;
    protected String encodedCert;
    protected String encodedInvalidCert;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION,
                null, false);
        encodedCert = URLEncoder.encode(TestConstant.CLIENT_CERT, "UTF-8");
        encodedInvalidCert = URLEncoder.encode(TestConstant.INVALID_CLIENT_CERT, "UTF-8");
    }

    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with encoded client cert" +
            " in header")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIHeaderSuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API without client cert in" +
            " header")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIHeaderFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with invalid encoded " +
            "client cert in header")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIHeaderFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedInvalidCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API without jwt token" +
            " & ApiKey")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIHeaderFailTest3() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with invalid jwt token" +
            " & ApiKey")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIHeaderFailTest4() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_INVALID_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with encoded client cert in" +
            " header")
    public void invokeMTLSOptionalApiKeyMandatoryAPIHeaderSuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API without client cert in header")
    public void invokeMTLSOptionalApiKeyMandatoryAPIHeaderNotFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with invalid encoded client " +
            "cert in header")
    public void invokeMTLSOptionalApiKeyMandatoryAPIHeaderNotFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedInvalidCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API without ApiKey")
    public void invokeMTLSOptionalApiKeyMandatoryAPIHeaderFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with invalid ApiKey")
    public void invokeMTLSOptionalApiKeyMandatoryAPIHeaderFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_INVALID_API_KEY);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with encoded client cert in" +
            " header")
    public void invokeMTLSMandatoryOAuth2OptionalAPIHeaderSuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API without client cert in header")
    public void invokeMTLSMandatoryOAuth2OptionalAPIHeaderFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with invalid encoded client" +
            " cert in header")
    public void invokeMTLSMandatoryOAuth2OptionalAPIHeaderFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedInvalidCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API without jwt token")
    public void invokeMTLSMandatoryOAuth2OptionalAPIHeaderNotFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with invalid jwt token")
    public void invokeMTLSMandatoryOAuth2OptionalAPIHeaderNotFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        headers.put(TestConstant.CLIENT_CERT_HEADER, encodedCert);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
