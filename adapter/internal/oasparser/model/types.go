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

import (
	"encoding/json"
	"errors"
	"os"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
	"gopkg.in/yaml.v2"
)

const (
	openAPIDir                 string = "Definitions"
	openAPIFilename            string = "swagger."
	apiYAMLFile                string = "api.yaml"
	deploymentsYAMLFile        string = "deployment_environments.yaml"
	apiJSONFile                string = "api.json"
	endpointCertDir            string = "Endpoint-certificates"
	interceptorCertDir         string = "Endpoint-certificates/interceptors"
	crtExtension               string = ".crt"
	pemExtension               string = ".pem"
	apiTypeFilterKey           string = "type"
	apiTypeYamlKey             string = "type"
	lifeCycleStatus            string = "lifeCycleStatus"
	securityScheme             string = "securityScheme"
	endpointImplementationType string = "endpointImplementationType"
	inlineEndpointType         string = "INLINE"
	endpointSecurity           string = "endpoint_security"
	production                 string = "production"
	sandbox                    string = "sandbox"
	zipExt                     string = ".zip"
)

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIYaml             APIYaml
	APIEnvProps         map[string]synchronizer.APIEnvProps
	Deployments         []Deployment
	OpenAPIJsn          []byte
	UpstreamCerts       []byte
	InterceptorCerts    []byte
	APIType             string              // read from api.yaml and formatted to upper case
	APILifeCycleStatus  string              // read from api.yaml and formatted to upper case
	ProductionEndpoints []Endpoint          // read from env variable
	SandboxEndpoints    []Endpoint          // read from env variable
	OrganizationID      string              // read from api.yaml or config
	EndpointSecurity    APIEndpointSecurity // derived from api.yaml or config
}

// EndpointSecurity contains parameters of endpoint security at api.json
type EndpointSecurity struct {
	Password         string `json:"password,omitempty"`
	Type             string `json:"type,omitempty"`
	Enabled          bool   `json:"enabled,omitempty"`
	Username         string `json:"username,omitempty"`
	CustomParameters string `json:"customparameters,omitempty"`
}

// APIEndpointSecurity represents the structure of endpoint_security param in api.yaml
type APIEndpointSecurity struct {
	Production EndpointSecurity
	Sandbox    EndpointSecurity
}

// ApimMeta represents APIM meta information of files received from APIM
type ApimMeta struct {
	Type    string `yaml:"type"`
	Version string `yaml:"version"`
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

// APIYaml contains everything necessary to extract api.json/api.yaml file
// To support both api.json and api.yaml we convert yaml to json and then use json.Unmarshal()
// Therefore, the params are defined to support json.Unmarshal()
type APIYaml struct {
	ApimMeta
	Data struct {
		ID                         string   `json:"Id,omitempty"`
		Name                       string   `json:"name,omitempty"`
		Context                    string   `json:"context,omitempty"`
		Version                    string   `json:"version,omitempty"`
		RevisionID                 int      `json:"revisionId,omitempty"`
		APIType                    string   `json:"type,omitempty"`
		LifeCycleStatus            string   `json:"lifeCycleStatus,omitempty"`
		EndpointImplementationType string   `json:"endpointImplementationType,omitempty"`
		AuthorizationHeader        string   `json:"authorizationHeader,omitempty"`
		SecurityScheme             []string `json:"securityScheme,omitempty"`
		OrganizationID             string   `json:"organizationId,omitempty"`
		EndpointConfig             struct {
			EndpointType                 string `json:"endpoint_type,omitempty"`
			LoadBalanceAlgo              string `json:"algoCombo,omitempty"`
			LoadBalanceSessionManagement string `json:"sessionManagement,omitempty"`
			LoadBalanceSessionTimeOut    string `json:"sessionTimeOut,omitempty"`
			EndpointSecurity             struct {
				Production struct {
					Password         string `json:"password,omitempty"`
					Type             string `json:"type,omitempty"`
					Enabled          bool   `json:"enabled,omitempty"`
					Username         string `json:"username,omitempty"`
					CustomParameters string `json:"customparameters,omitempty"`
				} `json:"production,omitempty"`
				Sandbox struct {
					Password         string `json:"password,omitempty"`
					Type             string `json:"type,omitempty"`
					Enabled          bool   `json:"enabled,omitempty"`
					Username         string `json:"username,omitempty"`
					CustomParameters string `json:"customparameters,omitempty"`
				} `json:"sandbox,omitempty"`
			} `json:"endpoint_security,omitempty"`
			RawProdEndpoints            interface{} `json:"production_endpoints,omitempty"`
			ProductionEndpoints         []EndpointInfo
			ProductionFailoverEndpoints []EndpointInfo `json:"production_failovers,omitempty"`
			RawSandboxEndpoints         interface{}    `json:"sandbox_endpoints,omitempty"`
			SandBoxEndpoints            []EndpointInfo
			SandboxFailoverEndpoints    []EndpointInfo `json:"sandbox_failovers,omitempty"`
		} `json:"endpointConfig,omitempty"`
	} `json:"data"`
}

// EndpointInfo holds config values regards to the endpoint
type EndpointInfo struct {
	Endpoint string `json:"url,omitempty"`
	Config   struct {
		ActionDuration string `json:"actionDuration,omitempty"`
		RetryTimeOut   string `json:"retryTimeOut,omitempty"`
	} `json:"config,omitempty"`
}

// ValidateAPIType checks if the apiProject is properly assigned with the type.
func (apiProject *ProjectAPI) ValidateAPIType() error {
	var err error
	if apiProject.APIYaml.Type == "" {
		// If no api.yaml file is included in the zip folder, return with error.
		err = errors.New("could not find api.yaml or api.json")
		return err
	} else if apiProject.APIType != HTTP && apiProject.APIType != WS && apiProject.APIType != WEBHOOK {
		errMsg := "API type is not currently supported with Choreo Connect"
		err = errors.New(errMsg)
		return err
	}
	return nil
}

// ProcessFilesInsideProject process single file inside API Project and update the apiProject instance appropriately.
func (apiProject *ProjectAPI) ProcessFilesInsideProject(fileContent []byte, fileName string) (err error) {
	newLineByteArray := []byte("\n")
	if strings.Contains(fileName, deploymentsYAMLFile) {
		loggers.LoggerAPI.Debug("Setting deployments of API")
		deployments, err := parseDeployments(fileContent)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occurred while parsing the deployment environments: %v %v",
				fileName, err.Error())
		}
		apiProject.Deployments = deployments
	}
	if strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+openAPIFilename) {
		loggers.LoggerAPI.Debugf("openAPI file : %v", fileName)
		swaggerJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error converting api file to json: %v", conversionErr.Error())
			return conversionErr
		}
		apiProject.OpenAPIJsn = swaggerJsn
		apiProject.APIType = HTTP
	} else if strings.Contains(fileName, interceptorCertDir+string(os.PathSeparator)) &&
		(strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension)) {
		if !tlsutils.IsPublicCertificate(fileContent) {
			loggers.LoggerAPI.Errorf("Provided interceptor certificate: %v is not in the PEM file format. ", fileName)
			return errors.New("interceptor certificate Validation Error")
		}
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, fileContent...)
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, newLineByteArray...)
	} else if strings.Contains(fileName, endpointCertDir+string(os.PathSeparator)) &&
		(strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension)) {
		if !tlsutils.IsPublicCertificate(fileContent) {
			loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", fileName)
			// TODO: (VirajSalaka) Create standard error handling mechanism
			return errors.New("certificate Validation Error")
		}
		apiProject.UpstreamCerts = append(apiProject.UpstreamCerts, fileContent...)
		apiProject.UpstreamCerts = append(apiProject.UpstreamCerts, newLineByteArray...)
	} else if (strings.Contains(fileName, apiYAMLFile) || strings.Contains(fileName, apiJSONFile)) &&
		!strings.Contains(fileName, openAPIDir) {
		loggers.LoggerAPI.Debugf("fileName : %v", fileName)
		apiJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error occured converting api file to json: %v", conversionErr.Error())
			return conversionErr
		}
		var apiYaml APIYaml
		err = json.Unmarshal(apiJsn, &apiYaml)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml or api.json %v", err.Error())
			return err
		}
		apiYaml = PopulateEndpointsInfo(apiYaml)

		err = VerifyMandatoryFields(apiYaml)
		if err != nil {
			loggers.LoggerAPI.Errorf("%v", err)
			return err
		}

		if apiYaml.Data.EndpointImplementationType == inlineEndpointType {
			errmsg := "inline endpointImplementationType is not currently supported with Choreo Connect"
			loggers.LoggerAPI.Warnf(errmsg)
			err = errors.New(errmsg)
			return err
		}
		apiProject.APIYaml = apiYaml
		ExtractAPIInformation(apiProject, apiYaml)
	}
	return nil
}

func parseDeployments(data []byte) ([]Deployment, error) {
	// deployEnvsFromAPI represents deployments read from API Project
	deployEnvsFromAPI := &DeploymentEnvironments{}
	if err := yaml.Unmarshal(data, deployEnvsFromAPI); err != nil {
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
