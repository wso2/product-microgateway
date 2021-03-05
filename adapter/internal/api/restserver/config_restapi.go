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
	apiServer "github.com/wso2/micro-gw/internal/api"
	"github.com/wso2/micro-gw/internal/api/models"
	"github.com/wso2/micro-gw/internal/api/restserver/operations"
	"github.com/wso2/micro-gw/internal/api/restserver/operations/api_collection"
	"github.com/wso2/micro-gw/internal/api/restserver/operations/api_individual"
	"github.com/wso2/micro-gw/internal/api/restserver/operations/authorization"
	"github.com/wso2/micro-gw/internal/auth"
	constants "github.com/wso2/micro-gw/internal/oasparser/model"
	"github.com/wso2/micro-gw/internal/tlsutils"
	logger "github.com/wso2/micro-gw/loggers"
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
	api.BasicAuthAuth = func(username, password string) (*models.Principal, error) {
		validCredentials := auth.ValidateCredentials(username, password, mgwConfig)
		if !validCredentials {
			logger.LoggerAPI.Info("Credentials provided for basic auth are invalid")
			return nil, errors.New(401, "Credentials are invalid")
		}
		// TODO: implement authentication principal
		p := models.Principal{
			Token:    "xxxx",
			Tenant:   "xxxx",
			Username: username,
		}
		logger.LoggerAPI.Debugf("Principal : %v", p)
		return &p, nil
	}

	api.BearerTokenAuth = func(token string, scopes []string) (*models.Principal, error) {
		valid, err := auth.ValidateToken(token, scopes)
		if err != nil || !valid {
			logger.LoggerAPI.Info("The provided token is not valid or server error")
			return nil, errors.Unauthenticated("The provided token is not valid or server error")
		}
		p := models.Principal{
			Token:    token,
			Tenant:   "xxxx",
			Username: "xxxx",
		}
		return &p, nil
	}

	// Handler for /oauth2/token
	api.AuthorizationPostOauth2TokenHandler = authorization.PostOauth2TokenHandlerFunc(func(
		params authorization.PostOauth2TokenParams) middleware.Responder {

		validCredentials := auth.ValidateCredentials(*params.Credentials.Username,
			*params.Credentials.Password, mgwConfig)
		if !validCredentials {
			logger.LoggerAPI.Info("Credentials provided to obtain a token are invalid. User: ",
				*params.Credentials.Username)
			return authorization.NewPostOauth2TokenUnauthorized()
		}
		accessToken, err := auth.GenerateToken(*params.Credentials.Username)
		if err != nil {
			return authorization.NewPostOauth2TokenInternalServerError()
		}
		token := authorization.PostOauth2TokenOKBody{}
		token.AccessToken = accessToken
		logger.LoggerAPI.Info("Access token created for user: ", *params.Credentials.Username)
		return authorization.NewPostOauth2TokenOK().WithPayload(&token)
	})

	// Handlers for /apis
	api.APIIndividualDeleteApisHandler = api_individual.DeleteApisHandlerFunc(func(
		params api_individual.DeleteApisParams, principal *models.Principal) middleware.Responder {

		err := apiServer.DeleteAPI(params.Vhost, params.APIName, params.Version)
		if err == nil {
			return api_individual.NewDeleteApisOK()
		}
		switch err.Error() {
		case constants.NotFound:
			return api_individual.NewDeleteApisNotFound()
		default:
			return api_individual.NewPostApisInternalServerError()
		}
	})
	api.APICollectionGetApisHandler = api_collection.GetApisHandlerFunc(func(
		params api_collection.GetApisParams, principal *models.Principal) middleware.Responder {

		return api_collection.NewGetApisOK().WithPayload(apiServer.ListApis(params.Query, params.Limit))
	})
	api.APIIndividualPostApisHandler = api_individual.PostApisHandlerFunc(func(
		params api_individual.PostApisParams, principal *models.Principal) middleware.Responder {
		jsonByteArray, _ := ioutil.ReadAll(params.File)
		err := apiServer.ApplyAPIProjectWithOverwrite(jsonByteArray, []string{}, params.Override)
		if err != nil {
			switch err.Error() {
			case constants.AlreadyExists:
				return api_individual.NewPostApisConflict()
			default:
				return api_individual.NewPostApisInternalServerError()
			}
		}
		return api_individual.NewPostApisOK()
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
