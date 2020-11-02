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
	"regexp"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/micro-gw/pkg/oasparser/envoyCodegen"
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
