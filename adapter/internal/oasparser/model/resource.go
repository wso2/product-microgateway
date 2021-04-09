/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// Package model contains the implementation of DTOs to convert OpenAPI/Swagger files
// and create a common model which can represent both types.
package model

import (
	"sort"
)

// Resource represents the object structure holding the information related to the
// pathItem object in OpenAPI definition. This is the most granular level in which the
// information can be stored as envoy architecture does not support having an operation level
// granularity out of the box.
//
// Each resource can contain the path, the http methods that support, security schemas, production
// endpoints, and sandbox endpoints. These values are populated from extensions/properties
// mentioned under pathItem.
type Resource struct {
	path             string
	methods          []Operation
	description      string
	consumes         []string
	schemes          []string
	tags             []string
	summary          string
	iD               string
	productionUrls   []Endpoint
	sandboxUrls      []Endpoint
	security         []map[string][]string
	vendorExtensions map[string]interface{}
}

// GetProdEndpoints returns the production endpoints array of a given resource.
func (resource *Resource) GetProdEndpoints() []Endpoint {
	return resource.productionUrls
}

// GetSandEndpoints returns the sandbox endpoints array of a given resource.
func (resource *Resource) GetSandEndpoints() []Endpoint {
	return resource.sandboxUrls
}

// GetPath returns the pathItem name (of openAPI definition) corresponding to a given resource
func (resource *Resource) GetPath() string {
	return resource.path
}

// GetID returns the id of a given resource.
// This is a randomly generated UUID
func (resource *Resource) GetID() string {
	return resource.iD
}

// GetMethod returns an array of http method  operations which are explicitly defined under
// a given resource.
func (resource *Resource) GetMethod() []Operation {
	return resource.methods
}

// GetMethodList returns a list of http Methods as strings which are explicitly defined under
// a given resource.
func (resource *Resource) GetMethodList() []string {
	var methodList = make([]string, len(resource.methods))
	for i, method := range resource.methods {
		methodList[i] = method.method
	}
	return methodList
}

// CreateDummyResourceForTests create an resource object which could be used for unit tests.
func CreateDummyResourceForTests(path string, method []Operation, description string, consumes, schemes,
	tags []string, summary, id string, productionUrls, sandboxUrls []Endpoint,
	security []map[string][]string, vendorExtensions map[string]interface{}) Resource {
	return Resource{
		path:             path,
		methods:          method,
		description:      description,
		consumes:         consumes,
		schemes:          schemes,
		tags:             tags,
		summary:          summary,
		iD:               id,
		productionUrls:   productionUrls,
		sandboxUrls:      sandboxUrls,
		security:         security,
		vendorExtensions: vendorExtensions,
	}
}

// CreateMinimalDummyResourceForTests create a resource object with minimal required set of values
// which could be used for unit tests.
func CreateMinimalDummyResourceForTests(path string, methods []Operation, id string, productionUrls,
	sandboxUrls []Endpoint) Resource {
	return Resource{
		path:           path,
		methods:        methods,
		iD:             id,
		productionUrls: productionUrls,
		sandboxUrls:    sandboxUrls,
	}
}

// Custom sort implementation to sort the Resources based on the resource path
type byPath []Resource

//Len Returns the length of the arry
func (a byPath) Len() int { return len(a) }

//Less  returns true if the first item is less than the second parameter
func (a byPath) Less(i, j int) bool { return a[i].path < a[j].path }

//Swap Swaps the input parameter values
func (a byPath) Swap(i, j int) { a[i], a[j] = a[j], a[i] }

// SortResources Sort the list of resources provided based on the resource path.
func SortResources(resources []Resource) []Resource {
	sort.Sort(byPath(resources))
	return resources
}
