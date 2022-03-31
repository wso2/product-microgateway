// This file is safe to edit. Once it exists it will not be overwritten

// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"fmt"
	"io/ioutil"
	"net/http"

	// enable profiling endpoints
	_ "net/http/pprof"
	"strconv"
	"strings"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/loads"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/middleware"

	"github.com/wso2/product-microgateway/adapter/pkg/health"
	"github.com/wso2/product-microgateway/adapter/pkg/logging"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"

	"github.com/wso2/product-microgateway/adapter/config"
	apiServer "github.com/wso2/product-microgateway/adapter/internal/api"
	"github.com/wso2/product-microgateway/adapter/internal/api/models"
	"github.com/wso2/product-microgateway/adapter/internal/api/restserver/operations"
	"github.com/wso2/product-microgateway/adapter/internal/api/restserver/operations/api_collection"
	"github.com/wso2/product-microgateway/adapter/internal/api/restserver/operations/api_individual"
	"github.com/wso2/product-microgateway/adapter/internal/api/restserver/operations/authorization"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
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

	// Get the organizationId
	tenantDomain := config.GetControlPlaneConnectedTenantDomain()

	// Applies when the Authorization header is set with the Basic scheme
	api.BasicAuthAuth = func(username, password string) (*models.Principal, error) {
		validCredentials := auth.ValidateCredentials(username, password, mgwConfig)
		if !validCredentials {
			logger.LoggerAPI.Info("Credentials provided for basic auth are invalid")
			return nil, errors.New(401, "Credentials are invalid")
		}
		p := models.Principal{}
		p.Username = username
		logger.LoggerAPI.Debugf("Principal : %v", p)
		return &p, nil
	}

	api.BearerTokenAuth = func(token string, scopes []string) (*models.Principal, error) {
		valid, err := auth.ValidateToken(token, scopes, mgwConfig)
		if err != nil {
			logger.LoggerAPI.ErrorC(logging.ErrorDetails{
				Message:   fmt.Sprintf("Error occurred while reading the token %v", err.Error()),
				Severity:  logging.CRITICAL,
				ErrorCode: 1203,
			})
			return nil, errors.New(500, "error occurred while reading the token")
		}
		if !valid {
			logger.LoggerAPI.Info("The provided token is not valid")
			return nil, errors.Unauthenticated("The provided token is not valid")
		}
		p := models.Principal{}
		p.Token = token
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

		conf, _ := config.ReadConfigs()

		if conf.ControlPlane.Enabled {
			errCode := int64(400)
			errMsg := "When control plane is enabled, APIs cannot be directly undeployed from the adapter. Using apictl, still you can undeploy an API in APIM instead."
			err := models.Error{
				Code:        &errCode,
				Description: "Bad request",
				Message:     &errMsg,
			}
			logger.LoggerAPI.Info(errMsg)
			return api_individual.NewDeleteApisBadRequest().WithPayload(&err)
		}

		vhost := ""
		if params.Vhost != nil {
			vhost = *params.Vhost
		}
		var environments []string
		if params.Environments != nil {
			environments = strings.Split(*params.Environments, ":")
		}
		err := xds.DeleteAPIs(vhost, params.APIName, params.Version, environments, tenantDomain)
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

		return api_collection.NewGetApisOK().WithPayload(apiServer.ListApis(params.Query, params.Limit, tenantDomain))
	})
	api.APIIndividualPostApisHandler = api_individual.PostApisHandlerFunc(func(
		params api_individual.PostApisParams, principal *models.Principal) middleware.Responder {

		conf, _ := config.ReadConfigs()

		if conf.ControlPlane.Enabled {
			errCode := int64(400)
			errMsg := "When control plane is enabled, APIs cannot be directly deployed to the adapter. Using apictl, still you can import APIs to APIM instead."
			err := models.Error{
				Code:    &errCode,
				Message: &errMsg,
			}
			logger.LoggerAPI.Info(errMsg)
			// TODO: (suksw) Generate new 400 error object for POST request and update apictl to print the message
			return api_individual.NewDeleteApisBadRequest().WithPayload(&err)
		}

		jsonByteArray, _ := ioutil.ReadAll(params.File)
		_, err := apiServer.ApplyAPIProjectInStandaloneMode(jsonByteArray, params.Override)
		if err != nil {
			if err.Error() == constants.AlreadyExists {
				return api_individual.NewPostApisConflict()
			} else if strings.HasPrefix(err.Error(), "An API exists with the same basepath") {
				return api_individual.NewPostApisConflict()
			} else {
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
	publicKeyLocation, privateKeyLocation, _ := tlsutils.GetKeyLocations()
	cert, err := tlsutils.GetServerCertificate(publicKeyLocation, privateKeyLocation)
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
		logger.LoggerAPI.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("Error while loading openAPI specification for Adapter REST API %v", err.Error()),
			Severity:  logging.BLOCKER,
			ErrorCode: 1201,
		})
	}

	api := operations.NewRestapiAPI(swaggerSpec)
	server := NewServer(api)
	defer server.Shutdown()

	server.ConfigureAPI()
	server.TLSHost = mgwConfig.Adapter.Server.Host
	port, err := strconv.Atoi(mgwConfig.Adapter.Server.Port)
	if err != nil {
		logger.LoggerAPI.ErrorC(logging.ErrorDetails{
			Message:   fmt.Sprintf("The provided port value for the REST Api Server :%v is not an integer. %v", mgwConfig.Adapter.Server.Port, err.Error()),
			Severity:  logging.BLOCKER,
			ErrorCode: 1200,
		})
		return
	}
	server.TLSPort = port

	// handle server interruption
	go func() {
		select {
		case _ = <-server.interrupt:
			logger.LoggerAPI.Info("Rest server is interrupted. Update health status of RestService")
			health.RestService.SetStatus(false)
		case _ = <-server.shutdown:
			logger.LoggerAPI.Info("Rest server is shutdown. Update health status of RestService")
			health.RestService.SetStatus(false)
		}
	}()

	// expose new port for serving pprof based go profiling endpoints
	go func() {
		logger.LoggerAPI.Fatal(http.ListenAndServe("localhost:6060", nil))
	}()

	health.RestService.SetStatus(true)
	if err := server.Serve(); err != nil {
		logger.LoggerAPI.Fatal(err)
		health.RestService.SetStatus(false)
	}
}
