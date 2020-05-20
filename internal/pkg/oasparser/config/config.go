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
package config

import "time"

const (
	LISTENER_ADDRESS  string = "0.0.0.0"
	LISTENER_PORT     uint32 = 10000
	API_DEFAULT_PORT   uint32 = 8080
	MANAGER_STATPREFIX    string = "ingress_http"
	CLUSTER_CONNECT_TIMEOUT    = 20 * time.Second
)
