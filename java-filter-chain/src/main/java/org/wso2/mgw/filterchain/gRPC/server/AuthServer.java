/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.mgw.filterchain.gRPC.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class AuthServer {

    public static void main ( String[] args ) throws Exception
    {
        // Create a new server to listen on port 8081
        Server server = ServerBuilder.forPort(8081)
                .addService(new ExtAuthService())
                .build();

        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("+++++++++++++++++++++++ gRPC server started ++++++++++++++++++++++++++++");
        // Don't exit the main thread. Wait until server is terminated.
        server.awaitTermination();
    }
}
