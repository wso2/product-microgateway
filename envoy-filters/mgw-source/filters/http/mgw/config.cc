#include "mgw-source/filters/http/mgw/config.h"

#include <chrono>
#include <string>

#include "envoy/config/core/v3/grpc_service.pb.h"
#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.h"
#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.validate.h"
#include "envoy/registry/registry.h"

#include "common/protobuf/utility.h"

#include "mgw-source/filters/common/mgw/mgw_res_grpc_impl.h"
#include "mgw-source/filters/http/mgw/analytics.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

Http::FilterFactoryCb MGWFilterConfig::createFilterFactoryFromProtoTyped(
    const envoy::extensions::filters::http::mgw::v3::MGW& proto_config,
    const std::string& stats_prefix, Server::Configuration::FactoryContext& context) {
  const auto res_filter_config =
      std::make_shared<FilterConfig>(proto_config, context.localInfo(), context.scope(),
                                     context.runtime(), context.httpContext(), stats_prefix);
  Http::FilterFactoryCb callback;

  const uint32_t res_timeout_ms =
      PROTOBUF_GET_MS_OR_DEFAULT(proto_config.grpc_service(), timeout, DefaultTimeout);
  callback = [res_grpc_service = proto_config.grpc_service(), &context,
                res_filter_config, res_timeout_ms](Http::FilterChainFactoryCallbacks& callbacks) {
    const auto res_async_client_factory =
        context.clusterManager().grpcAsyncClientManager().factoryForGrpcService(
            res_grpc_service, context.scope(), true);
    auto res_client = std::make_unique<Filters::Common::MGW::GrpcResClientImpl>(
        res_async_client_factory->create(), std::chrono::milliseconds(res_timeout_ms));
    callbacks.addStreamEncoderFilter(Http::StreamEncoderFilterSharedPtr{std::make_shared<Filter>(
         res_filter_config, std::move(res_client))});
  };

  return callback;
};

/**
 * Static registration for the mgw filter. @see RegisterFactory.
 */
REGISTER_FACTORY(MGWFilterConfig, Server::Configuration::NamedHttpFilterConfigFactory){"envoy.mgw"};

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy
