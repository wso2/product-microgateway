#!/bin/bash
# --------------------------------------------------------------------
# Copyright (c) 2022, WSO2 Inc. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------

set -e
ENVOY_REPO="${ENVOY_REPO:-./envoy}"
ENVOY_TAG="${ENVOY_TAG}"
CC_BASE_IMAGE="${CC_BASE_IMAGE}"
CC_PROFILE_IMAGE="${CC_PROFILE_IMAGE:-wso2am/choreo-connect-router:${CC_BASE_IMAGE##*:}-debug-$ENVOY_TAG}"
ENVOY_DOCKER_IMAGE="${ENVOY_DOCKER_IMAGE}"

if [ -z "$ENVOY_TAG" ] || [ -z "$CC_BASE_IMAGE" ]; then
  echo "error: 'ENVOY_TAG' and 'CC_BASE_IMAGE' are required" >&2
  exit 1
fi

echo "ENVOY_REPO=$ENVOY_REPO"
echo "ENVOY_TAG=$ENVOY_TAG"
echo "CC_BASE_IMAGE=$CC_BASE_IMAGE"
echo "CC_PROFILE_IMAGE=$CC_PROFILE_IMAGE"
echo "ENVOY_DOCKER_IMAGE=$ENVOY_DOCKER_IMAGE"
echo ""

if [ -d "$ENVOY_REPO" ]; then
  echo "Envoy git repository already exists."
else
  echo "Envoy git repository not exists, cloning the repo."
  git clone https://github.com/envoyproxy/envoy.git "$ENVOY_REPO"
fi

cd "$ENVOY_REPO"
git fetch origin --tags
git checkout "tags/$ENVOY_TAG" -b "$ENVOY_TAG-branch"

# Build linux binary using envoy-build-ubuntu image
export BAZEL_BUILD_EXTRA_OPTIONS='--define tcmalloc=gperftools'
./ci/run_envoy_docker.sh './ci/do_ci.sh bazel.release.server_only'

# Build envoy docker image
if [ -n "$ENVOY_DOCKER_IMAGE" ]; then
  docker build -f ci/Dockerfile-envoy --build-arg TARGETPLATFORM=linux/amd64 --build-arg ENVOY_BINARY_SUFFIX='' -t "$ENVOY_DOCKER_IMAGE" .
fi

# Build Choreo Connect Docker image
cat <<EOF > DockerfileChoreConnect
FROM $CC_BASE_IMAGE
COPY linux/amd64/build_envoy_release/envoy /usr/local/bin/envoy
EOF

docker build -t "$CC_PROFILE_IMAGE" -f DockerfileChoreConnect .
