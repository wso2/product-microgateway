package org.wso2.micro.gateway.enforcer.api;

/**
 * OK - WebSocket connection is at non-throttled state
 * OVER_LIMIT - WebSocket connection is at throttled state
 * UNKNOWN - Throttle state of the WebSocket connection is not known
 */
public enum WebSocketResponseObject {
    UNKNOWN,
    OK,
    OVER_LIMIT
}
