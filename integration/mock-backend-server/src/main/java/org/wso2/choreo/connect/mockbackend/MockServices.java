/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.mockbackend;

import java.util.Arrays;

public class MockServices {

    public static void main(String[] args) {
        MockBackendProd mockBackendProd = new MockBackendProd(Constants.MOCK_BACKEND_SERVER_PORT);
        MockBackendSandbox mockBackendSandbox = new MockBackendSandbox(Constants.MOCK_SANDBOX_SERVER_PORT);
        // TODO: (VirajSalaka) start analytics server only when it requires
        MockAnalyticsServer mockAnalyticsServer = new MockAnalyticsServer(Constants.MOCK_ANALYTICS_SERVER_PORT);
        mockBackendProd.start();
        mockBackendSandbox.start();
        mockAnalyticsServer.start();
        if (args.length > 0 && args[0].equals("-tls-enabled")) {
            MockBackendProd securedMockBackendProd = new MockBackendProd(Constants.SECURED_MOCK_BACKEND_SERVER_PORT,
                    true, false);
            MockBackendProd mtlsMockBackendProd = new MockBackendProd(Constants.MTLS_MOCK_BACKEND_SERVER_PORT,
                    true, true);
            securedMockBackendProd.start();
            mtlsMockBackendProd.start();
        }
        if (Arrays.asList(args).contains("-interceptor-svc-enabled")) {
            MockInterceptorServer mockInterceptorServer = new MockInterceptorServer(
                    Constants.INTERCEPTOR_STATUS_SERVER_PORT,
                    Constants.MTLS_INTERCEPTOR_HANDLER_SERVER_PORT
            );
            mockInterceptorServer.start();
        }
    }
}
