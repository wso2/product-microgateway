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
	"crypto/tls"
	"mcp-server/pkg/service"
	"net/http"
	"sync"
	"time"
)

var (
	syncOnce        sync.Once
	httpClient      *http.Client
	skipVerifying   bool
	maxIdleConns    int
	idleConnTimeout int
)

func InitHttpClient() *http.Client {
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
			httpClient = &client
		}
	})
	return httpClient
}
