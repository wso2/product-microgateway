/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"reflect"
	"sort"
	"testing"
)

func TestGetVhostOfAPI(t *testing.T) {
	setupInternalMemoryMapsWithTestSamples()
	tests := []struct {
		name              string
		uuid, environment string
		vhost             string
		exists            bool
	}{
		{
			name:        "Get_vhost_of_existing_uuid_and_environment",
			uuid:        "222-PetStore-org2",
			environment: "Default",
			vhost:       "org2.foo.com",
			exists:      true,
		},
		{
			name:        "Get_vhost_of_existing_uuid_and_not_existing_environment",
			uuid:        "222-PetStore-org2",
			environment: "us-region",
			vhost:       "",
			exists:      false,
		},
		{
			name:        "Get_vhost_of_not_existing_uuid",
			uuid:        "xxx",
			environment: "us-region",
			vhost:       "",
			exists:      false,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			vhost, exists := GetVhostOfAPI(test.uuid, test.environment)
			if vhost != test.vhost {
				t.Errorf("expected vhost %v but found %v", test.vhost, vhost)
			}
			if exists != test.exists {
				t.Errorf("expected existing bool value %v but found %v", test.exists, exists)
			}
		})
	}
}

func TestGetAllEnvironments(t *testing.T) {
	setupInternalMemoryMapsWithTestSamples()

	tests := []struct {
		name            string
		uuid, vhost     string
		newEnvironment  []string
		allEnvironments []string
	}{
		{
			name:            "No_existing_environments_new_API",
			uuid:            "new-uuid-xxxxx",
			vhost:           "us.wso2.com",
			newEnvironment:  []string{"Default", "eu-region"},
			allEnvironments: []string{"Default", "eu-region"},
		},
		{
			name:            "Existing_API_new_vhost",
			uuid:            "333-Pizza-org1",
			vhost:           "new.vhost",
			newEnvironment:  []string{"Default", "eu-region"},
			allEnvironments: []string{"Default", "eu-region"},
		},
		{
			name:            "Some_existing_environments",
			uuid:            "222-PetStore-org2",
			vhost:           "org2.foo.com",
			newEnvironment:  []string{"eu-region"},
			allEnvironments: []string{"Default", "eu-region"},
		},

		{
			name:            "new_environments",
			uuid:            "222-PetStore-org2",
			vhost:           "org2.foo.com",
			newEnvironment:  []string{"Default", "eu-region"},
			allEnvironments: []string{"Default", "eu-region"},
		},
		{
			name:            "All_existing_environments",
			uuid:            "111-PetStore-org",
			vhost:           "org2.foo.com",
			newEnvironment:  []string{"Default", "us-region"},
			allEnvironments: []string{"Default", "us-region"},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			allEnvironments := GetAllEnvironments(test.uuid, test.vhost, test.newEnvironment)
			sort.Strings(allEnvironments)
			sort.Strings(test.allEnvironments)
			if !reflect.DeepEqual(test.allEnvironments, allEnvironments) {
				t.Errorf("expected environments %v but found %v", test.allEnvironments, allEnvironments)
			}
		})
	}
}

func setupInternalMemoryMapsWithTestSamples() {
	apiToVhostsMap = map[string]map[string]struct{}{
		// The same API name:version is deployed in two org with two gateway environments
		"PetStore:v1": {"org1.wso2.com": void, "org2.foo.com": void},
		"Pizza:v1":    {"org1.foo.com": void, "org2.foo.com": void, "org2.wso2.com": void},
	}
	apiUUIDToGatewayToVhosts = map[string]map[string]string{
		// PetStore:v1 in Org1
		"111-PetStore-org1": {
			"Default":   "org1.wso2.com",
			"us-region": "org1.wso2.com",
		},
		// PetStore:v1 in Org2
		"222-PetStore-org2": {
			"Default": "org2.foo.com",
		},
		// Pizza:v1 in Org1
		"333-Pizza-org1": {
			"us-region": "org1.foo.com",
		},
		// Pizza:v1 in Org2
		"444-Pizza-org2": {
			"Default":   "org2.foo.com",
			"us-region": "org2.wso2.com",
		},
	}
}
