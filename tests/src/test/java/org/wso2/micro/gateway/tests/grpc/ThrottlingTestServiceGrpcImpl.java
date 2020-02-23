package org.wso2.micro.gateway.tests.grpc;

import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.tests.grpc.gen.TestRequest;
import org.wso2.micro.gateway.tests.grpc.gen.TestResponse;
import org.wso2.micro.gateway.tests.grpc.gen.ThrottlingTestServiceGrpc;

public class ThrottlingTestServiceGrpcImpl extends ThrottlingTestServiceGrpc.ThrottlingTestServiceImplBase {
    @Override
    public void testCallServiceThrottling(TestRequest testRequest, StreamObserver<TestResponse> responseObserver) {
        String receivedReq = testRequest.getTestReqString();
        TestResponse response = TestResponse.newBuilder().setTestResString("response received :" + receivedReq).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void testCallMethodThrottling(TestRequest testRequest, StreamObserver<TestResponse> responseObserver) {
        String receivedReq = testRequest.getTestReqString();
        TestResponse response = TestResponse.newBuilder().setTestResString("response received :" + receivedReq).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
