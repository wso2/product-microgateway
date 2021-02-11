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

// Package model contains the implementation of DTOs to convert OpenAPI/Swagger files
// and create a common model which can represent both types.
package model

// Operation type object holds data about each http method in the REST API.
type Operation struct {
	method 		string
	security	[]map[string][]string
}

// GetMethod returns the http method name of the give API operation
func (operation *Operation) GetMethod() string {
	return operation.method
}

// GetSecurity returns the security schemas defined for the http opeartion
func (operation *Operation) GetSecurity() []map[string][]string {
	return operation.security
}


// NewOperation Creates and returns operation type object
func NewOperation(method string, security []map[string][]string) Operation {
	return Operation{method, security}
}

