package org.wso2.micro.gateway.tests.grpc;

import io.grpc.stub.StreamObserver;

public class TestServiceImpl extends TestServiceGrpc.TestServiceImplBase{

    @Override
    public void testCall(TestRequest testRequest, StreamObserver<TestResponse> responseObserver) {
        String receivedReq = testRequest.getTestReqString();
        TestResponse response = TestResponse.newBuilder().setTestResString("response received :" + receivedReq).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
