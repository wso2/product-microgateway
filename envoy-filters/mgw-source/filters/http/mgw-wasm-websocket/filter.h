#pragma once

#include "proxy_wasm_intrinsics.h"
#include <memory>
#include <string>

#include "handler_impl.h"
#include "handler.h"

#define X_REQUEST_ID "x-request-id"
#define INITIAL_APIM_ERROR_CODE "initialAPIMErrorCode"
#define THROTTLE_CONDITION_EXPIRE_TIMESTAMP "ThrottleConditionExpireTimestamp"

#define STATUS_HEADER ":status"
#define STATUS_101 "101"
#define ENFORCER_NOT_REACHABLE_ERROR_CODE 102500


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
  void updateThrottlePeriod(const int throttle_period) override;
  void updateAPIMErrorCode(int apim_error_code) override;
  ~MgwWebSocketContext() override;

private:
  MgwGrpcStreamHandler* stream_handler_{};
  HandlerState handler_state_;
  std::string node_id_;
  ThrottleState throttle_state_{ThrottleState::UnderLimit};
  bool failure_mode_deny_;
  std::unique_ptr<Metadata> metadata_{new Metadata};
  int throttle_period_;
  int apim_error_code_;
  std::string x_request_id_;
  
  bool isDataFrame(const std::string_view data);
  void establishNewStream();
  void sendEnforcerRequest(MgwWebSocketContext* websocContext, WebSocketFrameRequest request);
};

