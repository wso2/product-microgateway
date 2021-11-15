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

package adapter

import (
	"time"

	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

// GetAPIs function calls the FetchAPIs() with relevant environment labels defined in the config.
func GetAPIs(c chan synchronizer.SyncAPIResponse, id *string, serviceURL string, userName string, password string,
	envs []string, skipSSL bool, truststoreLocation string, endpoint string, sendType bool, apiUUIDList []string, requestTimeOut time.Duration) {
	if len(envs) > 0 {
		// If the envrionment labels are present, call the controle plane with labels.
		logger.LoggerAdapter.Debugf("Environments label present: %v", envs)
		go synchronizer.FetchAPIs(id, envs, c, serviceURL, userName, password, skipSSL, truststoreLocation,
			endpoint, sendType, apiUUIDList, requestTimeOut)
	} else {
		// If the environments are not give, fetch the APIs from default envrionment
		logger.LoggerAdapter.Debug("Environments label  NOT present. Hence adding \"default\"")
		envs = append(envs, "default")
		go synchronizer.FetchAPIs(id, nil, c, serviceURL, userName, password, skipSSL, truststoreLocation,
			endpoint, sendType, apiUUIDList, requestTimeOut)
	}
}
