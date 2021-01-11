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

// Package mgw contains the implementation to start the adapter
package mgw

import (
	discoveryv3 "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	xdsv3 "github.com/envoyproxy/go-control-plane/pkg/server/v3"
	apiservice "github.com/envoyproxy/go-control-plane/wso2/discovery/service/api"
	configservice "github.com/envoyproxy/go-control-plane/wso2/discovery/service/config"
	"github.com/wso2/micro-gw/pkg/api/restserver"

	"context"
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"
	"time"

	"github.com/fsnotify/fsnotify"
	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/synchronizer"
	xds "github.com/wso2/micro-gw/pkg/xds"
	"google.golang.org/grpc"
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

func runManagementServer(server xdsv3.Server, enforcerServer xdsv3.Server, port uint) {
	var grpcOptions []grpc.ServerOption
	grpcOptions = append(grpcOptions, grpc.MaxConcurrentStreams(grpcMaxConcurrentStreams))
	grpcServer := grpc.NewServer()

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logger.LoggerMgw.Fatal("failed to listen: ", err)
	}

	// register services
	discoveryv3.RegisterAggregatedDiscoveryServiceServer(grpcServer, server)
	configservice.RegisterConfigDiscoveryServiceServer(grpcServer, enforcerServer)
	apiservice.RegisterApiDiscoveryServiceServer(grpcServer, enforcerServer)

	logger.LoggerMgw.Info("port: ", port, " management server listening")
	go func() {
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
	errC := watcherLogConf.Add("conf/log_config.toml")

	if errC != nil {
		logger.LoggerMgw.Fatal("Error reading the log configs. ", errC)
	}

	logger.LoggerMgw.Info("Starting adapter ....")
	cache := xds.GetXdsCache()
	enforcerCache := xds.GetEnforcerCache()
	srv := xdsv3.NewServer(ctx, cache, nil)
	enforcerXdsSrv := xdsv3.NewServer(ctx, enforcerCache, nil)

	runManagementServer(srv, enforcerXdsSrv, port)

	// Set enforcer startup configs
	xds.UpdateEnforcerConfig(conf)

	// Fetch APIs from control plane
	fetchAPIsOnStartUp(conf)

	go restserver.StartRestServer(conf)
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
	envs := conf.ControlPlane.EventHub.EnvironmentLabels
	// Create a channel for the byte slice (response from the APIs from control plane)
	c := make(chan synchronizer.SyncAPIResponse)
	if len(envs) > 0 {
		// If the envrionment labels are present, call the controle plane
		// with label concurrently (ControlPlane API is not supported for mutiple labels yet)
		logger.LoggerMgw.Debug("Environments label present: %v", envs)
		for _, env := range envs {
			go synchronizer.FetchAPIs(nil, &env, c)
		}
	} else {
		// If the environments are not give, fetch the APIs from default envrionment
		logger.LoggerMgw.Debug("Environments label  NOT present. Hence adding \"default\"")
		envs = append(envs, "default")
		go synchronizer.FetchAPIs(nil, nil, c)
	}

	// Wait for each environment to return it's resul
	for i := 0; i < len(envs); i++ {
		data := <-c
		logger.LoggerMgw.Debug("Receing data for an envrionment: %v", string(data.Resp))
		if data.Resp != nil {
			// For successfull fetches, data.Resp would return a byte slice with API project(s)
			logger.LoggerMgw.Debug("Pushing data to router and enforcer")
			err := synchronizer.PushAPIProjects(data.Resp)
			if err != nil {
				logger.LoggerMgw.Errorf("Error occurred while pushing API data: %v ", err)
			}
		} else {
			// Keep the iteration still until all the envrionment response properly.
			i--
			logger.LoggerMgw.Errorf("Error occurred while fetching data from control plane: %v", data.Err)
			go func(d synchronizer.SyncAPIResponse) {
				// Retry fetching from control plane after a configured time interval
				if conf.ControlPlane.EventHub.RetryInterval == 0 {
					// Assign default retry interval
					conf.ControlPlane.EventHub.RetryInterval = 5
				}
				logger.LoggerMgw.Debugf("Time Duration for retrying: %v", conf.ControlPlane.EventHub.RetryInterval*time.Second)
				time.Sleep(conf.ControlPlane.EventHub.RetryInterval * time.Second)
				logger.LoggerMgw.Infof("Retrying to fetch API data from control plane.")
				synchronizer.FetchAPIs(&d.APIID, &d.GatewayLabel, c)
			}(data)
		}
	}
}
