package org.wso2.choreo.connect.enforcer.websocket;

public class WebSocketFrameContext {
    private String streamId;
    private int frameLength;
    private String remoteIp;

    public WebSocketFrameContext(String streamId, int frameLength, String remoteIp) {
        this.streamId = streamId;
        this.frameLength = frameLength;
        this.remoteIp = remoteIp;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public int getFrameLength() {
        return frameLength;
    }

    public void setFrameLength(int frameLength) {
        this.frameLength = frameLength;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }
}
