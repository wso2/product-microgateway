/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// Package api contains the REST API implementation for the adapter
package api

import (
	"archive/zip"
	"bytes"
	"encoding/json"
	"errors"
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/micro-gw/loggers"
	apiModel "github.com/wso2/micro-gw/pkg/api/models"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
	"github.com/wso2/micro-gw/pkg/tlsutils"
	xds "github.com/wso2/micro-gw/pkg/xds"
)

// API Controller related constants
const (
	openAPIDir            string = "Definitions"
	openAPIFilename       string = "swagger."
	apiDefinitionFilename string = "api.yaml"
	endpointCertDir       string = "Endpoint-certificates"
	crtExtension          string = ".crt"
	pemExtension          string = ".pem"
)

// extractAPIProject accepts the API project as a zip file and returns the extracted content
// The apictl project must be in zipped format. And all the extensions should be defined with in the openAPI
// definition as only swagger.yaml is taken into consideration here. For websocket APIs api.yaml is taken into
// consideration. API type is decided by the type field in the api.yaml file.
func extractAPIProject(payload []byte) (apiJsn []byte, swaggerJsn []byte, upstreamCerts []byte,
	apiType string, err error) {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	newLineByteArray := []byte("\n")
	var conversionErr error

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return nil, nil, nil, "", err
	}
	// TODO: (VirajSalaka) this won't support for distributed openAPI definition
	for _, file := range zipReader.File {
		loggers.LoggerAPI.Debugf("File reading now: %v", file.Name)
		if strings.Contains(file.Name, openAPIDir+string(os.PathSeparator)+openAPIFilename) {
			loggers.LoggerAPI.Debugf("openAPI file : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the openapi file. %v", err.Error())
				continue
			}
			swaggerJsn, conversionErr = utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting api file to json: %v", conversionErr.Error())
				return nil, nil, nil, "", conversionErr
			}
			apiType = mgw.HTTP
		} else if strings.Contains(file.Name, endpointCertDir+string(os.PathSeparator)) &&
			(strings.HasSuffix(file.Name, crtExtension) || strings.HasSuffix(file.Name, pemExtension)) {
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the endpoint certificate : %v, %v", file.Name, err.Error())
				continue
			}

			if !tlsutils.IsPublicCertificate(unzippedFileBytes) {
				loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", file.Name)
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return nil, nil, nil, "", errors.New("Certificate Validation Error")
			}
			upstreamCerts = append(upstreamCerts, unzippedFileBytes...)
			upstreamCerts = append(upstreamCerts, newLineByteArray...)
		} else if strings.Contains(file.Name, apiDefinitionFilename) {
			loggers.LoggerAPI.Debugf("fileName : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the api definition file : %v %v", file.Name, err.Error())
				return nil, nil, nil, "", err
			}
			apiJsn, conversionErr = utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error occured converting api file to json: %v", conversionErr.Error())
				return nil, nil, nil, "", conversionErr
			}
			apiType, err = getAPIType(apiJsn)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the api type : %v", err.Error())
				return nil, nil, nil, "", err
			}

		}
	}
	if apiJsn == nil {
		// TODO : (LahiruUdayanga) Handle the default behaviour after when the APIDeployTestCase test is fixed.
		// If no api.yaml file is included in the zip folder, return with error.
		err := errors.New("Could not find api.yaml file")
		loggers.LoggerAPI.Errorf("Error occured while reading the api type : %v", err.Error())
		return nil, nil, nil, "", err
	} else if apiType != mgw.HTTP && apiType != mgw.WS {
		errMsg := "API type is not currently supported with WSO2 micro-gateway"
		loggers.LoggerAPI.Infof(errMsg)
		err = errors.New(errMsg)
		return nil, nil, nil, "", err
	}
	return apiJsn, swaggerJsn, upstreamCerts, apiType, nil
}

// ApplyAPIProject accepts an apictl project (as a byte array) and updates the xds servers based upon the
// content.
func ApplyAPIProject(payload []byte, environments []string) error {
	apiJsn, swaggerJsn, upstreamCerts, apiType, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	name, version, err := getAPINameAndVersion(apiJsn)
	if err != nil {
		return err
	}
	if apiType == mgw.HTTP {
		xds.UpdateAPI(name, version, swaggerJsn, upstreamCerts, apiType, environments)
	} else if apiType == mgw.WS {
		xds.UpdateAPI(name, version, apiJsn, upstreamCerts, apiType, environments)
	}
	return nil
}

// ApplyAPIProjectWithOverwrite is called by the rest implementation to differentiate
// between create and update using the overwrite param
func ApplyAPIProjectWithOverwrite(payload []byte, envrionments []string, overwriteP *bool) error {
	apiJsn, swaggerJsn, upstreamCerts, apiType, err := extractAPIProject(payload)
	if err != nil {
		return err
	}
	name, version, err := getAPINameAndVersion(apiJsn)
	if err != nil {
		return err
	}
	var overwrite bool
	if overwriteP == nil {
		overwrite = false
	} else {
		overwrite = *overwriteP
	}
	exists := xds.IsAPIExist("default", name, version) // TODO: (SuKSW) update once vhost feature added
	if overwrite && !exists {
		loggers.LoggerAPI.Infof("Error updating API. API %v:%v does not exist.", name, version)
		return errors.New("NOT_FOUND")
	}
	if !overwrite && exists {
		loggers.LoggerAPI.Infof("Error creating new API. API %v:%v already exists.", name, version)
		return errors.New("ALREADY_EXISTS")
	}
	if apiType == mgw.HTTP {
		xds.UpdateAPI(name, version, swaggerJsn, upstreamCerts, apiType, envrionments)
	} else if apiType == mgw.WS {
		xds.UpdateAPI(name, version, apiJsn, upstreamCerts, apiType, envrionments)
	}
	return nil
}

// DeleteAPI calls the DeleteAPI method in xds_server.go
func DeleteAPI(apiName string, version string, vhostP *string) (errorCode string, errorMsg string) {
	if vhostP == nil || *vhostP == "" {
		vhost := "default"
		vhostP = &vhost
	}
	return xds.DeleteAPI(apiName, version, *vhostP)
}

// ListApis calls the ListApis method in xds_server.go
func ListApis(apiTypeP *string, limitP *int64) *apiModel.APIMeta {
	var apiType string
	if apiTypeP == nil {
		apiType = ""
	} else {
		apiType = strings.ToUpper(*apiTypeP)
	}
	return xds.ListApis(apiType, limitP)
}

func readZipFile(zf *zip.File) ([]byte, error) {
	f, err := zf.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ioutil.ReadAll(f)
}

func getAPIType(apiJsn []byte) (string, error) {
	var apiDef map[string]interface{}
	unmarshalErr := json.Unmarshal(apiJsn, &apiDef)
	if unmarshalErr != nil {
		loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml %v", unmarshalErr.Error())
		return "", unmarshalErr
	}
	data := apiDef["data"].(map[string]interface{})
	apiType := strings.ToUpper(data["type"].(string))

	return apiType, nil
}

func getAPINameAndVersion(apiJsn []byte) (name string, version string, err error) {
	var apiDef map[string]interface{}
	err = json.Unmarshal(apiJsn, &apiDef)
	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while parsing api.yaml %v", err.Error())
		return "", "", err
	}
	data := apiDef["data"].(map[string]interface{})
	name = data["name"].(string)
	version = data["version"].(string)
	return name, version, nil
}
