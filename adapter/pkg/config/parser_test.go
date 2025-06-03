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
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/wso2/product-microgateway/adapter/config"
	pkgconf "github.com/wso2/product-microgateway/adapter/pkg/config"
)

func TestMain(m *testing.M) {
	exitVal := m.Run()
	os.Exit(exitVal)
}

type testStruct1 struct {
	Test testStruct2
}

type testStruct2 struct {
	StringArray []string
	IntArray    []int
	FloatArray  []float32
	Int32Array  []int32
	Int64Array  []int64
	Float32Val  float32
	Float64Val  float64
	UIntArray   []uint
	UInt32Array []uint32
	UInt64Array []uint64
	UIntArray2  []uint
}

func TestEnvConfigAssignment(t *testing.T) {
	conf, _ := config.ReadConfigs()
	logconfig := config.ReadLogConfigs()
	assert.Equal(t, "9401", conf.Adapter.Server.Port, "String value assignment from environment failed.")
	assert.Equal(t, true, conf.Enforcer.JwtGenerator.Enabled, "Boolean value assignment from environment failed.")
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

func TestArrayValueAssignmentFromEnv(t *testing.T) {
	var stringArray []string
	var intArray []int
	var floatArray []float32
	var uintArray []uint
	stringArray = append(stringArray, "bar1")
	intArray = append(intArray, 20)
	uintArray = append(uintArray, uint(3))
	var testStruct = &testStruct1{
		Test: testStruct2{
			StringArray: stringArray,
			IntArray:    intArray,
			FloatArray:  floatArray,
			UIntArray2:  uintArray,
		},
	}
	pkgconf.ResolveConfigEnvValues(reflect.ValueOf(testStruct).Elem(), "Test", true)
	assert.Equal(t, "foo2", testStruct.Test.StringArray[0])
	assert.Equal(t, "bar2", testStruct.Test.StringArray[1])
	assert.Equal(t, 1, testStruct.Test.IntArray[0])
	assert.Equal(t, float32(2.4), testStruct.Test.FloatArray[1])
	assert.Equal(t, int32(4), testStruct.Test.Int32Array[0])
	assert.Equal(t, int64(21474836479), testStruct.Test.Int64Array[0])
	assert.Equal(t, float32(1.5), testStruct.Test.Float32Val)
	assert.Equal(t, float64(6.5), testStruct.Test.Float64Val)
	assert.Equal(t, uint(50), testStruct.Test.UIntArray[0])
	assert.Equal(t, uint32(100), testStruct.Test.UInt32Array[0])
	assert.Equal(t, uint64(42949672959), testStruct.Test.UInt64Array[0])
	assert.Equal(t, uint(3), testStruct.Test.UIntArray2[0])
}
func TestResolveEnvValue(t *testing.T) {
	// Set up test environment variables
	os.Setenv("mykey1", "value1")
	os.Setenv("mykey2", "value2")
	os.Setenv("TESTKEY", "testvalue")
	os.Setenv("EMPTY_KEY", "")
	defer func() {
		os.Unsetenv("mykey1")
		os.Unsetenv("mykey2")
		os.Unsetenv("TESTKEY")
		os.Unsetenv("EMPTY_KEY")
	}()

	tests := []struct {
		name     string
		input    string
		expected string
	}{
		{
			name:     "Multiple placeholders in string",
			input:    "hello-$env{mykey1}-bar-$env{mykey2}-baz",
			expected: "hello-value1-bar-value2-baz",
		},
		{
			name:     "Single placeholder",
			input:    "$env{TESTKEY}",
			expected: "testvalue",
		},
		{
			name:     "Placeholder at beginning",
			input:    "$env{mykey1}-suffix",
			expected: "value1-suffix",
		},
		{
			name:     "Placeholder at end",
			input:    "prefix-$env{mykey2}",
			expected: "prefix-value2",
		},
		{
			name:     "Multiple same placeholders",
			input:    "$env{mykey1}-middle-$env{mykey1}",
			expected: "value1-middle-value1",
		},
		{
			name:     "Non-existent environment variable",
			input:    "hello-$env{NONEXISTENT}-world",
			expected: "hello-$env{NONEXISTENT}-world",
		},
		{
			name:     "Empty environment variable value",
			input:    "hello-$env{EMPTY_KEY}-world",
			expected: "hello--world",
		},
		{
			name:     "No placeholders",
			input:    "hello-world",
			expected: "hello-world",
		},
		{
			name:     "Empty string",
			input:    "",
			expected: "",
		},
		{
			name:     "Malformed placeholder - missing closing brace",
			input:    "hello-$env{mykey1-world",
			expected: "hello-$env{mykey1-world",
		},
		{
			name:     "Malformed placeholder - missing opening brace",
			input:    "hello-$envmykey1}-world",
			expected: "hello-$envmykey1}-world",
		},
		{
			name:     "Mixed valid and invalid placeholders",
			input:    "$env{mykey1}-$env{INVALID-$env{mykey2}",
			expected: "value1-$env{INVALID-value2",
		},
		{
			name:     "Placeholder with special characters in key",
			input:    "$env{my_key-1}",
			expected: "$env{my_key-1}",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := pkgconf.ResolveEnvValue(tt.input)
			assert.Equal(t, tt.expected, result)
		})
	}
}

func TestResolveEnvValueWithComplexValues(t *testing.T) {
	// Test with environment values that contain special characters
	os.Setenv("COMPLEX_VALUE", "value-with-$-and-{}-chars")
	os.Setenv("JSON_VALUE", `{"key": "value", "nested": {"inner": "data"}}`)
	defer func() {
		os.Unsetenv("COMPLEX_VALUE")
		os.Unsetenv("JSON_VALUE")
	}()

	tests := []struct {
		name     string
		input    string
		expected string
	}{
		{
			name:     "Environment value with special characters",
			input:    "config=$env{COMPLEX_VALUE}",
			expected: "config=value-with-$-and-{}-chars",
		},
		{
			name:     "Environment value with JSON",
			input:    "json_config=$env{JSON_VALUE}",
			expected: `json_config={"key": "value", "nested": {"inner": "data"}}`,
		},
		{
			name:     "Multiple complex values",
			input:    "$env{COMPLEX_VALUE}-separator-$env{JSON_VALUE}",
			expected: `value-with-$-and-{}-chars-separator-{"key": "value", "nested": {"inner": "data"}}`,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := pkgconf.ResolveEnvValue(tt.input)
			assert.Equal(t, tt.expected, result)
		})
	}
}
