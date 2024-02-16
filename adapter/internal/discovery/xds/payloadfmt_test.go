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

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetFilteredKeyManagerConfig(t *testing.T) {
	kmConfigMap := map[string]interface{}{
		"claim_mappings":     []string{},
		"authorize_endpoint": "https://api.asgardeo.io/t/renukafernando/oauth2/authorize",
		"grant_types": []string{
			"refresh_token ",
			"password",
			"client_credentials",
			"authorization_code",
			"implicit",
		},
		"enable_oauth_app_creation":      true,
		"certificate_value":              "https://api.asgardeo.io/t/renukafernando/oauth2/jwks",
		"enable_map_oauth_consumer_apps": false,
		"enable_token_hash":              false,
		"revoke_endpoint":                "https://api.asgardeo.io/t/renukafernando/oauth2/revoke",
		"well_known_endpoint":            "https://api.asgardeo.io/t/renukafernando/oauth2/token/.well-known/openid-configuration",
		"self_validate_jwt":              true,
		"scopes_claim":                   "scope",
		"enable_token_encryption":        false,
		"client_registration_endpoint":   "https://api.asgardeo.io/t/renukafernando/api/server/v1",
		"logout_endpoint":                "https://api.asgardeo.io/t/renukafernando/oidc/logout",
		"consumer_key_claim":             "azp",
		"certificate_type":               "JWKS",
		"token_endpoint":                 "https://api.asgardeo.io/t/renukafernando/oauth2/token",
	}
	expectedKmConfigMap := map[string]interface{}{
		"claim_mappings":     []string{},
		"certificate_value":  "https://api.asgardeo.io/t/renukafernando/oauth2/jwks",
		"self_validate_jwt":  true,
		"scopes_claim":       "scope",
		"consumer_key_claim": "azp",
		"certificate_type":   "JWKS",
	}

	filteredConfig := getFilteredKeyManagerConfig(kmConfigMap)
	assert.Equal(t, expectedKmConfigMap, filteredConfig, "Filtered Key Manager Configuration is not as expected")
}
