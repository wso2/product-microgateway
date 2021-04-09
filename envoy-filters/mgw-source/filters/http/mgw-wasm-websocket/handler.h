#pragma once

//#include "config.pb.h"
#include "service.pb.h"

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse;

enum class ResponseStatus{
    // The request is not over limit.
    OK,
    // The rate limit status is not known.
    Unknown,
    // The request is over limit.
    OverLimit,
};

enum class HandlerState{
    OK,
    Error
};

class HandlerCallbacks{
public:
    virtual ~HandlerCallbacks() = default;

    virtual void updateFilterState(ResponseStatus state);

    virtual void updateHandlerState(HandlerState state);
};

class StreamHanlderClient{
public:
    virtual ~StreamHanlderClient() = default;

    virtual bool sendMessage(WebSocketFrameRequest request);
};

using StreamHanlderClientPtr = std::unique_ptr<StreamHanlderClient>;