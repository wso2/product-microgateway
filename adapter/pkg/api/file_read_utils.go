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
	"io/ioutil"
	"os"
	"strings"

	"github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
	xds "github.com/wso2/micro-gw/pkg/xds"
)

// API Controller related constants
const (
	openAPIDir      string = "Definitions"
	openAPIFilename string = "swagger.yaml"
	endpointCertDir string = "Endpoint-certificates"
	crtExtension    string = ".crt"
	pemExtension    string = ".pem"
)

// ApplyAPIProject accepts an apictl project (as a byte array) and updates the xds servers based upon the
// content.
// The apictl project must be in zipped format. And all the extensions should be defined with in the openAPI
// definition as only swagger.yaml is taken into consideration here.
func ApplyAPIProject(payload []byte) error {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))
	var upstreamCerts []byte
	newLineByteArray := []byte("\n")
	var apiJsn []byte
	var conversionErr error

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
	}

	// TODO: (VirajSalaka) this won't support for distributed openAPI definition
	for _, file := range zipReader.File {
		if strings.HasSuffix(file.Name, openAPIDir+string(os.PathSeparator)+openAPIFilename) {
			loggers.LoggerAPI.Debugf("openAPI file : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the openapi file. %v", err.Error())
				continue
			}
			apiJsn, conversionErr = utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting api file to json: %v", err.Error())
				return conversionErr
			}
		} else if strings.Contains(file.Name, endpointCertDir+string(os.PathSeparator)) &&
			(strings.HasSuffix(file.Name, crtExtension) || strings.HasSuffix(file.Name, pemExtension)) {
			//TODO: (VirajSalaka) Validate the content of cert files
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the endpoint certificate : %v, %v", file.Name, err.Error())
				continue
			}
			upstreamCerts = append(upstreamCerts, unzippedFileBytes...)
			upstreamCerts = append(upstreamCerts, newLineByteArray...)
		}
	}
	xds.UpdateEnvoy(apiJsn, upstreamCerts)
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
