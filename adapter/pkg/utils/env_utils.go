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
 *
 */

package utils

import (
	"os"
	"strconv"

)

// IsGraphQLEnabled returns true only when MGW_ENABLE_GRAPHQL is set and parses to a true value.
// Unset or invalid values default to false.
func IsGraphQLEnabled() bool {
	if v, ok := os.LookupEnv("MGW_ENABLE_GRAPHQL"); ok {
		b, _ := strconv.ParseBool(v)
		return b
	}
	return false
}