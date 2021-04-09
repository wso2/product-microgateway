// NOLINT(namespace-envoy)
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

#include "handler_impl.h"

using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::WebSocketFrameRequest;
using envoy::extensions::filters::http::mgw_wasm_websocket::v3::Config;


MgwGrpcStreamHandler::MgwGrpcStreamHandler(HandlerCallbacks *callbacks){
    callbacks_ = callbacks;
}

MgwGrpcStreamHandler::~MgwGrpcStreamHandler(){
  LOG_INFO("XXXXXXXXXXXXXXXXXXX Handler destructed");
}

void MgwGrpcStreamHandler::onReceive(size_t body_size){
    LOG_INFO("gRPC streaming onReceive");
    //this->callbacks_->setEffectiveContext();
    this->callbacks_->updateFilterState(ResponseStatus::OK);
};

void MgwGrpcStreamHandler::onRemoteClose(GrpcStatus status){
    LOG_INFO(std::string("gRPC streaming onRemoteClose") + std::to_string(static_cast<int>(status)));
    //this->*context_->updateConnectionStatus(false);
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
