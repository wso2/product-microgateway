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

package org.wso2.choreo.connect.tests.testcases.withapim;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.ApimResourceProcessor;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class GzipCompressionTestCase extends ApimBaseTest {

    private static String applicationId;
    private static String accessToken;
    private static String endpointURL;
    private static final String API_CONTEXT = "compressionTestAPI";
    private static final String APPLICATION_NAME = "CompressionTestApp";
    private final Map<String, String> requestHeaders = new HashMap<>();

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();
        applicationId = ApimResourceProcessor.applicationNameToId.get(APPLICATION_NAME);
        Utils.delay(4000, "Interrupted while waiting to get the access token");
        accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps, applicationId, user, storeRestClient);
        Utils.delay(4000, "Interrupted while waiting to get the access token");
        requestHeaders.put(TestConstant.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/json");
    }

    @Test(description = "Test compression with Gzip supported server")
    public void testWithGzipSupportedServer() throws CCTestException, IOException {
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo-gzip");
        requestHeaders.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "{\"name\":\"User1\", \"age\":30, \"Data\":\"This is a simple text sending to test the compression filter\"}", requestHeaders);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("content-encoding"), "Response header does not contain an encoding type");
        Assert.assertTrue(response.getData().equals("{\"name\":\"User1\", \"age\":30, \"Data\":\"This is a simple text sending to test the compression filter\"}"), "Gzip Conversion mismatched");
        Map<String, String>  responseHeaders = response.getHeaders();
        Assert.assertNotNull(Utils.pickHeader(responseHeaders, HttpHeaders.CONTENT_ENCODING), HttpHeaders.CONTENT_ENCODING + " header is unavailable");
        Assert.assertEquals(responseHeaders.get(HttpHeaders.CONTENT_ENCODING.toLowerCase()), "gzip", "Content encoding header value mismatched");
    }

    @Test(description = "Test compression with Gzip unsupported server", dependsOnMethods = {"testWithGzipSupportedServer"})
    public void testWithGzipUnsupportedServer() throws MalformedURLException, CCTestException {
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo");
        requestHeaders.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "{\"name\":\"User1\", \"age\":30, \"Data\":\"This is a simple text sending to test the compression filter\"}", requestHeaders);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("content-encoding"), "Response header does not contain an encoding type");
        Assert.assertTrue(response.getData().equals("{\"name\":\"User1\", \"age\":30, \"Data\":\"This is a simple text sending to test the compression filter\"}"), "Gzip Conversion mismatched");
        Map<String, String>  responseHeaders = response.getHeaders();
        Assert.assertNotNull(Utils.pickHeader(responseHeaders, HttpHeaders.CONTENT_ENCODING), HttpHeaders.CONTENT_ENCODING + " header is unavailable");
        Assert.assertEquals(responseHeaders.get(HttpHeaders.CONTENT_ENCODING.toLowerCase()), "gzip", "Content encoding header value mismatched");
    }

    @Test(description = "Test compression with Gzip compressed data", dependsOnMethods = {"testWithGzipUnsupportedServer"})
    public void testWithGzipSupportedServerForGzipEncodedData() throws MalformedURLException, CCTestException {
        endpointURL = Utils.getServiceURLHttps(API_CONTEXT + "/1.0.0/echo-gzip");
        requestHeaders.put(HttpHeaders.ACCEPT_ENCODING, "gzip");
        requestHeaders.put(HttpHeaders.CONTENT_ENCODING, "gzip");
        requestHeaders.remove(HttpHeaders.CONTENT_TYPE);
        HttpResponse response = HttpsClientRequest.retryPostUntil200(endpointURL, "H4sIAAAAAAAAAA2JMQqAQAwEv7KktlDsrH2CPiBoPAPenVxSCOLfDUwxw7xUOAtNtJq0gToQp8ixD5vZOc5yqiFgmOb7Erg8DpOya0nwGm0OPwVbzXcTM60Fh14ujb4fAu/5o2EAAAA=",
                requestHeaders);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getHeaders().containsKey("content-encoding"), "Response header does not contain an encoding type");
        Assert.assertTrue(response.getData().equals("{\"name\":\"User1\", \"age\":30, \"Data\":\"This is a simple text sending to test the compression filter\"}"), "Gzip Conversion mismatched");
        Map<String, String>  responseHeaders = response.getHeaders();
        Assert.assertNotNull(Utils.pickHeader(responseHeaders, HttpHeaders.CONTENT_ENCODING), HttpHeaders.CONTENT_ENCODING + " header is unavailable");
        Assert.assertEquals(responseHeaders.get(HttpHeaders.CONTENT_ENCODING.toLowerCase()), "gzip", "Content encoding header value mismatched");
    }
}
