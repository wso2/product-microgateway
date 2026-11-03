/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
)

const (
	appEventTestUUID = "6f2b1d0e-9c3a-4f1e-8d55-2a7c4b8e1f90"
	appEventTestID   = "4021"
)

// seedApplicationsFromControlPlane mimics the startup pull of
// internal/data/v1/applications, which is the only path that initialises
// xds.ApplicationMap. Events applied before it would write to a nil map.
func seedApplicationsFromControlPlane(attributes map[string]string) {
	xds.MarshalMultipleApplications(&types.ApplicationList{
		List: []types.Application{
			{
				UUID:         appEventTestUUID,
				ID:           4021,
				Name:         "FinancePortal",
				SubName:      "malintha@wso2.com",
				Policy:       "10PerMin",
				TokenType:    "JWT",
				Attributes:   attributes,
				TenantDomain: "carbon.super",
			},
		},
	})
}

// resetApplicationEventState clears the package-level state that
// handleApplicationEvents mutates, so ordering between test cases does not leak.
// isLaterEvent records the last seen timestamp per application id, and
// xds.ApplicationMap persists across tests in this package.
func resetApplicationEventState() {
	delete(applicationListTimeStampMap, appEventTestID)
	xds.ApplicationMap = nil
}

func applicationAttributes(t *testing.T) map[string]string {
	app, ok := xds.ApplicationMap[appEventTestUUID]
	assert.True(t, ok, "Application is not present in the xDS application map")
	return app.GetAttributes()
}

// TestApplicationCreateEventCarriesAttributes covers the spec scenario
// "Properties survive an incremental application create": an application created
// at runtime must reach the enforcer with the same properties the startup pull
// would have supplied.
func TestApplicationCreateEventCarriesAttributes(t *testing.T) {
	resetApplicationEventState()
	defer resetApplicationEventState()

	// The map has to exist before any event is applied; the startup pull owns that.
	seedApplicationsFromControlPlane(nil)

	createEvent := readAndDecodeEventFromFile(t, "application-events/create_app_with_attributes.txt")
	handleApplicationEvents(createEvent, applicationEventType)

	assert.Equal(t, map[string]string{"department": "finance", "planTier": "gold"},
		applicationAttributes(t),
		"Application attributes from the create event did not reach the xDS application map")
}

// TestApplicationEventMatchesControlPlanePull covers the spec requirement that
// neither delivery path yields a narrower record than the other for the same
// application state.
func TestApplicationEventMatchesControlPlanePull(t *testing.T) {
	resetApplicationEventState()
	defer resetApplicationEventState()

	attributes := map[string]string{"department": "finance", "planTier": "gold"}

	// Path 1: bulk retrieval at startup.
	seedApplicationsFromControlPlane(attributes)
	fromPull := applicationAttributes(t)

	// Path 2: the same state delivered as an incremental update event.
	updateEvent := readAndDecodeEventFromFile(t, "application-events/update_app_same_attributes.txt")
	handleApplicationEvents(updateEvent, applicationEventType)
	fromEvent := applicationAttributes(t)

	assert.Equal(t, fromPull, fromEvent,
		"The event path produced a narrower application record than the control plane pull")
}

// TestApplicationUpdateEventReplacesAttributes covers the spec scenario
// "Property removal propagates". The xDS push is full replacement rather than a
// merge, so an update carrying no attributes must clear what was known before.
func TestApplicationUpdateEventReplacesAttributes(t *testing.T) {
	resetApplicationEventState()
	defer resetApplicationEventState()

	seedApplicationsFromControlPlane(map[string]string{"department": "finance", "planTier": "gold"})

	changedEvent := readAndDecodeEventFromFile(t, "application-events/update_app_changed_attributes.txt")
	handleApplicationEvents(changedEvent, applicationEventType)
	assert.Equal(t, map[string]string{"department": "treasury", "planTier": "platinum"},
		applicationAttributes(t),
		"Changed attributes from the update event were not applied")

	emptyEvent := readAndDecodeEventFromFile(t, "application-events/update_app_empty_attributes.txt")
	handleApplicationEvents(emptyEvent, applicationEventType)
	assert.Empty(t, applicationAttributes(t),
		"An update carrying no attributes must clear the previously known properties")
}

// TestApplicationEventOutOfOrderIsDropped guards the isLaterEvent behaviour that
// the reset helper depends on: an event older than what the adapter already holds
// must not roll the application record back.
func TestApplicationEventOutOfOrderIsDropped(t *testing.T) {
	resetApplicationEventState()
	defer resetApplicationEventState()

	seedApplicationsFromControlPlane(nil)

	changedEvent := readAndDecodeEventFromFile(t, "application-events/update_app_changed_attributes.txt")
	handleApplicationEvents(changedEvent, applicationEventType)

	// create_app_with_attributes.txt carries an earlier timeStamp than the update above.
	staleEvent := readAndDecodeEventFromFile(t, "application-events/create_app_with_attributes.txt")
	handleApplicationEvents(staleEvent, applicationEventType)

	assert.Equal(t, map[string]string{"department": "treasury", "planTier": "platinum"},
		applicationAttributes(t),
		"A stale application event overwrote a newer application record")
}
