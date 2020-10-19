#pragma once
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

#include "mgw-api/extensions/filters/http/mgw/v3/mgw.pb.h"
#include "envoy/http/filter.h"
#include "envoy/local_info/local_info.h"
#include "envoy/runtime/runtime.h"
#include "envoy/service/auth/v3/external_auth.pb.h"
#include "envoy/stats/scope.h"
#include "envoy/stats/stats_macros.h"
#include "envoy/upstream/cluster_manager.h"
#include "envoy/http/context.h"

#include "common/common/assert.h"
#include "common/common/logger.h"
#include "common/common/matchers.h"
#include "common/http/codes.h"
#include "common/http/header_map_impl.h"
#include "common/runtime/runtime_protos.h"

#include "mgw-source/filters/common/mgw/mgw.h"

namespace Envoy {
namespace Extensions {
namespace HttpFilters {
namespace MGW {

/**
 * All stats for the mgw filter. @see stats_macros.h
 */

#define ALL_mgw_FILTER_STATS(COUNTER)                                                        \
  COUNTER(ok)                                                                                      \
  COUNTER(denied)                                                                                  \
  COUNTER(error)                                                                                   \
  COUNTER(failure_mode_allowed)

/**
 * Wrapper struct for mgw filter stats. @see stats_macros.h
 */
struct MGWFilterStats {
  ALL_mgw_FILTER_STATS(GENERATE_COUNTER_STRUCT)
};

/**
 * Configuration for the External mgw request filter.
 */
class FilterConfig {
public:
  FilterConfig(const envoy::extensions::filters::http::mgw::v3::MGW& ,
               const LocalInfo::LocalInfo& local_info, Stats::Scope& scope,
               Runtime::Loader& runtime, Http::Context& http_context,
               const std::string& stats_prefix)
      : local_info_(local_info),
        scope_(scope), runtime_(runtime), http_context_(http_context),
        pool_(scope_.symbolTable()),
        stats_(generateStats(stats_prefix, scope)), mgw_ok_(pool_.add("mgw.ok")),
        mgw_denied_(pool_.add("mgw.denied")),
        mgw_error_(pool_.add("mgw.error")),
        mgw_failure_mode_allowed_(pool_.add("mgw.failure_mode_allowed")) {}

  const LocalInfo::LocalInfo& localInfo() const { return local_info_; }

  Runtime::Loader& runtime() { return runtime_; }

  Stats::Scope& scope() { return scope_; }

  Http::Context& httpContext() { return http_context_; }

  const MGWFilterStats& stats() const { return stats_; }

  void incCounter(Stats::Scope& scope, Stats::StatName name) {
    scope.counterFromStatName(name).inc();
  }

private:

  MGWFilterStats generateStats(const std::string& prefix, Stats::Scope& scope) {
    const std::string final_prefix = prefix + "mgw.";
    return {ALL_mgw_FILTER_STATS(POOL_COUNTER_PREFIX(scope, final_prefix))};
  }
  const LocalInfo::LocalInfo& local_info_;
  Stats::Scope& scope_;
  Runtime::Loader& runtime_;
  Http::Context& http_context_;

  // TODO(nezdolik): stop using pool as part of deprecating cluster scope stats.
  Stats::StatNamePool pool_;

  // The stats for the filter.
  MGWFilterStats stats_;

public:
  // TODO(nezdolik): deprecate cluster scope stats counters in favor of filter scope stats
  // (MGWFilterStats stats_).
  const Stats::StatName mgw_ok_;
  const Stats::StatName mgw_denied_;
  const Stats::StatName mgw_error_;
  const Stats::StatName mgw_failure_mode_allowed_;
};

} // namespace MGW
} // namespace HttpFilters
} // namespace Extensions
} // namespace Envoy