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

import org.testng.annotations.DataProvider;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.testcases.standalone.interceptor.InterceptorServiceRequestBodyTestCase;

public class InterceptorServiceRequestBodyAPIPolicyTestCase extends InterceptorServiceRequestBodyTestCase {
    // TestNG runs same test cases twice when we have test classes that extends the first test class.

    @Override
    @DataProvider(name = "interceptFlowProvider")
    public Object[][] interceptFlowProvider() {
        //  {basePath, expectedHandler, isRequestFlow}
        return new Object[][]{
                {"APIPolicyRequestInterceptorAPI", "/intercept-request", InterceptorConstants.Handler.REQUEST_ONLY, true},
                {"APIPolicyResponseInterceptorAPI", "/intercept-response", InterceptorConstants.Handler.RESPONSE_ONLY, false}
        };
    }
}
