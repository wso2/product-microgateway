#pragma once

#include "proxy_wasm_intrinsics.h"
//#include <google/protobuf/struct.pb.h>
#include <memory>
#include <string>

#include "handler_impl.h"
#include "handler.h"

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Metadata;

enum class ThrottleState {UnderLimit, OverLimit, FailureModeAllowed, FailureModeBlocked};

class MgwWebSocketRootContext: public RootContext{
public:
  explicit MgwWebSocketRootContext(uint32_t id, std::string_view root_id) : RootContext(id, root_id) {}

  bool onStart(size_t) override;
  bool onConfigure(size_t) override;
  void onTick() override {};

  envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config config_;
};

class MgwWebSocketContext : public Context , 
                            public HandlerCallbacks {
public:
  explicit MgwWebSocketContext(uint32_t id, RootContext* root) : Context(id, root) {}

  void onCreate() override;
  FilterHeadersStatus onRequestHeaders(uint32_t headers, bool end_of_stream) override;
  FilterDataStatus onRequestBody(size_t body_buffer_length, bool end_of_stream) override;
  FilterHeadersStatus onResponseHeaders(uint32_t headers, bool end_of_stream) override;
  FilterDataStatus onResponseBody(size_t body_buffer_length, bool end_of_stream) override;
  void onDone() override;
  void onLog() override;
  void onDelete() override;
  
  void updateFilterState(ResponseStatus status) override;
  void updateHandlerState(HandlerState state) override;
  

private:
  MgwGrpcStreamHandler* stream_handler_{};
  HandlerState handler_state_;
  std::string node_id_;
  //Metadata* ext_authz_metadata_{};
  bool isDataFrame(const std::string_view data);
  ThrottleState throttle_state_{ThrottleState::UnderLimit};
  bool failure_mode_deny_;
  std::unique_ptr<Metadata> metadata_{new Metadata};

};

