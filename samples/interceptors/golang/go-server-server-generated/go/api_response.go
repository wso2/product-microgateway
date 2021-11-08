/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package swagger

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
)

type Book struct {
	Name string
}

func HandleResponse(w http.ResponseWriter, r *http.Request) {
	reqBody, err := getResponseHandlerRequestBody(r)
	if err != nil {
		log.Print("Error parsing request body of interceptor", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	var respBody ResponseHandlerResponseBody
	if reqBody.ResponseCode == http.StatusOK {
		respBody.ResponseCode = http.StatusCreated
	}

	respBytes, err := json.Marshal(respBody)
	if err != nil {
		log.Print("Error parsing to ResponseHandlerResponseBody", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	if _, err = w.Write(respBytes); err != nil {
		log.Print("Error writing response body", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(http.StatusOK)
}

func getResponseHandlerRequestBody(r *http.Request) (*ResponseHandlerRequestBody, error) {
	reqBodyBytes, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return nil, err
	}

	var reqBody ResponseHandlerRequestBody
	if err = json.Unmarshal(reqBodyBytes, &reqBody); err != nil {
		return nil, err
	}
	return &reqBody, nil
}
