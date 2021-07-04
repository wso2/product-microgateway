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
 *
 */

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
)

func TestRemoveApplication(t *testing.T) {
	application1 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440000", ID: 1,
		Name: "app1"}
	application2 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440001", ID: 2,
		Name: "app2"}
	application3 := types.Application{UUID: "123e4567-e89b-42d3-a456-556642440003", ID: 3,
		Name: "app3"}
	var appArray []types.Application
	appArray = append(appArray, application1)
	appArray = append(appArray, application2)
	appArray = append(appArray, application3)

	appArray = removeApplication(appArray, application1.UUID)
	assert.Len(t, appArray, 2)
	appArray = removeApplication(appArray, application1.UUID)
	assert.Len(t, appArray, 2)
	assert.Equal(t, application3.ID, appArray[0].ID)
	assert.Equal(t, application2.ID, appArray[1].ID)
}

func TestRemoveSubscription(t *testing.T) {
	sub1 := types.Subscription{SubscriptionID: 1, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440001"}
	sub2 := types.Subscription{SubscriptionID: 2, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440002"}
	sub3 := types.Subscription{SubscriptionID: 2, SubscriptionUUID: "123e4567-e89b-42d3-a456-556642440003"}
	var subArray []types.Subscription
	subArray = append(subArray, sub1)
	subArray = append(subArray, sub2)
	subArray = append(subArray, sub3)

	subArray = removeSubscription(subArray, 1)
	assert.Len(t, subArray, 2)
	subArray = removeSubscription(subArray, 1)
	assert.Len(t, subArray, 2)
	assert.Equal(t, sub3.SubscriptionID, subArray[0].SubscriptionID)
	assert.Equal(t, sub2.SubscriptionID, subArray[1].SubscriptionID)
}

func TestRemoveSubPolicy(t *testing.T) {
	policy1 := types.SubscriptionPolicy{ID: 1}
	policy2 := types.SubscriptionPolicy{ID: 2}
	policy3 := types.SubscriptionPolicy{ID: 3}

	var policyArr []types.SubscriptionPolicy
	policyArr = append(policyArr, policy1)
	policyArr = append(policyArr, policy2)
	policyArr = append(policyArr, policy3)

	policyArr = removeSubPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	policyArr = removeSubPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	assert.Equal(t, policy3.ID, policyArr[0].ID)
	assert.Equal(t, policy2.ID, policyArr[1].ID)
}

func TestRemoveAppPolicy(t *testing.T) {
	policy1 := types.ApplicationPolicy{ID: 1}
	policy2 := types.ApplicationPolicy{ID: 2}
	policy3 := types.ApplicationPolicy{ID: 3}

	var policyArr []types.ApplicationPolicy
	policyArr = append(policyArr, policy1)
	policyArr = append(policyArr, policy2)
	policyArr = append(policyArr, policy3)

	policyArr = removeAppPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	policyArr = removeAppPolicy(policyArr, 1)
	assert.Len(t, policyArr, 2)
	assert.Equal(t, policy3.ID, policyArr[0].ID)
	assert.Equal(t, policy2.ID, policyArr[1].ID)
}
