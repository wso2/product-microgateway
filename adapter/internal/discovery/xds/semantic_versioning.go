/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package xds

import (
	"strconv"
	"strings"

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
	semantic_version "github.com/wso2/product-microgateway/adapter/pkg/semanticversion"
)

// GetVersionMatchRegex returns the regex to match the full version string
func GetVersionMatchRegex(version string) string {
	// Match "." character in the version by replacing it with "\\."
	return strings.ReplaceAll(version, ".", "\\.")
}

// GetMajorMinorVersionRangeRegex generates major and minor version compatible range regex for the given version
func GetMajorMinorVersionRangeRegex(semVersion semantic_version.SemVersion) string {
	majorVersion := strconv.Itoa(semVersion.Major)
	minorVersion := strconv.Itoa(semVersion.Minor)
	return "v" + majorVersion + "(?:\\." + minorVersion + ")?"
}

// GetMajorVersionRange generates major version range for the given version
func GetMajorVersionRange(semVersion semantic_version.SemVersion) string {
	return "v" + strconv.Itoa(semVersion.Major)
}

func updateRoutingRulesOnAPIUpdate(organizationID, apiIdentifier, apiName, apiVersion, vHost string) {
	apiSemVersion, err := semantic_version.ValidateAndGetVersionComponents(apiVersion, apiName)
	// If the version validation is not success, we just proceed without intelligent version
	// Valid version pattern: vx.y.z or vx.y where x, y and z are non-negative integers and v is a prefix
	if err != nil && apiSemVersion == nil {
		return
	}

	apiRangeIdentifier := GenerateIdentifierForAPIWithoutVersion(vHost, apiName)
	// Check the major version range of the current API
	existingMajorRangeLatestSemVersion, isMajorRangeRegexAvailable :=
		orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][GetMajorVersionRange(*apiSemVersion)]

	// Check whether the current API is the latest version in the major version range
	isLatestMajorVersion := !isMajorRangeRegexAvailable || existingMajorRangeLatestSemVersion.Compare(*apiSemVersion)

	// Remove the existing regexes from the path specifier when latest major version is available
	if isMajorRangeRegexAvailable && isLatestMajorVersion {
		// Organization's all apis
		for apiUUID, swagger := range orgIDAPIMgwSwaggerMap[organizationID] {
			// API's all versions in the same vHost
			if swagger.GetTitle() == apiName && swagger.GetVHost() == vHost {
				if swagger.GetVersion() == existingMajorRangeLatestSemVersion.Version {
					for _, route := range orgIDOpenAPIRoutesMap[organizationID][apiUUID] {
						regex := route.GetMatch().GetSafeRegex().GetRegex()
						regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
						existingMajorRangeLatestVersionRegex := GetVersionMatchRegex(existingMajorRangeLatestSemVersion.Version)

						regex = strings.Replace(regex, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), existingMajorRangeLatestVersionRegex, 1)
						regexRewritePattern = strings.Replace(regexRewritePattern, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), existingMajorRangeLatestVersionRegex, 1)

						pathSpecifier := &routev3.RouteMatch_SafeRegex{
							SafeRegex: &envoy_type_matcherv3.RegexMatcher{
								Regex: regex,
							},
						}
						route.Match.PathSpecifier = pathSpecifier
						action := route.Action.(*routev3.Route_Route)
						action.Route.RegexRewrite.Pattern.Regex = regexRewritePattern
						route.Action = action
					}
				}
			}
		}
	}

	if isLatestMajorVersion {
		// Update local memory map with the latest version range
		majorVersionRange := GetMajorVersionRange(*apiSemVersion)
		if _, orgExists := orgIDLatestAPIVersionMap[organizationID]; !orgExists {
			orgIDLatestAPIVersionMap[organizationID] = make(map[string]map[string]semantic_version.SemVersion)
		}
		if _, apiRangeExists := orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier]; !apiRangeExists {
			orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier] = make(map[string]semantic_version.SemVersion)
		}
		latestVersions := orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier]
		latestVersions[majorVersionRange] = *apiSemVersion

		// Add the major version range matching regexes to the path specifier when latest major version is available
		for _, route := range orgIDOpenAPIRoutesMap[organizationID][apiIdentifier] {
			regex := route.GetMatch().GetSafeRegex().GetRegex()
			regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
			apiVersionRegex := GetVersionMatchRegex(apiVersion)

			regex = strings.Replace(regex, apiVersionRegex, GetMajorMinorVersionRangeRegex(*apiSemVersion), 1)
			regexRewritePattern = strings.Replace(regexRewritePattern, apiVersionRegex, GetMajorMinorVersionRangeRegex(*apiSemVersion), 1)

			pathSpecifier := &routev3.RouteMatch_SafeRegex{
				SafeRegex: &envoy_type_matcherv3.RegexMatcher{
					Regex: regex,
				},
			}
			route.Match.PathSpecifier = pathSpecifier
			action := &routev3.Route_Route{}
			action = route.Action.(*routev3.Route_Route)
			action.Route.RegexRewrite.Pattern.Regex = regexRewritePattern
			route.Action = action
		}
	}
}

func updateRoutingRulesOnAPIDelete(organizationID, apiIdentifier string, api mgw.MgwSwagger) {
	// Update the intelligent routing if the deleting API is the latest version of the API range
	// and the API range has other versions
	apiRangeIdentifier := GenerateIdentifierForAPIWithoutVersion(api.GetVHost(), api.GetTitle())

	latestAPIVersionMap, latestAPIVersionMapExists := orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier]
	if !latestAPIVersionMapExists {
		return
	}
	deletingAPISemVersion, _ := semantic_version.ValidateAndGetVersionComponents(api.GetVersion(), api.GetTitle())
	if deletingAPISemVersion == nil {
		return
	}
	majorVersionRange := GetMajorVersionRange(*deletingAPISemVersion)
	newLatestMajorRangeAPIIdentifier := ""
	if deletingAPIsMajorRangeLatestAPISemVersion, ok := latestAPIVersionMap[majorVersionRange]; ok {
		if deletingAPIsMajorRangeLatestAPISemVersion.Version == api.GetVersion() {
			newLatestMajorRangeAPI := &semantic_version.SemVersion{
				Version: "",
				Major:   deletingAPISemVersion.Major,
				Minor:   0,
				Patch:   nil,
			}
			for currentAPIIdentifier, swagger := range orgIDAPIMgwSwaggerMap[organizationID] {
				// Iterate all the API versions other than the deleting API itself
				if swagger.GetTitle() == api.GetTitle() && currentAPIIdentifier != apiIdentifier && swagger.GetVHost() == api.GetVHost() {
					currentAPISemVersion, _ := semantic_version.ValidateAndGetVersionComponents(swagger.GetVersion(), swagger.GetTitle())
					if currentAPISemVersion != nil {
						if currentAPISemVersion.Major == deletingAPISemVersion.Major {
							if newLatestMajorRangeAPI.Compare(*currentAPISemVersion) {
								newLatestMajorRangeAPI = currentAPISemVersion
								newLatestMajorRangeAPIIdentifier = currentAPIIdentifier
							}
						}
					}
				}
			}
			if newLatestMajorRangeAPIIdentifier != "" {
				orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][majorVersionRange] = *newLatestMajorRangeAPI
				for _, route := range orgIDOpenAPIRoutesMap[organizationID][newLatestMajorRangeAPIIdentifier] {
					regex := route.GetMatch().GetSafeRegex().GetRegex()
					regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
					newLatestMajorRangeAPIVersionRegex := GetVersionMatchRegex(newLatestMajorRangeAPI.Version)
					regex = strings.Replace(
						regex,
						newLatestMajorRangeAPIVersionRegex,
						GetMajorMinorVersionRangeRegex(*newLatestMajorRangeAPI),
						1,
					)
					regexRewritePattern = strings.Replace(
						regexRewritePattern,
						newLatestMajorRangeAPIVersionRegex,
						GetMajorMinorVersionRangeRegex(*newLatestMajorRangeAPI),
						1,
					)
					pathSpecifier := &routev3.RouteMatch_SafeRegex{
						SafeRegex: &envoy_type_matcherv3.RegexMatcher{
							Regex: regex,
						},
					}
					route.Match.PathSpecifier = pathSpecifier
					action := &routev3.Route_Route{}
					action = route.Action.(*routev3.Route_Route)
					action.Route.RegexRewrite.Pattern.Regex = regexRewritePattern
					route.Action = action
				}
			} else {
				delete(orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier], majorVersionRange)
			}
		}
	}
}
