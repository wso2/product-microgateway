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

import org.testng.annotations.BeforeClass;
import org.wso2.choreo.connect.tests.testcases.standalone.interceptor.InterceptorRequestFlowTestcase;
import org.wso2.choreo.connect.tests.util.ApictlUtils;

import java.util.concurrent.TimeUnit;

public class InterceptorRequestFlowAPIPolicyTestcase extends InterceptorRequestFlowTestcase {
    // TestNG runs same test cases twice when we have test classes that extends the first test class.

    @BeforeClass(description = "Init APIs")
    void start() throws Exception {
        // Undeploy APIs tested for Interceptors with the same base path
        ApictlUtils.undeployAPI("SwaggerPetstoreRequestIntercept", "1.0.5", "test", null);
        ApictlUtils.undeployAPI("SwaggerPetstoreResponseIntercept", "1.0.5", "test", null);

        // Create APIs with same base path, that used same interceptor configurations but instead of define them in
        // x-wso2 vendor extensions define them using API Policies.
        ApictlUtils.createProject("api_policy_intercept_request_openAPI.yaml", "api_policy_intercept_request_petstore",
                "backend_tls.crt", null, "interceptor.crt", "api_policy_intercept_request.yaml", true);
        ApictlUtils.createProject("api_policy_intercept_response_openAPI.yaml", "api_policy_intercept_response_petstore",
                "backend_tls.crt", null, "interceptor.crt", "api_policy_intercept_response.yaml", true);

        ApictlUtils.deployAPI("api_policy_intercept_request_petstore", "test");
        ApictlUtils.deployAPI("api_policy_intercept_response_petstore", "test");
        TimeUnit.SECONDS.sleep(5);
    }
}
