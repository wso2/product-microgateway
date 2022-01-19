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
	"context"
	"crypto/tls"
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"
	"strings"

	discoveryv3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	xdsv3 "github.com/envoyproxy/go-control-plane/pkg/server/v3"
	"github.com/fsnotify/fsnotify"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/wso2/product-microgateway/adapter/pkg/adapter"
	apiservice "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/api"
	configservice "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/config"
	keymanagerservice "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/keymgt"
	subscriptionservice "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/subscription"
	throttleservice "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/throttle"
	wso2_server "github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/server/v3"
	"github.com/wso2/product-microgateway/adapter/pkg/health"
	healthservice "github.com/wso2/product-microgateway/adapter/pkg/health/api/wso2/health/service"
	sync "github.com/wso2/product-microgateway/adapter/pkg/synchronizer"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"

	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/api/deployer"
	"github.com/wso2/product-microgateway/adapter/internal/api/restserver"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	enforcerCallbacks "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/enforcercallbacks"
	routercb "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/routercallbacks"
	"github.com/wso2/product-microgateway/adapter/internal/eventhub"
	"github.com/wso2/product-microgateway/adapter/internal/ga"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/messaging"
	"github.com/wso2/product-microgateway/adapter/internal/synchronizer"
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
	ads          = "ads"
	amqpProtocol = "amqp"
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
	publicKeyLocation, privateKeyLocation, truststoreLocation := tlsutils.GetKeyLocations()
	cert, err := tlsutils.GetServerCertificate(publicKeyLocation, privateKeyLocation)

	caCertPool := tlsutils.GetTrustedCertPool(truststoreLocation)

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
	sig := make(chan os.Signal, 2)
	signal.Notify(sig, os.Interrupt)
	// TODO: (VirajSalaka) Support the REST API Configuration via flags only if it is a valid requirement
	flag.Parse()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// log config watcher
	watcherLogConf, _ := fsnotify.NewWatcher()
	logConfigPath, errC := config.GetLogConfigPath()
	if errC == nil {
		errC = watcherLogConf.Add(logConfigPath)
	}

	if errC != nil {
		logger.LoggerMgw.Error("Error reading the log configs. ", errC)
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
		xds.GenerateGlobalClusters(env)
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

	gaEnabled := conf.GlobalAdapter.Enabled
	if gaEnabled {
		go ga.InitGAClient()
		FetchAPIUUIDsFromGlobalAdapter()
	}

	eventHubEnabled := conf.ControlPlane.Enabled
	if eventHubEnabled {
		if !gaEnabled {
			// Load subscription data when GA is disabled.
			eventhub.LoadSubscriptionData(conf, nil)
			// Fetch APIs at start up when GA is disabled.
			fetchAPIsOnStartUp(conf, nil)
		}

		var connectionURLList = conf.ControlPlane.BrokerConnectionParameters.EventListeningEndpoints
		if strings.Contains(connectionURLList[0], amqpProtocol) {
			go messaging.ProcessEvents(conf)
		} else {
			messaging.InitiateAndProcessEvents(conf)
		}

		go synchronizer.UpdateRevokedTokens()
		// Fetch Key Managers from APIM
		synchronizer.FetchKeyManagersOnStartUp(conf)
		go synchronizer.UpdateKeyTemplates()
		go synchronizer.UpdateBlockingConditions()
	} else {
		err := deployer.ProcessMountedAPIProjects()
		if err != nil {
			logger.LoggerMgw.Error("Readiness probe is not set as local api artifacts processing has failed.")
			return
		}
		// We need to deploy the readiness probe when eventhub is disabled
		xds.DeployReadinessAPI(envs)
		logger.LoggerMgw.Info("Event hub disabled and hence deployed readiness probe")
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
func fetchAPIsOnStartUp(conf *config.Config, apiUUIDList []string) {
	// Populate data from config.
	serviceURL := conf.ControlPlane.ServiceURL
	userName := conf.ControlPlane.Username
	password := conf.ControlPlane.Password
	envs := conf.ControlPlane.EnvironmentLabels
	skipSSL := conf.ControlPlane.SkipSSLVerification
	retryInterval := conf.ControlPlane.RetryInterval
	truststoreLocation := conf.Adapter.Truststore.Location
	requestTimeOut := conf.ControlPlane.HTTPClient.RequestTimeOut

	// Create a channel for the byte slice (response from the APIs from control plane)
	c := make(chan sync.SyncAPIResponse)

	// Get API details.
	if apiUUIDList == nil {
		adapter.GetAPIs(c, nil, serviceURL, userName, password, envs, skipSSL, truststoreLocation,
			sync.RuntimeArtifactEndpoint, true, nil, requestTimeOut)
	} else {
		adapter.GetAPIs(c, nil, serviceURL, userName, password, envs, skipSSL, truststoreLocation,
			sync.APIArtifactEndpoint, true, apiUUIDList, requestTimeOut)
	}
	for i := 0; i < 1; i++ {
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
			sync.RetryFetchingAPIs(c, serviceURL, userName, password, skipSSL, truststoreLocation, retryInterval,
				data, sync.RuntimeArtifactEndpoint, true, requestTimeOut)
		}
	}
	// All apis are fetched. Deploy the /ready route for the readiness and startup probes.
	xds.DeployReadinessAPI(envs)
	logger.LoggerMgw.Info("Fetching APIs at startup is completed...")
}

// FetchAPIUUIDsFromGlobalAdapter get the UUIDs of the APIs at the LA startup from GA
func FetchAPIUUIDsFromGlobalAdapter() {
	logger.LoggerMgw.Info("Fetching APIs at Local Adapter startup...")
	apiEventsAtStartup := ga.FetchAPIsFromGA()
	conf, _ := config.ReadConfigs()
	initialAPIUUIDListMap := make(map[string]int)
	var apiUUIDList []string
	for i, apiEventAtStartup := range apiEventsAtStartup {
		apiUUIDList = append(apiUUIDList, apiEventAtStartup.APIUUID)
		initialAPIUUIDListMap[apiEventAtStartup.APIUUID] = i
	}
	// Load subscription data with the received API UUID list map when GA is enabled.
	eventhub.LoadSubscriptionData(conf, initialAPIUUIDListMap)
	// Fetch APIs at LA startup with the received API UUID list when GA is enabled.
	fetchAPIsOnStartUp(conf, apiUUIDList)
}
