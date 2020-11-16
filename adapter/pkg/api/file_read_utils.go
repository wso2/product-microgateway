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

//Package api contains the REST API implementation for the adapter
package api

import (
	"archive/zip"
	"bytes"
	"io/ioutil"
	"log"
	"strings"

	"github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
	xds "github.com/wso2/micro-gw/pkg/xds"
)

// ApplyAPIProject accepts an apictl project (as a byte array) and updates the xds servers based upon the
// content.
// The apictl project must be in zipped format. And all the extensions should be defined with in the openAPI
// definition as only swagger.yaml is taken into consideration here.
func ApplyAPIProject(payload []byte) error {
	zipReader, err := zip.NewReader(bytes.NewReader(payload), int64(len(payload)))

	if err != nil {
		loggers.LoggerAPI.Errorf("Error occured while unzipping the apictl project. Error: %v", err.Error())
		return err
	}

	//TODO: (VirajSalaka) this won't support for distributed openAPI definition
	for _, file := range zipReader.File {
		if strings.HasSuffix(file.Name, "Meta-information/swagger.yaml") {
			loggers.LoggerAPI.Debugf("openAPI file : %v", file.Name)
			unzippedFileBytes, err := readZipFile(file)
			if err != nil {
				loggers.LoggerAPI.Errorf("Error occured while reading the openapi file. %v", err.Error())
				continue
			}
			apiJsn, conversionErr := utills.ToJSON(unzippedFileBytes)
			if conversionErr != nil {
				loggers.LoggerAPI.Errorf("Error converting api file to json: %v", err.Error())
				return conversionErr
			}
			xds.UpdateEnvoy(apiJsn)
		}
	}
	return nil
}

//ApplyOpenAPIFile accepts an openapi definition as a bytearray and apply the changes to XDS servers.
//TODO: (VirajSalaka) Remove the code segment as it is not in use for the main flow.
func ApplyOpenAPIFile(payload []byte) {
	apiJsn, err := utills.ToJSON(payload)
	if err != nil {
		log.Fatal("Error converting api file to json:", err)
		return
	}
	xds.UpdateEnvoy(apiJsn)
}

func readZipFile(zf *zip.File) ([]byte, error) {
	f, err := zf.Open()
	if err != nil {
		return nil, err
	}
	defer f.Close()
	return ioutil.ReadAll(f)
}
