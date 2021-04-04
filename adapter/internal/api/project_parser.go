/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package api

import (
	"github.com/ghodss/yaml"
	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/loggers"
)

func parseDeployments(data *[]byte) ([]Deployment, error) {
	// deployEnvsFromAPI represents deployments read from API Project
	deployEnvsFromAPI := &DeploymentEnvironments{}
	if err := yaml.Unmarshal(*data, deployEnvsFromAPI); err != nil {
		loggers.LoggerAPI.Errorf("Error parsing content of deployment environments: %v", err.Error())
		return nil, err
	}

	deployments := make([]Deployment, 0, len(deployEnvsFromAPI.Data))
	for _, deployFromAPI := range deployEnvsFromAPI.Data {
		defaultVhost, exists, err := config.GetDefaultVhost(deployFromAPI.DeploymentEnvironment)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error reading default vhost of environment %v: %v",
				deployFromAPI.DeploymentEnvironment, err.Error())
			return nil, err
		}
		// if the environment is not configured, ignore it
		if !exists {
			continue
		}

		deployment := deployFromAPI
		// if vhost is not defined with the API project use the default vhost from config
		if deployFromAPI.DeploymentVhost == "" {
			deployment.DeploymentVhost = defaultVhost
		}
		deployments = append(deployments, deployment)
	}
	return deployments, nil
}
