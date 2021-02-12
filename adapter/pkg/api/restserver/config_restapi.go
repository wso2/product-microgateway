// This file is safe to edit. Once it exists it will not be overwritten

// Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Package restserver contains the server for the REST API implementation of the adapter
package restserver

import (
	"crypto/tls"
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/loads"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/middleware"

	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	apiServer "github.com/wso2/micro-gw/pkg/api"
	"github.com/wso2/micro-gw/pkg/api/models"
	"github.com/wso2/micro-gw/pkg/api/restserver/operations"
	"github.com/wso2/micro-gw/pkg/api/restserver/operations/api_collection"
	"github.com/wso2/micro-gw/pkg/api/restserver/operations/api_individual"
	"github.com/wso2/micro-gw/pkg/tlsutils"
)

var (
	mgwConfig *config.Config
)

//go:generate swagger generate server --target ../../api --name Restapi --spec ../../../../resources/adminAPI.yaml --server-package restserver --principal models.Principal

func configureFlags(api *operations.RestapiAPI) {
	// api.CommandLineOptionsGroups = []swag.CommandLineOptionsGroup{ ... }
}

func configureAPI(api *operations.RestapiAPI) http.Handler {
	// configure the api here
	api.ServeError = errors.ServeError

	api.JSONConsumer = runtime.JSONConsumer()
	api.MultipartformConsumer = runtime.DiscardConsumer

	api.JSONProducer = runtime.JSONProducer()

	// Applies when the Authorization header is set with the Basic scheme
	api.BasicAuthAuth = func(user string, pass string) (*models.Principal, error) {
		authenticated := false
		for _, regUser := range mgwConfig.Adapter.Server.Users {
			if user == regUser.Username && pass == regUser.Password {
				authenticated = true
			}
		}
		if !authenticated {
			logger.LoggerAPI.Info("Credentials provided for deploy command are invalid")
			return nil, errors.New(401, "Credentials are invalid")
		}
		// TODO: implement authentication principal
		p := models.Principal{
			Token:    "xxxx",
			Tenant:   "xxxx",
			Username: user,
		}
		logger.LoggerAPI.Debugf("Principal : %v", p)
		return &p, nil
	}

	api.APICollectionGetApisHandler = api_collection.GetApisHandlerFunc(func(params api_collection.GetApisParams,
		principal *models.Principal) middleware.Responder {
		return api_collection.NewGetApisOK().WithPayload(apiServer.ListApis(params.APIType, params.Limit))
	})
	api.APIIndividualPostApisHandler = api_individual.PostApisHandlerFunc(func(params api_individual.PostApisParams,
		principal *models.Principal) middleware.Responder {
		// TODO: (VirajSalaka) Error is not handled in the response.
		jsonByteArray, _ := ioutil.ReadAll(params.File)
		err := apiServer.ApplyAPIProjectWithOverwrite(jsonByteArray, []string{}, params.Overwrite)
		if err != nil {
			if err.Error() == "NOT_FOUND" {
				api_individual.NewPostApisNotFound()
			} else if err.Error() == "ALREADY_EXISTS" {
				api_individual.NewPostApisConflict()
			}
			return api_individual.NewPostApisInternalServerError()
		}
		return api_individual.NewPostApisOK()
	})
	api.APIIndividualPostApisDeleteHandler = api_individual.PostApisDeleteHandlerFunc(func(params api_individual.PostApisDeleteParams,
		principal *models.Principal) middleware.Responder {
		errCode, _ := apiServer.DeleteAPI(params.APIName, params.Version, params.Vhost)
		switch errCode {
		case "":
			return api_individual.NewPostApisDeleteOK()
		case "NOT_FOUND":
			return api_individual.NewPostApisDeleteNotFound()
		default:
			return api_individual.NewPostApisInternalServerError()
		}
	})

	api.PreServerShutdown = func() {}

	api.ServerShutdown = func() {}

	return setupGlobalMiddleware(api.Serve(setupMiddlewares))
}

// The TLS configuration before HTTPS server starts.
func configureTLS(tlsConfig *tls.Config) {
	cert, err := tlsutils.GetServerCertificate()
	if err == nil {
		tlsConfig.Certificates = []tls.Certificate{cert}
	}
}

// As soon as server is initialized but not run yet, this function will be called.
// If you need to modify a config, store server instance to stop it individually later, this is the place.
// This function can be called multiple times, depending on the number of serving schemes.
// scheme value will be set accordingly: "http", "https" or "unix"
func configureServer(s *http.Server, scheme, addr string) {
}

// The middleware configuration is for the handler executors. These do not apply to the swagger.json document.
// The middleware executes after routing but before authentication, binding and validation
func setupMiddlewares(handler http.Handler) http.Handler {
	return handler
}

// The middleware configuration happens before anything, this middleware also applies to serving the swagger.json document.
// So this is a good place to plug in a panic handling middleware, logging and metrics
func setupGlobalMiddleware(handler http.Handler) http.Handler {
	return handler
}

// StartRestServer starts the listener which is used to fetch the requests sent from apictl.
func StartRestServer(config *config.Config) {
	mgwConfig = config
	swaggerSpec, err := loads.Embedded(SwaggerJSON, FlatSwaggerJSON)
	if err != nil {
		logger.LoggerAPI.Fatal(err)
	}

	api := operations.NewRestapiAPI(swaggerSpec)
	server := NewServer(api)
	defer server.Shutdown()

	server.ConfigureAPI()
	server.TLSHost = mgwConfig.Adapter.Server.Host
	port, err := strconv.Atoi(mgwConfig.Adapter.Server.Port)
	if err != nil {
		logger.LoggerAPI.Fatalf("The provided port value for the REST Api Server :%v is not an integer. %v", mgwConfig.Adapter.Server.Port, err)
		return
	}
	server.TLSPort = port
	if err := server.Serve(); err != nil {
		logger.LoggerAPI.Fatal(err)
	}

}
