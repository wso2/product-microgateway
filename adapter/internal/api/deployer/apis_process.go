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

// Package deployer - file apis_process.go includes methods used during both modes; standalone and with API-M
package deployer

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
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
)

// extractAPIProject accepts the API project as a zip file and returns the extracted content.
// The API project must be in zipped format.
// API type is decided by the type field in the api.yaml file.
func extractAPIProject(payload []byte) (apiProject model.ProjectAPI, err error) {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occurred while unzipping the apictl project. Error: %v", err.Error())
		return apiProject, err
	}
	// TODO: (VirajSalaka) this won't support for distributed openAPI definition
	for _, file := range zipReader.File {
		loggers.LoggerAPI.Debugf("File reading now: %v", file.Name)
		unzippedFileBytes, err := readZipFile(file)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occurred while reading the file : %v %v", file.Name, err.Error())
			return apiProject, err
		}
		apiProject, err = ProcessFilesInsideProject(unzippedFileBytes, file.Name)
		if err != nil {
			return apiProject, err
		}
	}
	err = ValidateAPIType(&apiProject)
	if err != nil {
		return apiProject, err
	}
	return apiProject, nil
}

func readZipFile(zf *zip.File) ([]byte, error) {
	f, err := zf.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ioutil.ReadAll(f)
}

// ProcessFilesInsideProject process single file inside API Project and update the apiProject instance appropriately.
func ProcessFilesInsideProject(fileContent []byte, fileName string) (apiProject model.ProjectAPI, err error) {
	apiProject.UpstreamCerts = make(map[string][]byte)
	apiProject.EndpointCerts = make(map[string]string)
	newLineByteArray := []byte("\n")

	// Deployment file
	if strings.Contains(fileName, deploymentsYAMLFile) {
		loggers.LoggerAPI.Debug("Setting deployments of API")
		deployments, err := parseDeployments(fileContent)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occurred while parsing the deployment environments: %v %v",
				fileName, err.Error())
		}
		apiProject.Deployments = deployments

		// API definition
	} else if strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+openAPIFilename) ||
		strings.Contains(fileName, openAPIDir+string(os.PathSeparator)+asyncAPIFilename) {

		loggers.LoggerAPI.Debugf("openAPI file : %v", fileName)
		swaggerJsn, conversionErr := utills.ToJSON(fileContent)
		if conversionErr != nil {
			loggers.LoggerAPI.Errorf("Error converting api file to json: %v", conversionErr.Error())
			return apiProject, conversionErr
		}
		apiProject.APIDefinition = swaggerJsn

		// Interceptor certs
	} else if strings.Contains(fileName, interceptorCertDir+string(os.PathSeparator)) &&
		(strings.HasSuffix(fileName, crtExtension) || strings.HasSuffix(fileName, pemExtension)) {

		if !tlsutils.IsPublicCertificate(fileContent) {
			loggers.LoggerAPI.Errorf("Provided interceptor certificate: %v is not in the PEM file format. ", fileName)
			return apiProject, errors.New("interceptor certificate Validation Error")
		}
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, fileContent...)
		apiProject.InterceptorCerts = append(apiProject.InterceptorCerts, newLineByteArray...)

		// Endpoint certs
	} else if strings.Contains(fileName, endpointCertDir+string(os.PathSeparator)) {

		if strings.Contains(fileName, endpointCertFile) {
			epCertJSON, conversionErr := utills.ToJSON(fileContent)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting %v file to json: %v", fileName, conversionErr.Error())
				return apiProject, conversionErr
			}
			endpointCertificates := &model.EndpointCertificatesDetails{}
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
				return apiProject, errors.New("certificate Validation Error")
			}

			if fileNameArray := strings.Split(fileName, string(os.PathSeparator)); len(fileNameArray) > 0 {
				certFileName := fileNameArray[len(fileNameArray)-1]
				apiProject.UpstreamCerts[certFileName] = fileContent
			}
		}

		// api.yaml or api.json
	} else if (strings.Contains(fileName, apiYAMLFile) || strings.Contains(fileName, apiJSONFile)) &&
		!strings.Contains(fileName, openAPIDir) {

		apiYaml, err := ExtractAPIYaml(fileContent)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error while reading %v", fileName)
			return apiProject, errors.New("Error while reading api.yaml or api.json")
		}
		apiProject.APIYaml = apiYaml
	}
	return apiProject, nil
}

// ExtractAPIYaml returns an APIYaml struct after reading and validating api.yaml or api.json
func ExtractAPIYaml(fileContent []byte) (apiYaml model.APIYaml, err error) {
	apiJsn, err := utills.ToJSON(fileContent)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occurred converting api file to json: %v", err.Error())
		return apiYaml, err
	}

	err = json.Unmarshal(apiJsn, &apiYaml)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occurred while parsing api.yaml or api.json %v", err.Error())
		return apiYaml, err
	}

	apiYaml.PopulateEndpointsInfo()
	err = apiYaml.VerifyMandatoryFields()
	if err != nil {
		loggers.LoggerAPI.Errorf("%v", err)
		return apiYaml, err
	}

	if apiYaml.Data.EndpointImplementationType == constants.InlineEndpointType {
		errmsg := "inline endpointImplementationType is not currently supported with Choreo Connect"
		loggers.LoggerAPI.Warnf(errmsg)
		err = errors.New(errmsg)
		return apiYaml, err
	}
	return apiYaml, nil
}

func parseDeployments(data []byte) ([]model.Deployment, error) {
	// deployEnvsFromAPI represents deployments read from API Project
	deployEnvsFromAPI := &model.DeploymentEnvironments{}
	if err := yaml.Unmarshal(data, deployEnvsFromAPI); err != nil {
		loggers.LoggerAPI.Errorf("Error parsing content of deployment environments: %v", err.Error())
		return nil, err
	}

	deployments := make([]model.Deployment, 0, len(deployEnvsFromAPI.Data))
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

// ValidateAPIType checks if the apiProject is properly assigned with the type.
func ValidateAPIType(apiProject *model.ProjectAPI) (err error) {
	apiType := strings.ToUpper(apiProject.APIYaml.Type)
	if apiType == "" {
		// If no api.yaml file is included in the zip folder, return with error.
		err = errors.New("could not find api.yaml or api.json")
		return err
	} else if apiType != constants.HTTP && apiType != constants.WS && apiType != constants.WEBHOOK {
		errMsg := "API type is not currently supported with Choreo Connect"
		err = errors.New(errMsg)
		return err
	}
	return nil
}
