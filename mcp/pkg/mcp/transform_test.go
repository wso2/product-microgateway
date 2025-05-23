/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 */

package mcp

import (
	"encoding/json"
	"reflect"
	"testing"
)

type TestParam struct {
	name        string
	mcpRequest  *MCPRequest
	schema      *SchemaMapping
	wantQuery   string
	wantHeaders map[string]string
	wantPath    string
	wantErr     bool
}

func TestProcessQueryParameters(t *testing.T) {
	tests := []TestParam{
		{
			name: "all required present",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar","baz":"qux"}`,
			},
			schema: &SchemaMapping{
				QueryParameters: []Param{
					{Name: "foo", Required: true},
					{Name: "baz", Required: false},
				},
			},
			wantQuery: "?foo=bar&baz=qux",
			wantErr:   false,
		},
		{
			name: "missing required param",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar"}`,
			},
			schema: &SchemaMapping{
				QueryParameters: []Param{
					{Name: "foo", Required: true},
					{Name: "baz", Required: true},
				},
			},
			wantQuery: "",
			wantErr:   true,
		},
		{
			name: "optional param missing",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar"}`,
			},
			schema: &SchemaMapping{
				QueryParameters: []Param{
					{Name: "foo", Required: true},
					{Name: "baz", Required: false},
				},
			},
			wantQuery: "?foo=bar",
			wantErr:   false,
		},
		{
			name: "no query params",
			mcpRequest: &MCPRequest{
				Arguments: `{}`,
			},
			schema: &SchemaMapping{
				QueryParameters: []Param{},
			},
			wantQuery: "",
			wantErr:   false,
		},
		{
			name: "url encoded params",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar baz","baz":"qux!"}`,
			},
			schema: &SchemaMapping{
				QueryParameters: []Param{
					{Name: "foo", Required: true},
					{Name: "baz", Required: false},
				},
			},
			wantQuery: "?foo=bar%20baz&baz=qux%21",
			wantErr:   false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			args, err := parseArgs(tt.mcpRequest)
			if err != nil {
				t.Errorf("parseArgs() error = %v", err)
				return
			}
			got, err := processQueryParameters((args), tt.schema)
			if (err != nil) != tt.wantErr {
				t.Errorf("processQueryParameters() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.wantQuery {
				t.Errorf("processQueryParameters() = %v, want %v", got, tt.wantQuery)
			}
		})
	}
}

func TestProcessHeaderParameters(t *testing.T) {
	tests := []TestParam{
		{
			name: "all required present",
			mcpRequest: &MCPRequest{
				Arguments: `{"header1":"value1","header2":"value2"}`,
				API:       APIInfo{},
			},
			schema: &SchemaMapping{
				HeaderParameters: []Param{
					{Name: "header1", Required: true},
					{Name: "header2", Required: false},
				},
				ContentType: "application/json",
			},
			wantHeaders: map[string]string{
				"header1":      "value1",
				"header2":      "value2",
				"Content-Type": "application/json",
			},
			wantErr: false,
		},
		{
			name: "missing required header",
			mcpRequest: &MCPRequest{
				Arguments: `{"header1":"value1"}`,
				API:       APIInfo{},
			},
			schema: &SchemaMapping{
				HeaderParameters: []Param{
					{Name: "header1", Required: true},
					{Name: "header2", Required: true},
				},
				ContentType: "application/json",
			},
			wantHeaders: nil,
			wantErr:     true,
		},
		{
			name: "optional header missing",
			mcpRequest: &MCPRequest{
				Arguments: `{"header1":"value1"}`,
				API:       APIInfo{},
			},
			schema: &SchemaMapping{
				HeaderParameters: []Param{
					{Name: "header1", Required: true},
					{Name: "header2", Required: false},
				},
				ContentType: "application/json",
			},
			wantHeaders: map[string]string{
				"header1":      "value1",
				"Content-Type": "application/json",
			},
			wantErr: false,
		},
		{
			name: "auth header present",
			mcpRequest: &MCPRequest{
				Arguments: `{"header1":"value1"}`,
				API:       APIInfo{Auth: "Authorization: Bearer token"},
			},
			schema: &SchemaMapping{
				HeaderParameters: []Param{
					{Name: "header1", Required: true},
				},
				ContentType: "application/json",
			},
			wantHeaders: map[string]string{
				"header1":       "value1",
				"Authorization": "Bearer token",
				"Content-Type":  "application/json",
			},
			wantErr: false,
		},
		{
			name: "default content type",
			mcpRequest: &MCPRequest{
				Arguments: `{"header1":"value1"}`,
				API:       APIInfo{},
			},
			schema: &SchemaMapping{
				HeaderParameters: []Param{
					{Name: "header1", Required: true},
				},
				ContentType: "",
			},
			wantHeaders: map[string]string{
				"header1":      "value1",
				"Content-Type": ContentTypeJSON,
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := processHeaderParameters(tt.mcpRequest, tt.schema)
			if (err != nil) != tt.wantErr {
				t.Errorf("processHeaderParameters() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantHeaders) {
				gotJSON, _ := json.Marshal(got)
				wantJSON, _ := json.Marshal(tt.wantHeaders)
				t.Errorf("processHeaderParameters() = %s, want %s", gotJSON, wantJSON)
			}
		})
	}
}

func TestProcessPathParameters(t *testing.T) {
	baseUrl := "https://test.com/{foo}/test/{baz}"
	tests := []TestParam{
		{
			name: "all required present",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar","baz":"qux"}`,
			},
			schema: &SchemaMapping{
				PathParameters: []string{
					"foo", "baz",
				},
			},
			wantPath: "https://test.com/bar/test/qux",
			wantErr:  false,
		},
		{
			name: "missing required",
			mcpRequest: &MCPRequest{
				Arguments: `{"foo":"bar"}`,
			},
			schema: &SchemaMapping{
				PathParameters: []string{
					"foo", "baz",
				},
			},
			wantPath: "",
			wantErr:  true,
		},
		{
			name: "no path params",
			mcpRequest: &MCPRequest{
				Arguments: `{}`,
			},
			schema: &SchemaMapping{
				PathParameters: []string{},
			},
			wantPath: "https://test.com/{foo}/test/{baz}",
			wantErr:  false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			args, err := parseArgs(tt.mcpRequest)
			if err != nil {
				t.Errorf("parseArgs() error = %v", err)
				return
			}
			got, err := processPathParameters(args, tt.schema, baseUrl)
			if (err != nil) != tt.wantErr {
				t.Errorf("processPathParameters() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.wantPath {
				t.Errorf("processPathParameters() error = %v, want %v", err, tt.wantPath)
			}
		})
	}
}
