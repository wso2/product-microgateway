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
 * This file contains functions to retrieve revoked tokens.
 */

package synchronizer

import (
	"crypto/tls"
	"encoding/json"
	"errors"
	"io/ioutil"
	"net/http"
	"strings"

	keymgt "github.com/wso2/micro-gw/api/wso2/discovery/keymgt"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/auth"
	"github.com/wso2/micro-gw/pkg/tlsutils"
)

const (
	revokeEndpoint string = "internal/data/v1/revokedjwt"
)

func RetrieveTokens(c chan SyncAPIResponse) {
	respSyncAPI := SyncAPIResponse{}

	// Read configurations and derive the eventHub details
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	// Populate data from the config
	ehConfigs := conf.ControlPlane.EventHub
	ehURL := ehConfigs.ServiceURL
	// If the eventHub URL is configured with trailing slash
	if strings.HasSuffix(ehURL, "/") {
		ehURL += revokeEndpoint
	} else {
		ehURL += "/" + revokeEndpoint
	}
	logger.LoggerSync.Debugf("Fetching APIs from the URL %v: ", ehURL)

	ehUname := ehConfigs.Username
	ehPass := ehConfigs.Password
	basicAuth := "Basic " + auth.GetBasicAuth(ehUname, ehPass)

	// Check if TLS is enabled
	skipSSL := ehConfigs.SkipSSLVerfication
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

	// Configuring the http client
	client := &http.Client{
		Transport: tr,
	}

	// Create a HTTP request
	req, err := http.NewRequest("GET", ehURL, nil)

	// Setting authorization header
	req.Header.Set(authorization, basicAuth)
	// Make the request
	logger.LoggerSync.Debug("Sending the controle plane request")
	resp, err := client.Do(req)
	// In the event of a connection error, the error would not be nil, then return the error
	// If the error is not null, proceed
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while retrieving APIs from API manager: %v", err)
		respSyncAPI.Err = err
		respSyncAPI.Resp = nil
		c <- respSyncAPI
		return
	}

	// get the response in the form of a byte slice
	respBytes, err := ioutil.ReadAll(resp.Body)

	// If the reading response gives an error
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while reading the response: %v", err)
		respSyncAPI.Err = err
		respSyncAPI.ErrorCode = resp.StatusCode
		respSyncAPI.Resp = nil
		c <- respSyncAPI
		return
	}
	// For successful response, return the byte slice and nil as error
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

//

// PushTokens helps
func PushTokens(data []byte) {
	tokens := []RevokedToken{}
	err := json.Unmarshal(data, &tokens)
	stoken := &keymgt.RevokedToken{}
	ctoken := tokens[0]
	stoken.Jti = ctoken.JWT
	stoken.Expirytime = (int32)(ctoken.ExpiryTime)
	logger.LoggerSync.Infof("TOKENS %+v", stoken)
	if err != nil {
		logger.LoggerSubscription.Errorf("Error occurred while unmarshalling the response received for: %v ", err)
	}
	logger.LoggerSync.Infof("Printing TOKENS %+v", tokens)
}

func RetrieveTokensFromCP() {

	c := make(chan SyncAPIResponse)

	go RetrieveTokens(c)

	d := <-c
	logger.LoggerSync.Infof("SS %+v", d)
	logger.LoggerSync.Infof("SS %v", string(d.Resp))

	PushTokens(d.Resp)

}
