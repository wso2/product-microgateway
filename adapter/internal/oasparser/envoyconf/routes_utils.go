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

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
)

func generateHTTPMethodMatcher(methodRegex string) []*routev3.HeaderMatcher {
	headerMatcherArray := routev3.HeaderMatcher{
		Name: httpMethodHeader,
		HeaderMatchSpecifier: &routev3.HeaderMatcher_StringMatch{
			StringMatch: &envoy_type_matcherv3.StringMatcher{
				MatchPattern: &envoy_type_matcherv3.StringMatcher_SafeRegex{
					SafeRegex: &envoy_type_matcherv3.RegexMatcher{
						EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
							GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
								MaxProgramSize: nil,
							},
						},
						Regex: "^(" + methodRegex + ")$",
					},
				},
			},
		},
	}
	return []*routev3.HeaderMatcher{&headerMatcherArray}
}

func generateRegexMatchAndSubstitute(routePath, endpointBasePath,
	endpointResourcePath string) envoy_type_matcherv3.RegexMatchAndSubstitute {

	substitutionString := generateSubstitutionString(endpointBasePath, endpointResourcePath)

	return envoy_type_matcherv3.RegexMatchAndSubstitute{
		Pattern: &envoy_type_matcherv3.RegexMatcher{
			EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
				GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
					MaxProgramSize: nil,
				},
			},
			Regex: routePath,
		},
		Substitution: substitutionString,
	}
}

func castPolicyParamMap(policyParams interface{}) (map[string]string, error) {
	policyParamMap, ok := policyParams.(map[string]interface{})
	if !ok {
		return nil, errors.New("Error while converting policy parameters to map")
	}
	params := map[string]string{}

	for k, v := range policyParamMap {
		if v, ok := v.(string); ok {
			params[k] = v
		} else {
			return nil, errors.New("Error while converting policy parameter value. Parameter: " + k)
		}
	}
	return params, nil
}
