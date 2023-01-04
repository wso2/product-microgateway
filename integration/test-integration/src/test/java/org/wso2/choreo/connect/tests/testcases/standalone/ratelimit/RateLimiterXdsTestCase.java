/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org).
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
    public static final int DEPLOY_DELAY = 5000; // 5 seconds
    public static final String DEPLOY_DELAY_ERROR_MSG = "Could not wait till deploying API";

    @BeforeClass
    public void setup() throws CCTestException, IOException {
        ApictlUtils.addEnv(MG_ENV);
        ApictlUtils.login(MG_ENV);

        // Second API
        String apiProjectName = "rate_limiter_xds_2_test";
        ApictlUtils.createProject("rate_limiter_xds_2_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_2_operation_level_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
    }

    @AfterClass
    public void destroy() throws CCTestException {
        ApictlUtils.removeEnv(MG_ENV);
    }

    @Test(description = "Test deploy API with Operation Level Rate-Limits")
    public void testDeployAPIWithOperationLevelRateLimits() throws IOException, CCTestException {
        // First API
        String apiProjectName = "rate_limiter_xds_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_operation_level_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(DEPLOY_DELAY, DEPLOY_DELAY_ERROR_MSG);

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 20000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000);
        assertSecondAPI(configDump);
    }

    @Test(description = "Test redeploy API with updated Operation Level Rate-Limits",
            dependsOnMethods = {"testDeployAPIWithOperationLevelRateLimits"})
    public void testRedeployAPIWithUpdatedOperationLevelRateLimits() throws IOException, CCTestException {
        String apiProjectName = "rate_limiter_xds_updated_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_operation_level_updated_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(DEPLOY_DELAY, DEPLOY_DELAY_ERROR_MSG);

        // Remove the Operation level policy in "/rate-limiter-xds-test/pet" POST operation and add that policy in
        // "/rate-limiter-xds-test/pet" GET operation.
        // Change the policy value in "/rate-limiter-xds-test/pet/{petId}" GET operation from 20000 to 50000

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "GET", "MINUTE", 10000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 50000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000);
        assertSecondAPI(configDump);
    }

    @Test(description = "Test redeploy API with changing Rate-Limits policy level as API Level",
            dependsOnMethods = {"testRedeployAPIWithUpdatedOperationLevelRateLimits"})
    public void testRedeployAPIWithChangingRateLimitPolicyLevelAsAPILevel() throws IOException, CCTestException {
        // Redeploy the same API with changing operation level rate limits to API level rate limits
        String apiProjectName = "rate_limiter_xds_api_level_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_api_level_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(DEPLOY_DELAY, DEPLOY_DELAY_ERROR_MSG);

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        // Previous operation level rate limit configs of the API should not be in the config dump
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "GET", "MINUTE", 10000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 50000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000);
        // New API level rate limit policy should be there
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test", "ALL", "MINUTE", 50000);
        assertSecondAPI(configDump);
    }

    @Test(description = "Test redeploy API with changing Rate-Limits policy level as Operation Level",
            dependsOnMethods = {"testRedeployAPIWithChangingRateLimitPolicyLevelAsAPILevel"})
    public void testRedeployAPIWithChangingRateLimitPolicyLevelAsOperationLevel() throws IOException, CCTestException {
        // Redeploy the same API with changing API level rate limits to Operation level rate limits
        String apiProjectName = "rate_limiter_xds_operation_level_test";
        ApictlUtils.createProject("rate_limiter_xds_openAPI.yaml", apiProjectName,
                null, null, null, "rate_limiter_xds_operation_level_api.yaml", "rate_limiter_xds_policies.yaml");
        ApictlUtils.deployAPI(apiProjectName, MG_ENV);
        Utils.delay(DEPLOY_DELAY, DEPLOY_DELAY_ERROR_MSG);

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        // Previous API level rate limit config of the API should not be in the config dump
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test", "ALL", "MINUTE", 50000);
        // New operation level rate limit policies should be there
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "POST", "MINUTE", 10000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 20000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000);
        assertSecondAPI(configDump);
    }

    @Test(description = "Test undeploy API with Rate-Limit policies",
            dependsOnMethods = {"testRedeployAPIWithChangingRateLimitPolicyLevelAsOperationLevel"})
    public void testUndeployAPIWithRateLimitPolicies() throws CCTestException, IOException {
        ApictlUtils.undeployAPI("rate-limiter-xds-test", "5.0.3", MG_ENV, "localhost");
        Utils.delay(DEPLOY_DELAY, "Could not wait until undeploying the API");

        RateLimiterConfigDump configDump = RateLimiterConfigDump.getConfigDump();
        // Previous operation level rate limit configs of the API should not be in the config dump
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet", "GET", "MINUTE", 10000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "GET", "MINUTE", 50000);
        configDump.assertNotContainsRateLimitPolicyFor("/rate-limiter-xds-test/pet/{petId}", "DELETE", "MINUTE", 10000);

        assertSecondAPI(configDump);
    }

    private void assertSecondAPI(RateLimiterConfigDump configDump){
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-2-test/pet", "POST",
                "MINUTE", 10000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-2-test/pet/{petId}", "GET",
                "MINUTE", 20000);
        configDump.assertContainsRateLimitPolicyFor("/rate-limiter-xds-2-test/pet/{petId}", "DELETE",
                "MINUTE", 10000);
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
        return String.format("Default.org_carbon.super.vhost_localhost.path_%s.method_%s: unit=%s " +
                "requests_per_unit=%d, shadow_mode: false", path, method.toUpperCase(), unit.toUpperCase(), reqPerUnit);
    }

    private boolean containsRateLimitPolicyFor(String path, String method, String unit, int reqPerUnit) {
        String expectedDump = generateConfigDumpString(path, method, unit, reqPerUnit);
        return configDumpStr.contains(expectedDump);
    }

    public void assertContainsRateLimitPolicyFor(String path, String method, String unit, int reqPerUnit) {
        Assert.assertTrue(containsRateLimitPolicyFor(path, method, unit, reqPerUnit),
                String.format("Rate-limit config should exists for path %s, method %s with unit %s and count %d\n" +
                        "config dump:\n%s\n\n", path, method, unit, reqPerUnit, configDumpStr));
    }

    public void assertNotContainsRateLimitPolicyFor(String path, String method, String unit, int reqPerUnit) {
        Assert.assertFalse(containsRateLimitPolicyFor(path, method, unit, reqPerUnit),
                String.format("Rate-limit config should not exists for path %s, method %s with unit %s and count %d\n" +
                        "config dump:\n%s\n\n", path, method, unit, reqPerUnit, configDumpStr));
    }
}
