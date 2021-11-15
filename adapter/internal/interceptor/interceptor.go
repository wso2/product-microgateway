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
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"text/template"
)

//Interceptor hold values used for interceptor
type Interceptor struct {
	Context              *InvocationContext
	RequestExternalCall  *HTTPCallConfig
	ResponseExternalCall *HTTPCallConfig
	ReqFlowInclude       *RequestInclusions
	RespFlowInclude      *RequestInclusions
}

//HTTPCallConfig hold values used for external interceptor engine
type HTTPCallConfig struct {
	Enable      bool
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
{{if .ResponseExternalCall.Enable}} {{/* resp_flow details are required in req flow if request info needed in resp flow */}}
local resp_flow = {invocationContext={{.RespFlowInclude.InvocationContext}}, requestHeaders={{.RespFlowInclude.RequestHeaders}}, requestBody={{.RespFlowInclude.RequestBody}}, requestTrailer={{.RespFlowInclude.RequestTrailer}},
			responseHeaders={{.RespFlowInclude.ResponseHeaders}}, responseBody={{.RespFlowInclude.ResponseBody}}, responseTrailers={{.RespFlowInclude.ResponseTrailers}}}
{{else}}local resp_flow = {}{{end}} {{/* if resp_flow disabled no need req info in resp path */}}
{{if or .ReqFlowInclude.InvocationContext .RespFlowInclude.InvocationContext}}
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
{{else}}local inv_context = nil{{end}}
`
	requestInterceptorTemplate = `
local req_flow = {invocationContext={{.ReqFlowInclude.InvocationContext}}, requestHeaders={{.ReqFlowInclude.RequestHeaders}}, requestBody={{.ReqFlowInclude.RequestBody}}, requestTrailer={{.ReqFlowInclude.RequestTrailer}}}
function envoy_on_request(request_handle)
    interceptor.handle_request_interceptor(
		request_handle,
		{cluster_name="{{.RequestExternalCall.ClusterName}}", timeout={{.RequestExternalCall.Timeout}}},
		req_flow, resp_flow, inv_context
	)
end
`
	responseInterceptorTemplate = `
function envoy_on_response(response_handle)
    interceptor.handle_response_interceptor(
		response_handle,
		{cluster_name="{{.ResponseExternalCall.ClusterName}}", timeout={{.ResponseExternalCall.Timeout}}},
		resp_flow
	)
end
`
	// defaultRequestInterceptorTemplate is the template that is applied when request flow is disabled
	// just updated req flow info with  resp flow without calling interceptor service
	defaultRequestInterceptorTemplate = `
function envoy_on_request(request_handle)
	interceptor.handle_request_interceptor(request_handle, {}, {}, resp_flow, inv_context, true)
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
	templ := template.Must(template.New("lua-filter").Parse(getTemplate(values.RequestExternalCall.Enable,
		values.ResponseExternalCall.Enable)))
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
