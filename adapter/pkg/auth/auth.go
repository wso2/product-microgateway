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
	"encoding/base64"
	"errors"
	"io/ioutil"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/go-git/go-git/v5/plumbing/transport"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/go-git/go-git/v5/plumbing/transport/ssh"
)

// GetBasicAuth function returns the basicAuth header for the
// given usename and password.
// It returns the base64Encoded(username:password)
func GetBasicAuth(username, password string) string {
	auth := username + ":" + password
	return base64.StdEncoding.EncodeToString([]byte(auth))
}

// GetGitAuth returns the authentication for the repository
func GetGitAuth() (transport.AuthMethod, error) {
	conf, err := config.ReadConfigs()
	if err != nil {
		loggers.LoggerAPI.Errorf("Error reading configs: %v", err)
		return nil, err
	}

	username := conf.Adapter.SourceControl.Repository.Username
	sshKeyFile := conf.Adapter.SourceControl.Repository.SSHKeyFile
	if username == "" && sshKeyFile == "" {
		return &http.BasicAuth{}, nil
	} else if username != "" {
		accessToken := conf.Adapter.SourceControl.Repository.AccessToken
		return &http.BasicAuth{
			Username: username,
			Password: accessToken,
		}, nil
	} else if sshKeyFile != "" {
		sshKey, err := ioutil.ReadFile(sshKeyFile)
		if err != nil {
			loggers.LoggerAPI.Errorf("Error reading ssh key file: %v", err)
			return nil, err
		}

		publicKey, err := ssh.NewPublicKeys(ssh.DefaultUsername, sshKey, "")
		if err != nil {
			loggers.LoggerAPI.Errorf("Error creating public key: %v", err)
			return nil, err
		}

		return publicKey, nil
	}
	return nil, errors.New("No username or ssh key file provided")
}
