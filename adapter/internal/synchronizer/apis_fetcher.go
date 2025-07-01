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
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/common"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
	"github.com/wso2/product-microgateway/adapter/pkg/synchronizer"

	apiServer "github.com/wso2/product-microgateway/adapter/internal/api"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	sync "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
)

const (
	zipExt          string = ".zip"
	defaultCertPath string = "/home/wso2/security/controlplane.pem"
)

func init() {
	conf, _ := config.ReadConfigs()
	sync.InitializeWorkerPool(conf.ControlPlane.RequestWorkerPool.PoolSize, conf.ControlPlane.RequestWorkerPool.QueueSizePerPool,
		conf.ControlPlane.RequestWorkerPool.PauseTimeAfterFailure, conf.Adapter.Truststore.Location,
		conf.ControlPlane.SkipSSLVerification, conf.ControlPlane.HTTPClient.RequestTimeOut, conf.ControlPlane.RetryInterval,
		conf.ControlPlane.ServiceURL, conf.ControlPlane.Username, conf.ControlPlane.Password)
}

// PushAPIProjects configure the router and enforcer using the zip containing API project(s) as
// byte slice. This method ensures to update the enforcer and router using entries inside the
// downloaded apis.zip one by one.
// If the updating envoy or enforcer fails, this method returns an error, if not error would be nil.
func PushAPIProjects(payload []byte, environments []string, xdsOptions common.XdsOptions) error {
	var deploymentList []*notifier.DeployedAPIRevision
	// Reading the root zip
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	if err != nil {
		logger.LoggerSync.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
	}
	apisCount := len(zipReader.File) - 2
	logger.LoggerSync.Infof("Start Deploying %d API/s...", apisCount)

	// apiFiles represents zipped API files fetched from API Manager
	apiFiles := make(map[string]*zip.File, apisCount)
	// Read deployments from deployment.json file
	deploymentDescriptor, envProps, err := sync.ReadRootFiles(zipReader)
	if err != nil {
		logger.LoggerSync.Error("Error occured while reading root files ", err)
		return err
	}

	// Read the .zip files within the root apis.zip and add apis to apiFiles array.
	for _, file := range zipReader.File {
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
		gaProvidedAPIEnvMap := xdsOptions.APIIDEnvMap
		vhostToEnvsMap := make(map[string][]*synchronizer.GatewayLabel)
		for index := range deployment.Environments {
			env := deployment.Environments[index]
			if os.Getenv("FEATURE_ENV_BASED_FILTERING_IN_STARTUP") == "true" {
				if gaProvidedAPIEnvMap != nil {
					apiID := strings.Split(file.Name, "-")[0]
					if gaProvidedAPIEnvMap[apiID] == nil || gaProvidedAPIEnvMap[apiID][env.Name] == nil {
						logger.LoggerSync.Infof("Skip environment %s for API %s as it is not applicable for the API in the GA provided API map",
							env.Name, apiID)
						continue
					}
				}
			}
			vhostToEnvsMap[env.Vhost] = append(vhostToEnvsMap[env.Vhost], &env)
		}

		// If VhostToEnvsMap is empty, then there is nothing to deploy.
		if len(vhostToEnvsMap) == 0 {
			continue
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

		deployedRevisionList, err = apiServer.ApplyAPIProjectFromAPIM(apiFileData, vhostToEnvsMap, envProps, xdsOptions, deployment.IsPaidOrg)
		if err != nil {
			logger.LoggerSync.Errorf("Error occurred while applying project %v", err)
		} else if deployedRevisionList != nil {
			deploymentList = append(deploymentList, MergeDeployedRevisionList(deployedRevisionList)...)
		}
	}

	// TODO: (renuka) notify the revision deployment to the control plane once all chunks are deployed.
	// This is not fixed as notify the control plane chunk by chunk (even though the chunk is not really applied to the Enforcer and Router) is not a drastic issue.
	// This path is only happening when Adapter is restarting and at that time the deployed time is already updated in the control plane.
	notifier.SendRevisionUpdate(deploymentList)
	logger.LoggerSync.Infof("Successfully deployed %d API/s", len(deploymentList))
	// Error nil for successful execution
	return nil
}

// MergeDeployedRevisionList merge the deployment information by revision
func MergeDeployedRevisionList(deployedRevisionList []*notifier.DeployedAPIRevision) []*notifier.DeployedAPIRevision { // Combine env info of same revision id
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	revisionDeploymentMap := map[string]*notifier.DeployedAPIRevision{}
	for _, revision := range deployedRevisionList {
		var updatedEnvInfo []notifier.DeployedEnvInfo
		for _, env := range revision.EnvInfo {
			if env.VHost == conf.Adapter.SandboxVhost {
				env.Name = conf.Adapter.SandboxEnvName
			}
			updatedEnvInfo = append(updatedEnvInfo, env)
		}
		revision.EnvInfo = updatedEnvInfo
		mapKey := revision.APIID + strconv.Itoa(revision.RevisionID)
		revisionDeployment, exists := revisionDeploymentMap[mapKey]
		if exists {
			revisionDeployment.EnvInfo = append(revisionDeployment.EnvInfo, revision.EnvInfo...)
			revisionDeploymentMap[mapKey] = revisionDeployment
		} else {
			revisionDeploymentMap[mapKey] = revision
		}
	}
	var deploymentList []*notifier.DeployedAPIRevision
	for _, revision := range revisionDeploymentMap {
		deploymentList = append(deploymentList, revision)
	}
	return deploymentList
}

// FetchAPIsFromControlPlane method pulls API data for a given APIs according to a
// given API ID and a list of environments that API has been deployed to.
// updatedAPIID is the corresponding ID of the API in the form of an UUID
// updatedEnvs contains the list of environments the API deployed to.
func FetchAPIsFromControlPlane(updatedAPIID string, updatedEnvs []string, envToDpMap map[string]string,
	envToGwAccessibilityTypeMap map[string]string) {
	// Read configurations and derive the eventHub details
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	configuredEnvs := conf.ControlPlane.EnvironmentLabels
	//finalEnvs contains the actual environments that the adapter should update
	var finalEnvs []string

	// if the dynamic environment support feature enabled, finalEnvs should be the envs in envToDpMap,
	// whose data plane ID matches with the data Plane ID defined in the gateway configs
	if conf.ControlPlane.DynamicEnvironments.Enabled {
		for gwEnv, dpID := range envToDpMap {
			// following if condition checks whether the environment corresponds to the configured data-plane and
			// gateway accessibility type (internal or external).
			// it assumes that the envToDpMap and envToGwAccessibilityTypeMap are identical in gateway environments.
			if strings.EqualFold(conf.ControlPlane.DynamicEnvironments.DataPlaneID, dpID) &&
				strings.EqualFold(conf.ControlPlane.DynamicEnvironments.GatewayAccessibilityType,
					envToGwAccessibilityTypeMap[gwEnv]) {
				finalEnvs = append(finalEnvs, gwEnv)
			}
		}
	} else {
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
	}

	if len(finalEnvs) == 0 {
		// If the finalEnvs is empty -> it means, the configured environments  does not contain the affected/updated
		// environments. If that's the case, then APIs should not be fetched from the adapter.
		return
	}

	c := make(chan sync.SyncAPIResponse)
	logger.LoggerSync.Infof("API %s is added/updated to APIList for label %v", updatedAPIID, updatedEnvs)
	var queryParamMap map[string]string
	queryParamMap = common.PopulateQueryParamForOrganizationID(queryParamMap)
	go sync.FetchAPIs(&updatedAPIID, finalEnvs, c, sync.RuntimeArtifactEndpoint, true, nil, queryParamMap)

	retryCounter := 0
	retryLimit := 10
	receivedArtifact := false
	for {
		data := <-c
		logger.LoggerSync.Debugf("Receiving data for the API: %q", updatedAPIID)
		if data.Resp != nil {
			// For successfull fetches, data.Resp would return a byte slice with API project(s)
			logger.LoggerSync.Infof("Pushing data to router and enforcer for the API %q", updatedAPIID)
			receivedArtifact = true
			err := PushAPIProjects(data.Resp, finalEnvs, common.XdsOptions{})
			if err != nil {
				logger.LoggerSync.Errorf("Error occurred while pushing API data for the API %q: %v ", updatedAPIID, err)
			}
			break
		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
			logger.LoggerSync.Errorf("Error occurred when retrieving API %q from control plane: %v", updatedAPIID, data.Err)
			// If the request is rate limited retry after 10 seconds
			if data.ErrorCode == 429 {
				if retryCounter >= retryLimit {
					break
				}
				delayPeriod := time.Duration(10 * retryCounter)
				time.Sleep(delayPeriod * time.Second)
				sync.RetryFetchingAPIs(c, data, sync.RuntimeArtifactEndpoint, true, queryParamMap, nil)
				retryCounter++
				continue
			}
			break
		} else {
			// if retry limit exceeded then exit the loop
			if retryCounter >= retryLimit {
				break
			}
			// Keep the iteration still until all the envrionment response properly.
			logger.LoggerSync.Errorf("Error occurred while fetching data from control plane for the API %q: %v. Hence retrying..", updatedAPIID, data.Err)
			sync.RetryFetchingAPIs(c, data, sync.RuntimeArtifactEndpoint, true, queryParamMap, nil)
			retryCounter++
		}
	}

	if !receivedArtifact {
		// This logs statement is used to trigger the alert if the API is not fetched from the control plane.
		logger.LoggerSync.Errorf("Stop retrying to fetch data from control plane for the API %q: for environments %v as no artifact received after %d retries.",
			updatedAPIID, finalEnvs, retryCounter)
	}
}
