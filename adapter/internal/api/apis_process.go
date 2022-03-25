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
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
	"gopkg.in/yaml.v2"
)

const (
	openAPIDir                 string = "Definitions"
	openAPIFilename            string = "swagger."
	asyncAPIFilename           string = "asyncapi."
	apiYAMLFile                string = "api.yaml"
	deploymentsYAMLFile        string = "deployment_environments.yaml"
	endpointCertFile           string = "endpoint_certificates."
	apiJSONFile                string = "api.json"
	endpointCertDir            string = "Endpoint-certificates"
	interceptorCertDir         string = "Endpoint-certificates/interceptors"
	policiesDir                string = "Policies"
	policyDefFileExtension     string = ".gotmpl"
	crtExtension               string = ".crt"
	pemExtension               string = ".pem"
	apiTypeFilterKey           string = "type"
	apiTypeYamlKey             string = "type"
	lifeCycleStatus            string = "lifeCycleStatus"
	securityScheme             string = "securityScheme"
	endpointImplementationType string = "endpointImplementationType"
	endpointSecurity           string = "endpoint_security"
	production                 string = "production"
	sandbox                    string = "sandbox"
	zipExt                     string = ".zip"
	yamlExt                    string = ".yaml"
	jsonExt                    string = ".json"
)

// processFileInsideProject method process one file at a time and
// update the apiProject instance appropriately. Files could be: /petstore,
// /petstore/Definition, /petstore/Definition/swagger.yaml, /petstore/api.yaml, etc.
func processFileInsideProject(apiProject *model.ProjectAPI, fileContent []byte, fileName string) (err error) {
	newLineByteArray := []byte("\n")

	// Deployment file
	if strings.Contains(fileName, deploymentsYAMLFile) {
		loggers.LoggerAPI.Debug("Setting deployments of API")
		deployments, err := parseDeployments(fileContent)
		if err != nil {
			loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error occurred while parsing the deployment environments: %v %v", fileName, err.Error()),
				Severity:  logging.MAJOR,
				ErrorCode: 1212,
			})
		}
		apiProject.Deployments = deployments
	}

	// API definition file
	if strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+openAPIFilename) ||
		strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+asyncAPIFilename) {

		loggers.LoggerAPI.Debugf("API definition file : %v", fileName)
		swaggerJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error converting api file to json: %v", conversionErr.Error()),
				Severity:  logging.MINOR,
				ErrorCode: 1213,
			})
			return conversionErr
		}
		apiProject.APIDefinition = swaggerJsn

		// Interceptor certs
	} else if strings.Contains(fileName, interceptorCertDir+string(os.PathSeparator)) &&
		(strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension)) {
		if !tlsutils.IsPublicCertificate(fileContent) {
			loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Provided interceptor certificate: %v is not in the PEM file format. ", fileName),
				Severity:  logging.MINOR,
				ErrorCode: 1214,
			})
			return errors.New("interceptor certificate Validation Error")
		}
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, fileContent...)
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, newLineByteArray...)

		// Endpoint certs
	} else if strings.Contains(fileName, endpointCertDir+string(os.PathSeparator)) {
		if strings.Contains(fileName, endpointCertFile) {
			epCertJSON, conversionErr := utills.ToJSON(fileContent)
			if conversionErr != nil {
				loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
					Message:   fmt.Sprintf("Error converting %v file to json: %v", fileName, conversionErr.Error()),
					Severity:  logging.MINOR,
					ErrorCode: 1215,
				})
				return conversionErr
			}
			endpointCertificates := &model.EndpointCertificatesDetails{}
			err := json.Unmarshal(epCertJSON, endpointCertificates)
			if err != nil {
				loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
					Message:   fmt.Sprintf("Error parsing content of endpoint certificates: %v", err.Error()),
					Severity:  logging.MINOR,
					ErrorCode: 1216,
				})
			} else if endpointCertificates != nil && len(endpointCertificates.Data) > 0 {
				for _, val := range endpointCertificates.Data {
					apiProject.EndpointCerts[val.Endpoint] = val.Certificate
				}
			}
		} else if strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension) {
			if !tlsutils.IsPublicCertificate(fileContent) {
				loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
					Message:   fmt.Sprintf("Provided certificate: %v is not in the PEM file format. ", fileName),
					Severity:  logging.MINOR,
					ErrorCode: 1217,
				})
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return errors.New("certificate Validation Error")
			}

			if fileNameArray := strings.Split(fileName, string(os.PathSeparator)); len(fileNameArray) > 0 {
				certFileName := fileNameArray[len(fileNameArray)-1]
				apiProject.UpstreamCerts[certFileName] = fileContent
			}
		}

		// api.yaml or api.json
	} else if (strings.Contains(fileName, apiYAMLFile) || strings.Contains(fileName, apiJSONFile)) &&
		!strings.Contains(fileName, openAPIDir) {
		apiYaml, err := model.NewAPIYaml(fileContent)
		if err != nil {
			loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error while reading %v. %v", fileName, err.Error()),
				Severity:  logging.MINOR,
				ErrorCode: 1218,
			})
			return errors.New("Error while reading api.yaml or api.json")
		}
		apiProject.APIYaml = apiYaml

		// API policies
	} else if strings.Contains(fileName, policiesDir+string(os.PathSeparator)) { // handle "./Policy" dir
		// handle policy spec and def
		isSpec := strings.HasSuffix(fileName, jsonExt) || strings.HasSuffix(fileName, yamlExt)
		isDef := strings.HasSuffix(fileName, policyDefFileExtension)
		if !isSpec && !isDef {
			return nil
		}

		policyFullName := utills.FileNameWithoutExtension(fileName)
		policy := model.PolicyContainer{}
		if _, ok := apiProject.Policies[policyFullName]; ok {
			policy = apiProject.Policies[policyFullName]
		}

		if isSpec {
			// process policy specificationn
			spec := model.PolicySpecification{}
			if err := yaml.Unmarshal(fileContent, &spec); err != nil { // JSON is also a YAML, handled with gopkg.in/yaml.v2
				loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
					Message:   fmt.Sprintf("Error parsing content of policy specification %q: %s", fileName, err.Error()),
					Severity:  logging.MINOR,
					ErrorCode: 1221,
				})
				return err
			}
			policy.Specification = spec
			apiProject.Policies[policyFullName] = policy
		}
		if isDef {
			// process policy definition
			policy.Definition = model.PolicyDefinition{RawData: fileContent}
			apiProject.Policies[policyFullName] = policy
		}
	}
	return nil
}

func parseDeployments(data []byte) ([]model.Deployment, error) {
	// deployEnvsFromAPI represents deployments read from API Project
	deployEnvsFromAPI := &model.DeploymentEnvironments{}
	if err := yaml.Unmarshal(data, deployEnvsFromAPI); err != nil {
		loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error parsing content of deployment environments: %v", err.Error()),
			Severity:  logging.MAJOR,
			ErrorCode: 1219,
		})
		return nil, err
	}

	deployments := make([]model.Deployment, 0, len(deployEnvsFromAPI.Data))
	for _, deployFromAPI := range deployEnvsFromAPI.Data {
		defaultVhost, exists, err := config.GetDefaultVhost(deployFromAPI.DeploymentEnvironment)
		if err != nil {
			loggers.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error reading default vhost of environment %v: %v", deployFromAPI.DeploymentEnvironment, err.Error()),
				Severity:  logging.MINOR,
				ErrorCode: 1220,
			})
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
