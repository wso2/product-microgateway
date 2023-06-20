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
	"errors"
	"strconv"
	"strings"

	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	mgw "github.com/wso2/product-microgateway/adapter/internal/oasparser/model"
)

// GetVersionMatchRegex returns the regex to match the full version string
func GetVersionMatchRegex(version string) string {
	// Match "." character in the version by replacing it with "\\."
	return strings.ReplaceAll(version, ".", "\\.")
}

// ValidateAndGetVersionComponents validates version string and extracts version components
func ValidateAndGetVersionComponents(version string, apiName string) (*SemVersion, error) {
	versionComponents := strings.Split(version, ".")

	// If the versionComponents length is less than 2, return error
	if len(versionComponents) < 2 {
		logger.LoggerXds.Errorf("API version validation failed for API: %v. API Version: %v", apiName, version)
		errMessage := "Invalid version: " + version + " for API: " + apiName +
			". API version should be in the format x.y.z where x,y,z are non-negative integers"
		return nil, errors.New(errMessage)
	}

	majorVersion, majorVersionConvErr := strconv.Atoi(versionComponents[0])
	minorVersion, minorVersionConvErr := strconv.Atoi(versionComponents[1])
	if majorVersionConvErr != nil || majorVersion < 0 {
		logger.LoggerXds.Errorf("API major version should be a non-negative integer in API: %v. API Version: %v", apiName, version, majorVersionConvErr)
		return nil, errors.New("Invalid version format")
	}

	if minorVersionConvErr != nil || minorVersion < 0 {
		logger.LoggerXds.Errorf("API minor version should be a non-negative integer in API: %v. API Version: %v", apiName, version, minorVersionConvErr)
		return nil, errors.New("Invalid version format")
	}

	if len(versionComponents) == 2 {
		return &SemVersion{
			Version: version,
			Major:   majorVersion,
			Minor:   minorVersion,
			Patch:   nil,
		}, nil
	}

	patchVersion, patchVersionConvErr := strconv.Atoi(versionComponents[2])
	if patchVersionConvErr != nil {
		logger.LoggerXds.Errorf("API patch version should be an integer in API: %v. API Version: %v", apiName, version, patchVersionConvErr)
		return nil, errors.New("Invalid version format")
	}
	return &SemVersion{
		Version: version,
		Major:   majorVersion,
		Minor:   minorVersion,
		Patch:   &patchVersion,
	}, nil
}

// GetMajorMinorVersionRangeRegex generates major and minor version compatible range regex for the given version
func GetMajorMinorVersionRangeRegex(semVersion SemVersion) string {
	versionRegexString := GetVersionMatchRegex(semVersion.Version)
	majorVersion := strconv.Itoa(semVersion.Major)
	minorVersion := strconv.Itoa(semVersion.Minor)
	if semVersion.Patch == nil {
		return "(v" + majorVersion + "(\\." + minorVersion + ")?|" + versionRegexString + ")"
	}
	patchVersion := strconv.Itoa(*semVersion.Patch)
	return "(v" + majorVersion + "(\\." + minorVersion + "(\\." + patchVersion + ")?)?|" + versionRegexString + ")"
}

// GetMinorVersionRangeRegex generates minor version compatible range regex for the given version
func GetMinorVersionRangeRegex(semVersion SemVersion) string {
	versionRegexString := GetVersionMatchRegex(semVersion.Version)
	if semVersion.Patch == nil {
		return "(v" + versionRegexString + "|" + versionRegexString + ")"
	}
	majorVersion := strconv.Itoa(semVersion.Major)
	minorVersion := strconv.Itoa(semVersion.Minor)
	patchVersion := strconv.Itoa(*semVersion.Patch)
	return "(v" + majorVersion + "\\." + minorVersion + "(\\." + patchVersion + ")?|" + versionRegexString + ")"
}

// GetMajorVersionRange generates major version range for the given version
func GetMajorVersionRange(semVersion SemVersion) string {
	return "v" + strconv.Itoa(semVersion.Major)
}

// GetMinorVersionRange generates minor version range for the given version
func GetMinorVersionRange(semVersion SemVersion) string {
	return "v" + strconv.Itoa(semVersion.Major) + "." + strconv.Itoa(semVersion.Minor)
}

// CompareSemanticVersions compares two semantic versions and returns true
// if `version` is greater or equal than `baseVersion`
func CompareSemanticVersions(baseVersion, version SemVersion) bool {
	if baseVersion.Major < version.Major {
		return true
	} else if baseVersion.Major > version.Major {
		return false
	} else {
		if baseVersion.Minor < version.Minor {
			return true
		} else if baseVersion.Minor > version.Minor {
			return false
		} else {
			if baseVersion.Patch != nil && version.Patch != nil {
				if *baseVersion.Patch < *version.Patch {
					return true
				} else if *baseVersion.Patch > *version.Patch {
					return false
				}
			} else if baseVersion.Patch == nil && version.Patch != nil {
				return true
			} else if baseVersion.Patch != nil && version.Patch == nil {
				return false
			}
		}
	}
	return true
}

func updateRoutingRulesOnAPIUpdate(organizationID, apiIdentifier, apiName, apiVersion, vHost string) {
	apiSemVersion, err := ValidateAndGetVersionComponents(apiVersion, apiName)
	// If the version validation is not success, we just proceed without intelligent version
	// Valid version pattern: x.y.z or x.y where x, y and z are non-negative integers
	if err != nil && apiSemVersion == nil {
		return
	}

	apiRangeIdentifier := GenerateIdentifierForAPIWithoutVersion(vHost, apiName)
	// Check the major and minor version ranges of the current API
	existingMajorRangeLatestSemVersion, isMajorRangeRegexAvailable :=
		orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][GetMajorVersionRange(*apiSemVersion)]
	existingMinorRangeLatestSemVersion, isMinorRangeRegexAvailable :=
		orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][GetMinorVersionRange(*apiSemVersion)]

	// Check whether the current API is the latest version in the major and minor version ranges
	isLatestMajorVersion := !isMajorRangeRegexAvailable || CompareSemanticVersions(existingMajorRangeLatestSemVersion, *apiSemVersion)
	isLatestMinorVersion := !isMinorRangeRegexAvailable || CompareSemanticVersions(existingMinorRangeLatestSemVersion, *apiSemVersion)

	// Remove the existing regexes from the path specifier when latest major and/or minor version is available
	if (isMajorRangeRegexAvailable || isMinorRangeRegexAvailable) && (isLatestMajorVersion || isLatestMinorVersion) {
		// Organization's all apis
		for apiUUID, swagger := range orgIDAPIMgwSwaggerMap[organizationID] {
			// API's all versions in the same vHost
			if swagger.GetTitle() == apiName && swagger.GetVHost() == vHost {
				if isMajorRangeRegexAvailable && swagger.GetVersion() == existingMajorRangeLatestSemVersion.Version ||
					isMinorRangeRegexAvailable && swagger.GetVersion() == existingMinorRangeLatestSemVersion.Version {
					for _, route := range orgIDOpenAPIRoutesMap[organizationID][apiUUID] {
						regex := route.GetMatch().GetSafeRegex().GetRegex()
						regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
						existingMinorRangeLatestVersionRegex := GetVersionMatchRegex(existingMinorRangeLatestSemVersion.Version)
						existingMajorRangeLatestVersionRegex := GetVersionMatchRegex(existingMajorRangeLatestSemVersion.Version)
						if isMinorRangeRegexAvailable && swagger.GetVersion() == existingMinorRangeLatestSemVersion.Version && isLatestMinorVersion {
							regex = strings.Replace(regex, GetMinorVersionRangeRegex(existingMinorRangeLatestSemVersion), existingMinorRangeLatestVersionRegex, 1)
							regex = strings.Replace(regex, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), existingMajorRangeLatestVersionRegex, 1)
							regexRewritePattern = strings.Replace(regexRewritePattern, GetMinorVersionRangeRegex(existingMinorRangeLatestSemVersion), existingMinorRangeLatestVersionRegex, 1)
							regexRewritePattern = strings.Replace(regexRewritePattern, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), existingMajorRangeLatestVersionRegex, 1)
						}
						if isMajorRangeRegexAvailable && swagger.GetVersion() == existingMajorRangeLatestSemVersion.Version && isLatestMajorVersion {
							regex = strings.Replace(regex, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), GetMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), 1)
							regexRewritePattern = strings.Replace(regexRewritePattern, GetMajorMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), GetMinorVersionRangeRegex(existingMajorRangeLatestSemVersion), 1)
						}
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
		}
	}

	if isLatestMajorVersion || isLatestMinorVersion {
		// Update local memory map with the latest version ranges
		if _, ok := orgIDLatestAPIVersionMap[organizationID]; !ok {
			latestVersions := make(map[string]SemVersion)
			latestVersions[GetMajorVersionRange(*apiSemVersion)] = *apiSemVersion
			latestVersions[GetMinorVersionRange(*apiSemVersion)] = *apiSemVersion
			apiVersionMap := make(map[string]map[string]SemVersion)
			apiVersionMap[apiRangeIdentifier] = latestVersions
			orgIDLatestAPIVersionMap[organizationID] = apiVersionMap
		} else {
			if _, ok := orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier]; !ok {
				latestVersions := make(map[string]SemVersion)
				latestVersions[GetMajorVersionRange(*apiSemVersion)] = *apiSemVersion
				latestVersions[GetMinorVersionRange(*apiSemVersion)] = *apiSemVersion
				orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier] = latestVersions
			} else {
				if isLatestMajorVersion {
					orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][GetMajorVersionRange(*apiSemVersion)] = *apiSemVersion
				}
				orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][GetMinorVersionRange(*apiSemVersion)] = *apiSemVersion
			}
		}
		// Add the major and/or minor version range matching regexes to the path specifier when
		// latest major and/or minor version is available
		for _, route := range orgIDOpenAPIRoutesMap[organizationID][apiIdentifier] {
			regex := route.GetMatch().GetSafeRegex().GetRegex()
			regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
			apiVersionRegex := GetVersionMatchRegex(apiVersion)
			if isLatestMajorVersion {
				regex = strings.Replace(regex, apiVersionRegex, GetMajorMinorVersionRangeRegex(*apiSemVersion), 1)
				regexRewritePattern = strings.Replace(regexRewritePattern, apiVersionRegex, GetMajorMinorVersionRangeRegex(*apiSemVersion), 1)
			} else if isLatestMinorVersion {
				regex = strings.Replace(regex, apiVersionRegex, GetMinorVersionRangeRegex(*apiSemVersion), 1)
				regexRewritePattern = strings.Replace(regexRewritePattern, apiVersionRegex, GetMinorVersionRangeRegex(*apiSemVersion), 1)
			}
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
	deletingApiSemVersion, _ := ValidateAndGetVersionComponents(api.GetVersion(), api.GetTitle())
	if deletingApiSemVersion == nil {
		return
	}
	majorVersionRange := GetMajorVersionRange(*deletingApiSemVersion)
	newLatestMajorRangeApiUuid := ""
	if deletingApisMajorRangeLatestApiSemVersion, ok := latestAPIVersionMap[majorVersionRange]; ok {
		if deletingApisMajorRangeLatestApiSemVersion.Version == api.GetVersion() {
			newLatestMajorRangeApi := &SemVersion{
				Version: "",
				Major:   deletingApiSemVersion.Major,
				Minor:   0,
				Patch:   nil,
			}
			for apiUUID, swagger := range orgIDAPIMgwSwaggerMap[organizationID] {
				// Iterate all the API versions other than the deleting API itself
				if swagger.GetTitle() == api.GetTitle() && apiUUID != apiIdentifier {
					currentApiSemVersion, _ := ValidateAndGetVersionComponents(swagger.GetVersion(), swagger.GetTitle())
					if currentApiSemVersion != nil {
						if currentApiSemVersion.Major == deletingApiSemVersion.Major {
							if CompareSemanticVersions(*newLatestMajorRangeApi, *currentApiSemVersion) {
								newLatestMajorRangeApi = currentApiSemVersion
								newLatestMajorRangeApiUuid = apiUUID
							}
						}
					}
				}
			}
			if newLatestMajorRangeApiUuid != "" {
				orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][majorVersionRange] = *newLatestMajorRangeApi
				for _, route := range orgIDOpenAPIRoutesMap[organizationID][newLatestMajorRangeApiUuid] {
					regex := route.GetMatch().GetSafeRegex().GetRegex()
					regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
					newLatestMajorRangeApiVersionRegex := GetVersionMatchRegex(newLatestMajorRangeApi.Version)
					// Remove any available minor version range regexes and apply the minor range regex
					regex = strings.Replace(
						regex,
						GetMinorVersionRangeRegex(*newLatestMajorRangeApi),
						newLatestMajorRangeApiVersionRegex,
						1,
					)
					regexRewritePattern = strings.Replace(
						regexRewritePattern,
						GetMinorVersionRangeRegex(*newLatestMajorRangeApi),
						newLatestMajorRangeApiVersionRegex,
						1,
					)
					regex = strings.Replace(
						regex,
						newLatestMajorRangeApiVersionRegex,
						GetMajorMinorVersionRangeRegex(*newLatestMajorRangeApi),
						1,
					)
					regexRewritePattern = strings.Replace(
						regexRewritePattern,
						newLatestMajorRangeApiVersionRegex,
						GetMajorMinorVersionRangeRegex(*newLatestMajorRangeApi),
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
	minorVersionRange := GetMinorVersionRange(*deletingApiSemVersion)
	if deletingApisMinorRangeLatestApi, ok := latestAPIVersionMap[minorVersionRange]; ok {
		if deletingApisMinorRangeLatestApi.Version == api.GetVersion() {
			newLatestMinorRangeApi := &SemVersion{
				Version: "",
				Major:   deletingApiSemVersion.Major,
				Minor:   deletingApiSemVersion.Minor,
				Patch:   nil,
			}
			newLatestMinorRangeApiUuid := ""
			for apiUUID, swagger := range orgIDAPIMgwSwaggerMap[organizationID] {
				// Iterate all the API versions other than the deleting API itself
				if swagger.GetTitle() == api.GetTitle() && apiUUID != apiIdentifier {
					currentApiSemVersion, _ := ValidateAndGetVersionComponents(swagger.GetVersion(), swagger.GetTitle())
					if currentApiSemVersion != nil {
						if currentApiSemVersion.Major == deletingApiSemVersion.Major &&
							currentApiSemVersion.Minor == deletingApiSemVersion.Minor {
							if CompareSemanticVersions(*newLatestMinorRangeApi, *currentApiSemVersion) {
								newLatestMinorRangeApi = currentApiSemVersion
								newLatestMinorRangeApiUuid = apiUUID
							}
						}
					}
				}
			}
			if newLatestMinorRangeApiUuid != "" && newLatestMinorRangeApiUuid != newLatestMajorRangeApiUuid {
				orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier][minorVersionRange] = *newLatestMinorRangeApi
				for _, route := range orgIDOpenAPIRoutesMap[organizationID][newLatestMinorRangeApiUuid] {
					regex := route.GetMatch().GetSafeRegex().GetRegex()
					newLatestMinorRangeApiVersionRegex := GetVersionMatchRegex(newLatestMinorRangeApi.Version)
					regex = strings.Replace(
						regex,
						newLatestMinorRangeApiVersionRegex,
						GetMinorVersionRangeRegex(*newLatestMinorRangeApi),
						1,
					)
					pathSpecifier := &routev3.RouteMatch_SafeRegex{
						SafeRegex: &envoy_type_matcherv3.RegexMatcher{
							Regex: regex,
						},
					}
					regexRewritePattern := route.GetRoute().GetRegexRewrite().GetPattern().GetRegex()
					regexRewritePattern = strings.Replace(
						regexRewritePattern,
						newLatestMinorRangeApiVersionRegex,
						GetMinorVersionRangeRegex(*newLatestMinorRangeApi),
						1,
					)
					route.Match.PathSpecifier = pathSpecifier
					action := &routev3.Route_Route{}
					action = route.Action.(*routev3.Route_Route)
					action.Route.RegexRewrite.Pattern.Regex = regexRewritePattern
					route.Action = action
				}
			} else {
				delete(orgIDLatestAPIVersionMap[organizationID][apiRangeIdentifier], minorVersionRange)
			}
		}
	}
}
