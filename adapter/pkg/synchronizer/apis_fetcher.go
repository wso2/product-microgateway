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
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	parser "github.com/mitchellh/mapstructure"
	"github.com/wso2/product-microgateway/adapter/pkg/auth"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

const (
	apiID        string = "apiId"
	gatewayLabel string = "gatewayLabel"
	gwType       string = "type"
	envoy        string = "Envoy"
	// Authorization represent the authorization header string.
	Authorization            string = "Authorization"
	deploymentDescriptorFile string = "deployments.json"
	envPropsFile             string = "env_properties.json"
	// RuntimeArtifactEndpoint represents the /runtime-artifacts endpoint.
	RuntimeArtifactEndpoint string = "internal/data/v1/runtime-artifacts"
	// APIArtifactEndpoint represents the /retrieve-api-artifacts endpoint.
	APIArtifactEndpoint string = "internal/data/v1/retrieve-api-artifacts"
	// httpTimeout is for connection timeout of httpClient in seconds
	httpTimeout time.Duration = 30
)

// FetchAPIs pulls the API artifact calling to the API manager
// API Manager returns a .zip file as a response and this function
// returns a byte slice of that ZIP file.
func FetchAPIs(id *string, gwLabel []string, c chan SyncAPIResponse, resourceEndpoint string, sendType bool, apiUUIDList []string) {
	if id != nil {
		logger.LoggerSync.Infof("Fetching API from Control Plane for Id %q.", *id)
	} else {
		logger.LoggerSync.Info("Fetching APIs from Control Plane")
	}

	req := ConstructControlPlaneRequest(id, gwLabel, workerPool.controlPlaneParams, resourceEndpoint, sendType, apiUUIDList)
	workerReq := workerRequest{
		Req:                *req,
		APIUUID:            id,
		SyncAPIRespChannel: c,
	}
	if gwLabel != nil {
		workerReq.labels = gwLabel
	}

	if workerPool == nil {
		logger.LoggerSync.Fatal("WorkerPool is not inititated due to an internal error.")
	}
	// If adding task to the pool cannot be done, the whole thread hangs here.
	workerPool.Enqueue(workerReq)
}

// SendRequestToControlPlane is the function triggered to send the request to the control plane.
// It returns true if a response is received from the api manager.
func SendRequestToControlPlane(req *http.Request, apiID *string, gwLabels []string, c chan SyncAPIResponse,
	client *http.Client) bool {
	// Make the request
	logger.LoggerSync.Debug("Sending the controle plane request")
	resp, err := client.Do(req)

	respSyncAPI := SyncAPIResponse{}

	if apiID != nil {
		respSyncAPI.APIUUID = *apiID
	}
	if len(gwLabels) > 0 {
		respSyncAPI.GatewayLabels = gwLabels
	}

	// In the event of a connection error, the error would not be nil, then return the error
	// If the error is not null, proceed
	if err != nil {
		logger.LoggerSync.Errorf("Error occurred while retrieving APIs from API manager: %v", err)
		respSyncAPI.Err = err
		respSyncAPI.Resp = nil
		c <- respSyncAPI
		return false
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
		return false
	}
	// For successful response, return the byte slice and nil as error
	if resp.StatusCode == http.StatusOK {
		respSyncAPI.Err = nil
		respSyncAPI.Resp = respBytes
		c <- respSyncAPI
		return true
	}
	// If the response is not successful, create a new error with the response and log it and return
	// Ex: for 401 scenarios, 403 scenarios.
	logger.LoggerSync.Errorf("Failure response: %v", string(respBytes))
	respSyncAPI.Err = errors.New(string(respBytes))
	respSyncAPI.Resp = nil
	respSyncAPI.ErrorCode = resp.StatusCode
	c <- respSyncAPI
	return true
}

// ConstructControlPlaneRequest constructs the http Request used to send to the control plane
func ConstructControlPlaneRequest(id *string, gwLabel []string, controlPlaneParams controlPlaneParameters,
	resourceEndpoint string, sendType bool, apiUUIDList []string) *http.Request {
	var (
		req      *http.Request
		err      error
		bodyJSON []byte
	)
	serviceURL := controlPlaneParams.serviceURL
	userName := controlPlaneParams.username
	password := controlPlaneParams.password
	// postData contains the API UUID list in the payload of the post request.
	type postData struct {
		Uuids []string `json:"uuids"`
	}
	// NOTE: Getting resourceEndpoint as a parameter since GA and LA use different endpoints.
	if strings.HasSuffix(serviceURL, "/") {
		serviceURL += resourceEndpoint
	} else {
		serviceURL += "/" + resourceEndpoint
	}
	logger.LoggerSync.Debugf("Fetching APIs from the URL %v: ", serviceURL)

	basicAuth := "Basic " + auth.GetBasicAuth(userName, password)

	// Populating the payload body with API UUID list
	if apiUUIDList != nil {
		body := postData{
			Uuids: apiUUIDList,
		}
		bodyJSON, err = json.Marshal(body)
		if err != nil {
			logger.LoggerSync.Errorf("Error marshaling the uuid List: %v", err)
		}
	}

	// Create a HTTP request
	if apiUUIDList == nil {
		req, err = http.NewRequest("GET", serviceURL, nil)
	} else {
		req, err = http.NewRequest("POST", serviceURL, bytes.NewBuffer(bodyJSON))
	}
	if err != nil {
		logger.LoggerSync.Fatalf("Error while creating the HTTP request: %v", err)
	}
	// Making necessary query parameters for the request
	q := req.URL.Query()

	// If an API ID is present, make a query parameter
	if id != nil {
		logger.LoggerSync.Debugf("API ID: %v", *id)
		// respSyncAPI.APIUUID = *id
		q.Add(apiID, *id)
	}
	// If the gateway label is present, make a query parameter
	if len(gwLabel) > 0 {
		logger.LoggerSync.Debugf("Gateway Label: %v", gwLabel)
		// respSyncAPI.GatewayLabels = gwLabel
		gatewaysQStr := strings.Join(gwLabel, "|")
		q.Add(gatewayLabel, base64.StdEncoding.EncodeToString([]byte(gatewaysQStr)))
	}
	// NOTE: GA does not send this query parameter.
	if sendType {
		// Default "type" query parameter for adapter is "Envoy"
		q.Add(gwType, envoy)
	}
	req.URL.RawQuery = q.Encode()
	// Setting authorization header
	req.Header.Set(Authorization, basicAuth)
	// If API UUID list is present, set the content-type header
	if apiUUIDList != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	return req
}

// RetryFetchingAPIs function keeps retrying to fetch APIs from runtime-artifact endpoint.
func RetryFetchingAPIs(c chan SyncAPIResponse, data SyncAPIResponse, endpoint string, sendType bool) {
	retryInterval := workerPool.controlPlaneParams.retryInterval
	go func(d SyncAPIResponse) {
		// Retry fetching from control plane after a configured time interval
		if retryInterval == 0 {
			// Assign default retry interval
			retryInterval = 5
		}
		logger.LoggerSync.Debugf("Time Duration for retrying: %v", retryInterval*time.Second)
		time.Sleep(retryInterval * time.Second)
		logger.LoggerSync.Infof("Retrying to fetch API data from control plane.")
		FetchAPIs(&d.APIUUID, d.GatewayLabels, c, endpoint, sendType, nil)
	}(data)
}

// ReadRootFiles function reads following files inside the root zip
// deployment.json
// env_properties.json
func ReadRootFiles(reader *zip.Reader) (*DeploymentDescriptor, map[string]map[string]APIEnvProps, error) {
	deploymentDescriptor := &DeploymentDescriptor{}
	apiEnvProps := make(map[string]map[string]APIEnvProps)
	// Read the .zip files within the root apis.zip
	for _, file := range reader.File {
		// Open deployment descriptor file
		if strings.EqualFold(file.Name, deploymentDescriptorFile) {
			logger.LoggerSync.Debugf("Start reading %v file", deploymentDescriptorFile)
			f, err := file.Open()
			if err != nil {
				logger.LoggerSync.Error("Error reading deployment descriptor: ", err)
				return deploymentDescriptor, apiEnvProps, err
			}
			data, err := ioutil.ReadAll(f)
			_ = f.Close() // Close the file here (without defer)
			if err != nil {
				logger.LoggerSync.Error("Error reading deployment descriptor: ", err)
				return deploymentDescriptor, apiEnvProps, err
			}
			logger.LoggerSync.Debugf("Parsing content of deployment descriptor, content: %s", string(data))
			if err = json.Unmarshal(data, deploymentDescriptor); err != nil {
				logger.LoggerSync.Error("Error parsing JSON content of deployment descriptor: ", err)
				return deploymentDescriptor, apiEnvProps, err
			}
		} else if strings.EqualFold(file.Name, envPropsFile) {
			logger.LoggerSync.Debugf("Start reading %v file", envPropsFile)
			f, err := file.Open()
			if err != nil {
				logger.LoggerSync.Error("Error reading environment specific properties: ", err)
				return deploymentDescriptor, apiEnvProps, err
			}
			data, err := ioutil.ReadAll(f)
			_ = f.Close() // Close the file here (without defer)
			if err != nil {
				logger.LoggerSync.Error("Error reading environment specific properties: ", err)
				return deploymentDescriptor, apiEnvProps, err
			}
			logger.LoggerSync.Debugf("Parsing content of environment specific properties, content: %s", string(data))
			if apiEnvProps, err = parseEnvProps(data); err != nil {
				logger.LoggerSync.Errorf("Error occurred while parsing environment specific properties : %v : %v",
					file.Name, err.Error())
				return deploymentDescriptor, apiEnvProps, err
			}
		}
	}
	return deploymentDescriptor, apiEnvProps, nil
}

func parseEnvProps(data []byte) (map[string]map[string]APIEnvProps, error) {
	var envPropsFile map[string]interface{}
	envPropsMap := make(map[string]map[string]APIEnvProps)
	if err := json.Unmarshal(data, &envPropsFile); err != nil {
		logger.LoggerSync.Error("Error parsing Environment specific values: ", err)
		return nil, err
	}

	if apisData, found := envPropsFile["apis"]; found {
		if apis, ok := apisData.(map[string]interface{}); ok {
			for apiUUID, apiData := range apis {
				apiProps := make(map[string]APIEnvProps)
				if api, ok := apiData.(map[string]interface{}); ok {
					var envProps APIEnvProps
					for envLabel, envData := range api {
						if err := parser.Decode(envData, &envProps); err != nil {
							logger.LoggerSync.Error("Error parsing environment specific values: ", err)
							return nil, err
						}
						apiProps[envLabel] = envProps
					}
				}
				envPropsMap[apiUUID] = apiProps
			}
			return envPropsMap, nil
		}
		logger.LoggerSync.Error("Wrong format given for parsing environment specific values")
		return nil, errors.New("wrong format given for parsing environment specific values")
	}
	return nil, nil
}
