/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
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

package config_test

import (
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
)

func TestMain(m *testing.M) {
	// Write code here to run before tests

	// Run tests
	exitVal := m.Run()

	// Write code here to run after tests

	// Exit with exit value from tests
	os.Exit(exitVal)
}

func TestEnvConfigAssignment(t *testing.T) {
	conf, _ := config.ReadConfigs()
	logconfig := config.ReadLogConfigs()
	assert.Equal(t, "9401", conf.Adapter.Server.Port, "String value assignment from environment failed.")
	assert.Equal(t, true, conf.Enforcer.JwtGenerator.Enable, "Boolean value assignment from environment failed.")
	assert.Equal(t, true, conf.Adapter.Server.Enabled, "Boolean value assignment from environment failed.")
	assert.Equal(t, time.Duration(25), conf.GlobalAdapter.RetryInterval, "Time.Duration value assignment from environment failed.")
	assert.Equal(t, uint32(32768), conf.Analytics.Adapter.BufferSizeBytes, "Uint32 value assignment from environment failed.")
	assert.Equal(t, int32(1800), conf.Enforcer.JwtIssuer.ValidityPeriod, "Int32 value assignment from environment failed.")
	assert.Equal(t, 2, conf.Adapter.Consul.PollInterval, "Int value assignment from environment failed.")
	authToken := conf.Analytics.Enforcer.ConfigProperties["authToken"]
	assert.Equal(t, "test-token", authToken, "Map Value(String) assignment from environment failed.")
	assert.Equal(t, "MGW-Test", conf.Enforcer.Security.TokenService[1].Name,
		"String value assignement (within Struct Array) from environment failed.")
	assert.Equal(t, "password", conf.Adapter.Server.Users[0].Password,
		"$env{} resolution failed")
	assert.Equal(t, "INFO", logconfig.LogLevel, "Logconfig log level mismatch")
}
