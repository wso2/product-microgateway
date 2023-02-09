/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package messaging

import (
	"encoding/base64"
	"io/ioutil"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
)

func TestHandleKeyManagerEvents(t *testing.T) {

	// Event: adding a key manager: AsgardeoDevKM iss: https://dev.api.asgardeo.io/t/malinthaa/oauth2/token
	var addKMEvent1 = readAndDecodeEventFromFile(t, "keymgt-events/add_km1.txt")
	// Event: adding a key manager: AsgardeoDevKM2 iss: https://dev.api.asgardeo.io/t/malinthaamarasinghe/oauth2/token
	var addKMEvent2 = readAndDecodeEventFromFile(t, "keymgt-events/add_km2.txt")
	// Event: updating a key manager: AsgardeoDevKM2 iss changed to https://dev.api.asgardeo.io/t/malinthaamarasinghe2/oauth2/token
	var updateKMEvent = readAndDecodeEventFromFile(t, "keymgt-events/update_km2.txt")
	// Event: deleting a key manager: AsgardeoDevKM2
	var deleteKMEvent = readAndDecodeEventFromFile(t, "keymgt-events/delete_km2.txt")

	// add one key manager
	handleKeyManagerEvents(addKMEvent1)
	assert.Equal(t, 1, len(xds.KeyManagerList), "Key Manager list is not populated with the Key Manager from Event")
	assert.Equal(t, "AsgardeoDevKM", xds.KeyManagerList[0].Name, "Key Manager list is not populated properly with the Key Manager from Event")

	// try to add the same key manager again - should not duplicate internal the KM list
	handleKeyManagerEvents(addKMEvent1)
	assert.Equal(t, 1, len(xds.KeyManagerList), "Key Manager list might have been populated with duplicated entries")

	// add onother key manager
	handleKeyManagerEvents(addKMEvent2)
	assert.Equal(t, 2, len(xds.KeyManagerList), "Key Manager list is not populated with the new Key Manager from Event")

	found := false
	for _, keyManager := range xds.KeyManagerList {
		if strings.EqualFold(keyManager.Name, "AsgardeoDevKM2") {
			found = true
			issuer := keyManager.Configuration["issuer"]
			assert.Equal(t, "https://dev.api.asgardeo.io/t/malinthaamarasinghe/oauth2/token", issuer,
				"Key Manager list is not populated with the new Key Manager from Event. Configuration is incorrect.")
		}
	}
	assert.True(t, found, "Key Manager list is not populated properly with the new Key Manager from Event")

	// update the second key manager
	handleKeyManagerEvents(updateKMEvent)
	assert.Equal(t, 2, len(xds.KeyManagerList), "Key Manager list is not populated with the updated Key Manager from Event")
	found = false
	for _, keyManager := range xds.KeyManagerList {
		if strings.EqualFold(keyManager.Name, "AsgardeoDevKM2") {
			found = true
			issuer := keyManager.Configuration["issuer"]
			assert.Equal(t, "https://dev.api.asgardeo.io/t/malinthaamarasinghe2/oauth2/token", issuer,
				"Key Manager list is not populated with the updated Key Manager from Event. Configuration is incorrect.")
		}
	}

	// delete the second key manager
	handleKeyManagerEvents(deleteKMEvent)
	assert.Equal(t, 1, len(xds.KeyManagerList), "Key Manager is not removed from the Delete Key Manager Event")

	found = false
	for _, keyManager := range xds.KeyManagerList {
		if strings.EqualFold(keyManager.Name, "AsgardeoDevKM2") {
			found = true
		}
	}
	assert.False(t, found, "Key Manager is not deleted populated properly from the delete Key Manager Event")
}

func readAndDecodeEventFromFile(t *testing.T, path string) []byte {
	fileBytes, err := ioutil.ReadFile(config.GetMgwHome() + "/../adapter/test-resources/" + path)
	assert.Nil(t, err, "Error while reading the file from path: "+path)
	decoded, err := base64.StdEncoding.DecodeString(string(fileBytes))
	assert.Nil(t, err, "Error while decoding (base64) data from the file from path: "+path)
	return decoded
}
