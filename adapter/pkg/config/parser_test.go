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
