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
	"io"

	"github.com/gin-gonic/gin"
)

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

	httpClient := InitHttpClient()
	req, err := httpClient.GenerateThirdPartyRequest(payload.Endpoint, bodyReader, payload.Headers)
	if err != nil {
		c.SecureJSON(500, getResult(500, gin.H{"error": "Failed to generate request"}, "", true))
		return
	}

	resp, err := httpClient.DoRequest(req)
	if err != nil {
		logger.Error("Failed to send request to MCP server", "error", err)
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

func getResult(code int, response any, sessionID string, isError bool) MCPResult {
	return MCPResult{
		Code:      code,
		Response:  response,
		SessionID: sessionID,
		Error:     isError,
	}
}
