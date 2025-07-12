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
package client

import (
	"bytes"
	"encoding/json"
	"io"
	"mcp-server/pkg/service"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

var logger = service.GetLogger()

type MCPResult struct {
	Code      int    `json:"code"`
	Response  any    `json:"response"`
	SessionID string `json:"sessionId"`
	Error     bool   `json:"error"`
}

type ThirdPartyRequest struct {
	Endpoint string            `json:"endpoint"`
	Body     map[string]any    `json:"body"`
	Headers  map[string]string `json:"headers"`
}

func ServeThirdPartyRequest(c *gin.Context) {
	var payload ThirdPartyRequest
	if err := c.ShouldBindJSON(&payload); err != nil {
		logger.Error("Failed to bind JSON", "error", err)
		c.SecureJSON(400, getResult(400, gin.H{"error": "Invalid request payload"}, "", true))
		return
	}
	if payload.Body == nil {
		logger.Error("Request body is required")
		c.SecureJSON(400, getResult(400, gin.H{"error": "Missing request body"}, "", true))
		return
	}

	reqBody, err := json.Marshal(payload.Body)
	if err != nil {
		logger.Error("Failed to marshal request body", "error", err)
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to marshal request body"}, "", true))
		return
	}

	bodyReader := bytes.NewReader(reqBody)

	req, err := generateThirdPartyRequest(payload.Endpoint, bodyReader, payload.Headers)
	if err != nil {
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to generate request"}, "", true))
		return
	}

	resp, err := callMCPServer(req)
	if err != nil {
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to call the MCP Server"}, "", true))
		return
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		logger.Error("Failed to read the response from MCP Server", "error", err)
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to read the response"}, "", true))
		return
	}
	jsonResponse, err := processResponseJson(string(respBody))
	if err != nil {
		logger.Error("Failed to process JSON response", "error", err)
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to read the response"}, "", true))
		return
	}

	headers := resp.Header
	if (headers.Get("mcp-session-id") != "") && (headers.Get("mcp-session-id") != "null") {
		sessionID := headers.Get("mcp-session-id")
		c.SecureJSON(200, getResult(resp.StatusCode, json.RawMessage(jsonResponse), sessionID, false))
	} else {
		c.SecureJSON(200, getResult(resp.StatusCode, json.RawMessage(jsonResponse), "", false))
	}

}

func generateThirdPartyRequest(endpoint string, body *bytes.Reader, additionalHeaders map[string]string) (*http.Request, error) {
	var request *http.Request
	request, err := http.NewRequest(http.MethodPost, endpoint, body)
	if err != nil {
		logger.Error("Failed to create new HTTP request", "error", err)
		return nil, err
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Accept", "application/json, text/event-stream")

	for k, v := range additionalHeaders {
		request.Header.Set(k, v)
	}

	return request, nil
}

func callMCPServer(req *http.Request) (*http.Response, error) {
	client := InitHttpClient()
	resp, err := client.Do(req)
	if err != nil {
		logger.Error("Failed to send request to MCP server", "error", err)
		return nil, err
	}
	return resp, nil
}

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

func getResult(code int, response any, sessionID string, isError bool) MCPResult {
	return MCPResult{
		Code:      code,
		Response:  response,
		SessionID: sessionID,
		Error:     isError,
	}
}
