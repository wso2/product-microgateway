/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package mcp

import (
	"encoding/json"
	"encoding/xml"
	"fmt"
	"strings"
)

func processJsonResponse(inputString string) (string, error) {
	var data any

	err := json.Unmarshal([]byte(inputString), &data)
	if err != nil {
		logger.Error("Failed to unmarshal JSON", "error", err)
		return "", err
	}

	compactJSONBytes, err := json.Marshal(data)
	if err != nil {
		logger.Error("Error marshalling data back to JSON", "error", err)
		return "", err
	}

	return string(compactJSONBytes), nil
}

func mapToXMLElements(m map[string]any) []XMLElement {
	elements := []XMLElement{}
	for k, v := range m {
		elem := XMLElement{XMLName: xml.Name{Local: k}}

		switch val := v.(type) {
		case string:
			elem.Content = val
		case map[string]any:
			elem.Children = mapToXMLElements(val)
		case []any:
			for _, item := range val {
				if itemMap, ok := item.(map[string]any); ok {
					elem.Children = append(elem.Children, mapToXMLElements(itemMap)...)
				} else {
					elem.Children = append(elem.Children, XMLElement{XMLName: xml.Name{Local: "item"}, Content: fmt.Sprint(item)})
				}
			}
		default:
			elem.Content = fmt.Sprint(val)
		}

		elements = append(elements, elem)
	}
	return elements
}

// processResponseJson processes the input string as JSON, handling both standard JSON and event stream responses.
func processResponseJson(inputString string) (string, error) {
	var data any

	if inputString == "" {
		logger.Warn("Received an empty response")
		return "{}", nil
	}
	err := json.Unmarshal([]byte(inputString), &data)
	if err != nil {
		logger.Warn("Failed to unmarshal JSON", "cause", err)
		logger.Info("Attempting to process as an event stream response...")
		if strings.Contains(inputString, "data:") {
			lines := strings.Split(inputString, "\n")
			for _, line := range lines {
				if strings.HasPrefix(line, "data:") {
					jsonLine := strings.TrimPrefix(line, "data:")
					jsonLine = strings.TrimSpace(jsonLine)
					if jsonLine != "" {
						err = json.Unmarshal([]byte(jsonLine), &data)
						if err != nil {
							logger.Error("Failed to unmarshal JSON from event stream", "line", jsonLine, "error", err)
							return "", err
						}
						break
					}
				}
			}
		} else {
			logger.Error("Failed to process response as JSON", "error", err)
			return "", err
		}
	}

	compactJSONBytes, err := json.Marshal(data)
	if err != nil {
		logger.Error("Error marshalling data back to JSON", "error", err)
		return "", err
	}

	return string(compactJSONBytes), nil
}

func HandleBadRequest(message string) Result {
	return Result{
		Code:     400,
		Response: message,
	}
}
