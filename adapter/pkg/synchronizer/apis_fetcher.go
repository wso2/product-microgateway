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
	"archive/zip"
	"bytes"
	"crypto/tls"
	"crypto/x509"
	"errors"
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/pkg/auth"

	logger "github.com/wso2/micro-gw/loggers"
	apiServer "github.com/wso2/micro-gw/pkg/api"
)

const (
	apiID                   string = "apiId"
	gwType                  string = "type"
	gatewayLabel            string = "gatewayLabel"
	envoy                   string = "Envoy"
	runtimeArtifactEndpoint string = "internal/data/v1/runtime-artifacts"
	authorization           string = "Authorization"
	zipExt                  string = ".zip"
	defaultCertPath         string = "/home/wso2/security/controlplane.pem"
)

// FetchAPIs pulls the API artifact calling to the API manager
// API Manager returns a .zip file as a response and this function
// returns a byte slice of that ZIP file.
func FetchAPIs(id *string, gwLabel *string, c chan SyncAPIResponse) {
	logger.LoggerSync.Info("Fetching APIs from Control Plane.")
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
		ehURL += runtimeArtifactEndpoint
	} else {
		ehURL += "/" + runtimeArtifactEndpoint
	}
	logger.LoggerSync.Debugf("Fetching APIs from the URL %v: ", ehURL)

	ehUname := ehConfigs.Username
	ehPass := ehConfigs.Password
	basicAuth := "Basic " + auth.GetBasicAuth(ehUname, ehPass)

	// Check if TLS is enabled
	tlsEnabled := ehConfigs.TLSEnabled
	logger.LoggerSync.Debugf("TLS Enabled: %v", tlsEnabled)
	tr := &http.Transport{}
	if tlsEnabled {
		// Read the cert from the defined path
		certPath := ehConfigs.PublicCertPath
		logger.LoggerSync.Infof("Reading the cert at %v", certPath)

		if certPath == "" {
			// If cert is defined, read the default cert path
			logger.LoggerSync.Infof("Reading the defaul cert at %v", defaultCertPath)
			certPath = defaultCertPath
		}
		caCert, err := ioutil.ReadFile(certPath)
		if err != nil {
			logger.LoggerSync.Errorf("Error occurred when readin the cert form %v : %v", certPath, err)
		}
		caCertPool := x509.NewCertPool()
		caCertPool.AppendCertsFromPEM(caCert)
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
	// Making necessary query parameters for the request
	q := req.URL.Query()

	// If an API ID is present, make a query parameter
	if id != nil {
		logger.LoggerSync.Debugf("API ID: %v", *id)
		respSyncAPI.APIID = *id
		q.Add(apiID, *id)
	}
	// If the gateway label is present, make a query parameter
	if gwLabel != nil {
		logger.LoggerSync.Debugf("Gateway Label: %v", *gwLabel)
		respSyncAPI.GatewayLabel = *gwLabel
		q.Add(gatewayLabel, *gwLabel)
	}
	// Default "type" query parameter for adapter is "Envoy"
	q.Add(gwType, envoy)
	req.URL.RawQuery = q.Encode()
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
	c <- respSyncAPI
	return
}

// PushAPIProjects configure the router and enforcer using the zip containing API project(s) as
// byte slice. This method ensures to update the enforcer and router using entries inside the
// downloaded apis.zip one by one.
// If the updating envoy or enforcer fails, this method returns an error, if not error would be nil.
func PushAPIProjects(payload []byte) error {
	// Reading the root zip
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	if err != nil {
		logger.LoggerSync.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
	}
	// TODO: Currently the apis.zip file contains another zip files containing API projects.
	// But there would be a meta data file in future. Once that comes, this code segement should
	// handle that meta data file as well.

	// Read the .zip files within the root apis.zip
	for _, file := range zipReader.File {
		// open the zip files
		if strings.HasSuffix(file.Name, zipExt) {
			logger.LoggerSync.Debugf("Starting zip reading: %v", file.Name)
			// Open thezip
			f, err := file.Open()
			if err != nil {
				logger.LoggerSync.Errorf("Error zip reading: %v", err)
				return err
			}
			// Close the stream once the processing of f is done
			defer f.Close()
			//Read the files inside each xxxx-api.zip
			r, err := ioutil.ReadAll(f)
			// Pass the byte slice for the XDS APIs to push it to the enforcer and router
			err = apiServer.ApplyAPIProject(r)
			if err != nil {
				logger.LoggerSync.Errorf("Error occurred while applying project", err)
			}
		}
	}
	// Error nil for successful execution
	return nil
}
