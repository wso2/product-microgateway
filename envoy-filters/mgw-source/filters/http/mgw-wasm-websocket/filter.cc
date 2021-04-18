// NOLINT(namespace-envoy)
#include <algorithm>
#include <google/protobuf/stubs/status.h>
#include <memory>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <sstream>

#include "handler.h"
#include "proxy_wasm_intrinsics.h"
#include "proxy_wasm_intrinsics_lite.pb.h"

#include "google/protobuf/util/json_util.h"

#include "config.pb.h"
#include "filter.h"

static constexpr char EnforcerServiceName[] = "envoy.extensions.filters.http.mgw_wasm_websocket.v3.WebSocketFrameService";
static constexpr char PublishFrameData[] = "PublishFrameData";

using google::protobuf::util::JsonParseOptions;
using google::protobuf::util::error::Code;
using google::protobuf::util::Status;

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Metadata;
// using envoy::extensions::filters::http::mgw_wasm_websocket::v3::MetadataValue;


static RegisterContextFactory register_MgwWebSocketContext(CONTEXT_FACTORY(MgwWebSocketContext),
                                                      ROOT_FACTORY(MgwWebSocketRootContext),
                                                      "mgw_WASM_websocket_root");

bool MgwWebSocketRootContext::onStart(size_t) {
  LOG_TRACE("onStart RootContext mgw_WASM_websocket");
  return true;
}

bool MgwWebSocketRootContext::onConfigure(size_t config_size) {
  LOG_TRACE("onConfigure RootContext mgw_WASM_websocket");
  //proxy_set_tick_period_milliseconds(10000); // 1 sec
  const WasmDataPtr configuration = getBufferBytes(WasmBufferType::PluginConfiguration, 0, config_size);
  JsonParseOptions json_options;
  const Status options_status = google::protobuf::util::JsonStringToMessage(
      configuration->toString(),
      &config_, json_options);
  if (options_status != Status::OK) {
    LOG_WARN("Cannot parse plugin configuration JSON string: " + configuration->toString());
    return false;
  }
  LOG_INFO("Loading Config: " + config_.node_id());
  return true;
}

// void MgwWebSocketRootContext::onTick() { //LOG_TRACE("onTick"); }

void MgwWebSocketContext::onCreate() { 
  LOG_TRACE(std::string("onCreate " + std::to_string(id())));
  MgwWebSocketRootContext *r = dynamic_cast<MgwWebSocketRootContext*>(root());
  this->node_id_ = r->config_.node_id();
  this->failure_mode_deny_ = r->config_.failure_mode_deny();
}

FilterHeadersStatus MgwWebSocketContext::onRequestHeaders(uint32_t, bool) {
  LOG_TRACE(std::string("onRequestHeaders called mgw_WASM_websocket") + std::to_string(id()));
  // Initialize grpcStreamHandler and assign it as a member variable
  this->stream_handler_ = new MgwGrpcStreamHandler(this);
  
  // Initialize a gRPC bidi-stream using the created grpcStreamHandler
  GrpcService grpc_service;
  MgwWebSocketRootContext *r = dynamic_cast<MgwWebSocketRootContext*>(root());
  grpc_service.mutable_envoy_grpc()->set_cluster_name(r->config_.rate_limit_service());  
  std::string grpc_service_string;
  grpc_service.SerializeToString(&grpc_service_string);
  HeaderStringPairs initial_metadata;
  initial_metadata.push_back(std::pair("mgw_wasm_websocket", "initial"));
  auto handler_response = root()->grpcStreamHandler(grpc_service_string, EnforcerServiceName, PublishFrameData, initial_metadata, std::unique_ptr<GrpcStreamHandlerBase>(this->stream_handler_));
  if (handler_response != WasmResult::Ok) {
    LOG_TRACE("gRPC bidi stream initialization failed: " + toString(handler_response));
  }else{
    this->handler_state_ = HandlerState::OK;
    LOG_TRACE(std::string("gRPC bidi stream created successfully"));     
  }

  // Extract ext_authz dynamic metadata and assign it to a member variable 
  auto buffer = getProperty<std::string>({"metadata", "filter_metadata", "envoy.filters.http.ext_authz"});
  if (buffer.has_value() && buffer.value()->size() != 0) {
    auto pairs = buffer.value()->pairs();
    for (auto &p : pairs) {
      //(*this->ext_authz_metadata_)[std::string(p.first)] = std::string(p.second);
      (*this->metadata_->mutable_ext_authz_metadata())[std::string(p.first)] = std::string(p.second);
      LOG_TRACE(std::string(p.first) + std::string(" -> ") + std::string(p.second));
      //metadataValue->set_key(std::string(p.first));
      //metadataValue->set_value(std::string(p.second));
    }
  }
                     
  return FilterHeadersStatus::Continue;
}

FilterHeadersStatus MgwWebSocketContext::onResponseHeaders(uint32_t, bool) {
  LOG_TRACE(std::string("onResponseHeaders called mgw_WASM_websocket") + std::to_string(id()));
  auto result = getResponseHeaderPairs();
  auto pairs = result->pairs();
  LOG_TRACE(std::string("headers: ") + std::to_string(pairs.size()));
  for (auto& p : pairs) {
    LOG_TRACE(std::string(p.first) + std::string(" -> ") + std::string(p.second));
  }
  return FilterHeadersStatus::Continue;
}

FilterDataStatus MgwWebSocketContext::onRequestBody(size_t body_buffer_length,
                                               bool /* end_of_stream */) {
  auto body = getBufferBytes(WasmBufferType::HttpRequestBody, 0, body_buffer_length);
  LOG_TRACE(std::string("onRequestBody called mgw_WASM_websocket") + std::string(body->view()));
  auto data = body->view();
  //int frame_opcode = data[0] & 0x0F;
  // std::string opcode_str = std::to_string(frame_opcode);
  // LOG_INFO(opcode_str);
  
  if(isDataFrame(data)){
    // Get remoteIP of the upstream 
    std::string upstream_address;
    auto buffer = getValue({"upstream", "address"}, &upstream_address);
    
    // Create WebSocketFrameRequest with related fields
    WebSocketFrameRequest request;
    request.set_node_id(this->node_id_);
    request.set_frame_length(body_buffer_length);
    request.set_remote_ip(upstream_address);
    // Read ext_authz_metadata_ metdata saved as a member variable
    *request.mutable_metadata() = *this->metadata_;
    if(this->handler_state_ == HandlerState::OK && this->throttle_state_ == ThrottleState::UnderLimit){
      LOG_INFO(std::string("gRPC bidi stream available. publishing frame data..."));
      auto ack = this->stream_handler_->send(request, false);
      if (ack != WasmResult::Ok) {
        LOG_INFO(std::string("error sending frame data")+ toString(ack));
      }
      LOG_INFO(std::string("frame data successfully sent:"+ toString(ack)));
    }

    // Decide whether to pass the frame or block the frame
    if(this->throttle_state_ == ThrottleState::UnderLimit || this->throttle_state_ == ThrottleState::FailureModeAllowed){
      return FilterDataStatus::Continue;
    }else{
      HeaderStringPairs empty;
      sendLocalResponse(429,"Resource exhausted", "Throttling limit reached", empty, GrpcStatus::InvalidCode);
      return FilterDataStatus::StopIterationNoBuffer;
    }
  }else{
    LOG_TRACE("proxying web socket control frame");
    return FilterDataStatus::Continue;
  }
  
  
  // std::string remote_ip;
  // auto buffer5 = getValue({"upstream", "address"}, &remote_ip);
  // LOG_INFO("remote_ip >>>>>>>>>>>>>>>"+remote_ip);
  // std::string ext_authz_metadata_key = "envoy.filters.http.ext_authz";
  // WebSocketFrameRequest request;
  // request.set_node_id(this->node_id_);
  // (*request.mutable_filter_metadata())[ext_authz_metadata_key] = *this->ext_authz_metadata_;
  // if(this->handler_state_ == HandlerState::OK){
  //   LOG_INFO(std::string("stream available sending message"));
  //   auto res = this->stream_handler_->send(request, false);
  //   if (res != WasmResult::Ok) {
  //     LOG_INFO(std::string("error sending gRPC >>>>>>>")+ toString(res));
  //   }
  //   LOG_INFO(std::string("grpc sent:"+ toString(res)));
  // }
  // return FilterDataStatus::Continue;
}

FilterDataStatus MgwWebSocketContext::onResponseBody(size_t body_buffer_length,
                                                bool /* end_of_stream */) {
  //setBuffer(WasmBufferType::HttpResponseBody, 0, 12, "Hello, world");
  auto body = getBufferBytes(WasmBufferType::HttpResponseBody, 0, body_buffer_length);
  LOG_TRACE(std::string("onResponseBody called mgw_WASM_websocket") + std::string(body->view()));
  auto data = body->view();
  //int frame_opcode = data[0] & 0x0F;
  // std::string opcode_str = std::to_string(frame_opcode);
  // LOG_INFO(opcode_str);
  
  if(isDataFrame(data)){
    // Get remoteIP of the upstream 
    std::string upstream_address;
    auto buffer = getValue({"upstream", "address"}, &upstream_address);
    
    // Create WebSocketFrameRequest with related fields
    WebSocketFrameRequest request;
    request.set_node_id(this->node_id_);
    request.set_frame_length(body_buffer_length);
    request.set_remote_ip(upstream_address);
    // Read ext_authz_metadata_ metdata saved as a member variable
    *request.mutable_metadata() = *this->metadata_;
    if(this->handler_state_ == HandlerState::OK && this->throttle_state_ == ThrottleState::UnderLimit){
      LOG_INFO(std::string("gRPC bidi stream available. publishing frame data..."));
      auto ack = this->stream_handler_->send(request, false);
      if (ack != WasmResult::Ok) {
        LOG_INFO(std::string("error sending frame data")+ toString(ack));
      }
      LOG_INFO(std::string("frame data successfully sent:"+ toString(ack)));
    }

    // Decide whether to pass the frame or block the frame
    if(this->throttle_state_ == ThrottleState::UnderLimit || this->throttle_state_ == ThrottleState::FailureModeAllowed){
      return FilterDataStatus::Continue;
    }else{
      HeaderStringPairs empty;
      sendLocalResponse(429,"Resource exhausted", "Throttling limit reached", empty, GrpcStatus::InvalidCode);
      return FilterDataStatus::StopIterationNoBuffer;
    }
  }else{
    LOG_TRACE("proxying web socket control frame");
    return FilterDataStatus::Continue;
  }
  //return FilterDataStatus::Continue;
}

void MgwWebSocketContext::onDone() { LOG_WARN(std::string("onDone " + std::to_string(id()))); }

void MgwWebSocketContext::onLog() { LOG_WARN(std::string("onLog " + std::to_string(id()))); }

void MgwWebSocketContext::onDelete() { 
  LOG_WARN(std::string("onDelete " + std::to_string(id())));
  std::stringstream pointer_address;
  pointer_address << this->stream_handler_;
  LOG_INFO("handler pointer delete >>>"+ pointer_address.str());
  //this->stream_handler_->close();
 }

// void ExampleContext::updateConnectionStatus(bool status){
//   this->is_stream_ = status;
// }

void MgwWebSocketContext::updateFilterState(ResponseStatus status){
  LOG_INFO(std::string("updateFilterState") + std::to_string(static_cast<int>(status)));
  if(status == ResponseStatus::OK){
    this->throttle_state_ = ThrottleState::UnderLimit;
    LOG_INFO("mgw_wasm_websocket filter state changed to UnderLimit");
  }else if(status == ResponseStatus::OverLimit){
    this->throttle_state_ = ThrottleState::OverLimit;
    LOG_INFO("mgw_wasm_websocket filter state changed to OverLimit !!!");
  }else{
    LOG_INFO("Enforcer throttle decision unknown");
  }
}

void MgwWebSocketContext::updateHandlerState(HandlerState state){
    LOG_INFO(std::string("updateHandlerState") + std::to_string(static_cast<int>(state)));
    this->handler_state_ = state;
    if(this->failure_mode_deny_){
      this->throttle_state_ = ThrottleState::FailureModeBlocked;
    }else{
      this->throttle_state_ = ThrottleState::FailureModeAllowed;
    }
}

bool MgwWebSocketContext::isDataFrame(const std::string_view data){
  int frame_opcode = data[0] & 0x0F;
  if(frame_opcode >= 0 && frame_opcode <= 7){
    return true;
  }else{
    return false;
  }
}