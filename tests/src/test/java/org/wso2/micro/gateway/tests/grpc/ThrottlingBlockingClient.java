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
import org.wso2.micro.gateway.tests.grpc.gen.TestRequest;
import org.wso2.micro.gateway.tests.grpc.gen.TestResponse;
import org.wso2.micro.gateway.tests.grpc.gen.ThrottlingTestServiceGrpc;

/*
 * This class contains the gRPC client implementation for the throttling implementation scenario.
 */
public class ThrottlingBlockingClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
    private final ThrottlingTestServiceGrpc.ThrottlingTestServiceBlockingStub blockingStub;

    public ThrottlingBlockingClient(Channel channel, String token) {
        ThrottlingTestServiceGrpc.ThrottlingTestServiceBlockingStub stub =
                ThrottlingTestServiceGrpc.newBlockingStub(channel);
        //add metadata
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), token);
        blockingStub = MetadataUtils.attachHeaders(stub,metadata);
    }

    public String testCallServiceThrottling(String requestText) {
        TestRequest request = TestRequest.newBuilder().setTestReqString(requestText).build();
        TestResponse response;
        try{
            response = blockingStub.testCallServiceThrottling(request);
            for (int i=0; i<100; i++) {
                response = blockingStub.testCallServiceThrottling(request);
            }

        } catch (StatusRuntimeException e) {
            response = TestResponse.newBuilder().setTestResString(e.getStatus().getCode().value() + ":" +
                    e.getStatus().getDescription()).build();
        }
        if (response == null) {
            return null;
        }
        return response.getTestResString();
    }

    public String testCallMethodThrottling(String requestText) {
        TestRequest request = TestRequest.newBuilder().setTestReqString(requestText).build();
        TestResponse response;
        try{
            response = blockingStub.testCallMethodThrottling(request);
        } catch (StatusRuntimeException e) {
            response = TestResponse.newBuilder().setTestResString(e.getStatus().getCode().value() + ":" +
                    e.getStatus().getDescription()).build();
        }
        if (response == null) {
            return null;
        }
        return response.getTestResString();
    }
}
