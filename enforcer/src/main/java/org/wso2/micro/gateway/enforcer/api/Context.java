package org.wso2.micro.gateway.enforcer.api;

/**
 * Marker interface that wraps object types for WebSocket APIs in order to follow the generic interface for APIs.
 * e.g - WebSocketAPI implements API <Context, Context>. For the initial upgrade request , the websocket API needs
 * to be of type WebSocketAPI implements API <RequestContext, ResponseObject>. For websocket frames, the API needs
 * to be of type WebSocketAPI implements API <WebSocketMetadataContext, WebSocketResponseObject>.
 */
public interface Context {
}
