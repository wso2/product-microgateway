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
 *
 */

// Package api contains the REST API implementation for the adapter
package api

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/micro-gw/config"
	apiModel "github.com/wso2/micro-gw/internal/api/models"
	xds "github.com/wso2/micro-gw/internal/discovery/xds"
	mgw "github.com/wso2/micro-gw/internal/oasparser/model"
	"github.com/wso2/micro-gw/internal/oasparser/utills"
	"github.com/wso2/micro-gw/internal/tlsutils"
	"github.com/wso2/micro-gw/loggers"
)

// API Controller related constants
const (
	openAPIDir                 string = "Definitions"
	openAPIFilename            string = "swagger."
	apiYAMLFile                string = "api.yaml"
	apiJSONFile                string = "api.json"
	endpointCertDir            string = "Endpoint-certificates"
	crtExtension               string = ".crt"
	pemExtension               string = ".pem"
	defaultEnv                 string = "Production and Sandbox" //Todo: (SuKSW) update to `default` once APIM side changes.
	defaultVHost               string = "default"
	apiTypeFilterKey           string = "type"
	apiTypeYamlKey             string = "type"
	lifeCycleStatus            string = "lifeCycleStatus"
	securityScheme             string = "securityScheme"
	endpointImplementationType string = "endpointImplementationType"
)

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIJsn                     []byte
	SwaggerJsn                 []byte // TODO: (SuKSW) change to OpenAPIJsn
	UpstreamCerts              []byte
	APIType                    string
	APILifeCycleStatus         string
	ProductionEndpoint         string
	SandboxEndpoint            string
	SecurityScheme             []string
	endpointImplementationType string
}

// extractAPIProject accepts the API project as a zip file and returns the extracted content
// The apictl project must be in zipped format. And all the extensions should be defined with in the openAPI
// definition as only swagger.yaml is taken into consideration here. For websocket APIs api.yaml is taken into
// consideration. API type is decided by the type field in the api.yaml file.
func extractAPIProject(payload []byte) (apiProject ProjectAPI, err error) {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	newLineByteArray := []byte("\n")
	var upstreamCerts []byte

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return apiProject, err
	}
	// TODO: (VirajSalaka) this won't support for distributed openAPI definition
	for _, file := range zipReader.File {
		loggers.LoggerAPI.Debugf("File reading now: %v", file.Name)
		if strings.Contains(file.Name, openAPIDir+string(os.PathSeparator)+openAPIFilename) {
			loggers.LoggerAPI.Debugf("openAPI file : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the openapi file. %v", err.Error())
				continue
			}
			swaggerJsn, conversionErr := utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting api file to json: %v", conversionErr.Error())
				return apiProject, conversionErr
			}
			apiProject.SwaggerJsn = swaggerJsn
			apiProject.APIType = mgw.HTTP
		} else if strings.Contains(file.Name, endpointCertDir+string(os.PathSeparator)) &&
			(strings.HasSuffix(file.Name, crtExtension) || strings.HasSuffix(file.Name, pemExtension)) {
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the endpoint certificate : %v, %v", file.Name, err.Error())
				continue
			}
			if !tlsutils.IsPublicCertificate(unzippedFileBytes) {
				loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", file.Name)
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return apiProject, errors.New("Certificate Validation Error")
			}
			upstreamCerts = append(upstreamCerts, unzippedFileBytes...)
			upstreamCerts = append(upstreamCerts, newLineByteArray...)
		} else if strings.Contains(file.Name, apiYAMLFile) || strings.Contains(file.Name, apiJSONFile) {
			loggers.LoggerAPI.Debugf("fileName : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the api definition file : %v %v", file.Name, err.Error())
				return apiProject, err
			}
			apiJsn, conversionErr := utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error occured converting api file to json: %v", conversionErr.Error())
				return apiProject, conversionErr
			}

			apiProject.APIJsn = apiJsn
			extractAPIInformation(apiProject)
			if apiProject.endpointImplementationType == "INLINE" {
				errMsg := "Inline endpointImplementationType is not currently supported with WSO2 micro-gateway"
				loggers.LoggerAPI.Infof(errMsg)
				err = errors.New(errMsg)
				return apiProject, err
			}
		}
	}
	if apiProject.APIJsn == nil {
		// TODO : (LahiruUdayanga) Handle the default behaviour after when the APIDeployTestCase test is fixed.
		// If no api.yaml file is included in the zip folder, return with error.
		err := errors.New("Could not find api.yaml or api.json")
		loggers.LoggerAPI.Errorf("Error occured while reading the api type : %v", err.Error())
		return apiProject, err
	} else if apiProject.APIType != mgw.HTTP && apiProject.APIType != mgw.WS {
		errMsg := "API type is not currently supported with WSO2 micro-gateway"
		loggers.LoggerAPI.Infof(errMsg)
		err = errors.New(errMsg)
		return apiProject, err
	}
	apiProject.UpstreamCerts = upstreamCerts
	return apiProject, nil
}

// ApplyAPIProject accepts an apictl project (as a byte array) and updates the xds servers based upon the
// content.
func ApplyAPIProject(payload []byte, environments []string) error {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	name, version, err := getAPINameAndVersion(apiProject.APIJsn)
	if err != nil {
		return err
	}
	updateAPI(name, version, apiProject, environments)
	return nil
}

// ApplyAPIProjectWithOverwrite is called by the rest implementation to differentiate
// between create and update using the override param
func ApplyAPIProjectWithOverwrite(payload []byte, environments []string, override *bool) error {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	name, version, err := getAPINameAndVersion(apiProject.APIJsn)
	if err != nil {
		return err
	}
	var overrideValue bool
	if override == nil {
		overrideValue = false
	} else {
		overrideValue = *override
	}
	//TODO: force overwride
	exists := xds.IsAPIExist(defaultVHost, name, version) // TODO: (SuKSW) update once vhost feature added
	if !overrideValue && exists {
		loggers.LoggerAPI.Infof("Error creating new API. API %v:%v already exists.", name, version)
		return errors.New(mgw.AlreadyExists)
	}
	updateAPI(name, version, apiProject, environments)
	return nil
}

func updateAPI(name, version string, apiProject ProjectAPI, environments []string) {
	if len(environments) == 0 {
		environments = append(environments, defaultEnv)
	}
	var apiContent config.APIContent
	apiContent.VHost = defaultVHost
	apiContent.Name = name
	apiContent.Version = version
	apiContent.APIType = apiProject.APIType
	apiContent.LifeCycleStatus = apiProject.APILifeCycleStatus
	apiContent.UpstreamCerts = apiProject.UpstreamCerts
	apiContent.Environments = environments
	apiContent.ProductionEndpoint = apiProject.ProductionEndpoint
	apiContent.SandboxEndpoint = apiProject.SandboxEndpoint
	apiContent.SecurityScheme = apiProject.SecurityScheme

	if apiProject.APIType == mgw.HTTP {
		apiContent.APIDefinition = apiProject.SwaggerJsn
		xds.UpdateAPI(apiContent)
	} else if apiProject.APIType == mgw.WS {
		apiContent.APIDefinition = apiProject.APIJsn
		xds.UpdateAPI(apiContent)
	}
}

func extractAPIInformation(apiProject ProjectAPI) {
	var apiDef map[string]interface{}
	unmarshalErr := json.Unmarshal(apiProject.APIJsn, &apiDef)
	if unmarshalErr != nil {
		loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml %v", unmarshalErr.Error())
	}
	data := apiDef["data"].(map[string]interface{})
	apiProject.APIType = strings.ToUpper(data[apiTypeYamlKey].(string))
	apiProject.APILifeCycleStatus = strings.ToUpper(data[lifeCycleStatus].(string))

	var securitySchemesTypes []string = nil
	if data[securityScheme] != nil {
		securitySchemes := data[securityScheme].([]interface{})
		for _, scheme := range securitySchemes {
			securitySchemesTypes = append(securitySchemesTypes, scheme.(string))
		}
	}
	apiProject.SecurityScheme = securitySchemesTypes
	loggers.LoggerAPI.Infof("apiProject.SecurityScheme %v", apiProject.SecurityScheme)

	apiProject.endpointImplementationType = data[endpointImplementationType].(string)

	endpointConfig := data["endpointConfig"].(map[string]interface{})
	if endpointConfig["sandbox_endpoints"] != nil {
		sandboxEndpoints := endpointConfig["sandbox_endpoints"].(map[string]interface{})
		apiProject.SandboxEndpoint = sandboxEndpoints["url"].(string)
	}
	if endpointConfig["production_endpoints"] != nil {
		productionEndpoints := endpointConfig["production_endpoints"].(map[string]interface{})
		apiProject.ProductionEndpoint = productionEndpoints["url"].(string)
	}
}

// DeleteAPI calls the DeleteAPI method in xds_server.go
func DeleteAPI(vhost *string, apiName string, version string) error {
	if vhost == nil || *vhost == "" {
		vhostValue := defaultVHost
		vhost = &vhostValue
	}
	return xds.DeleteAPI(*vhost, apiName, version)
}

// ListApis calls the ListApis method in xds_server.go
func ListApis(query *string, limit *int64) *apiModel.APIMeta {
	var apiType string
	if query != nil {
		queryPair := strings.Split(*query, ":")
		if queryPair[0] == apiTypeFilterKey {
			apiType = strings.ToUpper(queryPair[1])
			return xds.ListApis(apiType, limit)
		}
	}
	return xds.ListApis("", limit)
}

func readZipFile(zf *zip.File) ([]byte, error) {
	f, err := zf.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ioutil.ReadAll(f)
}

func getAPINameAndVersion(apiJsn []byte) (name string, version string, err error) {
	var apiDef map[string]interface{}
	err = json.Unmarshal(apiJsn, &apiDef)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml %v", err.Error())
		return "", "", err
	}
	data := apiDef["data"].(map[string]interface{})
	name = data["name"].(string)
	version = data["version"].(string)
	return name, version, nil
}
