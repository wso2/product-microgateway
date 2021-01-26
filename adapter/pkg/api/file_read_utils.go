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
	"regexp"
	"strings"

	"github.com/wso2/micro-gw/loggers"
	mgw "github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
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

// ApplyAPIProject accepts an apictl project (as a byte array) and updates the xds servers based upon the
// content.
// The apictl project must be in zipped format. And all the extensions should be defined with in the openAPI
// definition as only swagger.yaml is taken into consideration here. For websocket APIs api.yaml is taken into
// consideration. API type is decided by the type field in the api.yaml file.
func ApplyAPIProject(payload []byte) error {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	var upstreamCerts []byte
	newLineByteArray := []byte("\n")
	var swaggerJsn []byte
	var apiJsn []byte
	var conversionErr error
	var apiType string

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
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
				return conversionErr
			}
		} else if strings.Contains(file.Name, endpointCertDir+string(os.PathSeparator)) &&
			(strings.HasSuffix(file.Name, crtExtension) || strings.HasSuffix(file.Name, pemExtension)) {
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the endpoint certificate : %v, %v", file.Name, err.Error())
				continue
			}
			certContentPattern := `\-\-\-\-\-BEGIN\sCERTIFICATE\-\-\-\-\-((.|\n)*)\-\-\-\-\-END\sCERTIFICATE\-\-\-\-\-`
			regex := regexp.MustCompile(certContentPattern)
			if !regex.Match(unzippedFileBytes) {
				loggers.LoggerAPI.Errorf("Provided certificate: %v is not in the PEM file format. ", file.Name)
				// TODO: (VirajSalaka) Create standard error handling mechanism
				return errors.New("Certificate Validation Error")
			}
			upstreamCerts = append(upstreamCerts, unzippedFileBytes...)
			upstreamCerts = append(upstreamCerts, newLineByteArray...)
		} else if strings.Contains(file.Name, apiDefinitionFilename) {
			loggers.LoggerAPI.Debugf("fileName : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the api definition file : %v %v", file.Name, err.Error())
				return err
			}
			apiJsn, conversionErr = utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error occured converting api file to json: %v", conversionErr.Error())
			}
			apiType, err = getAPIType(apiJsn)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the api type : %v", err.Error())
				return err
			}

		}
	}
	// TODO - (VirajSalaka) change the switch case and use one method with both api.yaml and swagger.yaml
	switch apiType {
	case mgw.HTTP:
		xds.UpdateAPI(swaggerJsn, upstreamCerts, apiType)
	case mgw.WS:
		xds.UpdateAPI(apiJsn, upstreamCerts, apiType)
	default:
		// If no api.yaml file is included in the zip folder , apiType defaults to HTTP to pass the APIDeployTestCase integration test.
		// TODO : (LahiruUdayanga) Handle the default behaviour after when the APIDeployTestCase test is fixed.
		apiType = mgw.HTTP
		xds.UpdateAPI(swaggerJsn, upstreamCerts, apiType)
		loggers.LoggerAPI.Infof("API type is not currently supported with WSO2 micro-gateway")
	}
	return nil
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
	apiType := data["type"].(string)

	return apiType, nil
}
