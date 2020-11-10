/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package envoyCodegen_test

import (
	"io/ioutil"
	"regexp"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/configs"
	"github.com/wso2/micro-gw/pkg/oasparser/envoyCodegen"
	enovoy "github.com/wso2/micro-gw/pkg/oasparser/envoyCodegen"
	"github.com/wso2/micro-gw/pkg/oasparser/swaggerOperator"
)

func TestGenerateRegex(t *testing.T) {

	type generateRegexTestItem struct {
		inputpath     string
		userInputPath string
		message       string
		isMatched     bool
	}
	dataItems := []generateRegexTestItem{
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5",
			message:       "when path parameter is provided end of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}/info",
			userInputPath: "/v2/pet/5/info",
			message:       "when path parameter is provided in the middle of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5",
			message:       "when path parameter is provided end of the path",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}/tst/{petId}",
			userInputPath: "/v2/pet/5/tst/3",
			message:       "when multiple path parameter are provided",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/{petId}",
			userInputPath: "/v2/pet/5/test",
			message:       "when path parameter is provided end of the path and provide incorrect path",
			isMatched:     false,
		},
		{
			inputpath:     "/v2/pet/5",
			userInputPath: "/v2/pett/5",
			message:       "when provide a incorrect path",
			isMatched:     false,
		},
		{
			inputpath:     "/v2/pet/findById",
			userInputPath: "/v2/pet/findById?status=availabe",
			message:       "when query parameter is provided",
			isMatched:     true,
		},
		{
			inputpath:     "/v2/pet/findById",
			userInputPath: "/v2/pet/findByIdstatus=availabe",
			message:       "when query parameter is provided without ?",
			isMatched:     false,
		},
	}

	for _, item := range dataItems {
		resultPattern := envoyCodegen.GenerateRegex(item.inputpath)
		resultIsMatching, err := regexp.MatchString(resultPattern, item.userInputPath)

		assert.Equal(t, item.isMatched, resultIsMatching, item.message)
		assert.Nil(t, err)
	}
}

func TestCreateRoutesWithClusters(t *testing.T) {
	//TODO: (VirajSalaka) Finalize if reading from a file and asserting is the correct approach for unit tests
	openapiFilePath := configs.GetMgwHome() + "/../pilot/test-resources/envoycodegen/openapi.yaml"
	openapiByteArr, err := ioutil.ReadFile(openapiFilePath)
	assert.Nil(t, err, "Error while reading the openapi file : "+openapiFilePath)
	mgwSwaggerForOpenapi := swaggerOperator.GetMgwSwagger(openapiByteArr)
	//TODO: (VirajSalaka) Test Sandbox endpoints
	routes, clusters, _, _, _, _ := enovoy.CreateRoutesWithClusters(mgwSwaggerForOpenapi)

	assert.Equal(t, 2, len(clusters), "Number of production clusters created is incorrect.")
	//TODO: (VirajSalaka) Test against path level endpoints
	//As the first cluster is always related to API level cluster
	apiLevelCluster := clusters[0]
	pathLevelCluster := clusters[1]
	assert.Equal(t, apiLevelCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0", "API Level cluster name mismatch")
	assert.Contains(t, pathLevelCluster.GetName(), "clusterProd_SwaggerPetstore1.0.0_", "Resource Level cluster name mismatch")

	assert.Equal(t, 2, len(routes), "Created number of routes are incorrect.")
	assert.Equal(t, "^/pets(\\?([^/]+))?$", routes[0].GetMatch().GetSafeRegex().Regex)
	assert.Equal(t, "^/pets/([^/]+)(\\?([^/]+))?$", routes[1].GetMatch().GetSafeRegex().Regex)
	routeRegexMatchesFound := false
	for _, route := range routes {
		if route.GetMatch().GetSafeRegex().Regex == "^/pets(\\?([^/]+))?$" {
			routeRegexMatchesFound = true
			assert.Equal(t, pathLevelCluster.GetName(), route.GetRoute().GetCluster(), "Path level cluster is not set correctly.")
		}
		if route.GetMatch().GetSafeRegex().Regex == "^/pets/([^/]+)(\\?([^/]+))?$" {
			routeRegexMatchesFound = true
			assert.Equal(t, apiLevelCluster.GetName(), route.GetRoute().GetCluster(), "API level cluster is not set correctly.")
		}
	}
	assert.Equal(t, true, routeRegexMatchesFound, "Generated route regex is incorrect.")
}
