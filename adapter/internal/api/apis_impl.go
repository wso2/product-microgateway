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
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/adapter/config"
	apiModel "github.com/wso2/adapter/internal/api/models"
	xds "github.com/wso2/adapter/internal/discovery/xds"
	mgw "github.com/wso2/adapter/internal/oasparser/model"
	"github.com/wso2/adapter/internal/oasparser/utills"
	"github.com/wso2/adapter/internal/tlsutils"
	"github.com/wso2/adapter/loggers"
)

// API Controller related constants
const (
	openAPIDir                 string = "Definitions"
	openAPIFilename            string = "swagger."
	apiYAMLFile                string = "api.yaml"
	deploymentsYAMLFile        string = "deployment_environments.yaml"
	apiJSONFile                string = "api.json"
	endpointCertDir            string = "Endpoint-certificates"
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
	BasicAuthSecurity          string = "BASIC"
)

// ProjectAPI contains the extracted from an API project zip
type ProjectAPI struct {
	APIJsn                     []byte
	Deployments                []Deployment
	SwaggerJsn                 []byte // TODO: (SuKSW) change to OpenAPIJsn
	UpstreamCerts              []byte
	APIType                    string
	APILifeCycleStatus         string
	ProductionEndpoint         string
	SandboxEndpoint            string
	SecurityScheme             []string
	endpointImplementationType string
	AuthHeader                 string
	OrganizationID             string
	EndpointSecurity           config.EndpointSecurity
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
		if strings.Contains(file.Name, deploymentsYAMLFile) {
			loggers.LoggerAPI.Debug("Setting deployments of API")
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occurred while reading the deployment environments: %v %v",
					file.Name, err.Error())
				return apiProject, err
			}
			deployments, err := parseDeployments(unzippedFileBytes)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occurred while parsing the deployment environments: %v %v",
					file.Name, err.Error())
			}
			apiProject.Deployments = deployments
		}
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
				return apiProject, errors.New("certificate Validation Error")
			}
			upstreamCerts = append(upstreamCerts, unzippedFileBytes...)
			upstreamCerts = append(upstreamCerts, newLineByteArray...)
		} else if (strings.Contains(file.Name, apiYAMLFile) || strings.Contains(file.Name, apiJSONFile)) &&
			!strings.Contains(file.Name, openAPIDir) {
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

			var apiObject config.APIJsonData

			unmarshalErr := json.Unmarshal(apiProject.APIJsn, &apiObject)
			if unmarshalErr != nil {
				loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml or api.json %v", unmarshalErr.Error())
				return apiProject, err
			}

			err = verifyMandatoryFields(apiObject)
			if err != nil {
				loggers.LoggerAPI.Errorf("%v", err)
				return apiProject, err
			}

			if apiObject.Data.EndpointImplementationType == inlineEndpointType {
				errmsg := "inline endpointImplementationType is not currently supported with WSO2 micro-gateway"
				loggers.LoggerAPI.Infof(errmsg)
				err = errors.New(errmsg)
				return apiProject, err
			}
			extractAPIInformation(&apiProject, apiObject)
		}
	}
	if apiProject.APIJsn == nil {
		// If no api.yaml file is included in the zip folder, return with error.
		err := errors.New("could not find api.yaml or api.json")
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

func verifyMandatoryFields(apiJSON config.APIJsonData) error {
	var errMsg string = ""
	var apiName string = apiJSON.Data.APIName
	var apiVersion string = apiJSON.Data.APIVersion

	if apiName == "" {
		apiName = "unknownAPIName"
		errMsg = "API Name "
	}

	if apiVersion == "" {
		apiVersion = "unknownAPIVersion"
		errMsg = errMsg + "API Version "
	}

	if apiJSON.Data.APIContext == "" {
		errMsg = errMsg + "API Context "
	}

	if apiJSON.Data.EndpointConfig.ProductionEndpoints.Endpoint == "" &&
		apiJSON.Data.EndpointConfig.SandBoxEndpoints.Endpoint == "" {
		errMsg = errMsg + "API production and sandbox endpoints "
	}

	if errMsg != "" {
		errMsg = errMsg + "fields cannot be empty for " + apiName + " " + apiVersion
		return errors.New(errMsg)
	}

	if strings.HasPrefix(apiJSON.Data.EndpointConfig.ProductionEndpoints.Endpoint, "/") ||
		strings.HasPrefix(apiJSON.Data.EndpointConfig.SandBoxEndpoints.Endpoint, "/") {
		errMsg = "relative urls are not supported for API production and sandbox endpoints"
		return errors.New(errMsg)
	}
	return nil
}

// ApplyAPIProjectFromAPIM accepts an apictl project (as a byte array), list of vhosts with respective environments
// and updates the xds servers based upon the content.
func ApplyAPIProjectFromAPIM(payload []byte, vhostToEnvsMap map[string][]string) error {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	apiInfo, err := parseAPIInfo(apiProject.APIJsn)
	if err != nil {
		return err
	}

	// vhostsToRemove contains vhosts and environments to undeploy
	vhostsToRemove := make(map[string][]string)

	// TODO: (renuka) optimize to update cache only once when all internal memory maps are updated
	for vhost, environments := range vhostToEnvsMap {
		// search for vhosts in the given environments
		for _, env := range environments {
			if existingVhost, exists := xds.GetVhostOfAPI(apiInfo.ID, env); exists {
				loggers.LoggerAPI.Debugf("API %v:%v with UUID \"%v\" already deployed to vhost: %v",
					apiInfo.Name, apiInfo.Version, apiInfo.ID, existingVhost)
				if vhost != existingVhost {
					loggers.LoggerAPI.Infof("Un-deploying API %v:%v with UUID \"%v\" which is already deployed to vhost: %v",
						apiInfo.Name, apiInfo.Version, apiInfo.ID, existingVhost)
					vhostsToRemove[existingVhost] = append(vhostsToRemove[existingVhost], env)
				}
			}
		}
		// first update the API for vhost
		updateAPI(vhost, apiInfo, apiProject, environments)
	}

	// undeploy APIs with other vhosts in the same gateway environment
	for vhost, environments := range vhostsToRemove {
		if vhost == "" {
			// ignore if vhost is empty, since it deletes all vhosts of API
			continue
		}
		if err := xds.DeleteAPIs(vhost, apiInfo.Name, apiInfo.Version, environments); err != nil {
			return err
		}
	}
	return nil
}

// ApplyAPIProjectInStandaloneMode is called by the rest implementation to differentiate
// between create and update using the override param
func ApplyAPIProjectInStandaloneMode(payload []byte, override *bool) error {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	apiInfo, err := parseAPIInfo(apiProject.APIJsn)
	if err != nil {
		return err
	}
	// TODO (renuka) when len of apiProject.deployments is 0, return err "nothing deployed" <- check
	var overrideValue bool
	if override == nil {
		overrideValue = false
	} else {
		overrideValue = *override
	}
	//TODO: force overwride
	if !overrideValue {
		// if the API already exists in the one of vhost, break deployment of the API
		exists := false
		for _, deployment := range apiProject.Deployments {
			if xds.IsAPIExist(deployment.DeploymentVhost, apiInfo.Name, apiInfo.Version) {
				exists = true
				break
			}
		}

		if exists {
			loggers.LoggerAPI.Infof("Error creating new API. API %v:%v already exists.",
				apiInfo.Name, apiInfo.Version)
			return errors.New(mgw.AlreadyExists)
		}
	}

	vhostToEnvsMap := make(map[string][]string)
	for _, environment := range apiProject.Deployments {
		vhostToEnvsMap[environment.DeploymentVhost] =
			append(vhostToEnvsMap[environment.DeploymentVhost], environment.DeploymentEnvironment)
	}

	// TODO: (renuka) optimize to update cache only once when all internal memory maps are updated
	for vhost, environments := range vhostToEnvsMap {
		updateAPI(vhost, apiInfo, apiProject, environments)
	}
	return nil
}

func updateAPI(vhost string, apiInfo ApictlProjectInfo, apiProject ProjectAPI, environments []string) {
	if len(environments) == 0 {
		environments = append(environments, config.DefaultGatewayName)
	}
	var apiContent config.APIContent
	apiContent.UUID = apiInfo.ID
	apiContent.VHost = vhost
	apiContent.Name = apiInfo.Name
	apiContent.Version = apiInfo.Version
	apiContent.APIType = apiProject.APIType
	apiContent.LifeCycleStatus = apiProject.APILifeCycleStatus
	apiContent.UpstreamCerts = apiProject.UpstreamCerts
	apiContent.Environments = environments
	apiContent.ProductionEndpoint = apiProject.ProductionEndpoint
	apiContent.SandboxEndpoint = apiProject.SandboxEndpoint
	apiContent.SecurityScheme = apiProject.SecurityScheme
	apiContent.AuthHeader = apiProject.AuthHeader
	apiContent.EndpointSecurity.Production.Enabled = apiProject.EndpointSecurity.Production.Enabled
	apiContent.EndpointSecurity.Production.Password = apiProject.EndpointSecurity.Production.Password
	apiContent.EndpointSecurity.Production.Username = apiProject.EndpointSecurity.Production.Username
	apiContent.EndpointSecurity.Production.SecurityType = apiProject.EndpointSecurity.Production.SecurityType
	apiContent.OrganizationID = apiProject.OrganizationID

	if apiProject.APIType == mgw.HTTP {
		apiContent.APIDefinition = apiProject.SwaggerJsn
	} else if apiProject.APIType == mgw.WS {
		apiContent.APIDefinition = apiProject.APIJsn
	}
	xds.UpdateAPI(apiContent)
}

func extractAPIInformation(apiProject *ProjectAPI, apiObject config.APIJsonData) {
	apiProject.APIType = strings.ToUpper(apiObject.Data.APIType)
	apiProject.APILifeCycleStatus = strings.ToUpper(apiObject.Data.LifeCycleStatus)

	apiProject.AuthHeader = apiObject.Data.AuthorizationHeader

	apiProject.SecurityScheme = apiObject.Data.SecurityScheme

	var apiHashValue string = generateHashValue(apiObject.Data.APIName, apiObject.Data.APIName)
	loggers.LoggerAPI.Infof("apiHashValue env value %v", apiHashValue)

	endpointConfig := apiObject.Data.EndpointConfig

	// production Endpoints set
	var productionEndpoint string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_prod_endpoint_0",
		endpointConfig.ProductionEndpoints.Endpoint)
	apiProject.ProductionEndpoint = productionEndpoint

	// sandbox Endpoints set
	var sandboxEndpoint string = resolveEnvValueForEndpointConfig("api_"+apiHashValue+"_sand_endpoint_0",
		endpointConfig.SandBoxEndpoints.Endpoint)
	apiProject.SandboxEndpoint = sandboxEndpoint

	// production Endpoint security
	prodEpSecurity, _ := retrieveEndPointSecurityInfo("api_"+apiHashValue,
		endpointConfig.EndpointSecurity.Production, "prod")

	// sandbox Endpoint security
	sandBoxEpSecurity, _ := retrieveEndPointSecurityInfo("api_"+apiHashValue,
		endpointConfig.EndpointSecurity.Sandbox, "sand")

	epSecurity := config.EndpointSecurity{
		SandBox:    sandBoxEpSecurity,
		Production: prodEpSecurity,
	}

	// organization ID would remain empty string if unassigned
	apiProject.OrganizationID = apiObject.Data.OrganizationID

	apiProject.EndpointSecurity = epSecurity
}

func generateHashValue(apiName string, apiVersion string) string {
	endpointConfigSHValue := sha1.New()
	endpointConfigSHValue.Write([]byte(apiName + ":" + apiVersion))
	return hex.EncodeToString(endpointConfigSHValue.Sum(nil)[:])
}

func resolveEnvValueForEndpointConfig(envKey string, defaultVal string) string {
	envValue, exists := os.LookupEnv(envKey)
	if exists {
		loggers.LoggerAPI.Infof("resolve env value %v", envValue)
		return envValue
	}
	return defaultVal
}

func retrieveEndPointSecurityInfo(value string, endPointSecurity config.EpSecurity,
	keyType string) (epSecurityInfo config.SecurityInfo, err error) {
	var username string
	var password string
	var securityType = endPointSecurity.Type

	if securityType != "" {
		if securityType == BasicAuthSecurity {
			username = resolveEnvValueForEndpointConfig(value+"_"+keyType+"_basic_username", endPointSecurity.Username)
			password = resolveEnvValueForEndpointConfig(value+"_"+keyType+"_basic_password", endPointSecurity.Password)

			epSecurityInfo.Username = username
			epSecurityInfo.Password = password
			epSecurityInfo.SecurityType = securityType
			epSecurityInfo.Enabled = endPointSecurity.Enabled
			return epSecurityInfo, nil
		}
		errMsg := securityType + " endpoint security type is not currently supported with" +
			"WSO2 micro-gateway"
		err = errors.New(errMsg)
		loggers.LoggerAPI.Error(errMsg)
	}
	return epSecurityInfo, err
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
