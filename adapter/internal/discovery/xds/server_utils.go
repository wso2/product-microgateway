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

package xds

import (
	"github.com/wso2/adapter/config"
	logger "github.com/wso2/adapter/loggers"
)

// getEnvironmentsToBeDeleted returns an slice of environments APIs to be u-deployed from
// by considering existing environments list and environments that APIs are wished to be un-deployed
func getEnvironmentsToBeDeleted(existingEnvs, deleteEnvs []string) (toBeDel []string, toBeKept []string) {
	toBeDel = make([]string, 0, len(deleteEnvs))
	toBeKept = make([]string, 0, len(deleteEnvs))

	// if deleteEnvs is empty (deleteEnvs wished to be deleted), delete all environments
	if len(deleteEnvs) == 0 {
		return existingEnvs, []string{}
	}
	// otherwise delete env if it wished to
	for _, existingEnv := range existingEnvs {
		if arrayContains(deleteEnvs, existingEnv) {
			toBeDel = append(toBeDel, existingEnv)
		} else {
			toBeKept = append(toBeKept, existingEnv)
		}
	}
	return
}

func updateVhostInternalMaps(apiContent config.APIContent, gwEnvs []string) {
	// update internal map: apiToVhostsMap
	apiIdentifierWithoutVhost := GenerateIdentifierForAPIWithoutVhost(apiContent.Name, apiContent.Version)
	if _, ok := apiToVhostsMap[apiIdentifierWithoutVhost]; ok {
		apiToVhostsMap[apiIdentifierWithoutVhost][apiContent.VHost] = void
	} else {
		apiToVhostsMap[apiIdentifierWithoutVhost] = map[string]struct{}{apiContent.VHost: void}
	}

	// update internal map: apiUUIDToGatewayToVhosts
	logger.LoggerXds.Debugf("Updating Vhost of API with UUID \"%v\" as %v.", apiContent.UUID, apiContent.VHost)
	var envToVhostMap map[string]string
	if existingMap, ok := apiUUIDToGatewayToVhosts[apiContent.UUID]; ok {
		logger.LoggerXds.Debugf("API with UUID \"%v\" already exist in vhosts internal map.", apiContent.UUID)
		envToVhostMap = existingMap
	} else {
		logger.LoggerXds.Debugf("API with UUID \"%v\" not exist in vhosts internal map and create new entry.",
			apiContent.UUID)
		envToVhostMap = make(map[string]string)
	}

	// if a vhost is already exists it is replaced
	// only one vhost is supported for environment
	// this map is only used for un-deploying APIs form APIM
	for _, env := range gwEnvs {
		envToVhostMap[env] = apiContent.VHost
	}
	apiUUIDToGatewayToVhosts[apiContent.UUID] = envToVhostMap
}
