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
	"crypto/tls"
	"fmt"
	"mcp-server/pkg/service"
	"net/http"
	"sync"
	"time"
)

var (
	syncOnce        sync.Once
	httpClient      *MCPHTTPClient
	skipVerifying   bool
	maxIdleConns    int
	idleConnTimeout int
)

type MCPHTTPClient struct {
	httpClient *http.Client
	UserAgent  string
}

func InitHttpClient() *MCPHTTPClient {
	skipVerifying = service.GetConfig().Http.Insecure
	maxIdleConns = service.GetConfig().Http.MaxIdleConns
	idleConnTimeout = service.GetConfig().Http.IdleConnTimeout
	syncOnce.Do(func() {
		if httpClient == nil {
			client := http.Client{
				Transport: &http.Transport{
					MaxIdleConns:    maxIdleConns,
					IdleConnTimeout: time.Duration(idleConnTimeout) * time.Second,
					TLSClientConfig: &tls.Config{
						InsecureSkipVerify: skipVerifying,
					},
				},
			}
			httpClient = &MCPHTTPClient{
				httpClient: &client,
				UserAgent:  "Bijira-MCP-Client-Go/0.1",
			}
		}
	})
	return httpClient
}

func (client *MCPHTTPClient) DoRequest(request *http.Request) (*http.Response, error) {
	resp, err := client.httpClient.Do(request)
	if err != nil {
		return nil, err
	}
	return resp, nil
}

func (client *MCPHTTPClient) GenerateRequest(httpRequest *TransformedRequest) (*http.Request, error) {
	var req *http.Request
	var err error
	if httpRequest.Body == nil {
		req, err = http.NewRequest(httpRequest.Method, httpRequest.URL, nil)
	} else {
		req, err = http.NewRequest(httpRequest.Method, httpRequest.URL, httpRequest.Body)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %v", err)
	}
	for k, v := range httpRequest.Headers {
		req.Header.Set(k, v)
	}
	req.Header.Set("User-Agent", client.UserAgent)

	return req, nil
}
