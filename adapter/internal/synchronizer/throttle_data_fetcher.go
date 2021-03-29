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

	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/internal/auth"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/throttle"
	"github.com/wso2/micro-gw/internal/discovery/xds"
	"github.com/wso2/micro-gw/internal/tlsutils"

	logger "github.com/wso2/micro-gw/loggers"
)

const (
	keyTemplatesEndpoint       string = "internal/data/v1/keyTemplates"
	blockingConditionsEndpoint string = "internal/data/v1/block"
)

// FetchThrottleData pulls the startup Throttle Data required for custom and blocking condition
// based throttling. This request goes to traffic manager node.
func FetchThrottleData(endpoint string, c chan SyncAPIResponse) {
	logger.LoggerSync.Infof("Fetching data from Traffic Manager. %v", endpoint)
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
		ehURL += endpoint
	} else {
		ehURL += "/" + endpoint
	}
	logger.LoggerSync.Debugf("Endpoint URL %v: ", ehURL)

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

	logger.LoggerSync.Debug("Sending the throttle data request to Traffic Manager")
	resp, err := client.Do(req)
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while fetching data from Traffic manager: %v. %v", endpoint, err)
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

// UpdateKeyTemplates pulls keytemplates from the traffic manager
// and updates the enforcer's xds cache
func UpdateKeyTemplates() {
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	c := make(chan SyncAPIResponse)
	go FetchThrottleData(keyTemplatesEndpoint, c)
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
				FetchThrottleData(keyTemplatesEndpoint, c)
			}()
		}
	}
}

// UpdateBlockingConditions pulls blocking conditions from the traffic manager
// and updates the enforcer's xds cache
func UpdateBlockingConditions() {
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	c := make(chan SyncAPIResponse)
	go FetchThrottleData(blockingConditionsEndpoint, c)
	for {
		data := <-c
		if data.Resp != nil {
			conditions := BlockConditions{}
			err := json.Unmarshal(data.Resp, &conditions)
			if err != nil {
				logger.LoggerSync.Errorf("Error occurred while unmarshalling blocking conditions %v", err)
			}
			pushBlockingConditions(conditions)
			break
		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
			logger.LoggerSync.Errorf("Error occurred when fetching blocking conditions: %v", data.Err)
			break
		} else {
			logger.LoggerSync.Errorf("Unexpected error occurred while fetching blocking conditions: %v", data.Err)
			go func() {
				// Retry fetching blocking conditions from traffic manager
				retryInterval := conf.ControlPlane.EventHub.RetryInterval
				if retryInterval == 0 {
					retryInterval = 5
				}
				logger.LoggerSync.Debugf("Time Duration for retrying: %v", retryInterval*time.Second)
				time.Sleep(retryInterval * time.Second)
				logger.LoggerSync.Info("Retrying to get blocking conditions from Traffic Manager.")
				FetchThrottleData(blockingConditionsEndpoint, c)
			}()
		}
	}
}

// pushKeyTemplates will update the ThrottleData xds snapshot with key templates
func pushKeyTemplates(tokens []string) {
	t := &throttle.ThrottleData{
		KeyTemplates: tokens,
	}
	xds.UpdateEnforcerThrottleData(t)
	logger.LoggerSync.Debug("Updated the snapshot for KeyTemplates")
}

// pushBlockingConditions will update the ThrottleData xds snapshot with blocking conditions
func pushBlockingConditions(conditions BlockConditions) {
	ips := []*throttle.IPCondition{}
	for _, c := range conditions.IP {
		ip := &throttle.IPCondition{
			Type:         c.Type,
			StartingIp:   c.StartingIP,
			EndingIp:     c.EndingIP,
			FixedIp:      c.FixedIP,
			TenantDomain: c.TenantDomain,
			Invert:       c.Invert,
			Id:           c.ID,
		}
		ips = append(ips, ip)
	}
	t := &throttle.ThrottleData{
		BlockingConditions: &throttle.BlockingConditions{
			Api:          conditions.API,
			Application:  conditions.Application,
			Subscription: conditions.Subscription,
			Custom:       conditions.Custom,
			User:         conditions.User,
			Ip:           ips,
		},
	}

	xds.UpdateEnforcerThrottleData(t)
	logger.LoggerSync.Debug("Updated the snapshot for BlockingConditions")
}
