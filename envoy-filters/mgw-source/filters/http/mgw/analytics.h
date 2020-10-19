#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.h"
#include "envoy/http/filter.h"
#include "envoy/local_info/local_info.h"
#include "envoy/runtime/runtime.h"
#include "mgw-api/services/response/v3/mgw_res.pb.h"
#include "envoy/stats/scope.h"
#include "envoy/stats/stats_macros.h"
#include "envoy/upstream/cluster_manager.h"

#include "common/common/assert.h"
#include "common/common/logger.h"
#include "common/common/matchers.h"
#include "common/http/codes.h"
#include "common/http/header_map_impl.h"
#include "common/runtime/runtime_protos.h"

#include "mgw-source/filters/common/mgw/mgw.h"
#include "mgw-source/filters/http/mgw/filter_config.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

using FilterConfigSharedPtr = std::shared_ptr<FilterConfig>;

/**
 * HTTP mgw filter. Depending on the route configuration, this filter calls the global
 * mgw service before allowing further filter iteration.
 */
class Filter : public Logger::Loggable<Logger::Id::filter>,
               public Http::StreamEncoderFilter,
               public Filters::Common::MGW::ResponseCallbacks {
public:
  Filter(const FilterConfigSharedPtr& res_config, Filters::Common::MGW::ResClientPtr&& res_client)
      : res_config_(res_config), res_client_(std::move(res_client)) {}

  // Http::StreamFilterBase
  void onDestroy() override;

  // Http::StreamEncoderFilter
  Http::FilterHeadersStatus encode100ContinueHeaders(Http::ResponseHeaderMap&) override;
  Http::FilterHeadersStatus encodeHeaders(Http::ResponseHeaderMap&, bool) override;
  Http::FilterDataStatus encodeData(Buffer::Instance&, bool) override;
  Http::FilterTrailersStatus encodeTrailers(Http::ResponseTrailerMap&) override;
  Http::FilterMetadataStatus encodeMetadata(Http::MetadataMap&) override;
  void setEncoderFilterCallbacks(Http::StreamEncoderFilterCallbacks&) override;

  // MGW::ResponseCallbacks
  void onResponseComplete(Filters::Common::MGW::ResponsePtr&&) override;

private:
  void continueEncoding();
  // State of this filter's communication with the external decode/encode service.
  // The filter has either not started calling the external service, in the middle of calling
  // it or has completed.
  enum class State { NotStarted, Calling, Complete };

  ////// response path members
  void initiateResponseInterceptCall();
  // FilterReturn is used to capture what the return code should be to the filter chain.
  // if this filter is either in the middle of calling the service or the result is denied then
  // the filter chain should stop. Otherwise the filter chain can continue to the next filter.
  enum class ResponseFilterReturn { ContinueEncoding, StopEncoding };
  ResponseFilterReturn response_filter_return_{ResponseFilterReturn::ContinueEncoding};
  FilterConfigSharedPtr res_config_;
  Filters::Common::MGW::ResClientPtr res_client_;
  Http::StreamEncoderFilterCallbacks* res_callbacks_{};
  State res_state_{State::NotStarted}; //state of response interceptor service
  // Used to identify if the response callback to onComplete() is synchronous (on the stack) or asynchronous.
  bool initiating_responce_call_{};
  envoy::service::mgw_res::v3::CheckRequest res_intercept_request_{};
};

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
