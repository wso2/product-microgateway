package(default_visibility = ["//visibility:public"])

load(
    "@envoy//bazel:envoy_build_system.bzl",
    "envoy_cc_library",
)

envoy_cc_library(
    name = "mgw",
    srcs = ["analytics.cc"],
    hdrs = ["analytics.h", "filter_config.h"],
    repository = "@envoy",
    deps = [
        # ":filter_config",
        "@envoy//include/envoy/http:codes_interface",
        "@envoy//include/envoy/http:context_interface",
        "@envoy//include/envoy/stats:stats_macros",
        "@envoy//source/common/buffer:buffer_lib",
        "@envoy//source/common/common:assert_lib",
        "@envoy//source/common/common:empty_string",
        "@envoy//source/common/common:enum_to_int",
        "@envoy//source/common/common:matchers_lib",
        "@envoy//source/common/common:minimal_logger_lib",
        "@envoy//source/common/http:codes_lib",
        "@envoy//source/common/http:utility_lib",
        "@envoy//source/common/router:config_lib",
        "//mgw-source/filters/common/mgw:mgw_res_grpc_lib",
        "@envoy_api//envoy/config/core/v3:pkg_cc_proto",
        "//mgw-api/extensions/filters/http/mgw/v3:pkg_cc_proto",
        "@envoy_api//envoy/service/auth/v3:pkg_cc_proto",
        "//mgw-api/services/response/v3:pkg_cc_proto",
    ],
)

envoy_cc_library(
    name = "config",
    srcs = ["config.cc"],
    hdrs = ["config.h"],
    repository = "@envoy",
    deps = [
        ":mgw",
        "@envoy//include/envoy/registry",
        "@envoy//include/envoy/stats:stats_macros",
        "@envoy//source/common/protobuf:utility_lib",
        "@envoy//source/extensions/filters/http:well_known_names",
        "@envoy//source/extensions/filters/http/common:factory_base_lib",
        "@envoy_api//envoy/config/core/v3:pkg_cc_proto",
        "//mgw-api/extensions/filters/http/mgw/v3:pkg_cc_proto",
    ],
)

