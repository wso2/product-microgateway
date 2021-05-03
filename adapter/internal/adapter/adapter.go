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

// Package adapter contains the implementation to start the adapter
package adapter

import (
	"crypto/tls"
	"strings"

	discoveryv3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	xdsv3 "github.com/envoyproxy/go-control-plane/pkg/server/v3"
	"github.com/wso2/adapter/internal/api/restserver"
	"github.com/wso2/adapter/internal/auth"
	apiservice "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/service/api"
	configservice "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/service/config"
	keymanagerservice "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/service/keymgt"
	subscriptionservice "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/service/subscription"
	throttleservice "github.com/wso2/adapter/internal/discovery/api/wso2/discovery/service/throtlle"
	wso2_server "github.com/wso2/adapter/internal/discovery/protocol/server/v3"
	enforcerCallbacks "github.com/wso2/adapter/internal/discovery/xds/enforcercallbacks"
	routercb "github.com/wso2/adapter/internal/discovery/xds/routercallbacks"
	"github.com/wso2/adapter/internal/health"
	healthservice "github.com/wso2/adapter/internal/health/api/wso2/health/service"
	"github.com/wso2/adapter/internal/tlsutils"

	"context"
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"
	"time"

	"github.com/fsnotify/fsnotify"
	"github.com/wso2/adapter/config"
	"github.com/wso2/adapter/internal/discovery/xds"
	"github.com/wso2/adapter/internal/eventhub"
	"github.com/wso2/adapter/internal/messaging"
	"github.com/wso2/adapter/internal/synchronizer"
	logger "github.com/wso2/adapter/loggers"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

var (
	debug       bool
	onlyLogging bool

	localhost = "0.0.0.0"

	port        uint
	gatewayPort uint
	alsPort     uint

	mode string
)

const (
	ads = "ads"
)

func init() {
	flag.BoolVar(&debug, "debug", true, "Use debug logging")
	flag.BoolVar(&onlyLogging, "onlyLogging", false, "Only demo AccessLogging Service")
	flag.UintVar(&port, "port", 18000, "Management server port")
	flag.UintVar(&gatewayPort, "gateway", 18001, "Management server port for HTTP gateway")
	flag.UintVar(&alsPort, "als", 18090, "Accesslog server port")
	flag.StringVar(&mode, "ads", ads, "Management server type (ads, xds, rest)")
}

const grpcMaxConcurrentStreams = 1000000

func runManagementServer(conf *config.Config, server xdsv3.Server, enforcerServer wso2_server.Server, enforcerSdsServer wso2_server.Server,
	enforcerAppDsSrv wso2_server.Server, enforcerAPIDsSrv wso2_server.Server, enforcerAppPolicyDsSrv wso2_server.Server,
	enforcerSubPolicyDsSrv wso2_server.Server, enforcerAppKeyMappingDsSrv wso2_server.Server,
	enforcerKeyManagerDsSrv wso2_server.Server, enforcerRevokedTokenDsSrv wso2_server.Server,
	enforcerThrottleDataDsSrv wso2_server.Server, port uint) {
	var grpcOptions []grpc.ServerOption
	grpcOptions = append(grpcOptions, grpc.MaxConcurrentStreams(grpcMaxConcurrentStreams))

	cert, err := tlsutils.GetServerCertificate()

	caCertPool := tlsutils.GetTrustedCertPool()

	if err == nil {
		grpcOptions = append(grpcOptions, grpc.Creds(
			credentials.NewTLS(&tls.Config{
				Certificates: []tls.Certificate{cert},
				ClientAuth:   tls.RequireAndVerifyClientCert,
				ClientCAs:    caCertPool,
			}),
		))
	} else {
		logger.LoggerMgw.Warn("failed to initiate the ssl context: ", err)
		panic(err)
	}
	grpcServer := grpc.NewServer(grpcOptions...)

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logger.LoggerMgw.Fatal("failed to listen: ", err)
	}

	// register services
	discoveryv3.RegisterAggregatedDiscoveryServiceServer(grpcServer, server)
	configservice.RegisterConfigDiscoveryServiceServer(grpcServer, enforcerServer)
	apiservice.RegisterApiDiscoveryServiceServer(grpcServer, enforcerServer)
	subscriptionservice.RegisterSubscriptionDiscoveryServiceServer(grpcServer, enforcerSdsServer)
	subscriptionservice.RegisterApplicationDiscoveryServiceServer(grpcServer, enforcerAppDsSrv)
	subscriptionservice.RegisterApiListDiscoveryServiceServer(grpcServer, enforcerAPIDsSrv)
	subscriptionservice.RegisterApplicationPolicyDiscoveryServiceServer(grpcServer, enforcerAppPolicyDsSrv)
	subscriptionservice.RegisterSubscriptionPolicyDiscoveryServiceServer(grpcServer, enforcerSubPolicyDsSrv)
	subscriptionservice.RegisterApplicationKeyMappingDiscoveryServiceServer(grpcServer, enforcerAppKeyMappingDsSrv)
	keymanagerservice.RegisterKMDiscoveryServiceServer(grpcServer, enforcerKeyManagerDsSrv)
	keymanagerservice.RegisterRevokedTokenDiscoveryServiceServer(grpcServer, enforcerRevokedTokenDsSrv)
	throttleservice.RegisterThrottleDataDiscoveryServiceServer(grpcServer, enforcerThrottleDataDsSrv)

	// register health service
	healthservice.RegisterHealthServer(grpcServer, &health.Server{})

	logger.LoggerMgw.Info("port: ", port, " management server listening")
	go func() {
		// if control plane enabled wait until it starts
		if conf.ControlPlane.Enabled {
			// wait current goroutine forever for until control plane starts
			health.WaitForControlPlane()
		}
		logger.LoggerMgw.Info("Starting XDS GRPC server.")
		if err = grpcServer.Serve(lis); err != nil {
			logger.LoggerMgw.Error(err)
		}
	}()
}

// Run starts the XDS server and Rest API server.
func Run(conf *config.Config) {
	sig := make(chan os.Signal)
	signal.Notify(sig, os.Interrupt)
	// TODO: (VirajSalaka) Support the REST API Configuration via flags only if it is a valid requirement
	flag.Parse()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// log config watcher
	// TODO: (VirajSalaka) Implement a rest endpoint to apply configurations
	watcherLogConf, _ := fsnotify.NewWatcher()
	errC := watcherLogConf.Add(config.GetMgwHome() + "/conf/log_config.toml")

	if errC != nil {
		logger.LoggerMgw.Fatal("Error reading the log configs. ", errC)
	}

	logger.LoggerMgw.Info("Starting adapter ....")
	cache := xds.GetXdsCache()
	enforcerCache := xds.GetEnforcerCache()
	enforcerSubscriptionCache := xds.GetEnforcerSubscriptionCache()
	enforcerApplicationCache := xds.GetEnforcerApplicationCache()
	enforcerAPICache := xds.GetEnforcerAPICache()
	enforcerApplicationPolicyCache := xds.GetEnforcerApplicationPolicyCache()
	enforcerSubscriptionPolicyCache := xds.GetEnforcerSubscriptionPolicyCache()
	enforcerApplicationKeyMappingCache := xds.GetEnforcerApplicationKeyMappingCache()
	enforcerKeyManagerCache := xds.GetEnforcerKeyManagerCache()
	enforcerRevokedTokenCache := xds.GetEnforcerRevokedTokenCache()
	enforcerThrottleDataCache := xds.GetEnforcerThrottleDataCache()

	srv := xdsv3.NewServer(ctx, cache, &routercb.Callbacks{})
	enforcerXdsSrv := wso2_server.NewServer(ctx, enforcerCache, &enforcerCallbacks.Callbacks{})
	enforcerSdsSrv := wso2_server.NewServer(ctx, enforcerSubscriptionCache, &enforcerCallbacks.Callbacks{})
	enforcerAppDsSrv := wso2_server.NewServer(ctx, enforcerApplicationCache, &enforcerCallbacks.Callbacks{})
	enforcerAPIDsSrv := wso2_server.NewServer(ctx, enforcerAPICache, &enforcerCallbacks.Callbacks{})
	enforcerAppPolicyDsSrv := wso2_server.NewServer(ctx, enforcerApplicationPolicyCache, &enforcerCallbacks.Callbacks{})
	enforcerSubPolicyDsSrv := wso2_server.NewServer(ctx, enforcerSubscriptionPolicyCache, &enforcerCallbacks.Callbacks{})
	enforcerAppKeyMappingDsSrv := wso2_server.NewServer(ctx, enforcerApplicationKeyMappingCache, &enforcerCallbacks.Callbacks{})
	enforcerKeyManagerDsSrv := wso2_server.NewServer(ctx, enforcerKeyManagerCache, &enforcerCallbacks.Callbacks{})
	enforcerRevokedTokenDsSrv := wso2_server.NewServer(ctx, enforcerRevokedTokenCache, &enforcerCallbacks.Callbacks{})
	enforcerThrottleDataDsSrv := wso2_server.NewServer(ctx, enforcerThrottleDataCache, &enforcerCallbacks.Callbacks{})

	runManagementServer(conf, srv, enforcerXdsSrv, enforcerSdsSrv, enforcerAppDsSrv, enforcerAPIDsSrv,
		enforcerAppPolicyDsSrv, enforcerSubPolicyDsSrv, enforcerAppKeyMappingDsSrv, enforcerKeyManagerDsSrv,
		enforcerRevokedTokenDsSrv, enforcerThrottleDataDsSrv, port)

	// Set enforcer startup configs
	xds.UpdateEnforcerConfig(conf)

	envs := conf.ControlPlane.EnvironmentLabels

	// If no environments are configured, default gateway label value is assigned.
	if len(envs) == 0 {
		envs = append(envs, config.DefaultGatewayName)
	}

	for _, env := range envs {
		listeners, clusters, routes, endpoints, apis := xds.GenerateEnvoyResoucesForLabel(env)
		xds.UpdateXdsCacheWithLock(env, endpoints, clusters, routes, listeners)
		xds.UpdateEnforcerApis(env, apis, "")
	}

	// Adapter REST API
	if conf.Adapter.Server.Enabled {
		if err := auth.Init(); err != nil {
			logger.LoggerMgw.Error("Error while initializing authorization component.", err)
		}
		go restserver.StartRestServer(conf)
	}

	eventHubEnabled := conf.ControlPlane.Enabled
	if eventHubEnabled {
		// Load subscription data
		eventhub.LoadSubscriptionData(conf)

		go messaging.ProcessEvents(conf)

		// Fetch APIs from control plane
		fetchAPIsOnStartUp(conf)

		go synchronizer.UpdateRevokedTokens()
		// Fetch Key Managers from APIM
		synchronizer.FetchKeyManagersOnStartUp(conf)
		go synchronizer.UpdateKeyTemplates()
		go synchronizer.UpdateBlockingConditions()
	}

OUTER:
	for {
		select {
		case l := <-watcherLogConf.Events:
			switch l.Op.String() {
			case "WRITE":
				logger.LoggerMgw.Info("Loading updated log config file...")
				config.ClearLogConfigInstance()
				logger.UpdateLoggers()
			}
		case s := <-sig:
			switch s {
			case os.Interrupt:
				logger.LoggerMgw.Info("Shutting down...")
				break OUTER
			}
		}
	}
	logger.LoggerMgw.Info("Bye!")
}

// fetch APIs from control plane during the server start up and push them
// to the router and enforcer components.
func fetchAPIsOnStartUp(conf *config.Config) {
	// NOTE: Currently controle plane API does not support multiple labels in the same
	// request. Hence until that is fixed, we have to make seperate requests.
	// Checking the envrionments to fetch the APIs from
	envs := conf.ControlPlane.EnvironmentLabels
	// Create a channel for the byte slice (response from the APIs from control plane)
	c := make(chan synchronizer.SyncAPIResponse)
	if len(envs) > 0 {
		// If the envrionment labels are present, call the controle plane
		// with label concurrently (ControlPlane API is not supported for mutiple labels yet)
		logger.LoggerMgw.Debugf("Environments label present: %v", envs)
		go synchronizer.FetchAPIs(nil, envs, c)
	} else {
		// If the environments are not give, fetch the APIs from default envrionment
		logger.LoggerMgw.Debug("Environments label  NOT present. Hence adding \"default\"")
		envs = append(envs, "default")
		go synchronizer.FetchAPIs(nil, nil, c)
	}

	// Wait for each environment to return it's result
	for i := 0; i < len(envs); i++ {
		data := <-c
		logger.LoggerMgw.Debug("Receiving data for an environment")
		if data.Resp != nil {
			// For successfull fetches, data.Resp would return a byte slice with API project(s)
			logger.LoggerMgw.Debug("Pushing data to router and enforcer")
			err := synchronizer.PushAPIProjects(data.Resp, envs)
			if err != nil {
				logger.LoggerMgw.Errorf("Error occurred while pushing API data: %v ", err)
			}
			health.SetControlPlaneRestAPIStatus(err == nil)
		} else if data.ErrorCode >= 400 && data.ErrorCode < 500 {
			logger.LoggerMgw.Errorf("Error occurred when retrieving APIs from control plane: %v", data.Err)
			isNoAPIArtifacts := data.ErrorCode == 404 && strings.Contains(data.Err.Error(), "No Api artifacts found")
			health.SetControlPlaneRestAPIStatus(isNoAPIArtifacts)
		} else {
			// Keep the iteration still until all the envrionment response properly.
			i--
			logger.LoggerMgw.Errorf("Error occurred while fetching data from control plane: %v", data.Err)
			health.SetControlPlaneRestAPIStatus(false)
			go func(d synchronizer.SyncAPIResponse) {
				// Retry fetching from control plane after a configured time interval
				if conf.ControlPlane.RetryInterval == 0 {
					// Assign default retry interval
					conf.ControlPlane.RetryInterval = 5
				}
				logger.LoggerMgw.Debugf("Time Duration for retrying: %v", conf.ControlPlane.RetryInterval*time.Second)
				time.Sleep(conf.ControlPlane.RetryInterval * time.Second)
				logger.LoggerMgw.Infof("Retrying to fetch API data from control plane.")
				synchronizer.FetchAPIs(&d.APIUUID, d.GatewayLabels, c)
			}(data)
		}
	}
}
