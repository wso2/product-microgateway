/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.choreo.connect.tests.testcases.standalone.metrics;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.tests.util.*;

public class PrometheusMetricEndpointsTestCase {
    @Test(description = "Test whether Enforcer metric endpoint is up")
    public void testEnforcerMetricEndpoint() throws Exception {
        HttpResponse response = HttpClientRequest.doGet("http://localhost:19080/metrics");
        Assert.assertNotNull(response);
        int responseCode = response.getResponseCode();
        Assert.assertEquals(200, responseCode, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("jvm_"), "Metric information not found in expected format");
    }

    @Test(description = "Test whether Adapter metric endpoint is up")
    public void testAdapterMetricEndpoint() throws Exception {
        HttpResponse response = HttpClientRequest.doGet("http://localhost:19085/metrics");
        Assert.assertNotNull(response);
        int responseCode = response.getResponseCode();
        Assert.assertEquals(200, responseCode, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("go_"), "Metric information not found in expected format");
    }


    @Test(description = "Test whether Router metric endpoint is up")
    public void testRouterMetricEndpoint() throws Exception {
        HttpResponse response = HttpClientRequest.doGet("http://localhost:9000/stats/prometheus");
        Assert.assertNotNull(response);
        int responseCode = response.getResponseCode();
        Assert.assertEquals(200, responseCode, "Response code mismatched");
        Assert.assertTrue(response.getData().contains("envoy_"), "Metric information not found in expected format");
    }
}

