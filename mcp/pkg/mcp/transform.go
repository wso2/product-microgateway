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
	"bytes"
	"encoding/json"
	"encoding/xml"
	"fmt"
	"net/http"
	"net/url"
	"strings"
)

func transformMCPRequest(mcpRequest *MCPRequest) (*TransformedRequest, error) {
	httpRequest := &TransformedRequest{
		Headers: make(map[string]string),
	}

	method, err := processHTTPMethod(mcpRequest.API.Verb)
	if err != nil {
		logger.Error("Failed to process HTTP method", "error", err)
		return nil, err
	}
	httpRequest.Method = method

	schemaMapping, err := processSchema(mcpRequest.Schema)
	if err != nil {
		logger.Error("Failed to process schema", "error", err)
		return nil, err
	}

	ep, err := processEndpoint(mcpRequest, schemaMapping)
	if err != nil {
		logger.Error("Failed to process endpoint", "error", err)
		return nil, err
	}
	httpRequest.URL = ep

	headers, err := processHeaderParameters(mcpRequest, schemaMapping)
	if err != nil {
		logger.Error("Failed to process header parameters", "error", err)
		return nil, err
	}
	httpRequest.Headers = headers

	if method == http.MethodPost || method == http.MethodPut || method == http.MethodPatch {
		bytesReader, err := processRequestBody(mcpRequest, schemaMapping)
		if err != nil {
			logger.Error("Failed to process request body", "error", err)
			return nil, err
		}
		if bytesReader != nil {
			httpRequest.Body = bytesReader
		} else {
			logger.Warn("Request body is nil")
		}
	}

	return httpRequest, nil
}

func processHTTPMethod(verb string) (string, error) {
	method := strings.ToUpper(verb)
	switch method {
	case "GET":
		return http.MethodGet, nil
	case "POST":
		return http.MethodPost, nil
	case "PUT":
		return http.MethodPut, nil
	case "DELETE":
		return http.MethodDelete, nil
	case "PATCH":
		return http.MethodPatch, nil
	case "OPTIONS":
		return http.MethodOptions, nil
	default:
		return "", fmt.Errorf("unsupported HTTP method: %s", verb)
	}
}

// processEndpoint constructs the endpoint URL for the request.
// It processes path parameters and query parameters based on the schema mapping.
// Returns the transformed endpoint URL or an error if the endpoint is invalid.
func processEndpoint(mcpRequest *MCPRequest, schemaMapping *SchemaMapping) (string, error) {
	args, err := parseArgs(mcpRequest)
	if err != nil {
		logger.Error("Failed to parse arguments", "error", err)
		return "", err
	}

	endpoint := mcpRequest.API.Endpoint
	transformedEp := ""
	if strings.HasPrefix(endpoint, "http://") || strings.HasPrefix(endpoint, "https://") {
		sanitizedEp := sanitizeStringSlashes(endpoint)
		sainitizedContext := sanitizeStringSlashes(mcpRequest.API.Context)
		sanitizedVersion := sanitizeStringSlashes(mcpRequest.API.Version)
		sanitizedPath := sanitizeStringSlashes(mcpRequest.API.Path)
		transformedEp = fmt.Sprintf("%s/%s/%s/%s", sanitizedEp, sainitizedContext, sanitizedVersion, sanitizedPath)
		// Process path parameters
		transformedEp, err = processPathParameters(args, schemaMapping, transformedEp)
		if err != nil {
			logger.Error("Failed to process path parameters", "error", err)
			return "", err
		}
		// Process query parameters
		queryParams, err := processQueryParameters(args, schemaMapping)
		if err != nil {
			return "", err
		}
		if queryParams != "" {
			transformedEp += queryParams
		}
		return transformedEp, nil
	}
	return "", fmt.Errorf("invalid endpoint: %s", endpoint)
}

// processQueryParameters generates a query string from the provided arguments and schema mapping.
// It URL-encodes parameter names and values and appends them to the query string.
// Returns the constructed query string or an error if required parameters are missing.
func processQueryParameters(args map[string]any, schemaMapping *SchemaMapping) (string, error) {
	queryParams := schemaMapping.QueryParameters
	if len(queryParams) > 0 {
		queryString := "?"
		for _, param := range queryParams {
			paramName := param.Name
			paramValue := args[paramName]
			if param.Required && paramValue == nil {
				logger.Error("Required query parameter value is not available", "parameter", paramName)
				return "", fmt.Errorf("required query parameter %s is missing", paramName)
			} else if paramValue == nil {
				logger.Warn("Query parameter value is not available", "parameter", paramName)
				continue
			}
			// URL encode the parameter name and value
			urlEncodedParam := url.PathEscape(fmt.Sprintf("%v", paramName))
			urlEncodedValue := url.PathEscape(fmt.Sprintf("%v", paramValue))
			queryString += fmt.Sprintf("%s=%s&", urlEncodedParam, urlEncodedValue)
		}
		queryString = strings.TrimSuffix(queryString, "&")
		return queryString, nil
	}
	return "", nil
}

// processPathParameters replaces placeholders in the URL with actual values from the arguments.
// It URL-encodes parameter names and values before substitution.
// Returns the transformed URL with path parameters replaced.
func processPathParameters(args map[string]any, schemaMapping *SchemaMapping, unProcessedUrl string) (string, error) {
	pathParams := schemaMapping.PathParameters
	transformedUrl := unProcessedUrl
	if len(pathParams) > 0 {
		for _, param := range pathParams {
			paramValue := args[param]
			if paramValue == nil {
				logger.Error("Path parameter value is not available", "parameter", param)
				return "", fmt.Errorf("path parameter %s is missing", param)
			}
			// URL encode the parameter name and value
			urlEncodedParam := url.PathEscape(fmt.Sprintf("%v", param))
			urlEncodedValue := url.PathEscape(fmt.Sprintf("%v", paramValue))
			processedUrl := strings.Replace(transformedUrl, "{"+urlEncodedParam+"}", urlEncodedValue, 1)
			transformedUrl = processedUrl
		}
	}
	return transformedUrl, nil
}

// processHeaderParameters generates a map of header parameters from the provided arguments and schema mapping.
// Returns a map of header names and values.
func processHeaderParameters(mcpRequest *MCPRequest, schemaMapping *SchemaMapping) (map[string]string, error) {
	args, err := parseArgs(mcpRequest)
	if err != nil {
		logger.Error("Failed to parse arguments", "error", err)
		return nil, err
	}
	headers := make(map[string]string)
	headerParams := schemaMapping.HeaderParameters
	if len(headerParams) > 0 {
		for _, param := range headerParams {
			paramName := param.Name
			paramValue := args[paramName]
			if param.Required && paramValue == nil {
				logger.Error("Required query parameter value is not available", "parameter", paramName)
				return nil, fmt.Errorf("required query parameter %s is missing", paramName)
			} else if paramValue == nil {
				logger.Warn("Query parameter value is not available", "parameter", paramName)
				continue
			}
			headers[paramName] = fmt.Sprintf("%v", paramValue)
		}
	}
	// Add authentication header if provided
	if mcpRequest.API.Auth != "" {
		k, v, found := strings.Cut(mcpRequest.API.Auth, ":")
		if found {
			headers[k] = strings.TrimSpace(v)
		}
	}
	// Add content type header
	if schemaMapping.ContentType != "" {
		headers[ContentType] = schemaMapping.ContentType
	} else {
		headers[ContentType] = ContentTypeJSON
	}
	return headers, nil
}

// processRequestBody processes the request body from the MCP request.
// Returns a bytes.Reader for the request body or an error if the body is invalid.
func processRequestBody(mcpRequest *MCPRequest, schemaMapping *SchemaMapping) (*bytes.Reader, error) {
	contentType := schemaMapping.ContentType
	args, err := parseArgs(mcpRequest)
	if err != nil {
		logger.Error("Failed to parse arguments", "error", err)
		return nil, err
	}

	var body map[string]any
	if args["requestBody"] != nil {
		body = args["requestBody"].(map[string]any)
		if contentType == ContentTypeJSON {
			jsonString, err := json.Marshal(body)
			if err != nil {
				logger.Error("Failed to marshal request body", "error", err)
				return nil, err
			}
			byteArray := []byte(jsonString)
			bodyReader := bytes.NewReader(byteArray)
			return bodyReader, nil
		} else if contentType == ContentTypeXML {
			//todo: Need to figure out how to handle the root element name
			root := XMLElement{XMLName: xml.Name{Local: "Body"}, Children: mapToXMLElements(body)}
			xmlString, err := xml.MarshalIndent(root, "", "  ")
			if err != nil {
				logger.Error("Failed to marshal request body", "error", err)
				return nil, err
			}
			byteArray := []byte(xmlString)
			bodyReader := bytes.NewReader(byteArray)
			return bodyReader, nil
		} else {
			logger.Error("Unsupported content type", "contentType", contentType)
			return nil, fmt.Errorf("unsupported content type: %s", contentType)
		}
	}
	return nil, nil
}

func parseArgs(mcpRequest *MCPRequest) (map[string]any, error) {
	var args map[string]any
	if mcpRequest.Arguments != "" {
		err := json.Unmarshal([]byte(mcpRequest.Arguments), &args)
		if err != nil {
			logger.Error("Failed to unmarshal arguments", "error", err)
			return nil, err
		}
	}
	return args, nil
}

func sanitizeStringSlashes(input string) string {
	// Remove any trailing or preceding slashes from the input
	return strings.TrimSuffix(strings.TrimPrefix(input, "/"), "/")
}
