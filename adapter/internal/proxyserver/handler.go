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
 */

/*
	Package handler contains logic related to authenticate the GA API requests and handle API requests accordingly.
*/

package proxyserver

import (
	"crypto/sha256"
	"crypto/subtle"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

// HTTPGetHandler function handles get requests
func (s *Server) HTTPGetToGAHandler(w http.ResponseWriter, r *http.Request) {
	buildRequestToGA(s.client, w, r, http.MethodGet, nil)
}

// HTTPPostHandler function handles post requests
func (s *Server) HTTPPostToGAHandler(w http.ResponseWriter, r *http.Request) {
	buildRequestToGA(s.client, w, r, http.MethodPost, r.Body)
}

// HTTPatchHandler function handles patch requests
func (s *Server) HTTPatchToGAHandler(w http.ResponseWriter, r *http.Request) {
	buildRequestToGA(s.client, w, r, http.MethodPatch, r.Body)
}

// HTTPGetForExternalMSHandler function handles get requests sending to external microservices
func (s *Server) HTTPGetToExternalMSHandler(msUniqueIdentifier string) http.HandlerFunc {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		buildRequestForExtMS(s.client, w, r, http.MethodGet, nil, msUniqueIdentifier)
	})
}

// HTTPPostToExternalMSHandler function handles post requests sending to external microservices
func (s *Server) HTTPPostToExternalMSHandler(msUniqueIdentifier string) http.HandlerFunc {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		buildRequestForExtMS(s.client, w, r, http.MethodPost, r.Body, msUniqueIdentifier)
	})
}

// HTTPPostToExternalMSHandler function handles post requests sending to external microservices
func (s *Server) HTTPPatchToExternalMSHandler(msUniqueIdentifier string) http.HandlerFunc {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		buildRequestForExtMS(s.client, w, r, http.MethodPatch, r.Body, msUniqueIdentifier)
	})
}

func buildRequestToGA(client *http.Client, w http.ResponseWriter, r *http.Request, httpMethod string, body io.Reader) {
	conf, error := config.ReadConfigs()

	if error != nil {
		logger.LoggerMgw.Error("Error occured while reading config.")
		return
	}
	var requestURL = conf.GlobalAdapter.ServiceURL + r.RequestURI

	sendHTTPRequestToGA(client, conf, requestURL, w, httpMethod, body)
}

func buildRequestForExtMS(client *http.Client, w http.ResponseWriter, r *http.Request, httpMethod string, body io.Reader, msUniqueIdentifier string) {
	conf, error := config.ReadConfigs()

	if error != nil {
		logger.LoggerMgw.Error("Error occured while reading config.")
		return
	}
	var msServiceURL string

	// Handle a new microservice by adding corresponding case.
	switch msUniqueIdentifier {
	case moeisfUniqueIdentifier:
		msServiceURL = conf.MoesifMicroservice.ServiceURL
	default:
		logger.LoggerMgw.Error("Unrecognizable microservice identifier has been passed: ", msUniqueIdentifier)
		return
	}
	var requestURL = msServiceURL + r.RequestURI

	sendHTTPRequestToExtMS(client, conf, requestURL, w, httpMethod, body, msUniqueIdentifier)
}

func sendHTTPRequestToGA(client *http.Client, conf *config.Config, requestURL string, w http.ResponseWriter, httpMethod string, body io.Reader) {
	logger.LoggerMgw.Debug("Request URL for Global Adapter: ", requestURL)
	req, err := http.NewRequest(httpMethod, requestURL, body)

	if err != nil {
		w.WriteHeader(500)
		logger.LoggerMgw.Error("Error occurred while constructing http request to Global Adapter ", err)
		return
	}
	req.SetBasicAuth(conf.GlobalAdapter.Username, conf.GlobalAdapter.Password)

	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)

	if err != nil {
		w.WriteHeader(500)
		logger.LoggerMgw.Error("Error occurred while connecting to Global Adapter ", err)
		return
	}

	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		bodyBytes, _ := ioutil.ReadAll(resp.Body)
		bodyString := string(bodyBytes)
		w.WriteHeader(resp.StatusCode)
		fmt.Fprintf(w, "%s", bodyString)
	}

	bodyBytes, _ := ioutil.ReadAll(resp.Body)
	bodyString := string(bodyBytes)
	fmt.Fprintf(w, "%s", bodyString)
}

func sendHTTPRequestToExtMS(client *http.Client, conf *config.Config, requestURL string, w http.ResponseWriter, httpMethod string, body io.Reader, msUniqueIdentifier string) {
	logger.LoggerMgw.Debug(fmt.Sprintf("Request URL of the microservice (%s): ", msUniqueIdentifier), requestURL)
	req, err := http.NewRequest(httpMethod, requestURL, body)

	if err != nil {
		w.WriteHeader(500)
		logger.LoggerMgw.Error(fmt.Sprintf("Error occurred while constructing http request sending to microservice (%s) ", msUniqueIdentifier),
			err)
		return
	}

	var authUsername string
	var authPassword string

	// Handle a new microservice by adding corresponding case.
	switch msUniqueIdentifier {
	case moeisfUniqueIdentifier:
		authUsername = conf.MoesifMicroservice.Username
		authPassword = conf.MoesifMicroservice.Password
	}

	req.SetBasicAuth(authUsername, authPassword)

	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")

	resp, err := client.Do(req)

	if err != nil {
		w.WriteHeader(500)
		logger.LoggerMgw.Error(fmt.Sprintf("Error occurred while connecting to microservice (%s) ", msUniqueIdentifier), err)
		return
	}

	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		bodyBytes, _ := ioutil.ReadAll(resp.Body)
		bodyString := string(bodyBytes)
		w.WriteHeader(resp.StatusCode)
		fmt.Fprintf(w, "%s", bodyString)
	}

	bodyBytes, _ := ioutil.ReadAll(resp.Body)
	bodyString := string(bodyBytes)
	fmt.Fprintf(w, "%s", bodyString)
}

// BasicAuth function authenticates the request.
func BasicAuth(next http.HandlerFunc) http.HandlerFunc {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		username, password, ok := r.BasicAuth()
		conf, error := config.ReadConfigs()

		if error != nil {
			logger.LoggerMgw.Error("Error occured while reading config.")
			return
		}
		if ok {
			usernameHash := sha256.Sum256([]byte(username))
			passwordHash := sha256.Sum256([]byte(password))
			expectedUsernameHash := sha256.Sum256([]byte(conf.LAProxyServer.Username))
			expectedPasswordHash := sha256.Sum256([]byte(conf.LAProxyServer.Password))

			usernameMatch := (subtle.ConstantTimeCompare(usernameHash[:], expectedUsernameHash[:]) == 1)
			passwordMatch := (subtle.ConstantTimeCompare(passwordHash[:], expectedPasswordHash[:]) == 1)

			if usernameMatch && passwordMatch {
				next.ServeHTTP(w, r)
				return
			}
		}

		w.Header().Set("WWW-Authenticate", `Basic realm="restricted", charset="UTF-8"`)
		http.Error(w, "Unauthorized Access", http.StatusUnauthorized)
	})
}
