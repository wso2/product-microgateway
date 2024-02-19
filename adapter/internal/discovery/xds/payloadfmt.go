/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package xds

import "github.com/wso2/product-microgateway/adapter/config"

func getFilteredKeyManagerConfig(kmConfigMap map[string]interface{}) map[string]interface{} {
	filteredKMConfigMap := make(map[string]interface{})
	conf, _ := config.ReadConfigs()

	for _, retainKey := range conf.Adapter.XdsPayloadFormatter.KeyManagerConfigs.RetainKeys {
		// Does not required to check for case sensitivity as the enforcer reads from a hash map
		val, ok := kmConfigMap[retainKey]
		if ok {
			filteredKMConfigMap[retainKey] = val
		}
	}
	return filteredKMConfigMap
}
