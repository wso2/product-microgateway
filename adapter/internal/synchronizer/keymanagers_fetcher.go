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

/*
 * Package "synchronizer" contains artifacts relate to fetching APIs and
 * API related updates from the control plane event-hub.
 * This file contains functions to retrieve APIs and API updates.
 */

package synchronizer

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	eventhubTypes "github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

const (
	keyManagersEndpoint string = "internal/data/v1/keymanagers"
	retryCount          int    = 5
)

var retryAttempt int

// FetchKeyManagersOnStartUp pulls the Key managers calling to the API manager
// API Manager returns a .zip file as a response and this function
// returns a byte slice of that ZIP file.
func FetchKeyManagersOnStartUp(conf *config.Config) {
	logger.LoggerSync.Info("Fetching KeyManagers from Control Plane.")

	// Read configurations and derive the eventHub details
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	// Populate data from the config
	ehConfigs := conf.ControlPlane
	ehURL := ehConfigs.ServiceURL
	// If the eventHub URL is configured with trailing slash
	if strings.HasSuffix(ehURL, "/") {
		ehURL += keyManagersEndpoint
	} else {
		ehURL += "/" + keyManagersEndpoint
	}
	logger.LoggerSync.Infof("Fetching KeyManagers from the URL %v: ", ehURL)

	ehUname := ehConfigs.Username
	ehPass := ehConfigs.Password
	basicAuth := "Basic " + auth.GetBasicAuth(ehUname, ehPass)

	// Check if TLS is enabled
	skipSSL := ehConfigs.SkipSSLVerification

	// Create a HTTP request
	req, err := http.NewRequest("GET", ehURL, nil)
	if err != nil {
		logger.LoggerSync.Infof("Error happens %v", err)
	}

	// Setting authorization header
	req.Header.Set(authorization, basicAuth)

	// Make the request
	logger.LoggerSync.Debug("Sending the control plane request")
	resp, err := tlsutils.InvokeControlPlane(req, skipSSL)
	var errorMsg string
	if err != nil {
		errorMsg = "Error occurred while calling the REST API: " + keyManagersEndpoint
		go retryFetchData(conf, errorMsg, err)
		return
	}
	responseBytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		errorMsg = "Error occurred while reading the response received for: " + keyManagersEndpoint
		go retryFetchData(conf, errorMsg, err)
		return
	}

	if resp.StatusCode == http.StatusOK {
		var keyManagers []eventhubTypes.KeyManager
		err := json.Unmarshal(responseBytes, &keyManagers)
		if err != nil {
			logger.LoggerInternalMsg.Errorf("Error occurred while unmarshelling Key Managers event data %v", err)
			return
		}

		for _, kmConfig := range keyManagers {
			xds.KeyManagerList = append(xds.KeyManagerList, kmConfig)
		}
		xds.GenerateAndUpdateKeyManagerList()
	} else {
		errorMsg = "Failed to fetch data! " + keyManagersEndpoint + " responded with " +
			strconv.Itoa(resp.StatusCode)
		go retryFetchData(conf, errorMsg, err)
	}

	return
}

func retryFetchData(conf *config.Config, errorMessage string, err error) {
	logger.LoggerSync.Debugf("Time Duration for retrying: %v",
		conf.ControlPlane.RetryInterval*time.Second)
	time.Sleep(conf.ControlPlane.RetryInterval * time.Second)
	FetchKeyManagersOnStartUp(conf)
	retryAttempt++
	if retryAttempt >= retryCount {
		logger.LoggerSync.Errorf(errorMessage, err)
		return
	}
}
