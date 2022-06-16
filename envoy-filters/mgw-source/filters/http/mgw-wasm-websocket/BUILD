load("@proxy_wasm_cpp_sdk//bazel:defs.bzl", "proxy_wasm_cc_binary")

proxy_wasm_cc_binary(
    name = "mgw-websocket.wasm",
    srcs = ["filter.cc", "filter.h", "handler_impl.h", "handler_impl.cc", "handler.h"],
    deps = [
        "@mgw_wasm_websocket_api//:api_cc_proto",
        "@proxy_wasm_cpp_sdk//:proxy_wasm_intrinsics_full"
    ],
)
