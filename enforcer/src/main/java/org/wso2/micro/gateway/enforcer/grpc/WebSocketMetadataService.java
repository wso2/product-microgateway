package org.wso2.micro.gateway.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.wso2.micro.gateway.enforcer.models.WebSocketResponseObserver;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;
import org.wso2.micro.gateway.enforcer.websocket.WebSocketMetadataServiceGrpc;

public class WebSocketMetadataService extends WebSocketMetadataServiceGrpc.WebSocketMetadataServiceImplBase {
    @Override
    public StreamObserver<RateLimitRequest> publishMetadata(StreamObserver<RateLimitResponse> responseObserver) {
        return new WebSocketResponseObserver(responseObserver);
    }
}
