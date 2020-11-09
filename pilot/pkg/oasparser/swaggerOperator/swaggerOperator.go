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
package swaggerOperator

import (
	"encoding/json"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-openapi/spec"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/models/apiDefinition"
	"github.com/wso2/micro-gw/pkg/oasparser/utills"
)

/**
 * Get mgw swagger instance.
 *
 * @param apiContent   Api content as a byte array
 * @return apiDefinition.MgwSwagger  Mgw swagger instance
 */
func GetMgwSwagger(apiContent []byte) apiDefinition.MgwSwagger {
	var mgwSwagger apiDefinition.MgwSwagger

	apiJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		//log.Fatal("Error converting api file to json:", err)
	}

	swaggerVerison := utills.FindSwaggerVersion(apiJsn)

	if swaggerVerison == "2" {
		//map json to struct
		var ApiData2 spec.Swagger
		err = json.Unmarshal(apiJsn, &ApiData2)
		if err != nil {
			//log.Fatal("Error openAPI unmarsheliing: %v\n", err)
			logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		} else {
			mgwSwagger.SetInfoSwagger(ApiData2)
		}

	} else if swaggerVerison == "3" {
		//map json to struct
		var ApiData3 *openapi3.Swagger

		err = json.Unmarshal(apiJsn, &ApiData3)

		if err != nil {
			//log.Fatal("Error openAPI unmarsheliing: %v\n", err)
			logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		} else {
			mgwSwagger.SetInfoOpenApi(*ApiData3)
		}
	}

	mgwSwagger.SetXWso2Extenstions()
	return mgwSwagger
}

func GetOpenAPIVersionAndJsonContent(apiContent []byte) (string, []byte, error) {
	apiJsn, err := utills.ToJSON(apiContent)
	if err != nil {
		logger.LoggerOasparser.Error("Error converting api file to json:", err)
		return "", apiContent, err
	}
	swaggerVerison := utills.FindSwaggerVersion(apiJsn)
	return swaggerVerison, apiJsn, nil
}

func GetOpenAPIV3Struct(openAPIJson []byte) (openapi3.Swagger, error) {
	var apiData3 openapi3.Swagger

	err := json.Unmarshal(openAPIJson, &apiData3)
	if err != nil {
		logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		return apiData3, err
	}
	return apiData3, nil
}

func GetOpenAPIV2Struct(openAPIJson []byte) (spec.Swagger, error) {
	var apiData2 spec.Swagger
	err := json.Unmarshal(openAPIJson, &apiData2)
	if err != nil {
		//log.Fatal("Error openAPI unmarsheliing: %v\n", err)
		logger.LoggerOasparser.Error("Error openAPI unmarsheliing", err)
		return apiData2, err
	}
	return apiData2, nil
}

//TODO: (VirajSalaka) generalize this with openAPI3 getLabels method
func GetXWso2Labels(vendorExtensionsMap map[string]interface{}) []string {
	var labelArray []string
	if y, found := vendorExtensionsMap["x-wso2-label"]; found {
		if val, ok := y.([]interface{}); ok {
			for _, label := range val {
				labelArray = append(labelArray, label.(string))
			}
			return labelArray
		} else {
			logger.LoggerOasparser.Errorln("Error while parsing the x-wso2-label")
		}
	}
	return []string{"default"}
}

/**
 * Check availability of endpoint.
 *
 * @param endpoints  Api endpoints array
 * @return bool Availability as a bool value
 */
func IsEndpointsAvailable(endpoints []apiDefinition.Endpoint) bool {
	if len(endpoints) > 0 {
		return true
	} else {
		return false
	}
}
