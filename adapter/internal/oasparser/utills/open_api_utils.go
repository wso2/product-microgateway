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

// Package utills holds the implementation for common utility functions
package utills

import (
	"bytes"
	"encoding/json"
	"strings"

	// TODO: (VirajSalaka) remove outdated dependency
	"unicode"

	"github.com/ghodss/yaml"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// ToJSON converts a single YAML document into a JSON document
// or returns an error. If the document appears to be JSON the
// YAML decoding path is not used.
// If the input file is json, it would be returned as it is.
func ToJSON(data []byte) ([]byte, error) {
	if hasJSONPrefix(data) {
		return data, nil
	}
	return yaml.YAMLToJSON(data)
}

var jsonPrefix = []byte("{")

func hasJSONPrefix(buf []byte) bool {
	return hasPrefix(buf, jsonPrefix)
}

func hasPrefix(buf []byte, prefix []byte) bool {
	trim := bytes.TrimLeftFunc(buf, unicode.IsSpace)
	return bytes.HasPrefix(trim, prefix)
}

// FindAPIDefinitionVersion finds the openapi version ("2" or "3") or Async API version for the given
// API Definition.
func FindAPIDefinitionVersion(jsn []byte) string {
	var version string = "2"
	var result map[string]interface{}

	err := json.Unmarshal(jsn, &result)
	if err != nil {
		logger.LoggerOasparser.Error("Error while JSON unmarshalling to find the API definition version.", err)
	}

	if _, ok := result["swagger"]; ok {
		return "2"
	} else if _, ok := result["openapi"]; ok {
		if versionNumber, ok := result["openapi"].(string); ok {
			if strings.HasPrefix(versionNumber, "3.0") {
				return "3"
			} else if strings.HasPrefix(versionNumber, "3.1") {
				return "3.1"
			}
			logger.LoggerOasparser.Errorf("OpenAPI version %v is not supported.", versionNumber)
			return "not_supported_openapi_version" + versionNumber
		}
	} else if versionNumber, ok := result["asyncapi"]; ok {
		if strings.HasPrefix(versionNumber.(string), "2") {
			return "asyncapi_2"
		}
		logger.LoggerOasparser.Errorf("AsyncAPI version %v is not supported.", versionNumber.(string))
		return "not_supported_asyncapi_version" + versionNumber.(string)
	}
	logger.LoggerOasparser.Error("API definition version is not defined.")
	return version
}
