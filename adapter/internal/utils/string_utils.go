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

package stringutils

import (
	"strings"
)

// MaskString takes string as argument and keep a visibleLength number of charators and mask rest of the string with maskCharacter
// It can mask the charators from either left or right hand side of the string depending on maskRight boolean
// It returns the masked string
func MaskString(str string, visibleLength int, maskCharacter string, maskRight bool) string {
	stringLength := len(str)

	if 0 <= visibleLength && visibleLength < stringLength {
		maskLength := stringLength - visibleLength
		mask := strings.Repeat(maskCharacter, maskLength)
		if maskRight {
			return str[:visibleLength] + mask
		}
		return mask + str[maskLength:]
	}

	return str
}
