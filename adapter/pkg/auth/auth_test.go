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

package auth

import (
	"testing"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/stretchr/testify/assert"
)

func setGitAuth(username, accessToken, sshKeyFile string){
	conf, _ := config.ReadConfigs()

	conf.Adapter.SourceControl.Repository.Username = username
	conf.Adapter.SourceControl.Repository.AccessToken = accessToken
	conf.Adapter.SourceControl.Repository.SSHKeyFile = sshKeyFile

	config.SetConfig(conf)
}

// Test the getAuth method without credentials
func TestGetGitAuthWithoutCredentials(t *testing.T) {
	setGitAuth("", "", "")

	auth, _ := GetGitAuth()

	expectedAuth := &http.BasicAuth{}

	assert.Equal(t, expectedAuth, auth, "Invalid auth")

	config.SetDefaultConfig()
}

// Test the getAuth method with credentials
func TestGetAuthWithCredentials(t *testing.T) {
	setGitAuth("admin", "admin", "")

	actualAuth, _ := GetGitAuth()

	expectedAuth := &http.BasicAuth{
		Username: "admin",
		Password: "admin",
	}

	assert.Equal(t, expectedAuth, actualAuth, "Invalid auth")

	config.SetDefaultConfig()
}
