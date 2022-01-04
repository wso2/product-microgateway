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

package interceptor

import (
	"bytes"
	"text/template"

	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

//Interceptor hold values used for interceptor
type Interceptor struct {
	Context            *InvocationContext
	RequestFlowEnable  bool
	ResponseFlowEnable bool
	RequestFlow        map[string]Config // key:operation method -> value:config
	ResponseFlow       map[string]Config // key:operation method -> value:config
}

//HTTPCallConfig hold values used for external interceptor engine
type Config struct {
	Enable       bool
	ExternalCall *HTTPCallConfig
	Include      *RequestInclusions
}

//HTTPCallConfig hold values used for external interceptor engine
type HTTPCallConfig struct {
	ClusterName string
	Timeout     string // in milli seconds
}

// RequestInclusions represents which should be included in the request payload to the interceptor service
type RequestInclusions struct {
	InvocationContext bool
	RequestHeaders    bool
	RequestBody       bool
	RequestTrailer    bool
	ResponseHeaders   bool
	ResponseBody      bool
	ResponseTrailers  bool
}

// InvocationContext represents static details of the invocation context of a request for the resource path
// runtime details such as actual path will be populated from the lua script and set in the invocation context
type InvocationContext struct {
	OrganizationID   string
	BasePath         string
	SupportedMethods string
	APIName          string
	APIVersion       string
	PathTemplate     string
	Vhost            string
	ProdClusterName  string
	SandClusterName  string
}

var (
	// commonTemplate contains common lua code for request and response intercept
	// Note: this template only applies if request or response interceptor is enabled
	commonTemplate = `
local interceptor = require 'home.wso2.interceptor.lib.interceptor'
{{if .ResponseFlowEnable}} {{/* resp_flow details are required in req flow if request info needed in resp flow */}}
local resp_flow_list = {
{{ range $key, $value := .ResponseFlow }}
{{ $key }} = {invocationContext={{$value.Include.InvocationContext}}, requestHeaders={{$value.Include.RequestHeaders}}, requestBody={{$value.Include.RequestBody}}, requestTrailer={{$value.Include.RequestTrailer}},
		responseHeaders={{$value.Include.ResponseHeaders}}, responseBody={{$value.Include.ResponseBody}}, responseTrailers={{$value.Include.ResponseTrailers}}}
{{ end }}
}
{{else}}local resp_flow_list = {}{{end}} {{/* if resp_flow disabled no need req info in resp path */}}
local inv_context = {
	organizationId = "{{.Context.OrganizationID}}",
	basePath = "{{.Context.BasePath}}",
	supportedMethods = "{{.Context.SupportedMethods}}",
	apiName = "{{.Context.APIName}}",
	apiVersion = "{{.Context.APIVersion}}",
	pathTemplate = "{{.Context.PathTemplate}}",
	vhost = "{{.Context.Vhost}}",
	prodClusterName = "{{.Context.ProdClusterName}}",
	sandClusterName = "{{.Context.SandClusterName}}"
}
`
	requestInterceptorTemplate = `
local req_flow_list = {
{{ range $key, $value := .RequestFlow }}
{{ $key }}= {invocationContext={{$value.Include.InvocationContext}}, requestHeaders={{$value.Include.RequestHeaders}}, requestBody={{$value.Include.RequestBody}}, requestTrailer={{$value.Include.RequestTrailer}}}
{{ end }}
}
local req_call_config = {
	{{ range $key, $value := .RequestFlow }}
	{{ $key }}={ClusterName={{$value.ExternalCall.ClusterName}}, Timeout={{$value.ExternalCall.Timeout}}
	{{ end }}
	}
function envoy_on_request(request_handle)
	method=request_handle:headers():get(":method")
    interceptor.handle_request_interceptor(
		request_handle,
		{cluster_name=req_call_config[method].ClusterName, timeout=req_call_config[method].Timeout},
		req_flow_list[method], resp_flow_list[method], inv_context
	)
end
`
	//get method in response flow
	responseInterceptorTemplate = `
local res_call_config = {
	{{ range $key, $value := .ResponseFlow }}
	{{ $key }}= {ClusterName={{$value.ExternalCall.ClusterName}}, Timeout={{$value.ExternalCall.Timeout}}
	{{ end }}
	}
function envoy_on_response(response_handle)
    interceptor.handle_response_interceptor(
		method=request_handle:headers():get(":method")
		response_handle,
		{cluster_name=req_call_config[method].ClusterName, timeout=req_call_config[method].Timeout},
		resp_flow_list[method]
	)
end
`
	// defaultRequestInterceptorTemplate is the template that is applied when request flow is disabled
	// just updated req flow info with  resp flow without calling interceptor service
	defaultRequestInterceptorTemplate = `
function envoy_on_request(request_handle)
    method=request_handle:headers():get(":method")
	interceptor.handle_request_interceptor(request_handle, {}, {}, resp_flow_list[method], inv_context, true)
end
`
	// defaultResponseInterceptorTemplate is the template that is applied when response flow is disabled
	defaultResponseInterceptorTemplate = `
function envoy_on_response(response_handle)
end
`
)

//GetInterceptor inject values and get request interceptor
// Note: This method is called only if one of request or response interceptor is enabled
func GetInterceptor(values *Interceptor) string {
	templ := template.Must(template.New("lua-filter").Parse(getTemplate(values.RequestFlowEnable,
		values.ResponseFlowEnable)))
	var out bytes.Buffer
	err := templ.Execute(&out, values)
	if err != nil {
		logger.LoggerInterceptor.Error("executing request interceptor template:", err)
	}
	return out.String()
}

func getTemplate(isReqIntercept bool, isResIntercept bool) string {
	reqT := defaultRequestInterceptorTemplate
	resT := defaultResponseInterceptorTemplate
	if isReqIntercept {
		reqT = requestInterceptorTemplate
	}
	if isResIntercept {
		resT = responseInterceptorTemplate
	}
	return commonTemplate + reqT + resT
}
