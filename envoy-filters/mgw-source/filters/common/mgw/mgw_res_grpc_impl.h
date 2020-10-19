#pragma once

#include <chrono>
#include <cstdint>
#include <string>
#include <vector>

#include "envoy/config/core/v3/base.pb.h"
#include "envoy/grpc/async_client.h"
#include "envoy/grpc/async_client_manager.h"
#include "envoy/http/filter.h"
#include "envoy/http/header_map.h"
#include "envoy/http/protocol.h"
#include "envoy/network/address.h"
#include "envoy/network/connection.h"
#include "envoy/network/filter.h"
#include "mgw-api/services/response/v3/mgw_res.pb.h"
#include "envoy/tracing/http_tracer.h"
#include "envoy/upstream/cluster_manager.h"

#include "common/grpc/typed_async_client.h"

#include "mgw-source/filters/common/mgw/mgw.h"

namespace Envoy {
namespace Extensions {
namespace Filters {
namespace Common {
namespace MGW {

using MGWAsyncResCallbacks = Grpc::AsyncRequestCallbacks<envoy::service::mgw_res::v3::CheckResponse>;

/*
 * This client implementation is used when the mgw filter needs to communicate with an gRPC
 * mgw response filter server. Unlike the HTTP client, the gRPC allows the server to define response
 * objects which contain the HTTP attributes to be sent to the the downstream client.
 * The gRPC client does not rewrite path. NOTE: We create gRPC client for each filter stack instead
 * of a client per thread. That is ok since this is unary RPC and the cost of doing this is minimal.
 */
class GrpcResClientImpl : public ResClient, public MGWAsyncResCallbacks {
public:
  GrpcResClientImpl(Grpc::RawAsyncClientPtr&& async_client,
                 const absl::optional<std::chrono::milliseconds>& timeout);
  ~GrpcResClientImpl() override;

  // MGW::Client
  void cancel() override;
  void intercept(ResponseCallbacks& callbacks,
                 const envoy::service::mgw_res::v3::CheckRequest& request,
                 Tracing::Span& parent_span, const StreamInfo::StreamInfo& stream_info) override;

  // Grpc::AsyncRequestCallbacks
  void onCreateInitialMetadata(Http::RequestHeaderMap&) override {}
  void onSuccess(std::unique_ptr<envoy::service::mgw_res::v3::CheckResponse>&& response,
                 Tracing::Span& span) override;
  void onFailure(Grpc::Status::GrpcStatus status, const std::string& message,
                 Tracing::Span& span) override;

private:
  static const Protobuf::MethodDescriptor& getMethodDescriptor();
  void toAuthzResponseHeader(
      ResponsePtr& response,
      const Protobuf::RepeatedPtrField<envoy::config::core::v3::HeaderValueOption>& headers);
  // grpc intercept method descriptor
  const Protobuf::MethodDescriptor& service_method_;
  //grpc async client
  Grpc::AsyncClient<envoy::service::mgw_res::v3::CheckRequest, envoy::service::mgw_res::v3::CheckResponse> async_client_;
  Grpc::AsyncRequest* request_{};

  //timeout for the intercept grpc client service
  absl::optional<std::chrono::milliseconds> timeout_;
  ResponseCallbacks* callbacks_{};
};

} // namespace MGW
} // namespace Common
} // namespace Filters
} // namespace Extensions
} // namespace Envoy
