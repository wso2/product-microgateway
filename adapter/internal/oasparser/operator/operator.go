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
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/utills"
)

// GetOpenAPIVersionAndJSONContent get the json content and openapi version
// The input can be either json content or yaml content
// TODO: (VirajSalaka) Use the MGWSwagger instead of this.
func GetOpenAPIVersionAndJSONContent(apiContent []byte) (string, []byte, error) {
	apiJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		logger.LoggerOasparser.Error("Error converting api file to json:", err)
		return "", apiContent, err
	}
	swaggerVersion := utills.FindAPIDefinitionVersion(apiJsn)
	return swaggerVersion, apiJsn, nil
}

// GetOpenAPIV3Struct converts the json content to the openAPIv3 struct
// TODO: (VirajSalaka) Use the MGWSwagger instead of this.
func GetOpenAPIV3Struct(openAPIJson []byte) (openapi3.T, error) {
	var apiData3 openapi3.T

	err := json.Unmarshal(openAPIJson, &apiData3)
	if err != nil {
		logger.LoggerOasparser.Error("Error openAPI unmarshalling", err)
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
		logger.LoggerOasparser.Error("Error openAPI unmarshalling", err)
		return apiData2, err
	}
	return apiData2, nil
}

// GetXWso2Labels returns the labels provided using x-wso2-label extension.
// If extension does not exit it would return 'default'
// TODO: (VirajSalaka) generalize this with openAPI3 getLabels method.
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

/*
GetXWso2LabelsWebSocket returns a string array of labels provided using extensions.
For web sockets, since we are using the api.yaml file, need to figure out a way
to pass labels. Currently value "DefaultGatewayName" is returned
*/
func GetXWso2LabelsWebSocket(webSocketAPIDef model.MgwSwagger) []string {
	return []string{config.DefaultGatewayName}
}
