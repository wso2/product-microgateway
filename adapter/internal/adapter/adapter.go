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
	"encoding/json"
	"strings"
	"time"

	discoveryv3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	xdsv3 "github.com/envoyproxy/go-control-plane/pkg/server/v3"
	"github.com/wso2/product-microgateway/adapter/internal/api"
	restserver "github.com/wso2/product-microgateway/adapter/internal/api/restserver"
	"github.com/wso2/product-microgateway/adapter/internal/auth"
	"github.com/wso2/product-microgateway/adapter/internal/common"
	enforcerCallbacks "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/enforcercallbacks"
	ratelimiterCallbacks "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/ratelimitercallbacks"
	routercb "github.com/wso2/product-microgateway/adapter/internal/discovery/xds/routercallbacks"
	"github.com/wso2/product-microgateway/adapter/internal/ga"
	"github.com/wso2/product-microgateway/adapter/internal/messaging"
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

	"context"
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"

	"github.com/fsnotify/fsnotify"
	"github.com/wso2/product-microgateway/adapter/config"
	"github.com/wso2/product-microgateway/adapter/internal/discovery/xds"
	"github.com/wso2/product-microgateway/adapter/internal/eventhub"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/internal/synchronizer"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/keepalive"
)

var (
	debug       bool
	onlyLogging bool

	localhost = "0.0.0.0"

	port    uint
	rlsPort uint
	alsPort uint

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
	flag.UintVar(&rlsPort, "rls-port", 18001, "Rate Limiter management server port")
	flag.UintVar(&alsPort, "als", 18090, "Accesslog server port")
	flag.StringVar(&mode, "ads", ads, "Management server type (ads, xds, rest)")
}

const grpcMaxConcurrentStreams = 1000000

func runManagementServer(conf *config.Config, server xdsv3.Server, rlsServer xdsv3.Server, enforcerServer wso2_server.Server, enforcerSdsServer wso2_server.Server,
	enforcerAppDsSrv wso2_server.Server, enforcerAPIDsSrv wso2_server.Server, enforcerAppPolicyDsSrv wso2_server.Server,
	enforcerSubPolicyDsSrv wso2_server.Server, enforcerAppKeyMappingDsSrv wso2_server.Server,
	enforcerKeyManagerDsSrv wso2_server.Server, enforcerRevokedTokenDsSrv wso2_server.Server,
	enforcerThrottleDataDsSrv wso2_server.Server, port, rlsPort uint) {
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

	grpcOptions = append(grpcOptions, grpc.KeepaliveParams(
		keepalive.ServerParameters{
			Time:    time.Duration(5 * time.Minute),
			Timeout: time.Duration(20 * time.Second),
		}),
	)
	grpcServer := grpc.NewServer(grpcOptions...)

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logger.LoggerMgw.Fatalf("Failed to listen %v: %v", port, err)
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
			logger.LoggerMgw.Error("Error serving xDS gRPC server: ", err)
		}
	}()

	if conf.Envoy.RateLimit.Enabled {
		// It is required a separate gRPC server for the rate limit xDS, since it is the same RPC method
		// ADS used in both envoy xDS and rate limiter xDS.
		// According to https://github.com/envoyproxy/ratelimit/pull/368#discussion_r995831078 a separate RPC service is not
		// defined specifically to the rate limit xDS, instead using the ADS.
		logger.LoggerMgw.Info("port: ", rlsPort, " rate-limiter management server listening")
		rlsGrpcServer := grpc.NewServer(grpcOptions...)
		rlsLis, err := net.Listen("tcp", fmt.Sprintf(":%d", rlsPort))
		if err != nil {
			logger.LoggerMgw.Fatalf("Failed to listen %v: %v", port, err)
		}

		discoveryv3.RegisterAggregatedDiscoveryServiceServer(rlsGrpcServer, rlsServer)
		go func() {
			logger.LoggerMgw.Info("Starting Rate Limiter xDS gRPC server.")
			health.RateLimiterGrpcService.SetStatus(true)
			if err = rlsGrpcServer.Serve(rlsLis); err != nil {
				health.RateLimiterGrpcService.SetStatus(false)
				logger.LoggerMgw.Error("Error serving Rate Limiter xDS gRPC server: ", err)
			}
		}()
	}

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

	if conf.GlobalAdapter.Enabled {
		logger.LoggerMgw.Infof("Preparing adapter in partition: %s to serve dynamic configurations via xDS....", conf.GlobalAdapter.LocalLabel)
	} else {
		logger.LoggerMgw.Info("Preparing adapter to serve dynamic configurations via xDS....")
	}
	cache := xds.GetXdsCache()
	rateLimiterCache := xds.GetRateLimiterCache()
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
	rlsSrv := xdsv3.NewServer(ctx, rateLimiterCache, &ratelimiterCallbacks.Callbacks{})
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

	runManagementServer(conf, srv, rlsSrv, enforcerXdsSrv, enforcerSdsSrv, enforcerAppDsSrv, enforcerAPIDsSrv,
		enforcerAppPolicyDsSrv, enforcerSubPolicyDsSrv, enforcerAppKeyMappingDsSrv, enforcerKeyManagerDsSrv,
		enforcerRevokedTokenDsSrv, enforcerThrottleDataDsSrv, port, rlsPort)

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
		xds.UpdateRateLimiterPolicies(env)
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
		err := api.ProcessMountedAPIProjects()
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
	envs := conf.ControlPlane.EnvironmentLabels

	// Create a channel for the byte slice (response from the APIs from control plane)
	c := make(chan sync.SyncAPIResponse)

	var queryParamMap map[string]string
	queryParamMap = common.PopulateQueryParamForOrganizationID(queryParamMap)
	// Get API details.
	if apiUUIDList == nil {
		if conf.ControlPlane.DynamicEnvironments.Enabled {
			queryParamMap = common.PopulateQueryParamForDataPlane(queryParamMap)
			adapter.GetAPIs(c, nil, nil, sync.RetrieveRuntimeArtifactEndpoint, true, nil, queryParamMap)
		} else {
			adapter.GetAPIs(c, nil, envs, sync.RuntimeArtifactEndpoint, true, nil, queryParamMap)
		}
	} else {
		if conf.ControlPlane.DynamicEnvironments.Enabled {
			queryParamMap = common.PopulateQueryParamForDataPlane(queryParamMap)
			adapter.GetAPIs(c, nil, nil, sync.RetrieveRuntimeArtifactEndpoint, true, apiUUIDList, queryParamMap)
		} else {
			adapter.GetAPIs(c, nil, envs, sync.APIArtifactEndpoint, true, apiUUIDList, queryParamMap)
		}
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
			if conf.ControlPlane.DynamicEnvironments.Enabled {
				sync.RetryFetchingAPIs(c, data, sync.RetrieveRuntimeArtifactEndpoint, true, queryParamMap, apiUUIDList)
			} else {
				sync.RetryFetchingAPIs(c, data, sync.RuntimeArtifactEndpoint, true, queryParamMap, apiUUIDList)
			}
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
	b, _ := json.Marshal(apiEventsAtStartup)
	logger.LoggerMgw.Debugf("apiEventsAtStartup : %s", string(b))
	conf, _ := config.ReadConfigs()
	envs := conf.ControlPlane.EnvironmentLabels
	initialAPIUUIDListMap := make(map[string]int)
	var apiUUIDList []string
	for i, apiEventAtStartup := range apiEventsAtStartup {
		logger.LoggerMgw.Debugf("Looping on apiEventsAtStartup iteration : %d", i)
		apiUUIDList = append(apiUUIDList, apiEventAtStartup.APIUUID)
		initialAPIUUIDListMap[apiEventAtStartup.APIUUID] = i
	}
	// Load subscription data with the received API UUID list map when GA is enabled.
	logger.LoggerMgw.Debugf("initialAPIUUIDListMap: %v", initialAPIUUIDListMap)
	eventhub.LoadSubscriptionData(conf, initialAPIUUIDListMap)
	// Fetch APIs at LA startup with the received API UUID list when GA is enabled.
	// When apiUUIDList is null, we don't need to fetch APIs.
	if apiUUIDList == nil {
		logger.LoggerMgw.Infof("No api artifacts received from the GA at the startup, hense deploying readiness health API..")
		health.SetControlPlaneRestAPIStatus(true)
		xds.DeployReadinessAPI(envs)
	} else {
		fetchAPIsOnStartUp(conf, apiUUIDList)
	}
	ga.StartConsumeGAAPIChannel()
}
