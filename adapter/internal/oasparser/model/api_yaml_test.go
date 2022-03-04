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

func TestPolicyListGetStats(t *testing.T) {
	pl := PolicyList{{PolicyName: "addHeader"}, {PolicyName: "rewriteMethod"}, {PolicyName: "addHeader"}}
	expSt := map[string]policyStats{
		"addHeader":     {firstIndex: 0, count: 2},
		"rewriteMethod": {firstIndex: 1, count: 1},
	}
	st := pl.getStats()
	assert.Equal(t, expSt, st)
}
