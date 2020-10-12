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

//TODO: (VirajSalaka) Remove the code as unused.
package api

import (
	"io/ioutil"
	"net/http"

	logger "github.com/wso2/micro-gw/internal/loggers"
)

//This package contains the REST API for the control plane configurations

type RESTService struct{}

// TODO: Implement. Simply copy the swagger content to the location defined in the configs or directly deploy the api.
// Deploy API in microgateway.
func (rest *RESTService) ApiPOST(w http.ResponseWriter, r *http.Request) {
	jsonByteArray, _ := ioutil.ReadAll(r.Body)
	UnzipAndApplyZippedProject(jsonByteArray)
	logger.LoggerApi.Info("Your API is added")
}

func (rest *RESTService) openAPIPOST(w http.ResponseWriter, r *http.Request) {
	logger.LoggerApi.Info("Your API is added")
	jsonByteArray, _ := ioutil.ReadAll(r.Body)
	ApplyOpenAPIFile(jsonByteArray)
}

// Update deployed api
func (rest *RESTService) ApiPUT(w http.ResponseWriter, r *http.Request) {

}

// Remove a deployed api
func (rest *RESTService) ApiDELETE(w http.ResponseWriter, r *http.Request) {

}
