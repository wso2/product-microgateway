/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// Package common includes the common functions shared between enforcer and router callbacks.
package common

import (
	"sync"

	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
)

const nodeIDArrayMaxLength int = 20
const instanceIdentifierKey string = "instanceIdentifier"

// NodeQueue struct is used to keep track of the nodes connected via the XDS.
type NodeQueue struct {
	lock  *sync.Mutex
	queue []string
}

// CheckEntryAndSwapToEnd function does the following. Recently accessed entry is removed last.
// Array should have a maximum length. If the the provided nodeId may or may not be within the array.
//
// 1. If the array's maximum length is not reached after adding the new element and the element is not inside the array,
// 		append the element to the end.
// 2. If the array is at maximum length and element is not within the array, the new entry should be appended to the end
//		and the 0th element should be removed.
// 3. If the array is at the maximum length and element is inside the array, the new element should be appended and the already
// 		existing entry should be removed from the position.
// Returns the modified array and true if the entry is a new addition.
func (nodeQueue *NodeQueue) checkEntryAndMoveToEnd(nodeID string) (isNewAddition bool) {
	matchedIndex := -1
	arraySize := len(nodeQueue.queue)
	for index := arraySize - 1; index >= 0; index-- {
		entry := nodeQueue.queue[index]
		if entry == nodeID {
			matchedIndex = index
			break
		}
	}

	if matchedIndex == nodeIDArrayMaxLength-1 {
		return false
	} else if matchedIndex > 0 {
		nodeQueue.queue = append(nodeQueue.queue[0:matchedIndex], nodeQueue.queue[matchedIndex+1:]...)
		nodeQueue.queue = append(nodeQueue.queue, nodeID)
		return false
	}
	if arraySize >= nodeIDArrayMaxLength {
		nodeQueue.queue = nodeQueue.queue[1:]
	}
	nodeQueue.queue = append(nodeQueue.queue, nodeID)
	return true
}

// GenerateNodeQueue creates an instance of nodeQueue with a mutex and a string array assigned.
func GenerateNodeQueue() *NodeQueue {
	return &NodeQueue{
		lock:  &sync.Mutex{},
		queue: []string{},
	}
}

// IsNewNode returns true if the provided nodeID does not exist in the nodeQueue
func (nodeQueue *NodeQueue) IsNewNode(nodeIdentifier string) bool {
	nodeQueue.lock.Lock()
	defer nodeQueue.lock.Unlock()
	return nodeQueue.checkEntryAndMoveToEnd(nodeIdentifier)
}

// GetNodeIdentifier constructs the nodeIdentifier from discovery request's node property, label:<instanceIdentifierProperty>
func GetNodeIdentifier(request *discovery.DiscoveryRequest) string {
	metadataMap := request.Node.Metadata.AsMap()
	nodeIdentifier := request.Node.Id
	if identifierVal, ok := metadataMap[instanceIdentifierKey]; ok {
		nodeIdentifier = request.Node.Id + ":" + identifierVal.(string)
	}
	return nodeIdentifier
}
