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

// Package operator converts the openAPI v3 and/or v2 content
// To MgwSwagger objects which is the intermediate representation
// maintained by the microgateway.
package operator

import (
	"encoding/json"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-openapi/spec"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/model"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
)

// GetMgwSwagger converts the openAPI v3 and v2 content
// To MgwSwagger objects
//TODO: (VirajSalaka) return the error and handle
func GetMgwSwagger(apiContent []byte) model.MgwSwagger {
	var mgwSwagger model.MgwSwagger

	apiJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		logger.LoggerOasparser.Error("Error converting api file to json", err)
		return mgwSwagger
	}
	swaggerVerison := utills.FindSwaggerVersion(apiJsn)

	if swaggerVerison == "2" {
		//map json to struct
		var apiData2 spec.Swagger
		err = json.Unmarshal(apiJsn, &apiData2)
		if err != nil {
			logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		} else {
			mgwSwagger.SetInfoSwagger(apiData2)
		}

	} else if swaggerVerison == "3" {
		//map json to struct
		var apiData3 *openapi3.Swagger

		err = json.Unmarshal(apiJsn, &apiData3)
		if err != nil {
			logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		} else {
			mgwSwagger.SetInfoOpenAPI(*apiData3)
		}
	}
	mgwSwagger.SetXWso2Extenstions()
	return mgwSwagger
}

// GetOpenAPIVersionAndJSONContent get the json content and openapi version
// The input can be either json content or yaml content
// TODO: (VirajSalaka) Use the MGWSwagger instead of this.
func GetOpenAPIVersionAndJSONContent(apiContent []byte) (string, []byte, error) {
	apiJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		logger.LoggerOasparser.Error("Error converting api file to json:", err)
		return "", apiContent, err
	}
	swaggerVerison := utills.FindSwaggerVersion(apiJsn)
	return swaggerVerison, apiJsn, nil
}

// GetOpenAPIV3Struct converts the json content to the openAPIv3 struct
// TODO: (VirajSalaka) Use the MGWSwagger instead of this.
func GetOpenAPIV3Struct(openAPIJson []byte) (openapi3.Swagger, error) {
	var apiData3 openapi3.Swagger

	err := json.Unmarshal(openAPIJson, &apiData3)
	if err != nil {
		logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		return apiData3, err
	}
	return apiData3, nil
}

// GetOpenAPIV2Struct converts the json content to the openAPIv2 struct
// TODO: (VirajSalaka) Use the MGWSwagger instead of this.
func GetOpenAPIV2Struct(openAPIJson []byte) (spec.Swagger, error) {
	var apiData2 spec.Swagger
	err := json.Unmarshal(openAPIJson, &apiData2)
	if err != nil {
		logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		return apiData2, err
	}
	return apiData2, nil
}

// GetXWso2Labels returns the labels provided using x-wso2-label extension.
// If extension does not exit it would return 'default'
//TODO: (VirajSalaka) generalize this with openAPI3 getLabels method.
func GetXWso2Labels(vendorExtensionsMap map[string]interface{}) []string {
	var labelArray []string
	if y, found := vendorExtensionsMap["x-wso2-label"]; found {
		if val, ok := y.([]interface{}); ok {
			for _, label := range val {
				labelArray = append(labelArray, label.(string))
			}
			return labelArray
		}
		logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
	}
	return []string{"default"}
}
