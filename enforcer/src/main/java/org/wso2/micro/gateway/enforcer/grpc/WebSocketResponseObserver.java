package org.wso2.micro.gateway.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.enforcer.api.WebSocketMetadata;
import org.wso2.micro.gateway.enforcer.api.WebSocketResponseObject;
import org.wso2.micro.gateway.enforcer.server.WebSocketHandler;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;

public class WebSocketResponseObserver implements StreamObserver<RateLimitRequest> {

    private WebSocketMetadata webSocketMetadata;
    private final StreamObserver<RateLimitResponse> responseStreamObserver;
    private final WebSocketHandler webSocketHandler = new WebSocketHandler();

    public WebSocketResponseObserver(StreamObserver<RateLimitResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(RateLimitRequest rateLimitRequest) {
        WebSocketResponseObject webSocketResponseObject = webSocketHandler.process(rateLimitRequest);

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {

    }
}
