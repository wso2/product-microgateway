#include "mgw-source/filters/http/mgw/config.h"

#include <chrono>
#include <string>

#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.h"
#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.validate.h"
#include "envoy/registry/registry.h"

#include "common/protobuf/utility.h"
#include "mgw-source/filters/http/mgw/mgw.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

Http::FilterFactoryCb MGWFilterConfig::createFilterFactoryFromProtoTyped(
    const envoy::extensions::filters::http::mgw::v3::MGW&, const std::string&,
    Server::Configuration::FactoryContext&) {
  Http::FilterFactoryCb callback;
  callback = [](Http::FilterChainFactoryCallbacks& callbacks) {
    callbacks.addStreamFilter(Http::StreamFilterSharedPtr{std::make_shared<Filter>()});
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
