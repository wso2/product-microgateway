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

package model

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetRewriteRegexFromPathTemplate(t *testing.T) {
	tests := []struct {
		pathTemplate        string
		rewritePathTemplate string
		regexRewritePath    string
		isExpError          bool
		message             string
	}{
		{
			pathTemplate:        `/abc/shop/{shopId}/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/shops/{uri.var.shopId}/pets/{uri.var.petId}`,
			regexRewritePath:    `/abc-shops/shops/\1/pets/\2`,
			isExpError:          false,
			message:             `Two params with same order`,
		},
		{
			pathTemplate:        `/abc/shop/{shopId}.xyz/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/shops/{uri.var.shopId}/pets/{uri.var.petId}.pet`,
			regexRewritePath:    `/abc-shops/shops/\1/pets/\2.pet`,
			isExpError:          false,
			message:             `Two params with same order`,
		},
		{
			pathTemplate:        `/abc/shop/{shopId}/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/pets/{uri.var.petId}/shops/{uri.var.shopId}`,
			regexRewritePath:    `/abc-shops/pets/\2/shops/\1`,
			isExpError:          false,
			message:             `Two params with different order`,
		},
		{
			pathTemplate:        `/abc/shop/{shopId}/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/pets/{uri.var.petId}/shops/{uri.var.shopId}/{uri.var.petId}`,
			regexRewritePath:    `/abc-shops/pets/\2/shops/\1/\2`,
			isExpError:          false,
			message:             `Two params with multiple times`,
		},
		{
			pathTemplate:        `/abc/shop/pets`,
			rewritePathTemplate: `/abc-shops/pets/`,
			regexRewritePath:    `/abc-shops/pets`,
			isExpError:          false,
			message:             `No params`,
		},
		{
			pathTemplate:        `/abc/shop/{shopId}/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/pets/{uri.var.nonExistingId}`,
			regexRewritePath:    `/abc-shops/pets/`,
			isExpError:          true,
			message:             `Non existing path param`,
		},
		{
			pathTemplate:        `/abc/shop/{shopId}/pets/{petId}`,
			rewritePathTemplate: `/abc-shops/pets/{invalidParam}`,
			regexRewritePath:    `/abc-shops/pets/`,
			isExpError:          true,
			message:             `Invalid characters`,
		},
	}

	for _, test := range tests {
		regexRewritePath, err := getRewriteRegexFromPathTemplate(test.pathTemplate, test.rewritePathTemplate)
		if test.isExpError {
			assert.Error(t, err, test.message)
		} else {
			assert.Equal(t, test.regexRewritePath, regexRewritePath)
		}
	}
}

func TestGetPathParamToIndexMap(t *testing.T) {
	tests := []struct {
		pathTemplate string
		indexMap     map[string]int
		message      string
	}{
		{
			pathTemplate: `/abc/shop/{shopId}/pets/{petId}`,
			indexMap:     map[string]int{"shopId": 1, "petId": 2},
			message:      `Two params`,
		},
		{
			pathTemplate: `/abc/shop`,
			indexMap:     map[string]int{},
			message:      `No params`,
		},
	}

	for _, test := range tests {
		indexMap := getPathParamToIndexMap(test.pathTemplate)
		assert.Equal(t, test.indexMap, indexMap)
	}
}
