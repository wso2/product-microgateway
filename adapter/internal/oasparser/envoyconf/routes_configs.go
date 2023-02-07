/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package envoyconf

import (
	"errors"
	"fmt"
	"regexp"
	"strings"
	"time"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/ext_authz/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes/any"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	"google.golang.org/protobuf/types/known/anypb"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

func generateRouteConfig(routeName string, match *routev3.RouteMatch, action *routev3.Route_Route,
	metadata *corev3.Metadata, decorator *routev3.Decorator, typedPerFilterConfig map[string]*anypb.Any,
	requestHeadersToAdd []*corev3.HeaderValueOption, requestHeadersToRemove []string,
	responseHeadersToAdd []*corev3.HeaderValueOption, responseHeadersToRemove []string) *routev3.Route {

	route := &routev3.Route{
		Name:                 routeName,
		Match:                match,
		Action:               action,
		Metadata:             metadata,
		Decorator:            decorator,
		TypedPerFilterConfig: typedPerFilterConfig,

		// headers common to all routes are removed at the Route Configuration level in listener.go
		// x-envoy headers are removed using the SuppressEnvoyHeaders param in http_filters.go
		RequestHeadersToAdd:     requestHeadersToAdd,
		RequestHeadersToRemove:  requestHeadersToRemove,
		ResponseHeadersToAdd:    responseHeadersToAdd,
		ResponseHeadersToRemove: responseHeadersToRemove,
	}

	return route
}

func generateRouteMatch(routeRegex string) *routev3.RouteMatch {
	match := &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_SafeRegex{
			SafeRegex: &envoy_type_matcherv3.RegexMatcher{
				Regex: routeRegex,
			},
		},
	}
	return match
}

func generateRouteAction(apiType string, prodRouteConfig, sandRouteConfig *model.EndpointConfig) (action *routev3.Route_Route) {

	config, _ := config.ReadConfigs()

	action = &routev3.Route_Route{
		Route: &routev3.RouteAction{
			HostRewriteSpecifier: &routev3.RouteAction_AutoHostRewrite{
				AutoHostRewrite: &wrapperspb.BoolValue{
					Value: true,
				},
			},
			UpgradeConfigs:    getUpgradeConfig(apiType),
			MaxStreamDuration: getMaxStreamDuration(apiType),
			Timeout:           durationpb.New(time.Duration(config.Envoy.Upstream.Timeouts.RouteTimeoutInSeconds) * time.Second),
			IdleTimeout:       durationpb.New(time.Duration(config.Envoy.Upstream.Timeouts.RouteIdleTimeoutInSeconds) * time.Second),
			ClusterSpecifier: &routev3.RouteAction_ClusterHeader{
				ClusterHeader: clusterHeaderName,
			},
		},
	}

	if (prodRouteConfig != nil && prodRouteConfig.RetryConfig != nil) ||
		(sandRouteConfig != nil && sandRouteConfig.RetryConfig != nil) {
		// Retry configs are always added via headers. This is to update the
		// default retry back-off base interval, which cannot be updated via headers.
		retryConfig := config.Envoy.Upstream.Retry
		commonRetryPolicy := &routev3.RetryPolicy{
			RetryOn: retryPolicyRetriableStatusCodes,
			NumRetries: &wrapperspb.UInt32Value{
				Value: 0,
				// If not set to 0, default value 1 will be
				// applied to both prod and sandbox even if they are not set.
			},
			RetriableStatusCodes: retryConfig.StatusCodes,
			RetryBackOff: &routev3.RetryPolicy_RetryBackOff{
				BaseInterval: &durationpb.Duration{
					Nanos: int32(retryConfig.BaseIntervalInMillis) * 1000,
				},
			},
		}
		action.Route.RetryPolicy = commonRetryPolicy
	}

	return action
}

func generateHTTPMethodMatcher(methodRegex string, isSandbox bool, sandClusterName string) []*routev3.HeaderMatcher {
	headerMatcher := generateHeaderMatcher(httpMethodHeader, methodRegex)
	headerMatcherArray := []*routev3.HeaderMatcher{headerMatcher}
	// if sandbox route, add additional header match based on cluster name header
	if isSandbox {
		clusterHeaderMatcher := routev3.HeaderMatcher{
			Name: clusterHeaderName,
			HeaderMatchSpecifier: &routev3.HeaderMatcher_StringMatch{
				StringMatch: &envoy_type_matcherv3.StringMatcher{
					MatchPattern: &envoy_type_matcherv3.StringMatcher_Exact{Exact: sandClusterName},
				},
			},
		}
		headerMatcherArray = append(headerMatcherArray, &clusterHeaderMatcher)
	}
	return headerMatcherArray
}

func generateHeaderMatcher(headerName, valueRegex string) *routev3.HeaderMatcher {
	headerMatcherArray := &routev3.HeaderMatcher{
		Name: headerName,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_StringMatch{
			StringMatch: &envoy_type_matcherv3.StringMatcher{
				MatchPattern: &envoy_type_matcherv3.StringMatcher_SafeRegex{
					SafeRegex: &envoy_type_matcherv3.RegexMatcher{
						Regex: "^" + valueRegex + "$",
					},
				},
			},
		},
	}
	return headerMatcherArray
}

func generateRegexMatchAndSubstitute(routePath, endpointBasePath,
	endpointResourcePath string) *envoy_type_matcherv3.RegexMatchAndSubstitute {

	substitutionString := generateSubstitutionString(endpointBasePath, endpointResourcePath)

	return &envoy_type_matcherv3.RegexMatchAndSubstitute{
		Pattern: &envoy_type_matcherv3.RegexMatcher{
			Regex: routePath,
		},
		Substitution: substitutionString,
	}
}

// Router configs for Operational Policies

// generateHeaderToAddRouteConfig returns Router config for SET_HEADER
func generateHeaderToAddRouteConfig(policyParams interface{}) (*corev3.HeaderValueOption, error) {
	var paramsToSetHeader map[string]interface{}
	var ok bool
	var headerName, headerValue string
	if paramsToSetHeader, ok = policyParams.(map[string]interface{}); !ok {
		return nil, fmt.Errorf("error while processing policy parameter map. Map: %v", policyParams)
	}
	if headerName, ok = paramsToSetHeader[constants.HeaderName].(string); !ok || strings.TrimSpace(headerName) == "" {
		return nil, errors.New("policy parameter map must include headerName")
	}
	if headerValue, ok = paramsToSetHeader[constants.HeaderValue].(string); !ok || strings.TrimSpace(headerValue) == "" {
		return nil, errors.New("policy parameter map must include headerValue")
	}
	headerToAdd := corev3.HeaderValueOption{
		Header: &corev3.HeaderValue{
			Key:   headerName,
			Value: headerValue,
		},
		AppendAction: *corev3.HeaderValueOption_OVERWRITE_IF_EXISTS_OR_ADD.Enum(),
	}
	return &headerToAdd, nil
}

func generateHeaderToRemoveString(policyParams interface{}) (string, error) {
	var paramsToRemoveHeader map[string]interface{}
	var ok bool
	var requestHeaderToRemove string
	if paramsToRemoveHeader, ok = policyParams.(map[string]interface{}); !ok {
		return "", fmt.Errorf("error while processing policy parameter map. Map: %v", policyParams)
	}
	if requestHeaderToRemove, ok = paramsToRemoveHeader[constants.HeaderName].(string); !ok ||
		requestHeaderToRemove == "" {
		return "", errors.New("policy parameter map must include headerName")
	}
	return requestHeaderToRemove, nil
}

func generateRewritePathRouteConfig(routePath, resourcePath, endpointBasepath string,
	policyParams interface{}) (*envoy_type_matcherv3.RegexMatchAndSubstitute, error) {

	var paramsToSetHeader map[string]interface{}
	var ok bool
	var rewritePath string
	if paramsToSetHeader, ok = policyParams.(map[string]interface{}); !ok {
		return nil, fmt.Errorf("error while processing policy parameter map. Map: %v", policyParams)
	}
	if rewritePath, ok = paramsToSetHeader[constants.RewritePathResourcePath].(string); !ok ||
		strings.TrimSpace(rewritePath) == "" {
		return nil, errors.New("policy parameter map must include rewritePath")
	}

	rewritePathIndexedWrtResourcePath, err := getRewriteRegexFromPathTemplate(resourcePath, rewritePath)
	if err != nil {
		return nil, err
	}
	rewriteRegex := generateRegexMatchAndSubstitute(routePath, endpointBasepath, rewritePathIndexedWrtResourcePath)
	return rewriteRegex, nil
}

func generateFilterConfigToSkipEnforcer() map[string]*anypb.Any {
	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_Disabled{
			Disabled: true,
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: extAuthzPerRouteName,
		Value:   b.Bytes(),
	}

	return map[string]*any.Any{
		wellknown.HTTPExternalAuthorization: filter,
	}
}

func generateMetadataMatcherForInternalRoutes(metadataValue string) (dynamicMetadata []*envoy_type_matcherv3.MetadataMatcher) {
	path := &envoy_type_matcherv3.MetadataMatcher_PathSegment{
		Segment: &envoy_type_matcherv3.MetadataMatcher_PathSegment_Key{
			Key: methodRewrite,
		},
	}
	metadataMatcher := &envoy_type_matcherv3.MetadataMatcher{
		Filter: extAuthzFilterName,
		Path:   []*envoy_type_matcherv3.MetadataMatcher_PathSegment{path},
		Value: &envoy_type_matcherv3.ValueMatcher{
			MatchPattern: &envoy_type_matcherv3.ValueMatcher_StringMatch{
				StringMatch: &envoy_type_matcherv3.StringMatcher{
					MatchPattern: &envoy_type_matcherv3.StringMatcher_Exact{
						Exact: metadataValue,
					},
				},
			},
		},
	}
	return []*envoy_type_matcherv3.MetadataMatcher{
		metadataMatcher,
	}
}

func generateMetadataMatcherForExternalRoutes() (dynamicMetadata []*envoy_type_matcherv3.MetadataMatcher) {
	path := &envoy_type_matcherv3.MetadataMatcher_PathSegment{
		Segment: &envoy_type_matcherv3.MetadataMatcher_PathSegment_Key{
			Key: methodRewrite,
		},
	}
	metadataMatcher := &envoy_type_matcherv3.MetadataMatcher{
		Filter: extAuthzFilterName,
		Path:   []*envoy_type_matcherv3.MetadataMatcher_PathSegment{path},
		Value: &envoy_type_matcherv3.ValueMatcher{
			MatchPattern: &envoy_type_matcherv3.ValueMatcher_PresentMatch{
				PresentMatch: true,
			},
		},
		Invert: true,
	}
	return []*envoy_type_matcherv3.MetadataMatcher{
		metadataMatcher,
	}
}

// getRewriteRegexFromPathTemplate returns a regex with capture groups for given rewritePathTemplate.
// It replaces {uri.var.petId} included in rewritePath of the path rewrite policy
// with indexes such as \1 \2 that are expected in the substitution string
func getRewriteRegexFromPathTemplate(pathTemplate, rewritePathTemplate string) (string, error) {
	rewriteRegex := "/" + strings.TrimSuffix(strings.TrimPrefix(rewritePathTemplate, "/"), "/")
	pathParamToIndexMap := getPathParamToIndexMap(pathTemplate)
	r := regexp.MustCompile(`{uri.var.([^{}]+)}`) // define a capture group to catch the path param
	matches := r.FindAllStringSubmatch(rewritePathTemplate, -1)
	for _, match := range matches {
		// match is slice always with length two (since one capture group is defined in the regex)
		// hence we do not want to explicitly validate the slice length
		templatedParam := match[0]
		param := match[1]
		if index, ok := pathParamToIndexMap[param]; ok {
			rewriteRegex = strings.ReplaceAll(rewriteRegex, templatedParam, fmt.Sprintf(`\%d`, index))
		} else {
			return "", fmt.Errorf("invalid path param %q in rewrite path", param)
		}
	}

	// validate rewriteRegex
	if matched, _ := regexp.MatchString(`^[a-zA-Z0-9~/_.\-\\]*$`, rewriteRegex); !matched {
		logger.LoggerOasparser.Error("Rewrite path includes invalid characters")
		return "", fmt.Errorf("rewrite path regex includes invalid characters, regex %q", rewriteRegex)
	}

	return rewriteRegex, nil
}

// getPathParamToIndexMap returns a map of path params to its index (map of path param -> index)
func getPathParamToIndexMap(pathTemplate string) map[string]int {
	indexMap := make(map[string]int)
	r := regexp.MustCompile(`{([^{}]+)}`) // define a capture group to catch the path param
	matches := r.FindAllStringSubmatch(pathTemplate, -1)
	for i, paramMatches := range matches {
		// paramMatches is slice always with length two (since one capture group is defined in the regex)
		// hence we do not want to explicitly validate the slice length
		indexMap[paramMatches[1]] = i + 1
	}
	return indexMap
}
