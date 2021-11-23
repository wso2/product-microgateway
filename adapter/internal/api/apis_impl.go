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
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	apiModel "github.com/wso2/product-microgateway/adapter/internal/api/models"
	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
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
	zipExt                     string = ".zip"
	apisArtifactDir            string = "apis"
)

// extractAPIProject accepts the API project as a zip file and returns the extracted content.
// The apictl project must be in zipped format.
// API type is decided by the type field in the api.yaml file.
func extractAPIProject(payload []byte) (apiProject mgw.ProjectAPI, err error) {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return apiProject, err
	}
	// TODO: (VirajSalaka) this won't support for distributed openAPI definition
	apiProject.UpstreamCerts = make(map[string][]byte)
	apiProject.EndpointCerts = make(map[string]string)
	for _, file := range zipReader.File {
		loggers.LoggerAPI.Debugf("File reading now: %v", file.Name)
		unzippedFileBytes, err := readZipFile(file)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error occured while reading the file : %v %v", file.Name, err.Error())
			return apiProject, err
		}
		err = apiProject.ProcessFilesInsideProject(unzippedFileBytes, file.Name)
		if err != nil {
			return apiProject, err
		}
	}
	err = apiProject.ValidateAPIType()
	if err != nil {
		return apiProject, err
	}
	return apiProject, nil
}

// ProcessMountedAPIProjects iterates through the api artifacts directory and apply the projects located within the directory.
func ProcessMountedAPIProjects() (err error) {
	conf, _ := config.ReadConfigs()
	apisDirName := filepath.FromSlash(conf.Adapter.ArtifactsDirectory + "/" + apisArtifactDir)
	files, err := ioutil.ReadDir((apisDirName))
	if err != nil {
		loggers.LoggerAPI.Error("Error while reading api artifacts during startup. ", err)
		// If Adapter Server which accepts apictl projects is closed then the adapter should not proceed.
		if !conf.Adapter.Server.Enabled {
			return err
		}
	}

	for _, apiProjectFile := range files {
		if apiProjectFile.IsDir() {
			apiProject := mgw.ProjectAPI{
				EndpointCerts: make(map[string]string),
				UpstreamCerts: make(map[string][]byte),
			}
			err = filepath.Walk(filepath.FromSlash(apisDirName+"/"+apiProjectFile.Name()), func(path string, info os.FileInfo, err error) error {

				if !info.IsDir() {
					fileContent, err := ioutil.ReadFile(path)
					if err != nil {
						return err
					}
					return apiProject.ProcessFilesInsideProject(fileContent, path)
				}
				return nil
			})
			if err != nil {
				loggers.LoggerAPI.Errorf("Error while processing api artifact - %s during startup : %v", apiProjectFile.Name(), err)
				continue
			}
			err = apiProject.ValidateAPIType()
			if err != nil {
				loggers.LoggerAPI.Errorf("Error while validation type of the api artifact - %s during startup : %v",
					apiProjectFile.Name(), err)
				continue
			}

			overrideValue := false
			err = validateAndUpdateXds(apiProject, &overrideValue)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error while processing api artifact - %s during startup : %v", apiProjectFile.Name(), err)
				continue
			}
			continue
		} else if !strings.HasSuffix(apiProjectFile.Name(), zipExt) {
			continue
		}
		data, err := ioutil.ReadFile(filepath.FromSlash(apisDirName + "/" + apiProjectFile.Name()))
		if err != nil {
			loggers.LoggerAPI.Errorf("Error while reading api artifact - %s during startup : %v", apiProjectFile.Name(), err)
			continue
		}

		// logger.LoggerMgw.Debugf("API artifact  - %s is read successfully.", file.Name())
		overrideAPIParam := false
		err = ApplyAPIProjectInStandaloneMode(data, &overrideAPIParam)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error while processing api artifact - %s during startup : %v", apiProjectFile.Name(), err)
			continue
		}
	}
	return nil
}

func validateAndUpdateXds(apiProject mgw.ProjectAPI, override *bool) (err error) {
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

// ApplyAPIProjectFromAPIM accepts an apictl project (as a byte array), list of vhosts with respective environments
// and updates the xds servers based upon the content.
func ApplyAPIProjectFromAPIM(payload []byte, vhostToEnvsMap map[string][]string, apiEnvs map[string]map[string]synchronizer.APIEnvProps) (deployedRevisionList []*notifier.DeployedAPIRevision, err error) {
	apiProject, err := extractAPIProject(payload)
	if err != nil {
		return nil, err
	}
	apiYaml := apiProject.APIYaml.Data
	if apiEnvProps, found := apiEnvs[apiProject.APIYaml.Data.ID]; found {
		loggers.LoggerAPI.Infof("Environment specific values found for the API %v ", apiProject.APIYaml.Data.ID)
		apiProject.APIEnvProps = apiEnvProps
	}

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
	return validateAndUpdateXds(apiProject, override)
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
