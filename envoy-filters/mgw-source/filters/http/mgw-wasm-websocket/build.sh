#!/bin/bash
SOURCE_DIR="${PWD}"
SOURCE_DIR_MOUNT_DEST=/source/mgw-source/filters/http/mgw-wasm-websocket
SOURCE_DIR_API="${PWD}"/../../../../mgw-api/extensions/filters/http/mgw-wasm-websocket/v3/
echo "${SOURCE_DIR_API}"
SOURCE_DIR_MOUNT_API=/source/mgw-api/extensions/filters/http/mgw-wasm-websocket/v3/
BUILD_DIR=/temp/envoy-wasm
BUILD_DIR_MOUNT_DEST=/build
ENVOY_BUILD_IMAGE=envoyproxy/envoy-build-ubuntu:e33c93e6d79804bf95ff80426d10bdcc9096c785

START_COMMAND=("/bin/sh" -c 'cd source/mgw-source/filters/http/mgw-wasm-websocket \
                            && bazel build //:mgw-websocket.wasm \
                            && cp -a bazel-bin/mgw-websocket.wasm /build')

docker run --rm \
    -it\
    -v "${BUILD_DIR}":"${BUILD_DIR_MOUNT_DEST}" \
    -v "${SOURCE_DIR}":"${SOURCE_DIR_MOUNT_DEST}" \
    -v "${SOURCE_DIR_API}":"${SOURCE_DIR_MOUNT_API}"\
    "${ENVOY_BUILD_IMAGE}" \
    "${START_COMMAND[@]}"

