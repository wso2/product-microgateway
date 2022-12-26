/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.testcases.standalone.ratelimit;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.context.CCTestException;
import org.wso2.choreo.connect.tests.util.ApictlUtils;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.Utils;

import java.io.IOException;

public class RateLimiterXdsTestCase {
    private static final String MG_ENV = "rate_limiter_mg_test";
    private static final String SHOULD_EXISTS = "Rate-limit config should exists";
    private static final String SHOULD_NOT_EXISTS = "Rate-limit config should not exists";

    @BeforeClass
    public void setup() throws CCTestException {
        ApictlUtils.addEnv(MG_ENV);
        ApictlUtils.login(MG_ENV);
    }

    @AfterClass
    public void destroy() throws CCTestException {
        ApictlUtils.removeEnv(MG_ENV);
    }

    @Test(description = "Test deploy API with Operation Level Rate-Limits")
    public void testDeployAPIWithOperationLevelRateLimits() throws IOException, CCTestException {
        String apiProjectName = "rate_limiter_xds_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_operation_level_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(5000, "Could not wait till deploying API.");

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000), SHOULD_EXISTS);
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 20000), SHOULD_EXISTS);
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000), SHOULD_EXISTS);
    }

    @Test(description = "Test redeploy API with updated Operation Level Rate-Limits",
            dependsOnMethods = {"testDeployAPIWithOperationLevelRateLimits"})
    public void testRedeployAPIWithUpdatedOperationLevelRateLimits() throws IOException, CCTestException {
        String apiProjectName = "rate_limiter_xds_updated_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_operation_level_updated_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(5000, "Could not wait till deploying API.");

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "GET", "MINUTE", 10000), SHOULD_EXISTS);
        Assert.assertFalse(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000), SHOULD_NOT_EXISTS);
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 50000), SHOULD_EXISTS);
        Assert.assertTrue(configDump.containsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000), SHOULD_EXISTS);
    }

    @Test(description = "Test redeploy API with changing Rate-Limits policy level as API Level",
            dependsOnMethods = {"testRedeployAPIWithUpdatedOperationLevelRateLimits"})
    public void testRedeployAPIWithChangingRateLimitPolicyLevelAsAPILevel() {
    }

    @Test(description = "Test redeploy API with changing Rate-Limits policy level as Operation Level",
            dependsOnMethods = {"testRedeployAPIWithChangingRateLimitPolicyLevelAsAPILevel"})
    public void testRedeployAPIWithChangingRateLimitPolicyLevelAsOperationLevel() {
    }

    @Test(description = "Test undeploy API with Rate-Limit policies",
            dependsOnMethods = {"testRedeployAPIWithChangingRateLimitPolicyLevelAsOperationLevel"})
    public void testUndeployAPIWithRateLimitPolicies() {
    }
}

class RateLimiterConfigDump {
    private final String configDumpStr;

    private RateLimiterConfigDump(String configDumpStr) {
        this.configDumpStr = configDumpStr;
    }

    public static RateLimiterConfigDump getConfigDump() throws IOException {
        HttpResponse httpResponse = HttpClientRequest.doGet("http://localhost:6070/rlconfig");
        Assert.assertNotNull(httpResponse);
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_OK);
        return new RateLimiterConfigDump(httpResponse.getData());
    }

    private String generateConfigDumpString(String path, String method, String unit, int reqPerUnit) {
        return String.format("default.org_carbon.super.vhost_localhost.path_%s.method_%s: unit=%s " +
                "requests_per_unit=%d, shadow_mode: false", path, method.toUpperCase(), unit.toUpperCase(), reqPerUnit);
    }

    public boolean containsRateLimitPolicyFor(String path, String method, String unit, int reqPerUnit) {
        String expectedDump = generateConfigDumpString(path, method, unit, reqPerUnit);
        return configDumpStr.contains(expectedDump);
    }
}
