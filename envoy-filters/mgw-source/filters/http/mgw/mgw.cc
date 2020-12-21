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
  ENVOY_LOG(trace, "decodeTrailers");
  // make sure to metadata were read at least once per request
  if (!read_metadata_) {
    // read metadata and check we need to modify the request body
    set_body_ = readMetadata(&req_callbacks_->streamInfo().dynamicMetadata());
  }
  // if we do not modify the body we continue to other filters
  if (!set_body_) {
     return Http::FilterTrailersStatus::Continue;
  }
  // once the stream is complete, we modify the payload.
  setPayload(modified_body_, req_callbacks_->decodingBuffer(), req_headers_);

  return Http::FilterTrailersStatus::Continue;
}

void Filter::setDecoderFilterCallbacks(Http::StreamDecoderFilterCallbacks& callbacks) {
  req_callbacks_ = &callbacks;
}

Http::FilterHeadersStatus Filter::decodeHeaders(Http::RequestHeaderMap& headers, bool end_stream) {
  ENVOY_LOG(trace, "decodeHeaders with end_stream = {}", end_stream);
  req_headers_ = &headers;
  if (!read_metadata_) {
    // read metadata and check we need to modify the request body
    set_body_ = readMetadata(&req_callbacks_->streamInfo().dynamicMetadata());
  }
  // if we do not modify the body we continue to other filters
  if (!set_body_) {
    return Http::FilterHeadersStatus::Continue;
  }

  if (!end_stream) {
    return Http::FilterHeadersStatus::StopIteration;
  }
  // once the stream is complete (end_stream == true), we modify the payload.
  setPayload(modified_body_, req_callbacks_->decodingBuffer(), req_headers_);
  return Http::FilterHeadersStatus::Continue;
}

Http::FilterDataStatus Filter::decodeData(Buffer::Instance& data, bool end_stream) {
  ENVOY_LOG(trace, "decodeData with end_stream = {}", end_stream);
  // if we do not modify the body we continue to other filters
  if (!set_body_) {
    return Http::FilterDataStatus::Continue;
  }

  // no need to keep buffer data since we are going to modify anyway.
  data.drain(data.length());
  // if stream is not ended, stop continuing to other filters and buffer.
  if (!end_stream) {
    return Http::FilterDataStatus::StopIterationAndBuffer;
  }
  // once the stream is complete (end_stream == true), we modify the payload.
  setPayload(modified_body_, req_callbacks_->decodingBuffer(), req_headers_);
  return Http::FilterDataStatus::Continue;
}

void Filter::setPayload(std::string new_payload, const Buffer::Instance* decoding_buffer,
                        Http::RequestHeaderMap* req_headers) {
  ENVOY_LOG(debug, "Modifying payload ...");
  // create a new buffer instance with new payload data.
  Buffer::OwnedImpl modified_body(new_payload);

  // if we never got a payload from the downstream/ header only request.
  if (decoding_buffer == nullptr) {
    // we are not streaming back this as it is already in memory. so we set streaming_filter = false.
    req_callbacks_->addDecodedData(modified_body, false);
  } else {
    // since we modifying the existing payload, we drain the data buffer and add new data.
    req_callbacks_->modifyDecodingBuffer([&modified_body](Buffer::Instance& data) {
      data.drain(data.length());
      data.move(modified_body);
    });
  }
  // since we modified the payload, need to set new content length.
  decoding_buffer = req_callbacks_->decodingBuffer();
  req_headers->setContentLength(decoding_buffer->length());
  ENVOY_LOG(debug, "payload successfully modified");
}

bool Filter::readMetadata(const envoy::config::core::v3::Metadata* metadata) {
  // we read metadata under ext_authz filter and if find a key "payload",
  // then decide to modify the payload.
  ENVOY_LOG(debug, "Reading metadata ...");
  // this will be used to check if the metadata was read once per request. 
  read_metadata_ = true;
  const std::string PayloadMetadataKey = "payload";
  // reading metadata under external authorization
  const auto* payload = &Config::Metadata::metadataValue(
      metadata, HttpFilterNames::get().ExtAuthorization, PayloadMetadataKey);

  if (payload != nullptr && (payload->kind_case() == ProtobufWkt::Value::kStringValue)) {
    modified_body_ = payload->string_value();
    ENVOY_LOG(debug, "retrieved new payload from metadata successfully");
    return true;
  }
  return false;
}

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
