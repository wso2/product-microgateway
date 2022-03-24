// NOLINT(namespace-envoy)
#include <algorithm>
#include <google/protobuf/stubs/status.h>
#include <memory>
#include <string>
#include <string_view>
#include <unordered_map>

#include "proxy_wasm_intrinsics.h"

#include "google/protobuf/util/json_util.h"

#include "config.pb.h"
#include "service.pb.h"

#include "handler_impl.h"

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse_Code_OK;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse_Code_OVER_LIMIT;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config;


MgwGrpcStreamHandler::MgwGrpcStreamHandler(HandlerCallbacks *callbacks){
    callbacks_ = callbacks;
}

MgwGrpcStreamHandler::~MgwGrpcStreamHandler(){
  LOG_TRACE("Handler destructed.");
}

void MgwGrpcStreamHandler::onReceive(size_t body_size){
  LOG_TRACE("gRPC streaming onReceive");
  WasmDataPtr message = getBufferBytes(WasmBufferType::GrpcReceiveBuffer, 0, body_size);
  const WebSocketFrameResponse& frame_response = message->proto<WebSocketFrameResponse>();
  LOG_TRACE(WebSocketFrameResponse_Code_Name(frame_response.throttle_state()));
  if(frame_response.throttle_state() == WebSocketFrameResponse_Code_OK){
    this->callbacks_->updateFilterState(ResponseStatus::OK);
  } else if (frame_response.throttle_state() == WebSocketFrameResponse_Code_OVER_LIMIT){
    this->callbacks_->updateThrottlePeriod(frame_response.throttle_period());
    this->callbacks_->updateFilterState(ResponseStatus::OverLimit);
  } else {
    this->callbacks_->updateFilterState(ResponseStatus::OK);
  }
  this->callbacks_->updateAPIMErrorCode(frame_response.apim_error_code());
};

void MgwGrpcStreamHandler::onRemoteClose(GrpcStatus status){
  LOG_TRACE(std::string("gRPC streaming onRemoteClose") + std::to_string(static_cast<int>(status)));
  this->callbacks_->updateHandlerState(HandlerState::Error);
};

bool MgwGrpcStreamHandler::sendMessage(WebSocketFrameRequest request){
  auto res = send(request, true);
  if(res != WasmResult::Ok){
    return false;
  }else{
    return true;
  }; 
};
