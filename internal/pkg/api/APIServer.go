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
package api

import (
	"github.com/gorilla/mux"
	"github.com/wso2/micro-gw/configs/confTypes"
	logger "github.com/wso2/micro-gw/internal/loggers"
	"net/http"
)

type Server struct {
}

func Start(config *confTypes.Config) {
	router := mux.NewRouter()

	apiService := new(RESTService)
	// API specific routs
	apiRouter := router.PathPrefix("/api").Subrouter()
	apiRouter.HandleFunc("/add", apiService.ApiPOST).Methods("POST")
	// TODO: Immplement
	//Configuration specific routes
	//configRouter := router.PathPrefix("/configs").Subrouter()
	//
	//authRouter := router.PathPrefix("/apikey").Subrouter()
	//configService := new(ConfigService)
	serverAddr := config.Server.IP + ":" + string(config.Server.Port)
	//certFile := configs.TLS.Alias

	server := &http.Server{
		Addr:    serverAddr,
		Handler: router,
	}
	logger.LoggerApi.Info("Starting API Server at ", serverAddr)
	logger.LoggerApi.Fatal(server.ListenAndServe())
}
