/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.tests.testcases.standalone.apipolicy;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpMethod;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.dto.EchoResponse;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class APIPolicyTestCase {
    private String jwtTokenProd;
    private static final String basePath = "/api-policy";
    private Map<String, String> headers;
    private final String queryParams = "?foo1=bar1&foo2=bar2";

    @BeforeClass(description = "Get Prod token")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
        Awaitility.await().pollInterval(2, TimeUnit.SECONDS).atMost(2, TimeUnit.MINUTES)
                .until(APIPolicyTestCase::checkOPAServerHealth);
    }

    @BeforeMethod
    void setClientRequestInfo() {
        headers = new HashMap<>();
        headers.put("Sample1", "Sample Value");
        headers.put("Sample2", "Sample Value");
    }

    @Test(description = "Test header based API Policies")
    public void testSetHeaderRemoveHeaderAPIPolicies() throws Exception {
        headers.put("RemoveThisHeader", "Unnecessary Header");
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath,
                "/echo-full/headers-policy/123" + queryParams, "Hello World!", headers, jwtTokenProd);
        
        Assert.assertFalse(echoResponse.getHeaders().containsKey("RemoveThisHeader"),
                getPolicyFailAssertMessage("Remove Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey1"), "newHeaderVal1",
                getPolicyFailAssertMessage("Add Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey2"), "newHeaderVal2",
                getPolicyFailAssertMessage("Add Header"));
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test header based unsupported API Policies")
    public void testUnsupportedAPIPolicies() throws Exception {
        headers.put("RemoveThisHeader", "Unnecessary Header");
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath,
                "/echo-full/unsupported-policy/123" + queryParams, "Hello World!", headers, jwtTokenProd);

        // check supported policies
        Assert.assertFalse(echoResponse.getHeaders().containsKey("RemoveThisHeader"),
                getPolicyFailAssertMessage("Remove Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey1"), "newHeaderVal1",
                getPolicyFailAssertMessage("Add Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey2"), "newHeaderVal2",
                getPolicyFailAssertMessage("Add Header"));
        assertOriginalClientRequestInfo(echoResponse);

        // check unsupported policies
        Assert.assertFalse(echoResponse.getHeaders().containsKey("newHeaderKeyFromUnsupportedAction"),
                "Unsupported policy has applied");
        Assert.assertFalse(echoResponse.getHeaders().containsKey("newHeaderKeyFromUnsupportedParam"),
                "Unsupported policy has applied");
    }

    @Test(description = "Test custom API Policies and Policy Versions")
    public void testCustomAPIPoliciesAndPolicyVersions() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath,
                "/echo-full/custom-policy/123" + queryParams, "Hello World!", headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getHeaders().getFirst("customV1NewHeaderKey"), "customV1NewHeaderVal",
                getPolicyFailAssertMessage("Custom Add Header V1"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("customV2NewHeaderKey"), "customV2NewHeaderVal",
                getPolicyFailAssertMessage("Custom Add Header V2"));
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test query based API Policies")
    public void testQueryAPIPolicy() throws Exception {
        EchoResponse echoResponse = Utils.invokeEchoGet(basePath,
                "/echo-full/query-policy" + queryParams, headers, jwtTokenProd);
        
        Assert.assertEquals(echoResponse.getQuery().get("helloQ1"), "worldQ1",
                getPolicyFailAssertMessage("Add Query Param"));
        Assert.assertEquals(echoResponse.getQuery().get("helloQ2"), "worldQ2",
                getPolicyFailAssertMessage("Add Query Param"));
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite method and rewrite path API Policies")
    public void testRewriteMethodAndPathAPIPolicy() throws Exception {
        // HTTP method: GET
        EchoResponse echoResponse = Utils.invokeEchoGet(basePath,
                "/echo-full/rewrite-policy/345" + queryParams, headers, jwtTokenProd);
        
        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path");
        assertOriginalClientRequestInfo(echoResponse);

        // HTTP method: POST
        echoResponse = Utils.invokeEchoPost(basePath,
                "/echo-full/rewrite-policy/345" + queryParams, "Hello World", headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.POST.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path API Policy with capture groups")
    public void testRewritePathAPIPolicyWithCaptureGroups() throws Exception {
        // HTTP method: GET
        EchoResponse echoResponse = Utils.invokeEchoGet(basePath,
                "/echo-full/rewrite-policy-with-capture-groups/shops/shop1234.xyz/pets/pet890/orders"
                        + queryParams, headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path API Policy with capture groups with trailing slash in path")
    public void testRewritePathAPIPolicyWithCaptureGroupsWithTrailingSlashInPath() throws Exception {
        // HTTP method: GET with trailing slash
        EchoResponse echoResponse = Utils.invokeEchoGet(basePath,
                "/echo-full/rewrite-policy-with-capture-groups/shops/shop1234.xyz/pets/pet890/orders/"
                        + queryParams, headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/pets/pet890.pets/hello-shops/abcd-shops/shop1234");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test rewrite path API Policy with capture groups with invalid param")
    public void testRewritePathAPIPolicyWithCaptureGroupsInvalidParam() throws Exception {
        boolean errorWhenDeploying = false;
        ApictlUtils.createProject( "api_policy_invalid_param_id_openAPI.yaml", "api_policy_invalid_param_id", null, null, null, "api_policies_invalid_param_id.yaml", true);

        try {
            ApictlUtils.deployAPI("api_policy_invalid_param_id", "test");
        } catch (CCTestException e) {
            errorWhenDeploying = true;
        }
        Assert.assertTrue(errorWhenDeploying, "An error must occur while deploying if an invalid param is provided.");
    }

    @Test(description = "Test rewrite path and discard queries in rewrite path API Policies")
    public void testRewritePathAndDiscardQueriesAPIPolicy() throws Exception {
        // HTTP method: GET
        EchoResponse echoResponse = Utils.invokeEchoGet(basePath,
                "/echo-full/rewrite-policy/discard-query-params" + queryParams, headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.DELETE.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path2");
        Assert.assertTrue(echoResponse.getQuery().isEmpty(), "Query params has not been discarded");
    }

    @Test(description = "Test rewrite path and discard queries in rewrite path API Policies with trailing slash in path")
    public void testRewritePathAndDiscardQueriesAPIPolicyWithTrailingSlashInPath() throws Exception {
        // HTTP method: GET with trailing slash
        EchoResponse echoResponse1 = Utils.invokeEchoGet(basePath, "/echo-full/rewrite-policy/discard-query-params/"
                + queryParams, headers, jwtTokenProd);

        Assert.assertEquals(echoResponse1.getMethod(), HttpMethod.DELETE.name());
        Assert.assertEquals(echoResponse1.getPath(), "/v2/echo-full/new-path2");
        Assert.assertTrue(echoResponse1.getQuery().isEmpty(), "Query params has not been discarded");
    }

    @Test(description = "Test OPA API policy - Success Validation")
    public void testOPAAPIPolicySuccessValidation() throws Exception {
        headers.put("foo", "bar"); // this header is validated in OPA policy
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath, "/echo-full/opa-policy" + queryParams,
                "Hello", headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getData(), "Hello");
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey1"), "newHeaderVal1");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test Custom OPA API policy - Success Validation")
    public void testCustomOPAAPIPolicySuccessValidation() throws Exception {
        headers.put("custom-foo", "custom-bar"); // this header is validated in OPA policy
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath, "/echo-full/custom-opa-policy" + queryParams,
                "Hello", headers, jwtTokenProd);

        Assert.assertEquals(echoResponse.getData(), "Hello");
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newHeaderKey1"), "newHeaderVal1");
        assertOriginalClientRequestInfo(echoResponse);
    }

    @Test(description = "Test OPA API policy - Failed Validation")
    public void testOPAAPIPolicyFailedValidation() throws Exception {
        // missing the header "foo"
        HttpResponse response = Utils.invokePost(basePath, "/echo-full/opa-policy" + queryParams,
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }

    @Test(description = "Test Custom OPA API policy - Failed Validation")
    public void testCustomOPAAPIPolicyFailedValidation() throws Exception {
        // missing the header "custom-foo"
        HttpResponse response = Utils.invokePost(basePath, "/echo-full/custom-opa-policy" + queryParams,
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }

    @Test(description = "Test Custom OPA API policy - Not found Impl of Request Generator")
    public void testCustomOPAAPIPolicyNotFoundImpl() throws Exception {
        // missing the header "custom-foo"
        HttpResponse response = Utils.invokePost(basePath, "/echo-full/custom-opa-policy-not-found" + queryParams,
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
    }

    @Test(description = "Test OPA API policy - No auth token - Failed Validation")
    public void testOPAAPIPolicyNoTokenFailedValidation() throws Exception {
        headers.put("foo", "bar"); // this header is validated in OPA policy
        // auth key type is validated in OPA policy, since it is missing, validation failed
        HttpResponse response = Utils.invokePost(basePath, "/echo-full/opa-policy-no-access-token" + queryParams,
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_FORBIDDEN, "Response code mismatched");
    }


    @Test(description = "Test OPA API policy - Invalid Response from OPA server")
    public void testOPAAPIPolicyInvalidResponse() throws Exception {
        HttpResponse response = Utils.invokePost(basePath, "/echo-full/opa-policy-invalid-response" + queryParams,
                "Hello", headers, jwtTokenProd);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
    }

    @Test(description = "Test all API Policies together")
    public void testAllPoliciesTogether() throws Exception {
        headers.put("RemoveThisHeader", "Unnecessary Header");
        headers.put("foo", "bar"); // this header is validated in OPA policy
        EchoResponse echoResponse = Utils.invokeEchoPost(basePath,
                "/echo-full/all-policies/123-abc" + queryParams, "Hello World!", headers, jwtTokenProd);

        Assert.assertFalse(echoResponse.getHeaders().containsKey("RemoveThisHeader"),
                getPolicyFailAssertMessage("Remove Header"));
        Assert.assertEquals(echoResponse.getHeaders().getFirst("newH1"), "newH1Value",
                getPolicyFailAssertMessage("Add Header"));
        Assert.assertEquals(echoResponse.getQuery().get("newQ1"), "newQ1Value",
                getPolicyFailAssertMessage("Add Query"));
        Assert.assertEquals(echoResponse.getMethod(), HttpMethod.PUT.name());
        Assert.assertEquals(echoResponse.getPath(), "/v2/echo-full/new-path-all-policies");
        Assert.assertEquals(echoResponse.getData(), "Hello World!");
        assertOriginalClientRequestInfo(echoResponse);
    }

    private void assertOriginalClientRequestInfo(EchoResponse echoResponse) {
        Assert.assertEquals(echoResponse.getHeaders().getFirst("Sample1"), "Sample Value",
                "Original header value in client request is missing");
        Assert.assertEquals(echoResponse.getHeaders().getFirst("Sample2"), "Sample Value",
                "Original header value in client request is missing");
        Assert.assertEquals(echoResponse.getQuery().get("foo1"), "bar1",
                "Original query value in client request is missing");
        Assert.assertEquals(echoResponse.getQuery().get("foo2"), "bar2",
                "Original query value in client request is missing");
    }

    private String getPolicyFailAssertMessage(String policyName) {
        return String.format("Gateway has failed to apply %s API policy", policyName);
    }

    private static Boolean checkOPAServerHealth() {
        try {
            HttpResponse response = HttpsClientRequest.doGet("https://localhost:8181/health?bundles");
            return response.getResponseCode() == HttpStatus.SC_OK;
        } catch (CCTestException e) {
            return false;
        }
    }
}
