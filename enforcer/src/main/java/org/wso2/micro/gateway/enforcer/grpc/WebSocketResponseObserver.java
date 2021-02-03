package org.wso2.micro.gateway.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.api.WebSocketMetadataContext;
import org.wso2.micro.gateway.enforcer.api.WebSocketResponseObject;
import org.wso2.micro.gateway.enforcer.server.WebSocketHandler;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;

public class WebSocketResponseObserver implements StreamObserver<RateLimitRequest> {

    private static final Logger logger = LogManager.getLogger(WebSocketResponseObserver.class);
    private WebSocketMetadataContext webSocketMetadataContext;
    private final StreamObserver<RateLimitResponse> responseStreamObserver;
    private final WebSocketHandler webSocketHandler = new WebSocketHandler();

    public WebSocketResponseObserver(StreamObserver<RateLimitResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(RateLimitRequest rateLimitRequest) {
        webSocketMetadataContext = webSocketHandler.process(rateLimitRequest);
        RateLimitResponse response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.OK).build();
//        if(webSocketResponseObject == WebSocketResponseObject.OK){
//            response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.OK).build();
//        }else if(webSocketResponseObject == WebSocketResponseObject.OVER_LIMIT){
//            response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.OVER_LIMIT).build();
//        }else{
//            response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.UNKNOWN).build();
//        }
        responseStreamObserver.onNext(response);

    }

    @Override
    public void onError(Throwable throwable) {
        logger.info("onError called");
    }

    @Override
    public void onCompleted() {
        logger.info("onCompleted");
    }
}
