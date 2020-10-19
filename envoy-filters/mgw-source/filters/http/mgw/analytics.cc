#include "mgw-source/filters/http/mgw/analytics.h"

#include "envoy/config/core/v3/base.pb.h"

#include "common/common/assert.h"
#include "common/common/enum_to_int.h"
#include "common/http/utility.h"
#include "common/router/config_impl.h"

#include "extensions/filters/http/well_known_names.h"
// #include "mgw-source/filters/common/mgw/check_response_utils.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {


// Http::StreamFilterBase
void Filter::onDestroy() {
  ENVOY_STREAM_LOG(trace, "[SIGH] filter destroyed", *res_callbacks_);
  if (res_state_ == State::Calling) {
    res_state_ = State::Complete;
    res_client_->cancel();
  }
}

// Http::StreamEncoderFilter
Http::FilterHeadersStatus Filter::encode100ContinueHeaders(Http::ResponseHeaderMap&) {
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterHeadersStatus Filter::encodeHeaders(Http::ResponseHeaderMap& , bool) {
  Router::RouteConstSharedPtr route = res_callbacks_->route();

  // Initiate a call to the authorization server since we are not disabled.
  initiateResponseInterceptCall();

  return response_filter_return_ == ResponseFilterReturn::StopEncoding
             ? Http::FilterHeadersStatus::StopAllIterationAndWatermark
             : Http::FilterHeadersStatus::Continue;
}

Http::FilterDataStatus Filter::encodeData(Buffer::Instance& , bool ) {
  ENVOY_LOG(info, "[woohoo] inside encode data");
  return Http::FilterDataStatus::Continue;
}

Http::FilterTrailersStatus Filter::encodeTrailers(Http::ResponseTrailerMap&) {
  return Http::FilterTrailersStatus::Continue;
}

Http::FilterMetadataStatus Filter::encodeMetadata(Http::MetadataMap&) {
  return Http::FilterMetadataStatus::Continue;
}

void Filter::setEncoderFilterCallbacks(Http::StreamEncoderFilterCallbacks& callbacks) {
  res_callbacks_ = &callbacks;
}

// MGW::ResponseCallbacks
void Filter::onResponseComplete(Filters::Common::MGW::ResponsePtr&& response) {
  res_state_ = State::Complete;
  using Filters::Common::MGW::CheckStatus;
  // Stats::StatName empty_stat_name;
  switch (response->status) {
  case CheckStatus::OK: {
    ENVOY_STREAM_LOG(trace, "mgw analytics filter successfully sent data to filter chain", *res_callbacks_);
    break;
  }

  case CheckStatus::Error: {
    // ENVOY_STREAM_LOG(trace,
    //                   "mgw filter rejected the request with an error. Response status code: {}",
    //                   *res_callbacks_, enumToInt(res_config_->statusOnError()));
    // TODO(amalimatharaarachchi)
    // Gracefully handle error response res_callbacks_->streamInfo().setResponseFlag(
    //     StreamInfo::ResponseFlag::UnauthorizedExternalService);
    // res_callbacks_->sendLocalReply(req_config_->statusOnError(), EMPTY_STRING, nullptr,
    // absl::nullopt,
    //                                RcDetails::get().AuthzError);
    break;
  }
  default:
    NOT_REACHED_GCOVR_EXCL_LINE;
    break;
  }
  continueEncoding();
}

void Filter::initiateResponseInterceptCall() {
  response_filter_return_ = ResponseFilterReturn::StopEncoding;
  Router::RouteConstSharedPtr route = res_callbacks_->route();

  ENVOY_STREAM_LOG(trace, "mgw filter calling response interceptor server", *res_callbacks_);
  res_state_ = State::Calling;
  res_client_->intercept(*this, res_intercept_request_, res_callbacks_->activeSpan(),
                         res_callbacks_->streamInfo());
}

void Filter::continueEncoding() {
  response_filter_return_ = ResponseFilterReturn::ContinueEncoding;
  if (!initiating_responce_call_) {
    res_callbacks_->continueEncoding();
  }
}

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
