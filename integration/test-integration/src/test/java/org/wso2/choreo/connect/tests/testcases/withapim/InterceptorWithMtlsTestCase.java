/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.tests.testcases.withapim;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.apim.ApimBaseTest;
import org.wso2.choreo.connect.tests.apim.dto.AppWithConsumerKey;
import org.wso2.choreo.connect.tests.apim.dto.Application;
import org.wso2.choreo.connect.tests.apim.utils.PublisherUtils;
import org.wso2.choreo.connect.tests.apim.utils.StoreUtils;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.Utils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.TestConstant;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tests an API with an Interceptor in the request flow.
 *
 * Since we cannot add an interceptor cert (the cert that identifies the interceptor service) via APIM,
 * interceptor.crt (located in test resources) is appended to ca-certificates.crt (located in project resources)
 * and is placed inside resources present in the distribution. This happens during tests, before the choreo-connect
 * instance starts. The value for "trustedCertPath" in [router.upstream.tls] in the config.toml is updated to this
 * location. The location is given relative to the security dir mounted in the router container.
 */
public class InterceptorWithMtlsTestCase extends ApimBaseTest {
    private static final String API_NAME = "InterceptorApimModeWithMtlsApi";
    private static final String API_CONTEXT = "interceptor-apim-mode-with-mtls";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "InterceptorApimModeWithMtlsApp";
    private static final String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";

    String apiId;
    String applicationId;
    String accessToken;

    @BeforeClass(alwaysRun = true, description = "initialize setup")
    void setup() throws Exception {
        super.initWithSuperTenant();

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", API_NAME);
        apiProperties.put("context", "/" + API_CONTEXT);
        apiProperties.put("version", API_VERSION);
        apiProperties.put("provider", user.getUserName());

        String targetDir = Utils.getTargetDirPath();
        String filePath = targetDir + ApictlUtils.OPENAPIS_PATH + "intercept_request_path_level_openAPI.yaml";
        apiId = PublisherUtils.createAPIUsingOAS(apiProperties, filePath, publisherRestClient);
        PublisherUtils.createAPIRevisionAndDeploy(apiId, publisherRestClient);
        PublisherUtils.publishAPI(apiId, API_NAME, publisherRestClient);

        //Create App. Subscribe.
        Application app = new Application(APP_NAME, TestConstant.APPLICATION_TIER.UNLIMITED);
        AppWithConsumerKey appWithConsumerKey = StoreUtils.createApplicationWithKeys(app, storeRestClient);
        applicationId = appWithConsumerKey.getApplicationId();
        StoreUtils.subscribeToAPI(apiId, applicationId, TestConstant.SUBSCRIPTION_TIER.UNLIMITED, storeRestClient);
        Utils.delay(TestConstant.DEPLOYMENT_WAIT_TIME, "Interrupted when waiting for the " +
                "subscription to be deployed");
        accessToken = StoreUtils.generateUserAccessToken(apimServiceURLHttps,
                appWithConsumerKey.getConsumerKey(), appWithConsumerKey.getConsumerSecret(),
                new String[]{}, user, storeRestClient);
    }

    @BeforeMethod(description = "clear the status of interceptor management service")
    void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    @Test
    public void testUpdateOnlyHeadersInRequestFlowInterception() throws Exception {
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("foo-update-not-exist", "Header_Updated_New_Val");
        headersToReplace.put("x-wso2-cluster-header", "foo-invalid-attempt"); // this header should be discarded
        headersToReplace.put("x-wso2-foo-header", "foo-invalid-update"); // this header should be discarded
        headersToReplace.put("x-envoy-foo-header", "foo-invalid-update"); // this header should be discarded
        headersToReplace.put("content-type", "application/xml");
        interceptorRespBodyJSON.put("headersToReplace", headersToReplace);
        interceptorRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + accessToken);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/json");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                API_CONTEXT + "/1.0.0/echo/123"), clientReqBody, headers);

        Assert.assertNotNull(response);
        int expectedRespCode = StringUtils.isEmpty(clientReqBody) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        Assert.assertEquals(handler, InterceptorConstants.Handler.REQUEST_ONLY.toString(), "Invalid interceptor handler");

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertFalse(respHeaders.containsKey("foo-remove"), "Failed to remove header");
        Assert.assertEquals(respHeaders.get("foo-add"), "Header_newly_added", "Failed to add new header");
        Assert.assertEquals(respHeaders.get("foo-update"), "Header_Updated", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-update-not-exist"), "Header_Updated_New_Val", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("content-type"), "application/xml", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-keep"), "Header_to_be_kept", "Failed to keep original header");

        // discarded headers
        Assert.assertFalse(respHeaders.containsKey("x-wso2-foo-header"), "Failed to remove discarded header");
        Assert.assertFalse(respHeaders.containsKey("x-envoy-foo-header"), "Failed to remove discarded header");

        // test body
        Assert.assertEquals(response.getData(), clientReqBody);
    }

    @Test
    public void testUpdateHeadersAndRequestBodyInRequestFlowInterception() throws Exception {
        JSONObject interceptorRespBodyJSON = new JSONObject();

        // set body to be updated to
        String interceptorRespBody = "<student><name>Foo</name><age type=\"Y\">16</age></student>";
        interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));

        // set header updates related info
        interceptorRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("content-type", "application/xml");
        interceptorRespBodyJSON.put("headersToReplace", headersToReplace);
        interceptorRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + accessToken);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/json");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                API_CONTEXT + "/1.0.0/echo/456"), clientReqBody, headers);

        Assert.assertNotNull(response);
        int expectedRespCode = StringUtils.isEmpty(clientReqBody) ? HttpStatus.SC_NO_CONTENT : HttpStatus.SC_OK;
        Assert.assertEquals(response.getResponseCode(), expectedRespCode, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = getInterceptorStatus();
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        Assert.assertEquals(handler, InterceptorConstants.Handler.REQUEST_ONLY.toString(), "Invalid interceptor handler");

        // test headers
        Map<String, String> respHeaders = response.getHeaders();
        Assert.assertFalse(respHeaders.containsKey("foo-remove"), "Failed to remove header");
        Assert.assertEquals(respHeaders.get("foo-add"), "Header_newly_added", "Failed to add new header");
        Assert.assertEquals(respHeaders.get("foo-update"), "Header_Updated", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("content-type"), "application/xml", "Failed to replace header");
        Assert.assertEquals(respHeaders.get("foo-keep"), "Header_to_be_kept", "Failed to keep original header");

        // test body
        Assert.assertEquals(response.getData(), interceptorRespBody);
    }

    void setResponseOfInterceptor(String responseBody, boolean isRequestFlow) throws Exception {
        String servicePath = isRequestFlow ? "interceptor/request" : "interceptor/response";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        HttpResponse response = HttpClientRequest.doPost(Utils.getMockInterceptorManagerHttp(servicePath),
                responseBody, headers);
        Assert.assertNotNull(response, "Invalid response when setting response body of interceptor.");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched.");
    }

    JSONObject getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status.");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched.");
        return new JSONObject(response.getData());
    }
}
