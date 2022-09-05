/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// This files contains models that holds the content of an API project,
// that are NOT used to describe the API itself

package model

import "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIYaml             APIYaml
	APIEnvProps         map[string]synchronizer.APIEnvProps
	Deployments         []Deployment
	APIDefinition       []byte
	InterceptorCerts    []byte
	UpstreamCerts       map[string][]byte  // cert filename -> cert bytes
	EndpointCerts       map[string]string  // url -> cert filename
	Policies            PolicyContainerMap // read from policy dir, policyName -> {policy spec, policy definition}
	DownstreamCerts     map[string][]byte  // cert filename -> cert bytes
	ClientCerts         []CertificateDetails
	GraphQLComplexities GraphQLComplexityYaml
}

// DeploymentEnvironments represents content of deployment_environments.yaml file
// of an API_CTL Project
type DeploymentEnvironments struct {
	Type    string       `yaml:"type" json:"type"`
	Version string       `yaml:"version" json:"version"`
	Data    []Deployment `yaml:"data"`
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
	Type    string                `yaml:"type" json:"type"`
	Version string                `yaml:"version" json:"version"`
	Data    []EndpointCertificate `json:"data"`
}

// EndpointCertificate represents certificate information of an API_CTL project
type EndpointCertificate struct {
	Alias       string `json:"alias"`
	Endpoint    string `json:"endpoint"`
	Certificate string `json:"certificate"`
}

// ClientCertificatesDetails represents content of client_certificates.yaml file
// of an API_CTL Project
type ClientCertificatesDetails struct {
	Type    string              `yaml:"type" json:"type"`
	Version string              `yaml:"version" json:"version"`
	Data    []ClientCertificate `json:"data"`
}

// ClientCertificate represents certificate information of an API_CTL project
type ClientCertificate struct {
	Alias         string  `json:"alias"`
	Certificate   string  `json:"certificate"`
	TierName      string  `json:"tierName"`
	APIIdentifier APIData `json:"apiIdentifier"`
}

// CertificateDetails represents certificates information that needed to passed to the enforcer
type CertificateDetails struct {
	Alias           string
	Tier            string
	CertificateName string
}

// APIData represents API information of relevant API of the certificate
type APIData struct {
	ProviderName string `json:"providerName"`
	APIName      string `json:"apiName"`
	Version      string `json:"version"`
	UUID         string `json:"uuid"`
	ID           uint32 `json:"id"`
}
