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

package sourcewatcher

import (
	"io/ioutil"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
)

func setSourceWatcherConfig(artifactsDir, username, accessToken, repositoryURL string){
	conf, _ := config.ReadConfigs()
	conf.Adapter.ArtifactsDirectory = artifactsDir
	conf.Adapter.SourceControl.ArtifactsDirectory = artifactsDir
	conf.Adapter.SourceControl.Repository.Username = username
	conf.Adapter.SourceControl.Repository.AccessToken = username
	conf.Adapter.SourceControl.Repository.URL = repositoryURL
	config.SetConfig(conf)
}

// Test the fetchArtifacts method for a public repository with invalid credentials
func TestFetchArtifactsWithInvalidCredentials(t *testing.T) {
	dir, err := ioutil.TempDir("", "test")
	defer os.RemoveAll(dir)
	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}
	setSourceWatcherConfig(dir, "admin", "admin", "https://github.com/wso2/product-microgateway")
	
	_, err = fetchArtifacts()
	assert.NotNil(t, err, "Error fetching artifacts")
	config.SetDefaultConfig()
}

// Test the fetchArtifacts method for an invalid repository
func TestFetchArtifactsWithInvalidRepository(t *testing.T) {
	dir, err := ioutil.TempDir("", "test")
	defer os.RemoveAll(dir)
	if err != nil {
		t.Error("Error creating temp directory." + err.Error())
	}
	setSourceWatcherConfig(dir, "admin", "admin", "https://github.com/user/repository")
	
	_, err = fetchArtifacts()
	assert.NotNil(t, err, "Fetching artifacts failed")
	config.SetDefaultConfig()
}
