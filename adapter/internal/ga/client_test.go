/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package ga

import (
	"testing"

	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	"github.com/stretchr/testify/assert"
	ga_model "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/ga"
	"google.golang.org/protobuf/types/known/anypb"
)

func TestAddAPIToChannel(t *testing.T) {
	/* Test Deploying a single API
	 */
	api := &ga_model.Api{
		ApiUUID:          "60ed6d12fd11bc53e9a582dd",
		RevisionUUID:     "60efe4fc7cf8af77c987882a",
		OrganizationUUID: "choreoTest",
	}
	var pbArray []*anypb.Any
	pb, _ := anypb.New(api)
	pbArray = append(pbArray, pb)
	testDiscoveryResponse := &discovery.DiscoveryResponse{
		Resources: pbArray,
	}
	isFirstResponse = false
	addAPIToChannel(testDiscoveryResponse)
	// Consume API events from channel.
	gaAPIEvent := <-GAAPIChannel
	// Check API UUID of the API event
	assert.Equal(t, gaAPIEvent.APIUUID, api.ApiUUID,
		"API UUID should be the same")
	// Check Revision UUID of the API event
	assert.Equal(t, gaAPIEvent.RevisionUUID, api.RevisionUUID,
		"Revision UUID should be the same")
	// Check Organization UUID of the API event
	assert.Equal(t, gaAPIEvent.OrganizationUUID, api.OrganizationUUID,
		"Organization UUID should be the same")
}

func TestFetchAPIsFromGA(t *testing.T) {
	/* Test LA startup receiving multiple API events
	 */
	api1 := &ga_model.Api{
		ApiUUID:          "60ed6d12fd11bc53e9a582dd",
		RevisionUUID:     "60efe4fc7cf8af77c987882a",
		OrganizationUUID: "choreoTest1",
	}
	api2 := &ga_model.Api{
		ApiUUID:          "60ed6d12fd11bc53e9a582de",
		RevisionUUID:     "60efe4fc7cf8af77c987882b",
		OrganizationUUID: "choreoTest2",
	}
	api3 := &ga_model.Api{
		ApiUUID:          "60ed6d12fd11bc53e9a582df",
		RevisionUUID:     "60efe4fc7cf8af77c987882c",
		OrganizationUUID: "choreoTest3",
	}
	// Create a API List Map containing API struct
	apiListMap := make(map[string]*ga_model.Api)
	var apiArray []*ga_model.Api
	apiArray = append(apiArray, api1, api2, api3)
	for _, api := range apiArray {
		apiListMap[api.ApiUUID] = api
	}

	var pbArrayStartup []*anypb.Any
	pbAPI1, _ := anypb.New(api1)
	pbAPI2, _ := anypb.New(api2)
	pbAPI3, _ := anypb.New(api3)
	pbArrayStartup = append(pbArrayStartup, pbAPI1, pbAPI2, pbAPI3)

	testDiscoveryResponseStartup := &discovery.DiscoveryResponse{
		Resources: pbArrayStartup,
	}
	isFirstResponse = true
	addAPIToChannel(testDiscoveryResponseStartup)
	// Test FetchAPIsFromGA() function
	startupAPIEventsArray := FetchAPIsFromGA()

	// Check Startup API event array
	for _, startupAPIEvent := range startupAPIEventsArray {
		if _, ok := apiListMap[startupAPIEvent.APIUUID]; ok {
			// Check API UUID of the API event
			assert.Equal(t, startupAPIEvent.APIUUID, apiListMap[startupAPIEvent.APIUUID].ApiUUID,
				"API UUID should be the same")
			// Check Revision UUID of the API event
			assert.Equal(t, startupAPIEvent.RevisionUUID, apiListMap[startupAPIEvent.APIUUID].RevisionUUID,
				"Revision UUID should be the same")
			// Check Organization UUID of the API event
			assert.Equal(t, startupAPIEvent.OrganizationUUID, apiListMap[startupAPIEvent.APIUUID].OrganizationUUID,
				"Organization UUID should be the same")
		}
	}

}
