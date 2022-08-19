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

package org.wso2.choreo.connect.tests.testcases.standalone.soap;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SOAPApisTestCase {

    private final String context = "/soap/1.0.0";
    private final String version = "1.0.0";
    private static final String SOAP_ACTION = "http://mockbackend:2340/phoneverify/query/CheckPhoneNumber";

    private String testKey;

    @BeforeClass
    public void start() throws Exception {
        API api = new API();
        api.setName("DefaultVersion");
        api.setContext(context);
        api.setVersion(version);
        api.setProvider("admin");

        ApplicationDTO app = new ApplicationDTO();
        app.setName("DefaultAPIApp");
        app.setTier("Unlimited");
        app.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, app, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
    }

    @Test(description = "Invoke SOAP API with test key using SOAP 1.1")
    public void testInvokeSOAPAPIv11() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, testKey);
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.TEXT_XML);
        headers.put(TestConstant.SOAP_ACTION_HEADER, SOAP_ACTION);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(context  +"/phoneverify11"),
                TestConstant.SOAP_ENVELOPES.SOAP11_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " +
                Utils.getServiceURLHttps(context  +"/phoneverify11"));
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("<Valid>true</Valid>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.TEXT_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Invoke SOAP API with test key using SOAP 1.2")
    public void testInvokeSOAPAPIv12() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, testKey);
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.SOAP_XML);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(context  +"/phoneverify12"),
                TestConstant.SOAP_ENVELOPES.SOAP12_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response, "Error occurred while invoking the endpoint " +
                Utils.getServiceURLHttps(context  +"/phoneverify12"));
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("<Valid>true</Valid>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.SOAP_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Invoke SOAP1.1 API with invalid token to check denied response from enforcer")
    public void testSoap11DeniedResponse401() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, "t0k15n");
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.TEXT_XML);
        headers.put(TestConstant.SOAP_ACTION_HEADER, SOAP_ACTION);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(context  +"/phoneverify11"),
                TestConstant.SOAP_ENVELOPES.SOAP11_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("http://schemas.xmlsoap.org/soap/envelope/"),
                "Response soap version mismatched");
        Assert.assertTrue(response.getData().contains("<SOAP-ENV:Fault><faultcode>Server</faultcode>" +
                        "<faultstring>Invalid Credentials</faultstring><detail>" +
                        "900901:Make sure you have provided the correct security credentials</detail></SOAP-ENV:Fault>"),
                "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.TEXT_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Invoke SOAP1.2 API with invalid token to check denied response from enforcer")
    public void testSoap12DeniedResponse401() throws CCTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, "t0k15n");
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.SOAP_XML);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(context  +"/phoneverify12"),
                TestConstant.SOAP_ENVELOPES.SOAP12_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("http://www.w3.org/2003/05/soap-envelope"),
                "Response soap version mismatched");
        Assert.assertTrue(response.getData().contains("<env:Fault><env:Code><env:Value>env:Receiver</env:Value></env:Code>" +
                        "<env:Reason><env:Text xml:lang=\"en-US\">Invalid Credentials</env:Text></env:Reason>" +
                        "<env:Detail>900901:Make sure you have provided the correct security credentials</env:Detail></env:Fault>"),
                "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.SOAP_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Invoke SOAP1.1 API which is not deployed, for testing response generated(404) from response mapper")
    public void testSoap11DeniedResponse404() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, testKey);
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.TEXT_XML);
        headers.put(TestConstant.SOAP_ACTION_HEADER, SOAP_ACTION);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps("/soap11ex404"),
                TestConstant.SOAP_ENVELOPES.SOAP11_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("http://schemas.xmlsoap.org/soap/envelope/"),
                "Response soap version mismatched");
        Assert.assertTrue(response.getData().contains("<soapenv:Fault><faultcode>soapenv:Server</faultcode>" +
                "<faultstring>Not Found</faultstring><detail>404:The requested resource is not available.</detail>" +
                "</soapenv:Fault>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.TEXT_XML,
                "Response content-type mismatch");
    }

    @Test(description = "Invoke SOAP1.2 API which is not deployed, for testing response generated(404) from response mapper")
    public void testSoap12DeniedResponse404() throws MalformedURLException, CCTestException {
        Map<String, String> headers = new HashMap<>();
        headers.put(TestConstant.INTERNAL_KEY_HEADER, testKey);
        headers.put(TestConstant.CONTENT_TYPE_HEADER, TestConstant.CONTENT_TYPES.SOAP_XML);
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps("/soap12ex404"),
                TestConstant.SOAP_ENVELOPES.SOAP12_SAMPLE_REQ_PAYLOAD, headers);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NOT_FOUND, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("http://www.w3.org/2003/05/soap-envelope/"),
                "Response soap version mismatched");
        Assert.assertTrue(response.getData().contains("<soapenv:Fault><faultcode>soapenv:Receiver</faultcode>" +
                "<faultstring>Not Found</faultstring><detail>404:The requested resource is not available.</detail>" +
                "</soapenv:Fault>"), "Response body mismatched");
        Map<String, String> responseHeaders = response.getHeaders().entrySet().stream().collect(
                Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> entry.getValue()
                )
        );
        Assert.assertEquals(responseHeaders.get(TestConstant.CONTENT_TYPE_HEADER), TestConstant.CONTENT_TYPES.SOAP_XML,
                "Response content-type mismatch");
    }
}
