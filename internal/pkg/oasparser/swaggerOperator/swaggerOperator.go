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
package swaggerOperator

import (
	"encoding/json"
	"github.com/getkin/kin-openapi/openapi3"
	"github.com/go-openapi/spec"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/apiDefinition"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/utills"
	"io/ioutil"
	"log"
	"os"
)

func GenerateMgwSwagger(location string) ([]apiDefinition.MgwSwagger, error) {
	var mgwSwaggers []apiDefinition.MgwSwagger

	files, err := ioutil.ReadDir(location)
	if err != nil {
		log.Fatal("Error reading",location,"directory:", err)
	}

	for _, f := range files {
		var mgwSwagger apiDefinition.MgwSwagger
		openApif, err := os.Open(location + f.Name())

		// if we os.Open returns an error then handle it
		if err != nil {
			log.Fatal("Error opening a api yaml file:", err)
		}
		//fmt.Println("Successfully Opened open api file",f.Name())
		log.Println("Successfully Opened open api file",f.Name())

		// defer the closing of our jsonFile so that we can parse it later on
		defer openApif.Close()

		// read our opened jsonFile as a byte array.
		jsn, _ := ioutil.ReadAll(openApif)

		apiJsn, err := utills.ToJSON(jsn)
		if err != nil {
			//log.Fatal("Error converting api file to json:", err)

		}

		swaggerVerison, err := utills.FindSwaggerVersion(apiJsn)
		if err != nil {
			log.Println("Error finding a swagger version of the api definition:", err)
		}

		if swaggerVerison == "2" {
			//map json to struct
			var ApiData spec.Swagger
			err = json.Unmarshal(apiJsn, &ApiData)
			if err != nil {
				//log.Fatal("Error openAPI unmarsheliing: %v\n", err)
			} else {
				mgwSwagger.SetInfoSwagger(ApiData)
			}

		} else if swaggerVerison == "3" {
			//map json to struct
			var ApiData openapi3.Swagger
			err = json.Unmarshal(apiJsn, &ApiData)
			if err != nil {
				//log.Fatal("Error openAPI unmarsheliing: %v\n", err)
			} else {
				mgwSwagger.SetInfoOpenApi(ApiData)
			}
		}

		mgwSwagger.SetXWso2Extenstions()
		mgwSwaggers = append(mgwSwaggers, mgwSwagger)

	}
	return mgwSwaggers, err
}

func IsEndpointsAvailable(endpoints []apiDefinition.Endpoint) bool {
	if len(endpoints) > 0 {
		return true
	} else {
		return false
	}
}