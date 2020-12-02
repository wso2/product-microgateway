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

Filter::Filter() {}

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
  set_body_ = readMetadata(&req_callbacks_->streamInfo().dynamicMetadata());

  if (!end_stream && set_body_) {
    return Http::FilterHeadersStatus::StopIteration;
  }
  if (set_body_) {
    setPayload(modified_body_, req_callbacks_->decodingBuffer(), req_headers_);
  }
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterDataStatus Filter::decodeData(Buffer::Instance& data, bool end_stream) {

  ENVOY_LOG(trace, "decodeData with data = {} , end_stream = {}", data.toString(), end_stream);

  if (!end_stream && set_body_) {
    return Http::FilterDataStatus::StopIterationAndBuffer;
  }

  if (set_body_) {
    setPayload(modified_body_, req_callbacks_->decodingBuffer(), req_headers_);
  }
  return Http::FilterDataStatus::Continue;
}

void Filter::setPayload(std::string new_payload, const Buffer::Instance* decoding_buffer,
                     Http::RequestHeaderMap* req_headers) {
  ENVOY_LOG(debug, "Setting payload ...");
  Buffer::OwnedImpl modified_body(new_payload);

  if (decoding_buffer == nullptr) {
    req_callbacks_->addDecodedData(modified_body, false);
  } else {
    req_callbacks_->modifyDecodingBuffer([&modified_body](Buffer::Instance& data) {
      data.drain(data.length());
      data.move(modified_body);
    });
  }
  // set new content length
  decoding_buffer = req_callbacks_->decodingBuffer();
  req_headers->setContentLength(decoding_buffer->length());
  ENVOY_LOG(debug, "Payload successfully modified");
}

bool Filter::readMetadata(const envoy::config::core::v3::Metadata* metadata) {
  std::string jsonJWTPayload;
  std::string PayloadMetadataKey = "payload";
  const auto* payload = &Config::Metadata::metadataValue(
      metadata, HttpFilterNames::get().ExtAuthorization, PayloadMetadataKey);

  if (payload != nullptr && (payload->kind_case() == ProtobufWkt::Value::kStringValue)) {
    modified_body_ = payload->string_value();
    ENVOY_LOG(debug, "Retrieved new payload from metadata successfully");
    return true;
  }
  return false;
}

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
