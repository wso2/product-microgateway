package org.wso2.micro.gateway.enforcer.grpc;

import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.api.WebSocketMetadataContext;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.server.WebSocketHandler;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitRequest;
import org.wso2.micro.gateway.enforcer.websocket.RateLimitResponse;

public class WebSocketResponseObserver implements StreamObserver<RateLimitRequest> {

    private static final Logger logger = LogManager.getLogger(WebSocketResponseObserver.class);
    private WebSocketMetadataContext webSocketMetadataContext;
    private final StreamObserver<RateLimitResponse> responseStreamObserver;
    private final WebSocketHandler webSocketHandler = new WebSocketHandler();
    private String streamId;
    private int count;

    public WebSocketResponseObserver(StreamObserver<RateLimitResponse> responseStreamObserver) {
        this.responseStreamObserver = responseStreamObserver;
    }

    @Override
    public void onNext(RateLimitRequest rateLimitRequest) {
        count++;
        webSocketMetadataContext = webSocketHandler.process(rateLimitRequest);
        streamId = getStreamId(rateLimitRequest);
        WebSocketMetadataService.addObserver(streamId,this);
        // Demo rate limit scenario
        if(count > 10 && count < 15){
            RateLimitResponse response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.OVER_LIMIT).build();
            responseStreamObserver.onNext(response);
        }else {
            RateLimitResponse response = RateLimitResponse.newBuilder().setOverallCode(RateLimitResponse.Code.OK).build();
            responseStreamObserver.onNext(response);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.debug("websocket metadata service onError: "+ throwable.toString());
        WebSocketMetadataService.removeObserver(streamId);
    }

    @Override
    public void onCompleted() {
        WebSocketMetadataService.removeObserver(streamId);
    }

    private String getStreamId(RateLimitRequest rateLimitRequest){
        return rateLimitRequest.getMetadataContext().getFilterMetadataMap().
                get(APIConstants.EXT_AUTHZ_METADATA).getFieldsMap().get(APIConstants.WEBSOCKET_STREAM_ID).getStringValue();
    }

}
