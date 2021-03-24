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
	"crypto/tls"
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/throttle"

	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/internal/auth"
	"github.com/wso2/micro-gw/internal/discovery/xds"
	"github.com/wso2/micro-gw/internal/tlsutils"

	logger "github.com/wso2/micro-gw/loggers"
)

const (
	keyTemplatesEndpoint string = "internal/data/v1/keyTemplates"
)

// FetchKeyTemplates pulls the startup Throttle Data required for custom and blocking condition
// based throttling. This request goes to traffic manager node.
func FetchKeyTemplates(c chan SyncAPIResponse) {
	logger.LoggerSync.Info("Fetching Key Templates from Traffic Manager.")
	respSyncAPI := SyncAPIResponse{}

	// Read configurations and derive the traffic manager endpoint details
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}

	ehConfigs := conf.ControlPlane.EventHub
	ehURL := ehConfigs.ServiceURL

	// If the traffic manager URL is configured with trailing slash
	if strings.HasSuffix(ehURL, "/") {
		ehURL += keyTemplatesEndpoint
	} else {
		ehURL += "/" + keyTemplatesEndpoint
	}
	logger.LoggerSync.Debugf("KeyTemplate endpoint URL %v: ", ehURL)

	ehUser := ehConfigs.Username
	ehPass := ehConfigs.Password
	basicAuth := "Basic " + auth.GetBasicAuth(ehUser, ehPass)

	// Check if TLS is enabled
	skipSSL := ehConfigs.SkipSSLVerification
	logger.LoggerSync.Debugf("Skip SSL Verification: %v", skipSSL)
	tr := &http.Transport{}
	if !skipSSL {
		caCertPool := tlsutils.GetTrustedCertPool()
		tr = &http.Transport{
			TLSClientConfig: &tls.Config{RootCAs: caCertPool},
		}
	} else {
		tr = &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
		}
	}

	client := &http.Client{
		Transport: tr,
	}

	req, err := http.NewRequest("GET", ehURL, nil)
	req.Header.Set(authorization, basicAuth)

	logger.LoggerSync.Debug("Sending the key template request to Traffic Manager")
	resp, err := client.Do(req)
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while retrieving Key Temaplate from Traffic manager: %v", err)
		respSyncAPI.Err = err
		respSyncAPI.Resp = nil
		c <- respSyncAPI
		return
	}

	// processing the response
	respBytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while reading the response: %v", err)
		respSyncAPI.Err = err
		respSyncAPI.ErrorCode = resp.StatusCode
		respSyncAPI.Resp = nil
		c <- respSyncAPI
		return
	}

	if resp.StatusCode == http.StatusOK {
		respSyncAPI.Err = nil
		respSyncAPI.Resp = respBytes
		c <- respSyncAPI
		return
	}
	// If the response is not successful, create a new error with the response and log it and return
	// Ex: for 401 scenarios, 403 scenarios.
	logger.LoggerSync.Errorf("Failure response: %v", string(respBytes))
	respSyncAPI.Err = errors.New(string(respBytes))
	respSyncAPI.Resp = nil
	respSyncAPI.ErrorCode = resp.StatusCode
	c <- respSyncAPI
	return
}

// pushKeyTemplates will update the ThrottleData xds snapshot
func pushKeyTemplates(tokens []string) {
	var templates []types.Resource
	t := &throttle.ThrottleData{
		KeyTemplates: tokens,
	}
	templates = append(templates, t)
	xds.UpdateEnforcerThrottleData(templates)
	logger.LoggerSync.Debug("Updated the snapshot for KeyTemplates")
}

// UpdateKeyTemplates pulls keytemplates from the traffic manager
// and updates the enforcer's xds cache
func UpdateKeyTemplates() {
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	c := make(chan SyncAPIResponse)
	go FetchKeyTemplates(c)
	for {
		data := <-c
		if data.Resp != nil {
			templates := []string{}
			err := json.Unmarshal(data.Resp, &templates)
			if err != nil {
				logger.LoggerSync.Errorf("Error occurred while unmarshalling key templates %v", err)
			}
			pushKeyTemplates(templates)
			break
		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
			logger.LoggerSync.Errorf("Error occurred when fetching key templates: %v", data.Err)
			break
		} else {
			logger.LoggerSync.Errorf("Unexpected error occurred while fetching key templates: %v", data.Err)
			go func() {
				// Retry fetching key templates from traffic manager
				retryInterval := conf.ControlPlane.EventHub.RetryInterval
				if retryInterval == 0 {
					retryInterval = 5
				}
				logger.LoggerSync.Debugf("Time Duration for retrying: %v", retryInterval*time.Second)
				time.Sleep(retryInterval * time.Second)
				logger.LoggerSync.Info("Retrying to get key templates from Traffic Manager.")
				FetchKeyTemplates(c)
			}()
		}
	}
}
