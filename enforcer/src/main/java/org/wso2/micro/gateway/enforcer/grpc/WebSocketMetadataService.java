package org.wso2.micro.gateway.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;
import org.wso2.micro.gateway.enforcer.websocket.WebSocketMetadataServiceGrpc;

import java.util.concurrent.ConcurrentHashMap;

public class WebSocketMetadataService extends WebSocketMetadataServiceGrpc.WebSocketMetadataServiceImplBase {
    private static final Logger logger = LogManager.getLogger(WebSocketMetadataService.class);
    private static ConcurrentHashMap<String, WebSocketResponseObserver> responseObservers = new ConcurrentHashMap<>();
    @Override
    public StreamObserver<RateLimitRequest> publishMetadata(StreamObserver<RateLimitResponse> responseObserver) {
        logger.debug("publishMetadata invoked from websocket meatadata service");
        return new WebSocketResponseObserver(responseObserver);
    }

    public static void addObserver(String streamId, WebSocketResponseObserver observer){
        responseObservers.put(streamId, observer);
    }

    public static void removeObserver(String streamId){
        responseObservers.remove(streamId);
    }


}
