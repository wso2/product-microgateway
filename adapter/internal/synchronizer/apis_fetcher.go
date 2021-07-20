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
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"

	apiServer "github.com/wso2/product-microgateway/adapter/internal/api"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

const (
	apiID                    string = "apiId"
	gwType                   string = "type"
	gatewayLabel             string = "gatewayLabel"
	envoy                    string = "Envoy"
	runtimeArtifactEndpoint  string = "internal/data/v1/runtime-artifacts"
	authorization            string = "Authorization"
	zipExt                   string = ".zip"
	defaultCertPath          string = "/home/wso2/security/controlplane.pem"
	deploymentDescriptorFile string = "deployments.json"
)

// FetchAPIs pulls the API artifact calling to the API manager
// API Manager returns a .zip file as a response and this function
// returns a byte slice of that ZIP file.
func FetchAPIs(id *string, gwLabel []string, c chan SyncAPIResponse) {
	logger.LoggerSync.Info("Fetching APIs from Control Plane.")
	respSyncAPI := SyncAPIResponse{}

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
		ehURL += runtimeArtifactEndpoint
	} else {
		ehURL += "/" + runtimeArtifactEndpoint
	}
	logger.LoggerSync.Debugf("Fetching APIs from the URL %v: ", ehURL)

	ehUname := ehConfigs.Username
	ehPass := ehConfigs.Password
	basicAuth := "Basic " + auth.GetBasicAuth(ehUname, ehPass)

	// Check if TLS is enabled
	skipSSL := ehConfigs.SkipSSLVerification

	// Create a HTTP request
	req, err := http.NewRequest("GET", ehURL, nil)
	// Making necessary query parameters for the request
	q := req.URL.Query()

	// If an API ID is present, make a query parameter
	if id != nil {
		logger.LoggerSync.Debugf("API ID: %v", *id)
		respSyncAPI.APIUUID = *id
		q.Add(apiID, *id)
	}
	// If the gateway label is present, make a query parameter
	if len(gwLabel) > 0 {
		logger.LoggerSync.Debugf("Gateway Label: %v", gwLabel)
		respSyncAPI.GatewayLabels = gwLabel
		gatewaysQStr := strings.Join(gwLabel, "|")
		q.Add(gatewayLabel, base64.StdEncoding.EncodeToString([]byte(gatewaysQStr)))
	}
	// Default "type" query parameter for adapter is "Envoy"
	q.Add(gwType, envoy)
	req.URL.RawQuery = q.Encode()
	// Setting authorization header
	req.Header.Set(authorization, basicAuth)
	// Make the request
	logger.LoggerSync.Debug("Sending the controle plane request")
	resp, err := tlsutils.InvokeControlPlane(req, skipSSL)
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

// PushAPIProjects configure the router and enforcer using the zip containing API project(s) as
// byte slice. This method ensures to update the enforcer and router using entries inside the
// downloaded apis.zip one by one.
// If the updating envoy or enforcer fails, this method returns an error, if not error would be nil.
func PushAPIProjects(payload []byte, environments []string) error {
	var deploymentList []*notifier.DeployedAPIRevision
	// Reading the root zip
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	if err != nil {
		logger.LoggerSync.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
	}
	logger.LoggerSync.Infof("Start Deploying %d API/s...", len(zipReader.File)-1)

	// apiFiles represents zipped API files fetched from API Manager
	apiFiles := make(map[string]*zip.File, len(zipReader.File)-1)
	deploymentDescriptor := &DeploymentDescriptor{}

	// Read the .zip files within the root apis.zip
	for _, file := range zipReader.File {
		// Open deployment descriptor file
		if strings.EqualFold(file.Name, deploymentDescriptorFile) {
			logger.LoggerSync.Debugf("Start reading %v file", deploymentDescriptorFile)
			f, err := file.Open()
			if err != nil {
				logger.LoggerSync.Errorf("Error reading deployment descriptor: %v", err)
				return err
			}
			data, err := ioutil.ReadAll(f)
			_ = f.Close() // Close the file here (without defer)
			if err != nil {
				logger.LoggerSync.Errorf("Error reading deployment descriptor: %v", err)
				return err
			}
			logger.LoggerSync.Debugf("Parsing content of deployment descriptor, content: %s", string(data))
			if err = json.Unmarshal(data, deploymentDescriptor); err != nil {
				// TODO: (renuka) shall we print content of deployment descriptor
				logger.LoggerSync.Errorf("Error parsing JSON content of deployment descriptor: %v", err)
				return err
			}
		}

		if strings.HasSuffix(file.Name, zipExt) {
			apiFiles[file.Name] = file
		}
	}

	// loop deployments in deployment descriptor file instead of files in the root zip
	for _, deployment := range deploymentDescriptor.Data.Deployments {
		file := apiFiles[deployment.APIFile]
		if file == nil {
			err := fmt.Errorf("API file \"%v\" defined in deployment descriptor not found",
				deployment.APIFile)
			logger.LoggerSync.Errorf("API file not found: %v", err)
			return err
		}

		vhostToEnvsMap := make(map[string][]string)
		for _, environment := range deployment.Environments {
			vhostToEnvsMap[environment.Vhost] = append(vhostToEnvsMap[environment.Vhost], environment.Name)
		}

		logger.LoggerSync.Infof("Start deploying api from file (API_ID:REVISION_ID).zip : %v", file.Name)
		f, err := file.Open()
		if err != nil {
			logger.LoggerSync.Errorf("Error reading zip file: %v", err)
			return err
		}
		//Read the files inside each xxxx-api.zip
		apiFileData, err := ioutil.ReadAll(f)
		_ = f.Close() // Close the file here (without defer)
		// Pass the byte slice for the XDS APIs to push it to the enforcer and router
		// TODO: (renuka) optimize applying API project, update maps one by one and apply xds once
		var deployedRevisionList []*notifier.DeployedAPIRevision
		deployedRevisionList, err = apiServer.ApplyAPIProjectFromAPIM(apiFileData, vhostToEnvsMap)
		if err != nil {
			logger.LoggerSync.Errorf("Error occurred while applying project %v", err)
		} else if deployedRevisionList != nil {
			deploymentList = append(deploymentList, deployedRevisionList...)
		}
	}
	if len(deploymentList) > 0 {
		notifier.SendRevisionUpdate(deploymentList)
	}
	logger.LoggerSync.Infof("Successfully deployed %d API/s", len(zipReader.File)-1)
	// Error nil for successful execution
	return nil
}

// FetchAPIsFromControlPlane method pulls API data for a given APIs according to a
// given API ID and a list of environments that API has been deployed to.
// updatedAPIID is the corresponding ID of the API in the form of an UUID
// updatedEnvs contains the list of environments the API deployed to.
func FetchAPIsFromControlPlane(updatedAPIID string, updatedEnvs []string) {
	// Read configurations and derive the eventHub details
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	// Take the configured labels from the adapter
	configuredEnvs := conf.ControlPlane.EnvironmentLabels
	//finalEnvs contains the actual envrionments that the adapter should update
	var finalEnvs []string
	if len(configuredEnvs) > 0 {
		// If the configuration file contains environment list, then check if then check if the
		// affected environments are present in the provided configs. If so, add that environment
		// to the finalEnvs slice
		for _, updatedEnv := range updatedEnvs {
			for _, configuredEnv := range configuredEnvs {
				if updatedEnv == configuredEnv {
					finalEnvs = append(finalEnvs, updatedEnv)
				}
			}
		}
	} else {
		// If the labels are not configured, publish the APIS to the default environment
		finalEnvs = []string{config.DefaultGatewayName}
	}

	if len(finalEnvs) == 0 {
		// If the finalEnvs is empty -> it means, the configured envrionments  does not contains the affected/updated
		// environments. If that's the case, then APIs should not be fetched from the adapter.
		return
	}

	c := make(chan SyncAPIResponse)
	logger.LoggerSync.Infof("API %s is added/updated to APIList for label %v", updatedAPIID, updatedEnvs)
	go FetchAPIs(&updatedAPIID, finalEnvs, c)
	for {
		data := <-c
		logger.LoggerSync.Debug("Receiving data for an environment")
		if data.Resp != nil {
			// For successfull fetches, data.Resp would return a byte slice with API project(s)
			logger.LoggerSync.Info("Pushing data to router and enforcer")
			err := PushAPIProjects(data.Resp, finalEnvs)
			if err != nil {
				logger.LoggerSync.Errorf("Error occurred while pushing API data: %v ", err)
			}
			break
		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
			logger.LoggerSync.Errorf("Error occurred when retrieving APIs from control plane: %v", data.Err)
		} else {
			// Keep the iteration still until all the envrionment response properly.
			logger.LoggerSync.Errorf("Error occurred while fetching data from control plane: %v", data.Err)
			go func(d SyncAPIResponse) {
				// Retry fetching from control plane after a configured time interval
				// Retry fetching from control plane after a configured time interval
				if conf.ControlPlane.RetryInterval == 0 {
					// Assign default retry interval
					conf.ControlPlane.RetryInterval = 5
				}
				logger.LoggerSync.Debugf("Time Duration for retrying: %v", conf.ControlPlane.RetryInterval*time.Second)
				time.Sleep(conf.ControlPlane.RetryInterval * time.Second)
				logger.LoggerSync.Info("Retrying to fetch API data from control plane.")
				FetchAPIs(&updatedAPIID, finalEnvs, c)
			}(data)
		}
	}

}
