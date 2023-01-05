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

package proxyserver

import (
	"net/http"
	"time"

	"github.com/gorilla/mux"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
)

const organizationID = "organizationId"
const queryParamValueALL = "ALL"

// for the pupose of versioning of endpoints to external microservices
const externalMicroserviceContextV1 = "/external/ms/v1/"

const moeisfUniqueIdentifier = "Moesif"

// Server is a wrapped http server with a http client
type Server struct {
	client *http.Client
}

// New function defines the new server structure with a http client.
func New(conf *config.Config) *Server {

	requestTimeOut := conf.ControlPlane.HTTPClient.RequestTimeOut

	client := &http.Client{
		Transport: nil,
		Timeout:   requestTimeOut * time.Second,
	}

	srv := &Server{
		client: client,
	}
	return srv
}

// RunAPIServer function initializes the GA API server.
func (s *Server) RunAPIServer(conf *config.Config) {
	router := mux.NewRouter()

	// HTTPGetToExternalMSHandler is wrapper that takes unique identifier for the microservice,
	// then return a function of type http.HandlerFunc
	router.HandleFunc(externalMicroserviceContextV1+"moesif", BasicAuth(s.HTTPGetToExternalMSHandler(moeisfUniqueIdentifier))).Methods(http.MethodGet)
	router.HandleFunc(externalMicroserviceContextV1+"moesif"+"?"+organizationID+"="+queryParamValueALL,
		BasicAuth(s.HTTPGetToGAHandler)).Methods(http.MethodGet)

	logger.LoggerMgw.Info("Starting LA Proxy Server...")
	srv := &http.Server{
		Handler:      router,
		Addr:         conf.LAProxyServer.Host + ":" + conf.LAProxyServer.Port,
		WriteTimeout: 60 * time.Second,
		ReadTimeout:  60 * time.Second,
	}

	logger.LoggerMgw.Fatal(srv.ListenAndServe())
}
