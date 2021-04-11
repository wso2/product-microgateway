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

package auth

import (
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"encoding/pem"
	"errors"
	"io/ioutil"
	"time"

	"github.com/lestrrat-go/jwx/jwa"
	"github.com/lestrrat-go/jwx/jwt"
	"github.com/wso2/adapter/config"
	"github.com/wso2/adapter/loggers"
)

const (
	usernameConst string = "username"
	scopeConst    string = "scope"
)

var storedPrivateKey *rsa.PrivateKey
var authTokenDuration *time.Duration

// GetBasicAuth function returns the basicAuth header for the
// given usename and password.
// It returns the base64Encoded(username:password)
func GetBasicAuth(username, password string) string {
	auth := username + ":" + password
	return base64.StdEncoding.EncodeToString([]byte(auth))
}

// ValidateCredentials checks whether the provided username and password are valid
func ValidateCredentials(username, password string, config *config.Config) bool {
	for _, regUser := range config.Adapter.Server.Users {
		if username == regUser.Username && password == regUser.Password {
			return true
		}
	}
	return false
}

func validateUser(username string, config *config.Config) bool {
	for _, regUser := range config.Adapter.Server.Users {
		if username == regUser.Username {
			return true
		}
	}
	return false
}

// Init prepares a private key to sign access tokens, and sets token duration
func Init() (err error) {
	conf, _ := config.ReadConfigs()
	if err = preparePrivateKey(conf); err != nil {
		return err
	}
	setTokenDuration(conf)
	return nil
}

func preparePrivateKey(conf *config.Config) (err error) {
	byteArray, err := ioutil.ReadFile(conf.Adapter.Server.TokenPrivateKeyPath)
	if err != nil {
		return err
	}
	block, _ := pem.Decode(byteArray)
	if block == nil {
		return errors.New("No PEM formatted block found")
	}
	privateKey, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	storedPrivateKey = privateKey.(*rsa.PrivateKey)
	return err
}

func setTokenDuration(conf *config.Config) {
	tokenDurationString := conf.Adapter.Server.TokenTTL
	if tokenDurationString == "" {
		loggers.LoggerAuth.Warn("Token duration not set. Set to default value: 1h")
		tokenDurationString = "1h"
	}
	tokenDuration, err := time.ParseDuration(tokenDurationString)
	if err != nil {
		loggers.LoggerAuth.Warn("Error parsing configured token duration. Set to default value: 1h")
		tokenDuration = time.Hour
	}
	if tokenDuration > 24*time.Hour {
		loggers.LoggerAuth.Warn("Configured token duration is larger than 24hr. Set to max value: 24h")
		tokenDuration = 24 * time.Hour
	}
	authTokenDuration = &tokenDuration
}

// GenerateToken generates an access token for the REST API
func GenerateToken(username string) (accessToken string, err error) {
	privateKey, err := getPrivateKey()
	if err != nil {
		return "", err
	}

	var payload []byte
	// Create signed payload
	token := jwt.New()
	token.Set(usernameConst, username)
	token.Set(scopeConst, "admin")
	expiresAt := time.Now().Add(*authTokenDuration)
	token.Set(jwt.ExpirationKey, expiresAt)

	payload, err = jwt.Sign(token, jwa.RS256, privateKey)
	if err != nil {
		loggers.LoggerAuth.Errorf("failed to generate signed payload: %s", err)
		return "", err
	}
	// loggers.LoggerAuth.Infof("failed to generate signed payload: %s", payload)
	// fmt.Printf(string(payload))
	return string(payload), nil
}

// ValidateToken verifies the signature and validates the access token
func ValidateToken(accessToken string, resourceScopes []string, conf *config.Config) (
	valid bool, err error) {

	privateKey, err := getPrivateKey()
	if err != nil {
		loggers.LoggerAuth.Errorf("Failed to retrive private key: %s", err)
		return false, err
	}
	token, err := jwt.ParseString(
		accessToken,
		jwt.WithValidate(true),
		jwt.WithVerify(jwa.RS256, &privateKey.PublicKey),
	)
	if err != nil {
		loggers.LoggerAPI.Errorf("Failed to parse JWT token: %s", err)
		return false, nil
	}
	tokenUser, _ := token.Get(usernameConst)
	if !validateUser(tokenUser.(string), conf) {
		loggers.LoggerAPI.Error("Invalid username in token.")
		return false, nil
	}
	tokenScope, _ := token.Get(scopeConst)
	if !stringInSlice(tokenScope.(string), resourceScopes) {
		loggers.LoggerAPI.Error("Invalid scope in token.")
		return false, nil
	}
	loggers.LoggerAPI.Info("Valid token recieved")
	return true, nil
}

func getPrivateKey() (*rsa.PrivateKey, error) {
	if storedPrivateKey != nil {
		return storedPrivateKey, nil
	}
	return nil, errors.New("private key not present")
}

func stringInSlice(a string, list []string) bool {
	for _, b := range list {
		if b == a {
			return true
		}
	}
	return false
}
