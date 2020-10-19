licenses(["notice"])  # Apache 2

load(
    "@envoy//bazel:envoy_build_system.bzl",
    "envoy_cc_library",
    "envoy_package",
)

envoy_package()

envoy_cc_library(
    name = "mgw_interface",
    hdrs = ["mgw.h"],
    repository = "@envoy",
    deps = [
        "@envoy//include/envoy/http:codes_interface",
        "@envoy//include/envoy/stream_info:stream_info_interface",
        "@envoy//source/common/tracing:http_tracer_lib",
        "@envoy_api//envoy/service/auth/v3:pkg_cc_proto",
        "//mgw-api/services/response/v3:pkg_cc_proto",
    ],
)

envoy_cc_library(
    name = "mgw_res_grpc_lib",
    srcs = ["mgw_res_grpc_impl.cc"],
    hdrs = ["mgw_res_grpc_impl.h"],
    repository = "@envoy",
    deps = [
        ":mgw_interface",
        "@envoy//include/envoy/grpc:async_client_interface",
        "@envoy//include/envoy/grpc:async_client_manager_interface",
        "@envoy//include/envoy/http:filter_interface",
        "@envoy//include/envoy/http:header_map_interface",
        "@envoy//include/envoy/http:protocol_interface",
        "@envoy//include/envoy/network:address_interface",
        "@envoy//include/envoy/network:connection_interface",
        "@envoy//include/envoy/network:filter_interface",
        "@envoy//include/envoy/upstream:cluster_manager_interface",
        "@envoy//source/common/common:assert_lib",
        "@envoy//source/common/grpc:async_client_lib",
        "@envoy//source/common/grpc:typed_async_client_lib",
        "@envoy//source/common/http:headers_lib",
        "@envoy//source/common/http:utility_lib",
        "@envoy//source/common/network:utility_lib",
        "@envoy//source/common/protobuf",
        "@envoy//source/common/tracing:http_tracer_lib",
        "@envoy_api//envoy/config/core/v3:pkg_cc_proto",
        "@envoy_api//envoy/service/auth/v3:pkg_cc_proto",
    ],
)

