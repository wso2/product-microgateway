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
package stringutils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMaskString(t *testing.T) {

	type TestMaskStringArguments struct {
		str           string
		visibleLength int
		maskCharacter string
		maskRight     bool
	}

	type MaskStringTestItem struct {
		input   TestMaskStringArguments
		output  string
		message string
	}
	dataItems := []MaskStringTestItem{
		{
			input: TestMaskStringArguments{
				str:           "abcdefghijklm",
				visibleLength: 4,
				maskCharacter: "*",
				maskRight:     true,
			},
			output:  "abcd**********",
			message: "mask from right hand side. keep left four charaters unmasked",
		},
		{
			input: TestMaskStringArguments{
				str:           "abcdefghijklm",
				visibleLength: 5,
				maskCharacter: "*",
				maskRight:     false,
			},
			output:  "**********ijklm",
			message: "mask from left hand side. keep last five characters unmasked",
		},
		{
			input: TestMaskStringArguments{
				str:           "abcdefghijklm",
				visibleLength: 5,
				maskCharacter: "x",
				maskRight:     false,
			},
			output:  "xxxxxxxxxxijklm",
			message: "use 'x' as the mask charactor",
		},
		{
			input: TestMaskStringArguments{
				str:           "pqrwxyz",
				visibleLength: 20,
				maskCharacter: "*",
				maskRight:     false,
			},
			output:  "*******",
			message: "if the visible length is greater than str length mask the whole string",
		},
		{
			input: TestMaskStringArguments{
				str:           "as7df-rjhf6-d764f-35",
				visibleLength: 0,
				maskCharacter: "x",
				maskRight:     false,
			},
			output:  "xxxxxxxxxx",
			message: "mask the whole string",
		},
	}

	for i, item := range dataItems {
		inputArgs := item.input
		result := MaskString(inputArgs.str, inputArgs.visibleLength, inputArgs.maskCharacter, inputArgs.maskRight)
		assert.Equal(t, item.output, result, item.message, i)
	}
}
