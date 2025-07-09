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
	"bytes"
	"encoding/xml"
)

type MCPRequest struct {
	ToolName   string      `json:"tool_name"`
	Arguments  string      `json:"arguments,omitempty"`
	Schema     string      `json:"schema,omitempty"`
	API        APIInfo     `json:"api"`
	Backend    BackendInfo `json:"backend,omitempty"`
	IsProxy    bool        `json:"is_proxy,omitempty"`
	BackendJWT string      `json:"backend_jwt,omitempty"`
}

type APIInfo struct {
	APIName  string `json:"api_name"`
	Endpoint string `json:"endpoint"`
	Context  string `json:"context"`
	Version  string `json:"version"`
	Path     string `json:"path"`
	Verb     string `json:"verb"`
	Auth     string `json:"auth,omitempty"`
}

type BackendInfo struct {
	Endpoint string `json:"endpoint"`
	Target   string `json:"target"`
	Verb     string `json:"verb"`
}

type TransformedRequest struct {
	Method  string
	URL     string
	Headers map[string]string
	Body    *bytes.Reader
}

type SchemaMapping struct {
	PathParameters   []string `json:"pathParameters"`
	QueryParameters  []Param  `json:"queryParameters"`
	HeaderParameters []Param  `json:"headerParameters"`
	HasBody          bool     `json:"hasBody"`
	ContentType      string   `json:"contentType,omitempty"`
}

type Param struct {
	Name     string `json:"name"`
	Required bool   `json:"required"`
}

type MCPInputSchema struct {
	Type        string         `json:"type"`
	Properties  map[string]any `json:"properties"`
	Required    []string       `json:"required"`
	ContentType string         `json:"contentType,omitempty"`
}

type Result struct {
	Code     int `json:"code"`
	Response any `json:"response"`
}

type XMLElement struct {
	XMLName  xml.Name
	Content  string       `xml:",chardata"`
	Children []XMLElement `xml:",any"`
}
