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

package deployer

import (
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
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

// API Controller related constants
const (
	apisArtifactDir string = "apis"
)

// ProcessMountedAPIProjects iterates through the api artifacts directory and apply the projects located within the directory.
func ProcessMountedAPIProjects() error {
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
			var apiProject model.ProjectAPI
			apiProject.UpstreamCerts = make(map[string][]byte)
			apiProject.EndpointCerts = make(map[string]string)
			err = filepath.Walk(filepath.FromSlash(apisDirName+"/"+apiProjectFile.Name()), func(path string, info os.FileInfo, err error) error {

				if !info.IsDir() {
					fileContent, err := ioutil.ReadFile(path)
					if err != nil {
						return err
					}
					err = ProcessFileInsideProject(&apiProject, fileContent, path)
					return err
				}
				return nil
			})
			if err != nil {
				loggers.LoggerAPI.Errorf("Error while processing api artifact - %s during startup : %v", apiProjectFile.Name(), err)
				continue
			}
			err = ValidateAPIType(&apiProject)
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

func validateAndUpdateXds(apiProject model.ProjectAPI, override *bool) (err error) {
	apiYaml := apiProject.APIYaml.Data
	organizationID := config.GetControlPlaneConnectedTenantDomain()

	// handle panic
	defer func() {
		if r := recover(); r != nil {
			loggers.LoggerAPI.Error("Recovered from panic. ", r)
			err = fmt.Errorf("%v:%v with UUID \"%v\"", apiYaml.Name, apiYaml.Version, apiYaml.ID)
		}
	}()

	var overrideValue bool
	if override == nil {
		overrideValue = false
	} else {
		overrideValue = *override
	}

	// when deployment-environments is missing in the API Project, definition we deploy to default
	// environment
	if apiProject.Deployments == nil {
		vhost, _, _ := config.GetDefaultVhost(config.DefaultGatewayName)
		deployment := model.Deployment{
			DisplayOnDevportal:    true,
			DeploymentEnvironment: config.DefaultGatewayName,
			DeploymentVhost:       vhost,
		}
		apiProject.Deployments = []model.Deployment{deployment}
	}

	//TODO: force overwride
	if !overrideValue {
		// if the API already exists in the one of vhost, break deployment of the API
		exists := false
		for _, deployment := range apiProject.Deployments {
			if xds.IsAPIExist(deployment.DeploymentVhost, apiYaml.ID, organizationID) {
				exists = true
				break
			}
		}

		if exists {
			loggers.LoggerAPI.Infof("Error creating new API. API %v:%v already exists.",
				apiYaml.Name, apiYaml.Version)
			return errors.New(constants.AlreadyExists)
		}
	}
	vhostToEnvsMap := make(map[string][]string)
	for _, environment := range apiProject.Deployments {
		vhostToEnvsMap[environment.DeploymentVhost] =
			append(vhostToEnvsMap[environment.DeploymentVhost], environment.DeploymentEnvironment)
	}

	// TODO: (renuka) optimize to update cache only once when all internal memory maps are updated
	for vhost, environments := range vhostToEnvsMap {
		_, err = xds.UpdateAPI(vhost, apiProject, environments)
		if err != nil {
			return
		}
	}
	return nil
}

// ApplyAPIProjectInStandaloneMode is called by the rest implementation to differentiate
// between create and update using the override param
func ApplyAPIProjectInStandaloneMode(payload []byte, override *bool) (err error) {
	apiProject, err := extractAPIProject(payload)

	if err != nil {
		return err
	}
	return validateAndUpdateXds(*apiProject, override)
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
