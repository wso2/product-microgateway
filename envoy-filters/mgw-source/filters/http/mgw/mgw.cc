#include "mgw-source/filters/http/mgw/mgw.h"

#include "envoy/config/core/v3/base.pb.h"

#include "common/common/assert.h"
#include "common/common/enum_to_int.h"
#include "common/buffer/buffer_impl.h"
#include "common/http/utility.h"
#include "common/router/config_impl.h"

#include "extensions/filters/http/well_known_names.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

// TODO(amalimatharaarachchi) change hardcoded request body value
Filter::Filter() : modified_body_("hello") {}

// Http::StreamFilterBase
void Filter::onDestroy() { ENVOY_LOG(debug, "filter destroyed"); }

// Http::StreamEncoderFilter
Http::FilterHeadersStatus Filter::encode100ContinueHeaders(Http::ResponseHeaderMap&) {
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterHeadersStatus Filter::encodeHeaders(Http::ResponseHeaderMap&, bool) {
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterDataStatus Filter::encodeData(Buffer::Instance&, bool) {
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

// Http::StreamDecoderFilter
Http::FilterTrailersStatus Filter::decodeTrailers(Http::RequestTrailerMap&) {
  return Http::FilterTrailersStatus::Continue;
}

void Filter::setDecoderFilterCallbacks(Http::StreamDecoderFilterCallbacks& callbacks) {
  req_callbacks_ = &callbacks;
}

///////////////////////////////

Http::FilterHeadersStatus Filter::decodeHeaders(Http::RequestHeaderMap& headers, bool end_stream) {
  ENVOY_LOG(trace, "decodeHeaders with end_stream = {}", end_stream);
  req_headers_ = &headers;
  // new_body = "ohyeah";
  if (!end_stream) {
    return Http::FilterHeadersStatus::StopIteration;
  }

  setBody();
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterDataStatus Filter::decodeData(Buffer::Instance& data, bool end_stream) {

  ENVOY_LOG(trace, "decodeData with data = {} , end_stream = {}", data.toString(), end_stream);

  if (!end_stream) {
    return Http::FilterDataStatus::StopIterationAndBuffer;
  }

  setBody();
  return Http::FilterDataStatus::Continue;
}

void Filter::setBody() {
  std::string modified_body = modified_body_;
  Buffer::OwnedImpl body(modified_body);
  const Buffer::Instance* decoding_buffer = req_callbacks_->decodingBuffer();

  if (decoding_buffer == nullptr) {
    const bool streaming_filter = false;
    req_callbacks_->addDecodedData(body, streaming_filter);
  } else {
    req_callbacks_->modifyDecodingBuffer([&body](Buffer::Instance& data) {
      data.drain(data.length());
      data.move(body);
    });
  }
  // set new content length
  decoding_buffer = req_callbacks_->decodingBuffer();
  req_headers_->setContentLength(decoding_buffer->length());
}

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
