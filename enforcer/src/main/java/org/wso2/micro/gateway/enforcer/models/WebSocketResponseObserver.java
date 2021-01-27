package org.wso2.micro.gateway.enforcer.models;

import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;

public class WebSocketResponseObserver implements StreamObserver<RateLimitRequest> {

    private WebSocketMetadata webSocketMetadata;
    private final StreamObserver<RateLimitResponse> responseStreamObserver;

    public WebSocketResponseObserver(StreamObserver<RateLimitResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(RateLimitRequest rateLimitRequest) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {

    }
}
