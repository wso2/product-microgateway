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

import org.wso2.choreo.connect.mockbackend.async.MockAsyncServer;
import org.wso2.choreo.connect.mockbackend.graphql.MockGraphQLServer;
import org.wso2.choreo.connect.mockbackend.http2.Http2MockBackend;

import java.util.Arrays;
import java.util.List;

public class MockServices {

    public static void main(String[] args) {
        MockBackendProd mockBackendProd = new MockBackendProd(Constants.MOCK_BACKEND_SERVER_PORT);
        MockBackendSandbox mockBackendSandbox = new MockBackendSandbox(Constants.MOCK_SANDBOX_SERVER_PORT);
        MockBackend2 mockBackend2 = new MockBackend2(Constants.MOCK_BACKEND2_SERVER_PORT);
        // TODO: (VirajSalaka) start analytics server only when it requires
        MockAnalyticsServer mockAnalyticsServer = new MockAnalyticsServer(Constants.MOCK_ANALYTICS_SERVER_PORT);
        mockBackendProd.start();
        mockBackendSandbox.start();
        mockBackend2.start();
        mockAnalyticsServer.start();
        List<String> argList = Arrays.asList(args);
        if (argList.contains("-tls-enabled")) {
            MockBackendProd securedMockBackendProd = new MockBackendProd(Constants.SECURED_MOCK_BACKEND_SERVER_PORT,
                    true, false);
            MockBackendProd mtlsMockBackendProd = new MockBackendProd(Constants.MTLS_MOCK_BACKEND_SERVER_PORT,
                    true, true);
            securedMockBackendProd.start();
            mtlsMockBackendProd.start();
        }
        if (argList.contains("-interceptor-svc-enabled")) {
            MockInterceptorServer mockInterceptorServer = new MockInterceptorServer(
                    Constants.INTERCEPTOR_STATUS_SERVER_PORT,
                    Constants.MTLS_INTERCEPTOR_HANDLER_SERVER_PORT
            );
            mockInterceptorServer.start();
        }
        if (argList.contains("-async-enabled")) {
            MockAsyncServer mockAsyncServer = new MockAsyncServer(Constants.WEBSOCKET_SERVER_PORT);
            mockAsyncServer.start();
        }

        if (argList.contains("-soap-enabled")) {
            MockBackendSOAP mockBackendSoap = new MockBackendSOAP(Constants.MOCK_BACKEND_SOAP_SERVER_PORT);
            mockBackendSoap.start();
        }

        if (argList.contains("-gql-enabled")) {
            MockGraphQLServer mockGraphQLServer = new MockGraphQLServer(Constants.MOCK_GRAPHQL_SERVER_PORT);
            mockGraphQLServer.start();
        }

        if(argList.contains("-http2-server-enabled")){
            // clear text server
            Http2MockBackend http2BackendProdClearText = new Http2MockBackend(Constants.MOCK_BACKEND_HTTP2_SERVER_CLEAR_TEXT_PORT, false, false);
            http2BackendProdClearText.startServer();
        }

        if(argList.contains("-http2-tls-server-enabled")){
            // secured server
            Http2MockBackend http2BackendProdTLS = new Http2MockBackend(Constants.MOCK_BACKEND_HTTP2_SERVER_SECURED_PORT, true, false);
            http2BackendProdTLS.startServer();
        }

        if (argList.contains("-echo-service")){
            EchoService echoService = new EchoService(Constants.ECHO_SERVICE_SERVER_PORT, true);
            echoService.start();
        }
    }
}
