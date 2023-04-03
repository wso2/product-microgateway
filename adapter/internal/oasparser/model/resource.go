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
	"regexp"
	"sort"

	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
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
	path                string
	methods             []*Operation
	description         string
	summary             string
	iD                  string
	productionEndpoints *EndpointCluster
	sandboxEndpoints    *EndpointCluster
	vendorExtensions    map[string]interface{}
	hasPolicies         bool
	amznResourceName    string
}

// GetProdEndpoints returns the production endpoints object of a given resource.
func (resource *Resource) GetProdEndpoints() *EndpointCluster {
	return resource.productionEndpoints
}

// GetSandEndpoints returns the sandbox endpoints object of a given resource.
func (resource *Resource) GetSandEndpoints() *EndpointCluster {
	return resource.sandboxEndpoints
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
func (resource *Resource) GetMethod() []*Operation {
	return resource.methods
}

// GetVendorExtensions returns vendor extensions which are explicitly defined under
// a given resource.
func (resource *Resource) GetVendorExtensions() map[string]interface{} {
	return resource.vendorExtensions
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

// GetOperations returns the array of operations of the resource.
func (resource *Resource) GetOperations() []*Operation {
	return resource.methods
}

// GetAmznResourceName returns the amazon resourse name related to aws lambda endpoint of given resource.
func (resource *Resource) GetAmznResourceName() string {
	return resource.amznResourceName
}

// SetAmznResourceName sets the amazon resource name.
func (resource *Resource) SetAmznResourceName(amznResourceName string) {
	resource.amznResourceName = amznResourceName
}

// HasPolicies returns whether the resource has operations that includes policies.
func (resource *Resource) HasPolicies() bool {
	return resource.hasPolicies
}

// CreateMinimalDummyResourceForTests create a resource object with minimal required set of values
// which could be used for unit tests.
func CreateMinimalDummyResourceForTests(path string, methods []*Operation, id string, productionUrls,
	sandboxUrls []Endpoint) Resource {

	prodEndpints := generateEndpointCluster(constants.ProdClustersConfigNamePrefix, productionUrls, constants.LoadBalance)
	sandboxEndpints := generateEndpointCluster(constants.SandClustersConfigNamePrefix, sandboxUrls, constants.LoadBalance)

	return Resource{
		path:                path,
		methods:             methods,
		iD:                  id,
		productionEndpoints: prodEndpints,
		sandboxEndpoints:    sandboxEndpints,
	}
}

// CreateDummyResourceForAwsLambdaTests create a resource object with amazon resource name(arn)
// which could be used for unit tests.
func CreateDummyResourceForAwsLambdaTests(methods []*Operation, amznResourceName string) Resource {
	return Resource{
		methods:          methods,
		amznResourceName: amznResourceName,
	}
}

// Custom sort implementation to sort the Resources based on the resource path
type byPath []*Resource

//Len Returns the length of the arry
func (a byPath) Len() int { return len(a) }

//Less  returns true if the first item is less than the second parameter
func (a byPath) Less(i, j int) bool {
	// Get the less weighted path.
	// Paths can be in several types.
	// - /pet
	// - /pet/{id}
	// - /pet/index.html
	// - /pet/{id}/price
	// - /pet/*
	// When representing these resources in envoy configuration, they must be ordered correctly.
	// Sorted order
	// - /pet/index.html
	// - /pet
	// - /pet/{id}
	// - /pet/{id}/price
	// - /pet/{id}/{price}
	// - pet/*
	// Considerations...
	// The concrete paths are matched first
	// Any path with . character gets higher precidence
	// Precedence decreases when the number of path parameters increses.
	// The wild card path is matched last.

	// Replace all the non symbol characters with empty string ("") Because the alphabetical order is not mandatory.
	charMatcher := regexp.MustCompile(`[\w\s]`)
	pathI := charMatcher.ReplaceAllString(a[i].path, "")
	pathJ := charMatcher.ReplaceAllString(a[j].path, "")

	// if wildcard is matched for either i or j, it will be returned as greater.
	wildCardMatcher := regexp.MustCompile(`(\/[*]$)`)
	if wildCardMatcher.Match([]byte(pathI)) || wildCardMatcher.Match([]byte(pathJ)) {
		return !wildCardMatcher.Match([]byte(pathI)) || wildCardMatcher.Match([]byte(pathJ))
	}

	// if the dot is matched (either i or j), the path is considered less than the other one.
	// If both i and j match this at the same time, compare the full path.
	dotMatcher := regexp.MustCompile(`\.`)
	if dotMatcher.Match([]byte(pathI)) && dotMatcher.Match([]byte(pathJ)) {
		return pathI < pathJ
	} else if dotMatcher.Match([]byte(pathI)) {
		return true
	} else if dotMatcher.Match([]byte(pathJ)) {
		return false
	}
	// If non of the above matched, compare the strings.
	return pathI < pathJ
}

//Swap Swaps the input parameter values
func (a byPath) Swap(i, j int) { a[i], a[j] = a[j], a[i] }

// SortResources Sort the list of resources provided based on the resource path.
func SortResources(resources []*Resource) []*Resource {
	sort.Sort(byPath(resources))
	return resources
}
