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
package model

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSortResources(t *testing.T) {
	resources := getResources()
	sortedPaths := []string{
		"/pet/index.html",
        "/pet/pet.{anc}",
        "/pet/{petId}.com",
        "/pet/{pet}.{anc}",
        "/pet",
        "/pet/{id}",
        "/pet/{id}/price",
        "/pet/{id}/{price}",
        "/pet/*",
	}
	sortedResources := SortResources(resources)

	for sortedPathIndex := range sortedResources {
		assert.Equal(t, sortedResources[sortedPathIndex].path, sortedPaths[sortedPathIndex], "Path is out of order")
	}
}

func getResources() []Resource {
	paths := []string{
		"/pet",
		"/pet/{id}",
		"/pet/index.html",
		"/pet/{id}/price",
		"/pet/{id}/{price}",
		"/pet/*",
		"/pet/{petId}.com",
		"/pet/pet.{anc}",
		"/pet/{pet}.{anc}",
	}
	resources := make([]Resource, len(paths))
	for index := range paths {
		res := CreateMinimalDummyResourceForTests(paths[index], make([]Operation, 0), "",
		        make([]Endpoint, 0), make([]Endpoint, 0))
		resources[index] = res
	}
	return resources
}
