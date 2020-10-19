#pragma once

#include <chrono>
#include <memory>
#include <string>
#include <vector>

#include "envoy/common/pure.h"
#include "envoy/http/codes.h"
#include "envoy/service/auth/v3/external_auth.pb.h"
#include "mgw-api/services/response/v3/mgw_res.pb.h"
#include "envoy/stream_info/stream_info.h"
#include "envoy/tracing/http_tracer.h"

#include "common/singleton/const_singleton.h"

namespace Envoy {
namespace Extensions {
namespace Filters {
namespace Common {
namespace MGW {

/**
 * Constant values used for tracing metadata.
 */
struct TracingConstantValues {
  const std::string TraceStatus = "mgw_status";
  const std::string TraceUnauthz = "mgw_unauthorized";
  const std::string TraceOk = "mgw_ok";
  const std::string HttpStatus = "mgw_http_status";
};

using TracingConstants = ConstSingleton<TracingConstantValues>;

/**
 * Possible async results for a check call.
 */
enum class CheckStatus {
  // The request is authorized.
  OK,
  // The authz service could not be queried.
  Error,
  // The request is denied.
  Denied
};

/**
 * Authorization response object for a RequestCallback.
 */
struct Response {
  // Call status.
  CheckStatus status;
  // Optional http headers used on either denied or ok responses.
  Http::HeaderVector headers_to_append;
  // Optional http headers used on either denied or ok responses.
  Http::HeaderVector headers_to_add;
  // Optional http body used only on denied response.
  std::string body;
  // Optional http status used only on denied response.
  Http::Code status_code{};
};

using ResponsePtr = std::unique_ptr<Response>;

/**
 * Async callbacks used during response incept() calls.
 */
class ResponseCallbacks {
public:
  virtual ~ResponseCallbacks() = default;

  /**
   * Called when a intercept request is complete. The resulting ResponsePtr is supplied.
   */
  virtual void onResponseComplete(ResponsePtr&& response) PURE;
};

class ResClient {
public:
  // Destructor
  virtual ~ResClient() = default;

  /**
   * Cancel an inflight Intercept request.
   */
  virtual void cancel() PURE;

  /**
   * Request a check call to an external mgw response service which can use the
   * passed request parameters to make a permit/deny decision.
   * @param callback supplies the completion callbacks.
   *        NOTE: The callback may happen within the calling stack.
   * @param request is the proto message with the attributes of the specific payload.
   * @param parent_span source for generating an egress child span as part of the trace.
   * @param res_stream_info supplies the client's stream info.
   */
  virtual void intercept(ResponseCallbacks& callback,
                         const envoy::service::mgw_res::v3::CheckRequest& request,
                         Tracing::Span& parent_span,
                         const StreamInfo::StreamInfo& res_stream_info) PURE;
};

using ResClientPtr = std::unique_ptr<ResClient>;

} // namespace MGW
} // namespace Common
} // namespace Filters
} // namespace Extensions
} // namespace Envoy
