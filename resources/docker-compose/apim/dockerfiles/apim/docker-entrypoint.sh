#!/bin/bash
# ------------------------------------------------------------------------
# Copyright 2023 WSO2, LLC. (http://wso2.com)
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
# limitations under the License
# ------------------------------------------------------------------------

set -e

# volume mounts
config_volume=${WORKING_DIRECTORY}/wso2-config-volume
artifact_volume=${WORKING_DIRECTORY}/wso2-artifact-volume
# home of the directories to be artifact synced within the WSO2 product home
deployment_volume=${WSO2_SERVER_HOME}/repository/deployment/server
# home of the directories with preserved, default deployment artifacts
original_deployment_artifacts=${WORKING_DIRECTORY}/wso2-tmp

# check if the WSO2 non-root user home exists
test ! -d ${WORKING_DIRECTORY} && echo "WSO2 Docker non-root user home does not exist" && exit 1

# check if the WSO2 product home exists
test ! -d ${WSO2_SERVER_HOME} && echo "WSO2 Docker product home does not exist" && exit 1

# shared artifact directories
directories=("executionplans" "synapse-configs")
# if the original directory locations of artifacts to be synced between nodes are empty,
# copy the preserved, default content of these folders to these original locations
for shared_directory in ${directories[@]}; do
  if test -d ${original_deployment_artifacts}/${shared_directory};
  then
    if [[ -z "$(ls -A ${deployment_volume}/${shared_directory})" ]]; then
      if ! cp -R ${original_deployment_artifacts}/${shared_directory}/* ${deployment_volume}/${shared_directory};
      then
        echo "Failed to copy the preserved, default artifacts to original location (${deployment_volume}/${shared_directory})"
        exit 1
      fi
      echo "Successfully copied the preserved, default artifacts to original location (${deployment_volume}/${shared_directory})"
    fi
  fi
done

# optimize WSO2 Carbon Server, if the profile name is defined as an environment variable
if [[ ! -z "${PROFILE_NAME}" ]]
then
  echo "Optimizing WSO2 Carbon Server" >&2
  sh ${WSO2_SERVER_HOME}/bin/profileSetup.sh -Dprofile=${PROFILE_NAME}
fi

# copy any configuration changes mounted to config_volume
test -d ${config_volume} && [[ "$(ls -A ${config_volume})" ]] && cp -RL ${config_volume}/* ${WSO2_SERVER_HOME}/
# copy any artifact changes mounted to artifact_volume
test -d ${artifact_volume} && [[ "$(ls -A ${artifact_volume})" ]] && cp -RL ${artifact_volume}/* ${WSO2_SERVER_HOME}/

# start WSO2 Carbon server
echo "Start WSO2 Carbon server" >&2
if [[ -z "${PROFILE_NAME}" ]]
then
  # start the server with the provided startup arguments
  sh ${WSO2_SERVER_HOME}/bin/api-manager.sh "$@"
else
  # start the server with the specified profile and provided startup arguments
  sh ${WSO2_SERVER_HOME}/bin/api-manager.sh -Dprofile=${PROFILE_NAME} "$@"
fi
