/*
 *  Copyright (c) 2023, WSO2 LLC (http://www.wso2.org).
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
package synchronizer

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/notifier"
)

func TestMergeDeployedRevisionList(t *testing.T) {
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		// This has to be error. For debugging purpose info
		logger.LoggerSync.Errorf("Error reading configs: %v", errReadConfig)
	}
	input := []*notifier.DeployedAPIRevision{
		{
			APIID:      "63c8bf26dbe45c52fe7ed1cf",
			RevisionID: 2,
			EnvInfo: []notifier.DeployedEnvInfo{
				{
					Name:  "dev",
					VHost: conf.Adapter.SandboxVhost,
				},
			},
		},
		{
			APIID:      "63c8bf26dbe45c52fe7ed1cf",
			RevisionID: 2,
			EnvInfo: []notifier.DeployedEnvInfo{
				{
					Name:  "dev",
					VHost: "dev.host",
				},
			},
		},
		{
			APIID:      "63c8bf26dbe45c52fe7ed1c3",
			RevisionID: 2,
			EnvInfo: []notifier.DeployedEnvInfo{
				{
					Name:  "dev",
					VHost: "dev.host",
				},
			},
		},
	}
	expectedOutput := map[string]*notifier.DeployedAPIRevision{
		"63c8bf26dbe45c52fe7ed1cf": {
			APIID:      "63c8bf26dbe45c52fe7ed1cf",
			RevisionID: 2,
			EnvInfo: []notifier.DeployedEnvInfo{
				{
					Name:  conf.Adapter.SandboxEnvName,
					VHost: conf.Adapter.SandboxVhost,
				},
				{
					Name:  "dev",
					VHost: "dev.host",
				},
			},
		},
		"63c8bf26dbe45c52fe7ed1c3": {
			APIID:      "63c8bf26dbe45c52fe7ed1c3",
			RevisionID: 2,
			EnvInfo: []notifier.DeployedEnvInfo{
				{
					Name:  "dev",
					VHost: "dev.host",
				},
			},
		},
	}
	deploymentList := MergeDeployedRevisionList(input)

	if len(deploymentList) != 2 {
		t.Errorf("Expected length of deployment list is 2, but got %d", len(deploymentList))
	}

	for _, deploymentListItem := range deploymentList {
		assert.NotNil(t, expectedOutput[deploymentListItem.APIID], "APIID not found in expected output")
		assert.Equal(t, expectedOutput[deploymentListItem.APIID].APIID, deploymentListItem.APIID, "APIID mismatch")
		assert.Equal(t, expectedOutput[deploymentListItem.APIID].RevisionID, deploymentListItem.RevisionID, "RevisionID mismatch")
		assert.Equal(t, len(expectedOutput[deploymentListItem.APIID].EnvInfo), len(deploymentListItem.EnvInfo), "EnvInfo length mismatch")
		assert.Equal(t, reflect.DeepEqual(expectedOutput[deploymentListItem.APIID].EnvInfo, deploymentListItem.EnvInfo), true, "EnvInfo mismatch")
	}
}
