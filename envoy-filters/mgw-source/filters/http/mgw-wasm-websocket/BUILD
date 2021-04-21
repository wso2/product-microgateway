load("@proxy_wasm_cpp_sdk//bazel/wasm:wasm.bzl", "wasm_cc_binary")

wasm_cc_binary(
    name = "mgw-websocket.wasm",
    srcs = ["filter.cc", "filter.h", "handler_impl.h", "handler_impl.cc", "handler.h"],
    deps = [
        "@proxy_wasm_cpp_sdk//:proxy_wasm_intrinsics",
        "@mgw_wasm_websocket_api//:api_cc_proto",
        "@proxy_wasm_cpp_sdk//:proxy_wasm_intrinsics_lite"
    ],
)
