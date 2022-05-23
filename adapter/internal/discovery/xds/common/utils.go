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

import "sync"

const nodeIDArrayMaxLength int = 20

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
func CheckEntryAndSwapToEnd(array []string, nodeID string) (modifiedArray []string, isNewAddition bool) {
	matchedIndex := -1
	arraySize := len(array)
	for index, entry := range array {
		if entry == nodeID {
			matchedIndex = index
			break
		}
	}

	if matchedIndex < 0 && arraySize < nodeIDArrayMaxLength-1 {
		array = append(array, nodeID)
		return array, true
	} else if matchedIndex < 0 && arraySize >= nodeIDArrayMaxLength {
		array = append(array, nodeID)
		array = array[1:]
		return array, true
	} else if matchedIndex == 9 {
		return array, false
	}
	array = append(array[0:matchedIndex], array[matchedIndex+1:]...)
	array = append(array, nodeID)
	return array, false
}

// GenerateNodeQueue creates an instance of nodeQueue with a mutex and a string array assigned.
func GenerateNodeQueue() *NodeQueue {
	return &NodeQueue{
		lock:  &sync.Mutex{},
		queue: []string{},
	}
}

// IsNewNode returns true if the provided nodeID does not exist in the nodeQueue
func IsNewNode(nodeQueueInstance *NodeQueue, nodeIdentifier string) bool {
	nodeQueueInstance.lock.Lock()
	defer nodeQueueInstance.lock.Unlock()
	var isNewAddition bool
	nodeQueueInstance.queue, isNewAddition = CheckEntryAndSwapToEnd(nodeQueueInstance.queue, nodeIdentifier)
	return isNewAddition
}
