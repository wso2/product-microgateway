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
	"fmt"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"

	xds "github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
)

// ApplyAPIProjectFromAPIM accepts an apictl project (as a byte array), list of vhosts with respective environments
// and updates the xds servers based upon the content.
func ApplyAPIProjectFromAPIM(
	payload []byte,
	vhostToEnvsMap map[string][]string,
	apiEnvs map[string]map[string]synchronizer.APIEnvProps,
) (deployedRevisionList []*notifier.DeployedAPIRevision, err error) {
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
