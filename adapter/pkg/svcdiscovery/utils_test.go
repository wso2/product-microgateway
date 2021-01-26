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
package svcdiscovery

import (
	"errors"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestIsDiscoveryServiceEndpoint(t *testing.T) {
	type isDiscoveryServiceEndpointList struct {
		input   string
		output  bool
		message string
	}
	dataItems := []isDiscoveryServiceEndpointList{
		{
			input:   " consul (dc1.dev.serviceA.tag1 , http://localhost:4000 ) ",
			output:  true,
			message: "with spaces",
		},
		{
			input:   " consul",
			output:  false,
			message: "invalid",
		},
		{
			input:   "consul([dc1,dc2].dev.serviceA.[tag1,tag2],http://localhost:4000)",
			output:  true,
			message: "valid",
		},
		{
			input:   "",
			output:  false,
			message: "empty",
		},
		{
			input:   "consul([dc1,dc2].dev.serviceA.[tag1,tag2],http://localhost:4000",
			output:  false,
			message: "missing parenthesis",
		},
		{
			input:   "consul([dc1,dc2].dev.serviceA.[tag1,tag2] http://localhost:4000",
			output:  false,
			message: "missing middle comma",
		},
	}

	for i, item := range dataItems {
		result := IsDiscoveryServiceEndpoint(item.input)
		assert.Equal(t, item.output, result, item.message, i)
	}
}

func TestParseQueryString(t *testing.T) {
	type parseQueryStringItem struct {
		input   string
		result  Query
		err     error
		message string
	}
	dataItems := []parseQueryStringItem{
		{
			input: "[dc1,dc2].dev.serviceA.[tag1,tag2]",
			result: Query{
				Datacenters: []string{"dc1", "dc2"},
				ServiceName: "serviceA",
				Namespace:   "dev",
				Tags:        []string{"tag1", "tag2"},
			},
			err:     nil,
			message: "simple scenario with namespace",
		},
		{
			input: "[dc 1,dc 2].service A.[tag1,tag2]",
			result: Query{
				Datacenters: []string{"dc 1", "dc 2"},
				ServiceName: "service A",
				Namespace:   "",
				Tags:        []string{"tag1", "tag2"},
			},
			err:     nil,
			message: "simple scenario without namespace",
		},
		{
			input: "[].prod.serviceA.[*]",
			result: Query{
				Datacenters: []string{""},
				ServiceName: "serviceA",
				Namespace:   "prod",
				Tags:        []string{""},
			},
			err:     nil,
			message: "empty dcs and tags",
		},
		{
			input:   "[].fake.another.prod.serviceA.[*]",
			err:     errors.New("bad consul query syntax"),
			message: "5 pieces in syntax",
		},
		{
			input: "[dc , dc1 ]. prod . serviceA . [ * ] ",
			result: Query{
				Datacenters: []string{"dc", "dc1"},
				ServiceName: "serviceA",
				Namespace:   "prod",
				Tags:        []string{""},
			},
			err:     nil,
			message: "spaces should be trimmed",
		}, {
			input: " serviceA ",
			result: Query{
				Datacenters: []string{""},
				ServiceName: "serviceA",
				Namespace:   "",
				Tags:        []string{""},
			},
			err:     nil,
			message: "service name only",
		},
		{
			input: " dc1.serviceA.tagA",
			result: Query{
				Datacenters: []string{"dc1"},
				ServiceName: "serviceA",
				Namespace:   "",
				Tags:        []string{"tagA"},
			},
			err:     nil,
			message: "without brackets",
		},
	}
	for i, item := range dataItems {
		result, err := ParseQueryString(item.input)
		assert.Equal(t, item.result, result, item.message, i)
		assert.Equal(t, item.err, err, item.message)
	}
}

func TestParseList(t *testing.T) {
	type parseListTestItem struct {
		inputString string
		resultList  []string
		message     string
	}
	dataItems := []parseListTestItem{
		{
			inputString: "[dc1,dc2,aws-us-central-1]",
			resultList:  []string{"dc1", "dc2", "aws-us-central-1"},
			message:     "Simple scenario with 3dcs",
		},
		{
			inputString: "[]",
			resultList:  []string{""},
			message:     "Empty list :(all)",
		},
		{
			inputString: "[*]",
			resultList:  []string{""},
			message:     "Empty list with * :(all)",
		},
		{
			inputString: "[abc]",
			resultList:  []string{"abc"},
			message:     "List with one dc",
		},
	}
	for _, item := range dataItems {
		result := parseList(item.inputString)
		assert.Equal(t, item.resultList, result, item.message)
	}
}

func TestParseConsulSyntax(t *testing.T) {
	type parseConsulSyntaxTestItem struct {
		inputString string
		result1     string
		result2     string
		err         error
		message     string
	}

	dataItems := []parseConsulSyntaxTestItem{
		{
			inputString: "consul([dc 1,dc 2].service A.[tag1,tag2],http://192.168.0.1:80)",
			result1:     "[dc 1,dc 2].service A.[tag1,tag2]",
			result2:     "http://192.168.0.1:80",
			err:         nil,
			message:     "valid case",
		},
		{
			inputString: " consul ( [dc 1,dc 2].service A.[tag1,tag2] , http://192.168.0.1:80 ) ",
			result1:     "[dc 1,dc 2].service A.[tag1,tag2]",
			result2:     "http://192.168.0.1:80",
			err:         nil,
			message:     "valid case with extra spaces",
		},
		{
			inputString: "",
			result1:     "",
			result2:     "",
			err:         errors.New("default host not provided"),
			message:     "empty string",
		},
	}

	for _, item := range dataItems {
		result1, result2, err := ParseConsulSyntax(item.inputString)
		assert.Equal(t, item.result1, result1, item.message)
		assert.Equal(t, item.result2, result2, item.message)
		assert.Equal(t, item.err, err, item.message)
	}

}
