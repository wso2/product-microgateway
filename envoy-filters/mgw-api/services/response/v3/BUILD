load("@envoy_api//bazel:api_build_system.bzl", "api_proto_package")

licenses(["notice"])

api_proto_package(
    has_services = True,
    deps = [
        "@envoy_api//envoy/config/core/v3:pkg",
        "@envoy_api//envoy/type/v3:pkg",
        "@com_github_cncf_udpa//udpa/annotations:pkg",
    ],
)
