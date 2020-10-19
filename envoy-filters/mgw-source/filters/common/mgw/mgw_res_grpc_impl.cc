#include "mgw-source/filters/common/mgw/mgw_res_grpc_impl.h"

#include "envoy/config/core/v3/base.pb.h"
#include "mgw-api/services/response/v3/mgw_res.pb.h"

#include "common/common/assert.h"
#include "common/grpc/async_client_impl.h"
#include "common/http/headers.h"
#include "common/http/utility.h"
#include "common/network/utility.h"
#include "common/protobuf/protobuf.h"

namespace Envoy {
namespace Extensions {
namespace Filters {
namespace Common {
namespace MGW {

// selecting service path.
constexpr char V2[] = "envoy.service.mgw_res.v3.MGWResponse.Intercept";

GrpcResClientImpl::GrpcResClientImpl(Grpc::RawAsyncClientPtr&& async_client,
                               const absl::optional<std::chrono::milliseconds>& timeout)
    : service_method_(getMethodDescriptor()), async_client_(std::move(async_client)),
      timeout_(timeout) {}

GrpcResClientImpl::~GrpcResClientImpl() { ASSERT(!callbacks_); }

void GrpcResClientImpl::cancel() {
  ASSERT(callbacks_ != nullptr);
  request_->cancel();
  callbacks_ = nullptr;
}

void GrpcResClientImpl::intercept(ResponseCallbacks& callbacks,
                                  const envoy::service::mgw_res::v3::CheckRequest& request,
                                  Tracing::Span& parent_span, const StreamInfo::StreamInfo&) {
  ASSERT(callbacks_ == nullptr);
  callbacks_ = &callbacks;
  request_ = async_client_->send(service_method_, request, *this, parent_span,
                                 Http::AsyncClient::RequestOptions().setTimeout(timeout_));
}

void GrpcResClientImpl::onSuccess(
    std::unique_ptr<envoy::service::mgw_res::v3::CheckResponse>&& response, Tracing::Span& span) {
  ResponsePtr mgw_response = std::make_unique<Response>(Response{});
  if (response->status().code() == Grpc::Status::WellKnownGrpcStatus::Ok) {
    span.setTag(TracingConstants::get().TraceStatus, TracingConstants::get().TraceOk);
    mgw_response->status = CheckStatus::OK;
  } else {
    span.setTag(TracingConstants::get().TraceStatus, TracingConstants::get().TraceUnauthz);
    mgw_response->status = CheckStatus::Denied;
  }

  callbacks_->onResponseComplete(std::move(mgw_response));
  callbacks_ = nullptr;
}

void GrpcResClientImpl::onFailure(Grpc::Status::GrpcStatus status, const std::string&,
                               Tracing::Span&) {
  ASSERT(status != Grpc::Status::WellKnownGrpcStatus::Ok);
  Response response{};
  response.status = CheckStatus::Error;
  response.status_code = Http::Code::Forbidden;
  callbacks_->onResponseComplete(std::make_unique<Response>(response));
  callbacks_ = nullptr;
}

// TODO(amalimatharaarachchi) change the response to be accordingly to intercept service. This is same as the old authz success way for request path
void GrpcResClientImpl::toAuthzResponseHeader(
    ResponsePtr& response,
    const Protobuf::RepeatedPtrField<envoy::config::core::v3::HeaderValueOption>& headers) {
  for (const auto& header : headers) {
    if (header.append().value()) {
      response->headers_to_append.emplace_back(Http::LowerCaseString(header.header().key()),
                                               header.header().value());
    } else {
      response->headers_to_add.emplace_back(Http::LowerCaseString(header.header().key()),
                                            header.header().value());
    }
  }
}

const Protobuf::MethodDescriptor& GrpcResClientImpl::getMethodDescriptor() {
  const auto* descriptor = Protobuf::DescriptorPool::generated_pool()->FindMethodByName(V2);
  ASSERT(descriptor != nullptr);
  return *descriptor;
}

} // namespace MGW
} // namespace Common
} // namespace Filters
} // namespace Extensions
} // namespace Envoy
