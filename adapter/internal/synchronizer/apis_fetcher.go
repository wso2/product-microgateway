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
	"fmt"
	"io/ioutil"
	"strings"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	"github.com/wso2/product-microgateway/adapter/pkg/health"

	apiServer "github.com/wso2/product-microgateway/adapter/internal/api"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	sync "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

const (
	zipExt          string = ".zip"
	defaultCertPath string = "/home/wso2/security/controlplane.pem"
)

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

	// Read deployments from deployment.json file
	deploymentDescriptor, envProps, err := sync.ReadRootFiles(zipReader)
	if err != nil {
		logger.LoggerSync.Error("Error occured while reading root files ", err)
		return err
	}

	numberOfAPIDeployments := len(deploymentDescriptor.Data.Deployments)

	// apiFiles represents zipped API files fetched from API Manager
	apiFiles := make(map[string]*zip.File, numberOfAPIDeployments)
	// Read the .zip files within the root apis.zip and add apis to apiFiles array.
	for _, file := range zipReader.File {
		if strings.HasSuffix(file.Name, zipExt) {
			apiFiles[file.Name] = file
		}
	}

	logger.LoggerSync.Infof("Start Deploying %d API/s...", numberOfAPIDeployments)

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
		// Updating cache one API by one API, if one API failed to update cache continue with others.
		var deployedRevisionList []*notifier.DeployedAPIRevision
		deployedRevisionList, err = apiServer.ApplyAPIProjectFromAPIM(apiFileData, vhostToEnvsMap, envProps)
		if err != nil {
			logger.LoggerSync.Errorf("Error occurred while applying project %v", err)
		} else if deployedRevisionList != nil {
			deploymentList = append(deploymentList, deployedRevisionList...)
		}
	}
	conf, _ := config.ReadConfigs()
	if conf.GlobalAdapter.Enabled && len(deploymentList) > 0 {
		notifier.SendRevisionUpdate(deploymentList)
	}
	logger.LoggerSync.Infof("Successfully deployed %d API/s", len(deploymentList))
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
	// Populate data from config.
	serviceURL := conf.ControlPlane.ServiceURL
	userName := conf.ControlPlane.Username
	password := conf.ControlPlane.Password
	configuredEnvs := conf.ControlPlane.EnvironmentLabels
	skipSSL := conf.ControlPlane.SkipSSLVerification
	retryInterval := conf.ControlPlane.RetryInterval
	truststoreLocation := conf.Adapter.Truststore.Location
	requestTimeOut := conf.ControlPlane.HTTPClient.RequestTimeOut
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

	c := make(chan sync.SyncAPIResponse)
	logger.LoggerSync.Infof("API %s is added/updated to APIList for label %v", updatedAPIID, updatedEnvs)
	go sync.FetchAPIs(&updatedAPIID, finalEnvs, c, serviceURL, userName, password, skipSSL, truststoreLocation,
		sync.RuntimeArtifactEndpoint, true, nil, requestTimeOut)
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
			health.SetControlPlaneRestAPIStatus(false)
		} else {
			// Keep the iteration still until all the envrionment response properly.
			logger.LoggerSync.Errorf("Error occurred while fetching data from control plane: %v", data.Err)
			sync.RetryFetchingAPIs(c, serviceURL, userName, password, skipSSL, truststoreLocation, retryInterval,
				data, sync.RuntimeArtifactEndpoint, true, requestTimeOut)
		}
	}

}
