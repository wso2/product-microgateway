package interceptor

import (
	"bytes"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"text/template"
)

//Interceptor hold values used for interceptor
type Interceptor struct {
	RequestExternalCall  HTTPCallConfig
	ResponseExternalCall HTTPCallConfig
}

//HTTPCallConfig hold values used for external interceptor engine
type HTTPCallConfig struct {
	Enable      bool
	ClusterName string
	Path        string
	Timeout     string
	Headers     map[string]string
}

var (
	requestInterceptorTemplate = `
function envoy_on_request(request_handle)
	local headers, response = request_handle:httpCall(
		"{{.RequestExternalCall.ClusterName}}",
		{
		[":method"] = "POST",
		[":path"] = "{{.RequestExternalCall.Path}}",
		[":authority"] = "cc-interceptor",
		},
		"hello from lua request",
		{{.RequestExternalCall.Timeout}} 
	)
	request_handle:logInfo("Hello from router.")
	request_handle:headers():add("custom-header", "hello")
end
`
	responseInterceptorTemplate = `
function envoy_on_response(response_handle)
	local headers, response = response_handle:httpCall(
		"{{.ResponseExternalCall.ClusterName}}",
		{
		[":method"] = "POST",
		[":path"] = "{{.ResponseExternalCall.Path}}",
		[":authority"] = "cc-interceptor",
		},
		"bye from lua response",
		{{.ResponseExternalCall.Timeout}} 
	)
	response_handle:logInfo("Bye from router.")
	response_handle:headers():add("custom-header", "bye")
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
