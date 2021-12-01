/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package ga

import (
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	eh "github.com/wso2/product-microgateway/adapter/internal/eventhub"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	msg "github.com/wso2/product-microgateway/adapter/internal/messaging"
	"github.com/wso2/product-microgateway/adapter/internal/synchronizer"
)

//handleAPIEventsFromGA handles the API events from GA that are coming through the channel
func handleAPIEventsFromGA(channel chan APIEvent) {
	for event := range channel {
		logger.LoggerGA.Infof("Received Event: %v", event)
		conf, _ := config.ReadConfigs()
		configuredEnvs := conf.ControlPlane.EnvironmentLabels
		if len(configuredEnvs) == 0 {
			configuredEnvs = append(configuredEnvs, config.DefaultGatewayName)
		}
		if event.IsDeployEvent {
			go synchronizer.FetchAPIsFromControlPlane(event.APIUUID, configuredEnvs)
		}
		for _, env := range configuredEnvs {
			if event.IsDeployEvent {
				if _, ok := eh.APIListMap[env]; ok {
					apiListOfEnv := eh.APIListMap[env].List
					for i := range apiListOfEnv {
						// If API is already found, it is a new revision deployment.
						// Subscription relates details of an API does not change between new revisions
						if event.APIUUID == apiListOfEnv[i].UUID {
							logger.LoggerGA.Debugf("APIList for API UUID: %s is not updated as it already "+
								"exists", event.APIUUID)
							return
						}
					}
					queryParamMap := make(map[string]string, 2)
					queryParamMap[eh.GatewayLabelParam] = env
					queryParamMap[eh.APIUUIDParam] = event.APIUUID
					logger.LoggerGA.Infof("Invoking the apis service endpoint")
					go eh.InvokeService(eh.ApisEndpoint, eh.APIListMap[env], queryParamMap,
						eh.APIListChannel, 0)
				}
			} else if !event.IsDeployEvent {
				if _, ok := eh.APIListMap[env]; ok {
					apiListOfEnv := eh.APIListMap[env].List
					for i := range apiListOfEnv {
						if event.APIUUID == apiListOfEnv[i].UUID {
							xds.DeleteAPIWithAPIMEvent(event.APIUUID, apiListOfEnv[i].Name, apiListOfEnv[i].Version,
								configuredEnvs, event.OrganizationUUID, event.RevisionUUID)
							logger.LoggerGA.Debugf("Removed API from router")
							eh.APIListMap[env].List = msg.DeleteAPIFromList(apiListOfEnv, i, event.APIUUID, env)
							xds.UpdateEnforcerAPIList(env, xds.MarshalAPIList(eh.APIListMap[env]))
							break
						}
					}
				}
			}
		}
	}
}
