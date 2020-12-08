#pragma once

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.h"
#include "envoy/http/filter.h"
#include "envoy/local_info/local_info.h"
#include "envoy/runtime/runtime.h"
#include "envoy/stats/scope.h"
#include "envoy/stats/stats_macros.h"
#include "envoy/upstream/cluster_manager.h"

#include "common/common/assert.h"
#include "common/common/logger.h"
#include "common/common/matchers.h"
#include "common/http/codes.h"
#include "common/http/header_map_impl.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

/**
 * HTTP mgw filter. Depending on the route configuration, this filter calls the global
 * mgw service before allowing further filter iteration.
 */
class Filter : public Logger::Loggable<Logger::Id::filter>, public Http::StreamFilter {
public:
  Filter();

  // Http::StreamFilterBase
  void onDestroy() override;

  // Http::StreamEncoderFilter
  Http::FilterHeadersStatus encode100ContinueHeaders(Http::ResponseHeaderMap&) override;
  Http::FilterHeadersStatus encodeHeaders(Http::ResponseHeaderMap&, bool) override;
  Http::FilterDataStatus encodeData(Buffer::Instance&, bool) override;
  Http::FilterTrailersStatus encodeTrailers(Http::ResponseTrailerMap&) override;
  Http::FilterMetadataStatus encodeMetadata(Http::MetadataMap&) override;
  void setEncoderFilterCallbacks(Http::StreamEncoderFilterCallbacks&) override;

  // Http::StreamDecoderFilter
  Http::FilterHeadersStatus decodeHeaders(Http::RequestHeaderMap& headers,
                                          bool end_stream) override;
  Http::FilterDataStatus decodeData(Buffer::Instance& data, bool end_stream) override;
  Http::FilterTrailersStatus decodeTrailers(Http::RequestTrailerMap& trailers) override;
  void setDecoderFilterCallbacks(Http::StreamDecoderFilterCallbacks& callbacks) override;

private:
  /**
   * Modifies the request payload
   * @param new_payload new payload
   * @param decoding_buffer buffer that contains the current payload
   * @param req_headers request headers
   */
  void setPayload(std::string new_payload, const Buffer::Instance* decoding_buffer,
                  Http::RequestHeaderMap* req_headers);
  /**
   * Reads the metadata and get details
   * @param metadata stream metadata
   * @return bool whether requested data is present and processed successfully
   */
  bool readMetadata(const envoy::config::core::v3::Metadata* metadata);
  Http::StreamEncoderFilterCallbacks* res_callbacks_{};
  Http::StreamDecoderFilterCallbacks* req_callbacks_{};
  Http::RequestHeaderMap* req_headers_{};
  std::string modified_body_;
  bool set_body_;
  bool read_metadata_;
};

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
