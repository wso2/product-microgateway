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
	endpointCertFile           string = "endpoint_certificates."
	apiJSONFile                string = "api.json"
	rateLimitPoliciesFile      string = "rate-limit-policies.yaml"
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
	APIYaml            APIYaml
	RateLimitPolicy    RateLimitPolicy
	APIEnvProps        map[string]synchronizer.APIEnvProps
	Deployments        []Deployment
	OpenAPIJsn         []byte
	InterceptorCerts   []byte
	APIType            string // read from api.yaml and formatted to upper case
	APILifeCycleStatus string // read from api.yaml and formatted to upper case
	OrganizationID     string // read from api.yaml or config

	//UpstreamCerts cert filename -> cert bytes
	UpstreamCerts map[string][]byte
	//EndpointCerts url -> cert filename
	EndpointCerts map[string]string
}

// EndpointSecurity contains parameters of endpoint security at api.json
type EndpointSecurity struct {
	Password         string            `json:"password,omitempty" mapstructure:"password"`
	Type             string            `json:"type,omitempty" mapstructure:"type"`
	Enabled          bool              `json:"enabled,omitempty" mapstructure:"enabled"`
	Username         string            `json:"username,omitempty" mapstructure:"username"`
	CustomParameters map[string]string `json:"customparameters,omitempty" mapstructure:"customparameters"`
}

// APIEndpointSecurity represents the structure of endpoint_security param in api.yaml
type APIEndpointSecurity struct {
	Production EndpointSecurity `json:"production,omitempty"`
	Sandbox    EndpointSecurity `json:"sandbox,omitempty"`
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
		Provider                   string   `json:"provider,omitempty"`
		RateLimitLevel             string   `json:"rateLimitLevel,omitempty"`
		RateLimitPolicy            string   `json:"rateLimitPolicy,omitempty"`
		EndpointConfig             struct {
			EndpointType                 string              `json:"endpoint_type,omitempty"`
			LoadBalanceAlgo              string              `json:"algoCombo,omitempty"`
			LoadBalanceSessionManagement string              `json:"sessionManagement,omitempty"`
			LoadBalanceSessionTimeOut    string              `json:"sessionTimeOut,omitempty"`
			APIEndpointSecurity          APIEndpointSecurity `json:"endpoint_security,omitempty"`
			RawProdEndpoints             interface{}         `json:"production_endpoints,omitempty"`
			ProductionEndpoints          []EndpointInfo
			ProductionFailoverEndpoints  []EndpointInfo `json:"production_failovers,omitempty"`
			RawSandboxEndpoints          interface{}    `json:"sandbox_endpoints,omitempty"`
			SandBoxEndpoints             []EndpointInfo
			SandboxFailoverEndpoints     []EndpointInfo `json:"sandbox_failovers,omitempty"`
			ImplementationStatus         string         `json:"implementation_status,omitempty"`
		} `json:"endpointConfig,omitempty"`
		Operations []OperationYaml `json:"Operations,omitempty"`
	} `json:"data"`
}

// OperationYaml holds attributes of APIM operations
type OperationYaml struct {
	ID              string `json:"id,omitempty"`
	Target          string `json:"target,omitempty"`
	Verb            string `json:"verb,omitempty"`
	RateLimitPolicy string `json:"rateLimitPolicy,omitempty"`
}

// APIRateLimitPolicy holds policy details relevant to the rate limiting
type APIRateLimitPolicy struct {
	PolicyName string `json:"policyName,omitempty"`
	Type       string `json:"type,omitempty"`
	Count      int    `json:"count,omitempty"`
	Unit       string `json:"unit,omitempty"`
	Span       uint32 `json:"span,omitempty"`
	SpanUnit   string `json:"spanUnit,omitempty"`
}

// RateLimitPolicy contains all the fields in the rate-limit_policies.yaml file
type RateLimitPolicy struct {
	ApimMeta
	Data struct {
		APIRateLimitPolicies []APIRateLimitPolicy
	}
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
	} else if strings.Contains(fileName, endpointCertDir+string(os.PathSeparator)) {
		if strings.Contains(fileName, endpointCertFile) {
			epCertJSON, conversionErr := utills.ToJSON(fileContent)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting %v file to json: %v", fileName, conversionErr.Error())
				return conversionErr
			}
			endpointCertificates := &EndpointCertificatesDetails{}
			err := json.Unmarshal(epCertJSON, endpointCertificates)
			if err != nil {
				loggers.LoggerAPI.Error("Error parsing content of endpoint certificates: ", err)
			} else if endpointCertificates != nil && len(endpointCertificates.Data) > 0 {
				for _, val := range endpointCertificates.Data {
					apiProject.EndpointCerts[val.Endpoint] = val.Certificate
				}
			}
		} else if strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension) {
			if !tlsutils.IsPublicCertificate(fileContent) {
				loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", fileName)
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return errors.New("certificate Validation Error")
			}

			if fileNameArray := strings.Split(fileName, string(os.PathSeparator)); len(fileNameArray) > 0 {
				certFileName := fileNameArray[len(fileNameArray)-1]
				apiProject.UpstreamCerts[certFileName] = fileContent
			}
		}
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
	} else if strings.Contains(fileName, rateLimitPoliciesFile) {
		loggers.LoggerAPI.Debugf("fileName : %v available for the API project.", fileName)
		rlPoliciesJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error occured rate limit policies file to json: %v", conversionErr.Error())
			return conversionErr
		}
		var rlPolicies RateLimitPolicy
		err := json.Unmarshal(rlPoliciesJsn, &rlPolicies)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occured while parsing rate-limit-policies.yaml %v", err.Error())
			return err
		}
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
