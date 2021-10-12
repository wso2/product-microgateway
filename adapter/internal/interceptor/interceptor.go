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
	RequestExternalCall  *HTTPCallConfig
	ResponseExternalCall *HTTPCallConfig
	RequestBody          *RequestBodyInclusions
	ResponseBody         *RequestBodyInclusions
}

//HTTPCallConfig hold values used for external interceptor engine
type HTTPCallConfig struct {
	Enable      bool
	ClusterName string
	Path        string
	Timeout     string
}

// RequestBodyInclusions represents which should be included in the request payload to the interceptor service
type RequestBodyInclusions struct {
	RequestHeaders   bool
	RequestBody      bool
	RequestTrailer   bool
	ResponseHeaders  bool
	ResponseBody     bool
	ResponseTrailers bool
}

var (
	requestInterceptorTemplate = `
local interceptor = require 'home.wso2.interceptor.lib.interceptor'
function envoy_on_request(request_handle)
    interceptor.handle_request_interceptor(
		request_handle,
		{cluster_name="{{.RequestExternalCall.ClusterName}}", resource_path="{{.RequestExternalCall.Path}}", timeout={{.RequestExternalCall.Timeout}}},
		{headers={{.RequestBody.RequestHeaders}}, body={{.RequestBody.RequestBody}}, trailers={{.RequestBody.RequestTrailer}}},
		{headers={{.RequestBody.ResponseHeaders}}, body={{.RequestBody.ResponseBody}}, trailers={{.RequestBody.ResponseTrailers}}}
	)
end
`
	responseInterceptorTemplate = `
local interceptor = require 'home.wso2.interceptor.lib.interceptor'
function envoy_on_response(response_handle)
    interceptor.handle_response_interceptor(
		response_handle,
		{cluster_name="{{.ResponseExternalCall.ClusterName}}", resource_path="{{.ResponseExternalCall.Path}}", timeout={{.ResponseExternalCall.Timeout}}},
		{headers={{.ResponseBody.RequestHeaders}}, body={{.ResponseBody.RequestBody}}, trailers={{.ResponseBody.RequestTrailer}}},
		{headers={{.ResponseBody.ResponseHeaders}}, body={{.ResponseBody.ResponseBody}}, trailers={{.ResponseBody.ResponseTrailers}}}
	)
end
`
	defaultRequestInterceptorTemplate = `
function envoy_on_request(request_handle)
end
`
	defaultResponseInterceptorTemplate = `
function envoy_on_response(response_handle)
end
`
)

//GetInterceptor inject values and get request interceptor
func GetInterceptor(values Interceptor) string {
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
	return reqT + resT
}
