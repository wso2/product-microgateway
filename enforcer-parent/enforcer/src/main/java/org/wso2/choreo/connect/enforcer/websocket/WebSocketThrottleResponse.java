/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.websocket;

/**
 * OK - WebSocket connection is at non-throttled state
 * OVER_LIMIT - WebSocket connection is at throttled state
 * UNKNOWN - Throttle state of the WebSocket connection is not known
 */
enum WebSocketThrottleState {
    UNKNOWN,
    OK,
    OVER_LIMIT
}

/**
 * Represents the object that is sent through the gRPC bidirectional stream as WebSocketFrameResponse
 */
public class WebSocketThrottleResponse {
    private WebSocketThrottleState webSocketThrottleState;
    private long throttlePeriod;
    private int apimErrorCode = 0;

    public WebSocketThrottleState getWebSocketThrottleState() {
        return webSocketThrottleState;
    }

    public void setOkState() {
        this.webSocketThrottleState = WebSocketThrottleState.OK;
    }

    public void setOverLimitState() {
        this.webSocketThrottleState = WebSocketThrottleState.OVER_LIMIT;
    }

    public void setUnknownState() {
        this.webSocketThrottleState = WebSocketThrottleState.UNKNOWN;
    }

    public long getThrottlePeriod() {
        return throttlePeriod;
    }

    public void setThrottlePeriod(long throttlePeriod) {
        this.throttlePeriod = throttlePeriod;
    }

    public int getApimErrorCode() {
        return apimErrorCode;
    }

    public void setApimErrorCode(int apimErrorCode) {
        this.apimErrorCode = apimErrorCode;
    }
}
