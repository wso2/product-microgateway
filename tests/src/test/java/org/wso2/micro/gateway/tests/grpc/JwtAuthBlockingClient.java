/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.micro.gateway.tests.grpc;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.tests.grpc.gen.JwtAuthTestServiceGrpc;
import org.wso2.micro.gateway.tests.grpc.gen.TestRequest;
import org.wso2.micro.gateway.tests.grpc.gen.TestResponse;

/*
 * This class contains the gRPC client implementation for the JWT Authentication scenario.
 */
public class JwtAuthBlockingClient {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthBlockingClient.class);
    private final JwtAuthTestServiceGrpc.JwtAuthTestServiceBlockingStub blockingStub;

    public JwtAuthBlockingClient(Channel channel, String token) {
        JwtAuthTestServiceGrpc.JwtAuthTestServiceBlockingStub stub = JwtAuthTestServiceGrpc.newBlockingStub(channel);
        //add metadata
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), token);
        blockingStub = MetadataUtils.attachHeaders(stub,metadata);
    }

    public String testCall(String requestText) {
        TestRequest request = TestRequest.newBuilder().setTestReqString(requestText).build();
        TestResponse response;
        try{
            response = blockingStub.testCall(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {0}", e.getStatus());
            response = TestResponse.newBuilder().setTestResString(e.getStatus().getCode().value() + ":" +
                    e.getStatus().getDescription()).build();
        }
        if (response == null) {
            return null;
        }
        return response.getTestResString();
    }

    public String testCallWithScopes(String requestText) {
        TestRequest request = TestRequest.newBuilder().setTestReqString(requestText).build();
        TestResponse response;
        try{
            response = blockingStub.testCallWithScopes(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {0}", e.getStatus());
            response = TestResponse.newBuilder().setTestResString(e.getStatus().getCode().value() + ":" +
                    e.getStatus().getDescription()).build();
        }
        if (response == null) {
            return null;
        }
        return response.getTestResString();
    }

}
