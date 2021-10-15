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
	"testing"
)

func TestGetEnvironmentsToBeDeleted(t *testing.T) {
	tests := []struct {
		name                     string
		existingEnvs, deleteEnvs []string
		toBeDel, toBeKept        []string
	}{
		{
			// Delete all envs
			name:         "Delete_all_environments_when_envs_supplied",
			existingEnvs: []string{"Label1", "Label2"},
			deleteEnvs:   []string{"Label1", "Label2"},
			toBeDel:      []string{"Label1", "Label2"},
			toBeKept:     []string{},
		},
		{
			// Delete all envs
			name:         "Delete_all_environments_when_no_envs_supplied",
			existingEnvs: []string{"Label1", "Label2", "Label3"},
			deleteEnvs:   []string{},
			toBeDel:      []string{"Label1", "Label2", "Label3"},
			toBeKept:     []string{},
		},
		{
			// Delete all envs
			name:         "Delete_all_environments_when_no_envs_supplied_nil",
			existingEnvs: []string{"Label1", "Label2", "Label3"},
			deleteEnvs:   nil,
			toBeDel:      []string{"Label1", "Label2", "Label3"},
			toBeKept:     []string{},
		},
		{
			// Delete some envs
			name:         "Delete_some_envs",
			existingEnvs: []string{"Label1", "Label2", "Label3"},
			deleteEnvs:   []string{"Label2", "Foo"}, // Foo should be ignored
			toBeDel:      []string{"Label2"},
			toBeKept:     []string{"Label1", "Label3"},
		},
		{
			// Delete nothing
			name:         "Delete_nothing",
			existingEnvs: []string{"Label1", "Label2", "Label3"},
			deleteEnvs:   []string{"Foo"}, // Foo should be ignored
			toBeDel:      []string{},
			toBeKept:     []string{"Label1", "Label2", "Label3"},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			toBeDel, toBeKept := getEnvironmentsToBeDeleted(test.existingEnvs, test.deleteEnvs)
			if !reflect.DeepEqual(toBeDel, test.toBeDel) {
				t.Errorf("expected deleted environments %v but found %v", test.toBeDel, toBeDel)
			}
			if !reflect.DeepEqual(toBeKept, test.toBeKept) {
				t.Errorf("expected deleted environments %v but found %v", test.toBeKept, toBeKept)
			}
		})
	}
}

func TestUpdateVhostInternalMaps(t *testing.T) {
	setupInternalMemoryMapsWithTestSamples()
	uuid1 := "111-PetStore-org1"
	vhost1 := "org1.wso2.com"

	// force update existing API with same name and same UUID to same vhost in same environment
	updateVhostInternalMaps(uuid1, "PetStore", "v1", vhost1, []string{"us-region"})
	if _, ok := apiToVhostsMap[uuid1][vhost1]; len(apiToVhostsMap[uuid1]) != 2 || !ok {
		t.Errorf("expected the vhost %v in the map only once", vhost1)
	}
	vhost := apiUUIDToGatewayToVhosts[uuid1]["us-region"]
	if vhost != vhost1 {
		t.Errorf("expected the vhost %v of the API is but found %v", vhost1, vhost)
	}

	// add new API
	uuid2 := "xxx"
	updateVhostInternalMaps(uuid2, "NewAPI", "v1", vhost1, []string{"us-region"})
	if _, ok := apiToVhostsMap[uuid2][vhost1]; len(apiToVhostsMap[uuid2]) != 1 || !ok {
		t.Errorf("expected the vhost %v in the map only once", vhost1)
	}
	vhost = apiUUIDToGatewayToVhosts[uuid2]["us-region"]
	if vhost != vhost1 {
		t.Errorf("expected the vhost %v of the API is but found %v", vhost1, vhost)
	}
}
