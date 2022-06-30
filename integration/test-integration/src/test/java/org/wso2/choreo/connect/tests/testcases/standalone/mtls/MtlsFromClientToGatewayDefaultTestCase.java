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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class MtlsFromClientToGatewayDefaultTestCase {
    protected String jwtTokenProd;
    protected String validKeyStore;
    protected String invalidKeyStore;

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION,
                null, false);
        validKeyStore = "client-keystore.jks";
        invalidKeyStore = "invalid-client-keystore.jks";
    }

    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with client cert")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPISuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API without client cert")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with invalid client cert")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers, invalidKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API without jwt token" +
            " & ApiKey")
    public void invoke1MTLSMandatoryOauth2ApiKeyMandatoryAPIFailTest3() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Mandatory OAuth2 & ApiKey enabled API with invalid jwt token" +
            " & ApiKey")
    public void invokeMTLSMandatoryOauth2ApiKeyMandatoryAPIFailTest4() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_INVALID_API_KEY);
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/1.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with client cert")
    public void invokeMTLSOptionalApiKeyMandatoryAPISuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API without client cert")
    public void invokeMTLSOptionalApiKeyMandatoryAPINotFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with invalid client cert")
    public void invokeMTLSOptionalApiKeyMandatoryAPINotFailTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_API_KEY);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers, invalidKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API without ApiKey")
    public void invokeMTLSOptionalApiKeyMandatoryAPIFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Optional MTLS and Mandatory ApiKey enabled API with invalid ApiKey")
    public void invokeMTLSOptionalApiKeyMandatoryAPIFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("apiKey", TestConstant.MUTUAL_SSL_OPTIONAL_INVALID_API_KEY);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/2.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with client cert")
    public void invokeMTLSMandatoryOAuth2OptionalAPISuccessTest() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API without client cert")
    public void invokeMTLSMandatoryOAuth2OptionalAPIFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with invalid client cert")
    public void invokeMTLSMandatoryOAuth2OptionalAPIFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers, invalidKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API without jwt token")
    public void invokeMTLSMandatoryOAuth2OptionalAPINotFailTest1() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
    @Test(description = "Invoke with Mandatory MTLS and Optional OAuth2 enabled API with invalid jwt token")
    public void invokeMTLSMandatoryOAuth2OptionalAPINotFailTest2() throws Exception {

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        CloseableHttpResponse response = HttpsClientRequest.doMutualSSLGet(Utils.getServiceURLHttps(
                "/mtls/3.0.0/pet/findByStatus"), headers, validKeyStore);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code mismatched");
    }
}
