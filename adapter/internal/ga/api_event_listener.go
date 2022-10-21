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
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
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
		if !event.IsDeployEvent {
			xds.DeleteAPIWithAPIMEvent(event.APIUUID, event.OrganizationUUID, configuredEnvs, event.RevisionUUID)
			continue
		}

		go synchronizer.FetchAPIsFromControlPlane(event.APIUUID, configuredEnvs)
	}
}
