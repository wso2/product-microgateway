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

// Protobuf defined package and method name.
static constexpr char EnforcerServiceName[] = "envoy.extensions.filters.http.mgw_wasm_websocket.v3.WebSocketFrameService";
static constexpr char PublishFrameData[] = "PublishFrameData";

using google::protobuf::util::JsonParseOptions;
using google::protobuf::util::error::Code;
using google::protobuf::util::Status;

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest_MessageDirection_HANDSHAKE;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest_MessageDirection_PUBLISH;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest_MessageDirection_SUBSCRIBE;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameResponse;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Metadata;

// mgw_WASM_websocket_root is the root_id for the filter. Root ID is unique ID for a set of filters/services in a VM which will 
// share a RootContext and Contexts if applicable. If two or more filters have a common base, then we can assign the same root_id
// and share the RootContext. Otherwise it is recommended to have separate RootContexts for separate filters. 'mgw_WASM_websocket_root'
// should be same as the value assigned in the xDS configuration.
static RegisterContextFactory register_MgwWebSocketContext(CONTEXT_FACTORY(MgwWebSocketContext),
                                                      ROOT_FACTORY(MgwWebSocketRootContext),
                                                      "mgw_WASM_websocket_root");
// Called when RootContext gets created
bool MgwWebSocketRootContext::onStart(size_t) {
  LOG_TRACE("onStart RootContext mgw_WASM_websocket");
  return true;
}

// Called once when the VM loads and once when each hook loads and whenever
// configuration changes. Returns false if the configuration is invalid.
bool MgwWebSocketRootContext::onConfigure(size_t config_size) {
  LOG_TRACE("onConfigure RootContext mgw_WASM_websocket");
  const WasmDataPtr configuration = getBufferBytes(WasmBufferType::PluginConfiguration, 0, config_size);
  JsonParseOptions json_options;
  const Status options_status = google::protobuf::util::JsonStringToMessage(
      configuration->toString(),
      &config_, json_options);
  if (options_status != Status::OK) {
    LOG_WARN("Cannot parse plugin configuration JSON string: " + configuration->toString());
    return false;
  }
  LOG_TRACE("Loading Config: " + config_.node_id());
  return true;
}

MgwWebSocketContext::~MgwWebSocketContext(){
  LOG_TRACE(std::string("MgwContext destructed") + this->x_request_id_);
}

// Called when a new HTTP filter is created.
void MgwWebSocketContext::onCreate() { 
  LOG_TRACE(std::string("onCreate " + std::to_string(id())));
  MgwWebSocketRootContext *r = dynamic_cast<MgwWebSocketRootContext*>(root());
  // Read config provided by xDS and initialize member varibales.
  this->node_id_ = r->config_.node_id();
  this->failure_mode_deny_ = r->config_.failure_mode_deny();
  this->stream_handler_ = new MgwGrpcStreamHandler(this);
  // Initialize throttle period to now or 0
  struct timeval now;
  // NULL value is provided since we want UTC. Otherwise we should provide a param for the relevant timezone.
  int rc = gettimeofday(&now, NULL);
  if(rc == 0){
    this->throttle_period_ = now.tv_sec;
  }else{
    LOG_WARN("Throttle period initialization failed. Default set to 0");
    this->throttle_period_ = 0;
  }
}

// Called when the initial HTTP upgrade request intercepted by the filter. gRPC bidirectional service 
// is initiated to the enforcer and ext_authz dynamic metadata are assigned as a member variable for 
// reference in the onRequestBody() and onResponseBody() callbacks when a data frame is intercepted.
FilterHeadersStatus MgwWebSocketContext::onRequestHeaders(uint32_t, bool) {
  LOG_TRACE(std::string("onRequestHeaders called mgw_WASM_websocket") + std::to_string(id()));

  auto requestHeaderResult = getRequestHeaderPairs();
  auto headerPairs = requestHeaderResult->pairs();
  for (auto& p : headerPairs) {
    if (std::string(p.first) == X_REQUEST_ID) {
      this->x_request_id_ = std::string(p.second);
    }
  }
  // Extract ext_authz dynamic metadata and assign it to a member variable 
  auto buffer = getProperty<std::string>({"metadata", "filter_metadata", "envoy.filters.http.ext_authz"});
  if (buffer.has_value() && buffer.value()->size() != 0) {
    auto pairs = buffer.value()->pairs();
    for (auto &p : pairs) {
      if (std::string(p.first) == "isThrottled" && std::string(p.second) == "true") {
        LOG_TRACE(std::string("Initial throttle state is overlimit for the request : ") + this->x_request_id_);
        this->throttle_state_ = ThrottleState::OverLimit;
      } else if (std::string(p.first) == INITIAL_APIM_ERROR_CODE) {
        int errorCode;
        sscanf(std::string(p.second).c_str(), "%d", &errorCode);
        this->apim_error_code_ = errorCode;
        LOG_TRACE(std::string("Initial APIM Error code is ")  + std::string(p.second) + std::string(" for the request : ") + this->x_request_id_);
      } else if (std::string(p.first) == THROTTLE_CONDITION_EXPIRE_TIMESTAMP) {
        int timestamp;
        sscanf(std::string(p.second).c_str(), "%d", &timestamp);
        this->throttle_period_ = timestamp;
        LOG_TRACE(std::string("Throttle Period is till ")  + std::string(p.second) + std::string(" for the request : ") + this->x_request_id_);
      } else {
        // The above metadata is only required for determining throttling state in the start. Hence they are not
        // required to stored in metadata separately. Everything else will be stored under metadata.
        (*this->metadata_->mutable_ext_authz_metadata())[std::string(p.first)] = std::string(p.second);
        LOG_TRACE(std::string(p.first) + std::string(" -> ") + std::string(p.second) +
            std::string(" dynamic metadata for the request : ") + this->x_request_id_);
      }
    }
  }
  // Create a new gRPC bidirectional stream.
  establishNewStream();
  LOG_TRACE(std::string("onRequestHeaders is complete for  mgw_WASM_websocket ") + std::to_string(id()) + std::string(" : ") + this->x_request_id_);                    
  return FilterHeadersStatus::Continue;
}

FilterHeadersStatus MgwWebSocketContext::onResponseHeaders(uint32_t, bool) {
  LOG_TRACE(std::string("onResponseHeaders called mgw_WASM_websocket ") + std::to_string(id()) + std::string(" : ") + this->x_request_id_ );
  auto result = getResponseHeaderPairs();
  auto pairs = result->pairs();
  for (auto& p : pairs) {
    if (std::string(p.first) == STATUS_HEADER && std::string(p.second) == STATUS_101) {
      std::string upstream_address;
      auto buffer = getValue({"upstream", "address"}, &upstream_address);
      WebSocketFrameRequest request;
      request.set_node_id(this->node_id_);
      request.set_frame_length(0);
      request.set_remote_ip(upstream_address);
      // Read ext_authz_metadata_ metdata saved as a member variable
      *request.mutable_metadata() = *this->metadata_;
      request.set_payload("");
      request.set_direction(WebSocketFrameRequest_MessageDirection_HANDSHAKE);
      request.set_apim_error_code(0);
      sendEnforcerRequest(this, request);
    }
  }
  LOG_TRACE(std::string("onResponseHeaders complete for mgw_WASM_websocket ") + std::to_string(id()) + std::string(" : ") + this->x_request_id_ );
  return FilterHeadersStatus::Continue;
}

// Called when a web socket frame from downstream is intercepted by the filter.
// Publish frame data through the gRPC bidi stream opened in the onRequestHeaders method.
// Handles all the throttling logic.
FilterDataStatus MgwWebSocketContext::onRequestBody(size_t body_buffer_length,
                                               bool /* end_of_stream */) {
  auto body = getBufferBytes(WasmBufferType::HttpRequestBody, 0, body_buffer_length);
  LOG_TRACE(std::string("onRequestBody called mgw_WASM_websocket ") + std::string(body->view()) + std::string(" : ") + this->x_request_id_ );
  auto data = body->view();
  
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
    request.set_payload(std::string(body->view()));
    request.set_direction(WebSocketFrameRequest_MessageDirection_PUBLISH);
    
    // Perform throttling logic.
    // If the throttle state is underlimit and if the gRPC stream is open, send WebSocketFrameRequest. 
    // If no gRPC stream, try to open a new stream and then send. 
    if(this->throttle_state_ == ThrottleState::UnderLimit){
      request.set_apim_error_code(0);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::Continue;
    // If throttle state is FailureModeAllowed, then try to esatblish a new gRPC stream and 
    // pass the request to next filter. This state switch happens when the filter-enforcer connection fails.
    }else if (this->throttle_state_ == ThrottleState::FailureModeAllowed){
      request.set_apim_error_code(0);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::Continue;
    // If throttle state is FailureModeBlocked, then try to establish a new gRPC stream and 
    // stop interation. This state switch happens when the filter-enforcer connection fails.
    }else if(this->throttle_state_ == ThrottleState::FailureModeBlocked){
      request.set_apim_error_code(ENFORCER_NOT_REACHABLE_ERROR_CODE);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::StopIterationNoBuffer;
    // If throttle state is overlimit, then check the throttle period before making a decision. 
    // If the current time has passed the throttle period in UTC seconds, then continue to the 
    // next filter and change the status to UnderLimit. If the current time is in the throttle period
    // then stop iteration. If we can't get the current time due to some error , then the default is to 
    // stop iteration.
    }else{
      struct timeval now;
      int rc = gettimeofday(&now, NULL);
      if(rc == 0){
        if(this->throttle_period_ <= now.tv_sec){
          this->throttle_state_ = ThrottleState::UnderLimit;
          // publish to enforcer
          request.set_apim_error_code(0);
          sendEnforcerRequest(this, request);
          return FilterDataStatus::Continue;
        }else{
          request.set_apim_error_code(this->apim_error_code_);
          sendEnforcerRequest(this, request);
          return FilterDataStatus::StopIterationNoBuffer;
        }
      }else{
        // It is unlikely that the return value would be zero https://man7.org/linux/man-pages/man2/gettimeofday.2.html
        LOG_ERROR("Current Time cannot be processed. Hence the websocket stream is closed." + std::string(" : ") + this->x_request_id_);
        return FilterDataStatus::StopIterationNoBuffer;
      }
    }
  }else{
    LOG_TRACE("proxying web socket control frame");
    return FilterDataStatus::Continue;
  }
}

// Called when a web socket frame from upstream is intercepted by the filter.
// Publish frame data through the gRPC bidi stream opened in the onRequestHeaders method.
// Handles all the throttling logic.
FilterDataStatus MgwWebSocketContext::onResponseBody(size_t body_buffer_length,
                                                bool /* end_of_stream */) {
  auto body = getBufferBytes(WasmBufferType::HttpResponseBody, 0, body_buffer_length);
  LOG_TRACE(std::string("onResponseBody called mgw_WASM_websocket") + std::string(body->view()) + std::string(" : ") + this->x_request_id_);
  auto data = body->view();
  
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
    request.set_payload(std::string(body->view()));
    request.set_direction(WebSocketFrameRequest_MessageDirection_SUBSCRIBE);

    // Perform throttling logic.
    // If the throttle state is underlimit and if the gRPC stream is open, send WebSocketFrameRequest. 
    // If no gRPC stream, try to open a new stream and then send. 
    if(this->throttle_state_ == ThrottleState::UnderLimit){
      request.set_apim_error_code(0);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::Continue;
    // If throttle state is FailureModeAllowed, then try to esatblish a new gRPC stream and 
    // pass the request to next filter.
    }else if (this->throttle_state_ == ThrottleState::FailureModeAllowed){
      request.set_apim_error_code(0);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::Continue;
    // If throttle state is FailureModeBlocked, then try to establish a new gRPC stream and 
    // stop interation.
    }else if(this->throttle_state_ == ThrottleState::FailureModeBlocked){
      request.set_apim_error_code(ENFORCER_NOT_REACHABLE_ERROR_CODE);
      sendEnforcerRequest(this, request);
      return FilterDataStatus::StopIterationNoBuffer;
    // If throttle state is overlimit, then check the throttle period before making a decision. 
    // If the current time has passed the throttle period in UTC seconds, then continue to the 
    // next filter and change the status to UnderLimit. If the current time is in the throttle period
    // then stop iteration. If we can't get the current time due to some error , then the default is to 
    // stop iteration.
    }else{
      struct timeval now;
      int rc = gettimeofday(&now, NULL);
      if(rc == 0){
        // If throttle_period is less than current time, then the condition is expired.
        if(this->throttle_period_ <= now.tv_sec){
          this->throttle_state_ = ThrottleState::UnderLimit;
          // publish to enforcer
          request.set_apim_error_code(0);
          sendEnforcerRequest(this, request);
          return FilterDataStatus::Continue;
        }else{
          request.set_apim_error_code(this->apim_error_code_);
          sendEnforcerRequest(this, request);
          return FilterDataStatus::StopIterationNoBuffer;
        }
      }else{
        // It is unlikely that the return value would be zero https://man7.org/linux/man-pages/man2/gettimeofday.2.html
        LOG_ERROR(std::string("Current Time cannot be processed. Hence the websocket stream is closed. : ") +
            this->x_request_id_);
        return FilterDataStatus::StopIterationNoBuffer;
        return FilterDataStatus::StopIterationNoBuffer;
      }
    }
  }else{
    LOG_TRACE("proxying web socket control frame");
    return FilterDataStatus::Continue;
  }
}

void MgwWebSocketContext::onDone() { LOG_TRACE(std::string("onDone " + std::to_string(id())) + std::string(" : ") + this->x_request_id_); }

void MgwWebSocketContext::onLog() { LOG_TRACE(std::string("onLog " + std::to_string(id())) + std::string(" : ") + this->x_request_id_); }

void MgwWebSocketContext::onDelete() {
  LOG_TRACE(std::string("onDelete " + std::to_string(id())));
  this->stream_handler_->close();
  this->stream_handler_->reset();
 }

// Callback used by the handler to pass the throttle response received by the gRPC stream.
void MgwWebSocketContext::updateFilterState(ResponseStatus status){
  LOG_TRACE(std::string("updateFilterState") + std::to_string(static_cast<int>(status)) + std::string(" : ") + this->x_request_id_);
  if(status == ResponseStatus::OK){
    this->throttle_state_ = ThrottleState::UnderLimit;
    LOG_TRACE("mgw_wasm_websocket filter state changed to UnderLimit" + std::string(" : ") + this->x_request_id_);
  }else if(status == ResponseStatus::OverLimit){
    this->throttle_state_ = ThrottleState::OverLimit;
    LOG_TRACE("mgw_wasm_websocket filter state changed to OverLimit !!!" + std::string(" : ") + this->x_request_id_);
  }else{
    LOG_TRACE("Enforcer throttle decision unknown" + std::string(" : ") + this->x_request_id_);
  }
}

void MgwWebSocketContext::updateAPIMErrorCode(int code) {
  this->apim_error_code_ = code;
}

// Callback used by the handler to update the handler state reference in the filter.
void MgwWebSocketContext::updateHandlerState(HandlerState state){
    LOG_TRACE(std::string("updateHandlerState ") + std::to_string(static_cast<int>(state)) + std::string(" : ")
        + this->x_request_id_);
    this->handler_state_ = state;
    if(this->failure_mode_deny_){
      this->throttle_state_ = ThrottleState::FailureModeBlocked;
    }else{
      this->throttle_state_ = ThrottleState::FailureModeAllowed;
    }
}

// Check for data frames by reading the opcode and validate frame length of min 3.
bool MgwWebSocketContext::isDataFrame(const std::string_view data){
  int frame_opcode = data[0] & 0x0F;
  if(!(frame_opcode >= 8 && frame_opcode <= 15) && data.length() >= 3){
    return true;
  }else{
    return false;
  }
}

// Establish a new gRPC stream.
void MgwWebSocketContext::establishNewStream() {
  LOG_TRACE(std::string("establish new stream called. : ") + this->x_request_id_);
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
    // TODO : check throttle_period and decide
    if(this->throttle_state_ == ThrottleState::FailureModeBlocked || this->throttle_state_ == ThrottleState::FailureModeAllowed){
      this->throttle_state_ = ThrottleState::UnderLimit;
    }
    LOG_TRACE(std::string("gRPC bidi stream created successfully"));     
  }
}

// Callback used by the handler to update throttle period.
void MgwWebSocketContext::updateThrottlePeriod(const int throttle_period){
  this->throttle_period_ = throttle_period;
  LOG_TRACE("Throttle period updated to"+ std::to_string(throttle_period) + std::string(" : ") + this->x_request_id_);
}

void MgwWebSocketContext::sendEnforcerRequest(MgwWebSocketContext* websocContext, WebSocketFrameRequest request) {
  if (websocContext->handler_state_ == HandlerState::OK) {
    LOG_TRACE(std::string("gRPC bidi stream available. publishing frame data...") + std::string(" : ") + this->x_request_id_);
  } else {
    establishNewStream();
  }
  auto ack = websocContext->stream_handler_->send(request, false);
  if (ack != WasmResult::Ok) {
    LOG_WARN(std::string("error sending frame data")+ toString(ack) + std::string(" : ") + this->x_request_id_);
  } else {
    LOG_TRACE(std::string("frame data successfully sent:"+ toString(ack)) + std::string(" : ") + this->x_request_id_);
  }
}
