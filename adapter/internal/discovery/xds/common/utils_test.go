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

package common_test

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds/common"
)

func TestCheckEntryAndSwapToEnd(t *testing.T) {
	array := generateNodeArray(5)
	entry := "node-40"
	resultArray, isNewAddition := common.CheckEntryAndSwapToEnd(array, entry)
	assert.Equal(t, 6, len(resultArray), "array length mismatch")
	assert.Equal(t, "node-40", resultArray[5], "array element mismatch")
	assert.True(t, isNewAddition, "isNewAddition flag is not correct.")

	array = generateNodeArray(5)
	entry = "node-2"
	resultArray, isNewAddition = common.CheckEntryAndSwapToEnd(array, entry)
	assert.Equal(t, 5, len(resultArray), "array length mismatch")
	assert.Equal(t, "node-2", resultArray[4], "array element mismatch")
	assert.Equal(t, "node-3", resultArray[2], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")

	array = generateNodeArray(20)
	entry = "node-19"
	resultArray, isNewAddition = common.CheckEntryAndSwapToEnd(array, entry)
	assert.Equal(t, 20, len(resultArray), "array length mismatch")
	assert.Equal(t, "node-19", resultArray[19], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")

	array = generateNodeArray(20)
	entry = "node-40"
	resultArray, isNewAddition = common.CheckEntryAndSwapToEnd(array, entry)
	assert.Equal(t, 20, len(resultArray), "array length mismatch")
	assert.Equal(t, "node-40", resultArray[19], "array element mismatch")
	assert.Equal(t, "node-1", resultArray[0], "array element mismatch")
	assert.True(t, isNewAddition, "isNewAddition flag is not correct.")

	array = generateNodeArray(20)
	entry = "node-10"
	resultArray, isNewAddition = common.CheckEntryAndSwapToEnd(array, entry)
	assert.Equal(t, 20, len(resultArray), "array length mismatch")
	assert.Equal(t, "node-10", resultArray[19], "array element mismatch")
	assert.Equal(t, "node-11", resultArray[10], "array element mismatch")
	assert.Equal(t, "node-9", resultArray[9], "array element mismatch")
	assert.False(t, isNewAddition, "isNewAddition flag is not correct.")
}

func generateNodeArray(length int) []string {
	array := []string{}
	for i := 0; i < length; i++ {
		array = append(array, fmt.Sprintf("node-%d", i))
	}
	return array
}
