package org.wso2.micro.gateway.tests.grpc;

import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.tests.grpc.gen.JwtAuthTestServiceGrpc;
import org.wso2.micro.gateway.tests.grpc.gen.TestRequest;
import org.wso2.micro.gateway.tests.grpc.gen.TestResponse;

public class JwtAuthTestServiceGrpcImpl extends JwtAuthTestServiceGrpc.JwtAuthTestServiceImplBase {
    @Override
    public void testCall(TestRequest testRequest, StreamObserver<TestResponse> responseObserver) {
        String receivedReq = testRequest.getTestReqString();
        TestResponse response = TestResponse.newBuilder().setTestResString("response received :" + receivedReq).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void testCallWithScopes(TestRequest testRequest, StreamObserver<TestResponse> responseObserver) {
        String receivedReq = testRequest.getTestReqString();
        TestResponse response = TestResponse.newBuilder().setTestResString("response received :" + receivedReq).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
