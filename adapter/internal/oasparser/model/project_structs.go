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

package model

import "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIYaml          APIYaml
	APIEnvProps      map[string]synchronizer.APIEnvProps
	Deployments      []Deployment
	APIDefinition    []byte
	InterceptorCerts []byte
	UpstreamCerts    map[string][]byte // cert filename -> cert bytes
	EndpointCerts    map[string]string // cert url -> cert filename
}

// EndpointSecurity contains parameters of endpoint security at api.json
type EndpointSecurity struct {
	Password         string            `json:"password,omitempty" mapstructure:"password"`
	Type             string            `json:"type,omitempty" mapstructure:"type"`
	Enabled          bool              `json:"enabled,omitempty" mapstructure:"enabled"`
	Username         string            `json:"username,omitempty" mapstructure:"username"`
	CustomParameters map[string]string `json:"customparameters,omitempty" mapstructure:"customparameters"`
}

// ApimMeta represents APIM meta information of files received from APIM
type ApimMeta struct {
	Type    string `yaml:"type" json:"type"`
	Version string `yaml:"version" json:"version"`
}

// DeploymentEnvironments represents content of deployment_environments.yaml file
// of an API_CTL Project
type DeploymentEnvironments struct {
	ApimMeta
	Data []Deployment `yaml:"data"`
}

// Deployment represents deployment information of an API_CTL project
type Deployment struct {
	DisplayOnDevportal    bool   `yaml:"displayOnDevportal"`
	DeploymentVhost       string `yaml:"deploymentVhost"`
	DeploymentEnvironment string `yaml:"deploymentEnvironment"`
}

// EndpointCertificatesDetails represents content of endpoint_certificates.yaml file
// of an API_CTL Project
type EndpointCertificatesDetails struct {
	ApimMeta
	Data []EndpointCertificate `json:"data"`
}

// EndpointCertificate represents certificate information of an API_CTL project
type EndpointCertificate struct {
	Alias       string `json:"alias"`
	Endpoint    string `json:"endpoint"`
	Certificate string `json:"certificate"`
}
