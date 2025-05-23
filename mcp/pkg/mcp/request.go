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
	"context"
	"io"
	"mcp-server/pkg/service"
	"net/http"
	"strings"
)

var logger = service.GetLogger()

func CallUnderlyingAPI(ctx context.Context, payload *MCPRequest) (string, int, error) {
	httpClient := InitHttpClient()
	httpRequest, err := transformMCPRequest(payload)
	if err != nil {
		logger.ErrorContext(ctx, "Failed to transform request", "error", err)
		return "", http.StatusInternalServerError, err
	}
	request, err := httpClient.GenerateRequest(httpRequest)
	if err != nil {
		logger.ErrorContext(ctx, "Failed to generate request", "error", err)
		return "", http.StatusInternalServerError, err
	}
	resp, err := httpClient.DoRequest(request)
	if err != nil {
		logger.ErrorContext(ctx, "Failed to send request", "error", err)
		return "", http.StatusInternalServerError, err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		logger.ErrorContext(ctx, "Failed to read response body", "error", err)
		return "", http.StatusInternalServerError, err
	}
	response := string(body)
	if strings.Contains(resp.Header.Get(ContentType), ContentTypeJSON) {
		response, err = processJsonResponse(response)
		if err != nil {
			logger.ErrorContext(ctx, "Failed to process JSON response", "error", err)
			return "", http.StatusInternalServerError, err
		}
	}
	return response, resp.StatusCode, nil
}
