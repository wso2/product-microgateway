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

package common

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCheckEntryAndSwapToEnd(t *testing.T) {
	entry := "node-40"
	nodeQueue := GenerateNodeQueue()
	nodeQueue.queue = generateNodeArray(5)
	isNewAddition := nodeQueue.IsNewNode(entry)
	assert.Equal(t, 6, len(nodeQueue.queue), "array length mismatch")
	assert.Equal(t, "node-40", nodeQueue.queue[5], "array element mismatch")
	assert.True(t, isNewAddition, "isNewAddition flag is not correct.")

	entry = "node-2"
	nodeQueue.queue = generateNodeArray(5)
	isNewAddition = nodeQueue.IsNewNode(entry)
	assert.Equal(t, 5, len(nodeQueue.queue), "array length mismatch")
	assert.Equal(t, "node-2", nodeQueue.queue[4], "array element mismatch")
	assert.Equal(t, "node-3", nodeQueue.queue[2], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")

	entry = "node-19"
	nodeQueue.queue = generateNodeArray(20)
	isNewAddition = nodeQueue.IsNewNode(entry)
	assert.Equal(t, 20, len(nodeQueue.queue), "array length mismatch")
	assert.Equal(t, "node-19", nodeQueue.queue[19], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")

	nodeQueue.queue = generateNodeArray(20)
	entry = "node-40"
	isNewAddition = nodeQueue.IsNewNode(entry)
	assert.Equal(t, 20, len(nodeQueue.queue), "array length mismatch")
	assert.Equal(t, "node-40", nodeQueue.queue[19], "array element mismatch")
	assert.Equal(t, "node-1", nodeQueue.queue[0], "array element mismatch")
	assert.True(t, isNewAddition, "isNewAddition flag is not correct.")

	nodeQueue.queue = generateNodeArray(20)
	entry = "node-10"
	isNewAddition = nodeQueue.IsNewNode(entry)
	assert.Equal(t, 20, len(nodeQueue.queue), "array length mismatch")
	assert.Equal(t, "node-10", nodeQueue.queue[19], "array element mismatch")
	assert.Equal(t, "node-11", nodeQueue.queue[10], "array element mismatch")
	assert.Equal(t, "node-9", nodeQueue.queue[9], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")
}

func generateNodeArray(length int) []string {
	array := []string{}
	for i := 0; i < length; i++ {
		array = append(array, fmt.Sprintf("node-%d", i))
	}
	return array
}
