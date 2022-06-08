// NOLINT(namespace-envoy)
#pragma once
#include <algorithm>
#include <google/protobuf/stubs/status.h>
#include <memory>
#include <string>
#include <string_view>
#include <unordered_map>

#include "proxy_wasm_intrinsics.h"
//#include "proxy_wasm_intrinsics_lite.pb.h"

#include "google/protobuf/util/json_util.h"

#include "config.pb.h"
#include "service.pb.h"
#include "handler.h"


using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config;

class MgwGrpcStreamHandler : public GrpcStreamHandler<WebSocketFrameRequest, WebSocketFrameRequest>, 
                                 public StreamHanlderClient {
  public:
    MgwGrpcStreamHandler(HandlerCallbacks *callbacks);
    ~MgwGrpcStreamHandler() override;

    void onReceive(size_t body_size) override;

    void onRemoteClose(GrpcStatus status) override;

    void sendMessage(WebSocketFrameRequest request) override;

  private:
    HandlerCallbacks *callbacks_;

};
