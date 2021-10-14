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
	"fmt"
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	apiModel "github.com/wso2/product-microgateway/adapter/internal/api/models"
	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

// API Controller related constants
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
)

// extractAPIProject accepts the API project as a zip file and returns the extracted content.
// The apictl project must be in zipped format.
// API type is decided by the type field in the api.yaml file.
func extractAPIProject(payload []byte) (apiProject mgw.ProjectAPI, err error) {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	newLineByteArray := []byte("\n")
	var upstreamCerts []byte
	var interceptorCerts []byte

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
			apiProject.OpenAPIJsn = swaggerJsn
			apiProject.APIType = mgw.HTTP
		} else if strings.Contains(file.Name, interceptorCertDir+string(os.PathSeparator)) &&
			(strings.HasSuffix(file.Name, crtExtension) || strings.HasSuffix(file.Name, pemExtension)) {
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the intercept endpoint certificate : %v, %v",
					file.Name, err.Error())
				continue
			}
			if !tlsutils.IsPublicCertificate(unzippedFileBytes) {
				loggers.LoggerAPI.Errorf("Provided interceptor certificate: %v is not in the PEM file format. ", file.Name)
				return apiProject, errors.New("interceptor certificate Validation Error")
			}
			interceptorCerts = append(interceptorCerts, unzippedFileBytes...)
			interceptorCerts = append(interceptorCerts, newLineByteArray...)
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
			var apiYaml mgw.APIYaml
			err = json.Unmarshal(apiJsn, &apiYaml)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml or api.json %v", err.Error())
				return apiProject, err
			}
			apiYaml = mgw.PopulateEndpointsInfo(apiYaml)

			err = mgw.VerifyMandatoryFields(apiYaml)
			if err != nil {
				loggers.LoggerAPI.Errorf("%v", err)
				return apiProject, err
			}

			if apiYaml.Data.EndpointImplementationType == inlineEndpointType {
				errmsg := "inline endpointImplementationType is not currently supported with Choreo Connect"
				loggers.LoggerAPI.Warnf(errmsg)
				err = errors.New(errmsg)
				return apiProject, err
			}
			apiProject.APIYaml = apiYaml
			mgw.ExtractAPIInformation(&apiProject, apiYaml)
		}
	}
	if apiProject.APIYaml.Type == "" {
		// If no api.yaml file is included in the zip folder, return with error.
		err := errors.New("could not find api.yaml or api.json")
		loggers.LoggerAPI.Errorf("Error occured while reading the api type : %v", err.Error())
		return apiProject, err
	} else if apiProject.APIType != mgw.HTTP && apiProject.APIType != mgw.WS && apiProject.APIType != mgw.WEBHOOK {
		errMsg := "API type is not currently supported with Choreo Connect"
		loggers.LoggerAPI.Warnf(errMsg)
		err = errors.New(errMsg)
		return apiProject, err
	}
	apiProject.UpstreamCerts = upstreamCerts
	apiProject.InterceptorCerts = interceptorCerts
	return apiProject, nil
}

// ApplyAPIProjectFromAPIM accepts an apictl project (as a byte array), list of vhosts with respective environments
// and updates the xds servers based upon the content.
func ApplyAPIProjectFromAPIM(payload []byte, vhostToEnvsMap map[string][]string) (deployedRevisionList []*notifier.DeployedAPIRevision, err error) {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return nil, err
	}
	apiYaml := apiProject.APIYaml.Data

	// handle panic
	defer func() {
		if r := recover(); r != nil {
			loggers.LoggerAPI.Error("Recovered from panic. ", r)
			err = fmt.Errorf("%v:%v with UUID \"%v\"", apiYaml.Name, apiYaml.Version, apiYaml.ID)
		}
	}()

	if apiProject.OrganizationID == "" {
		apiProject.OrganizationID = config.GetControlPlaneConnectedTenantDomain()
	}
	loggers.LoggerAPI.Infof("Deploying api %s:%s in Organization %s", apiYaml.Name, apiYaml.Version, apiProject.OrganizationID)

	// vhostsToRemove contains vhosts and environments to undeploy
	vhostsToRemove := make(map[string][]string)

	// TODO: (renuka) optimize to update cache only once when all internal memory maps are updated
	for vhost, environments := range vhostToEnvsMap {
		// search for vhosts in the given environments
		for _, env := range environments {
			if existingVhost, exists := xds.GetVhostOfAPI(apiYaml.ID, env); exists {
				loggers.LoggerAPI.Infof("API %v:%v with UUID \"%v\" already deployed to vhost: %v",
					apiYaml.Name, apiYaml.Version, apiYaml.ID, existingVhost)
				if vhost != existingVhost {
					loggers.LoggerAPI.Infof("Un-deploying API %v:%v with UUID \"%v\" which is already deployed to vhost: %v",
						apiYaml.Name, apiYaml.Version, apiYaml.ID, existingVhost)
					vhostsToRemove[existingVhost] = append(vhostsToRemove[existingVhost], env)
				}
			}
		}

		// allEnvironments represent all the environments the API should be deployed
		allEnvironments := xds.GetAllEnvironments(apiYaml.ID, vhost, environments)
		loggers.LoggerAPI.Debugf("Update all environments (%v) of API %v %v:%v with UUID \"%v\".",
			allEnvironments, vhost, apiYaml.Name, apiYaml.Version, apiYaml.ID)
		// first update the API for vhost
		deployedRevision, err := xds.UpdateAPI(vhost, apiProject, allEnvironments)
		if err != nil {
			return deployedRevisionList, fmt.Errorf("%v:%v with UUID \"%v\"", apiYaml.Name, apiYaml.Version, apiYaml.ID)
		}
		if deployedRevision != nil {
			deployedRevisionList = append(deployedRevisionList, deployedRevision)
		}
	}

	// undeploy APIs with other vhosts in the same gateway environment
	for vhost, environments := range vhostsToRemove {
		if vhost == "" {
			// ignore if vhost is empty, since it deletes all vhosts of API
			continue
		}
		if err := xds.DeleteAPIsWithUUID(vhost, apiYaml.ID, environments, apiProject.OrganizationID); err != nil {
			return deployedRevisionList, err
		}
	}
	return deployedRevisionList, nil
}

// ApplyAPIProjectInStandaloneMode is called by the rest implementation to differentiate
// between create and update using the override param
func ApplyAPIProjectInStandaloneMode(payload []byte, override *bool) (err error) {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	apiYaml := apiProject.APIYaml.Data
	apiProject.OrganizationID = config.GetControlPlaneConnectedTenantDomain()

	// handle panic
	defer func() {
		if r := recover(); r != nil {
			loggers.LoggerAPI.Error("Recovered from panic. ", r)
			err = fmt.Errorf("%v:%v with UUID \"%v\"", apiYaml.Name, apiYaml.Version, apiYaml.ID)
		}
	}()

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
			if xds.IsAPIExist(deployment.DeploymentVhost, apiYaml.ID, apiProject.OrganizationID) {
				exists = true
				break
			}
		}

		if exists {
			loggers.LoggerAPI.Infof("Error creating new API. API %v:%v already exists.",
				apiYaml.Name, apiYaml.Version)
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
		_, err := xds.UpdateAPI(vhost, apiProject, environments)
		if err != nil {
			return err
		}
	}
	return nil
}

// ListApis calls the ListApis method in xds_server.go
func ListApis(query *string, limit *int64, organizationID string) *apiModel.APIMeta {
	var apiType string
	if query != nil {
		queryPair := strings.Split(*query, ":")
		if queryPair[0] == apiTypeFilterKey {
			apiType = strings.ToUpper(queryPair[1])
			return xds.ListApis(apiType, organizationID, limit)
		}
	}
	return xds.ListApis("", organizationID, limit)
}

func readZipFile(zf *zip.File) ([]byte, error) {
	f, err := zf.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ioutil.ReadAll(f)
}
