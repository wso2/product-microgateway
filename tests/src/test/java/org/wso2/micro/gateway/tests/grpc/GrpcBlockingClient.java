package org.wso2.micro.gateway.tests.grpc;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.micro.gateway.tests.grpc.gen.TestRequest;
import org.wso2.micro.gateway.tests.grpc.gen.TestResponse;
import org.wso2.micro.gateway.tests.grpc.gen.TestServiceGrpc;

public class GrpcBlockingClient {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);
    private final TestServiceGrpc.TestServiceBlockingStub blockingStub;

    public GrpcBlockingClient(Channel channel) {
        blockingStub = TestServiceGrpc.newBlockingStub(channel);
    }

    public String testCall(String requestText) {
        TestRequest request = TestRequest.newBuilder().setTestReqString(requestText).build();
        TestResponse response;
        try{
            response = blockingStub.testCall(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {0}", e.getStatus());
            response = TestResponse.newBuilder().setTestResString(e.getStatus().getDescription()).build();
        }
        if (response == null) {
            return null;
        }
        return response.getTestResString();
    }
}
