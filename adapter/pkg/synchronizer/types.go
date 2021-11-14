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

/*
 * Package "synchronizer" contains artifacts relate to fetching APIs and
 * API related updates from the control plane event-hub.
 * This file contains types to retrieve APIs and API updates.
 */

package synchronizer

// SyncAPIResponse struct contains information related to
// response of the API pulling/fetching from control plane
// along with the apiId and the gateway label that the call
// was made with.
type SyncAPIResponse struct {
	Resp          []byte
	Err           error
	ErrorCode     int
	APIUUID       string
	GatewayLabels []string
}

// DeploymentDescriptor represents deployment descriptor file contains in Artifact
// received from control plane
type DeploymentDescriptor struct {
	Type    string         `json:"type"`
	Version string         `json:"version"`
	Data    DeploymentData `json:"data"`
}

// DeploymentData contains list of APIDeployment to be deployed to the gateway
type DeploymentData struct {
	Deployments []APIDeployment `json:"deployments"`
}

// APIDeployment represents an API project that contains zip file name and
// gateway environments (labels) that the project to be deployed
type APIDeployment struct {
	APIFile      string         `json:"apiFile"`
	Environments []GatewayLabel `json:"environments"`
	// These properties are used by global Adapter
	OrganizationID string `json:"organizationId"`
	APIContext     string `json:"apiContext"`
	Version        string `json:"version"`
}

// GatewayLabel represents gateway environment name and VHost of an API project
type GatewayLabel struct {
	Name  string `json:"name"`
	Vhost string `json:"vhost"`
}

// APIConfigs represents env properties belongs to the API
type APIConfigs struct {
	ProductionEndpoint string `mapstructure:"production_endpoint,omitempty"`
	SandBoxEndpoint    string `mapstructure:"sandbox_endpoint,omitempty"`
}

// APIEnvProps represents env properties
type APIEnvProps struct {
	EnvID      string     `mapstructure:"envId,omitempty"`
	APIConfigs APIConfigs `mapstructure:"configs,omitempty"`
}
