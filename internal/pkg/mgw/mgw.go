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
package mgw

import (
	"context"
	"flag"
	"fmt"
	v2 "github.com/envoyproxy/go-control-plane/envoy/api/v2"
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	"github.com/fsnotify/fsnotify"
	//myals "github.com/wso2/micro-gw/internal/pkg/logging"
	apiserver "github.com/wso2/micro-gw/internal/pkg/api"
	mgwconfig "github.com/wso2/micro-gw/internal/pkg/confTypes"
	//"google.golang.org/appengine/log"
	"net"
	"os"
	"os/signal"
	"sync/atomic"

	cachev2 "github.com/envoyproxy/go-control-plane/pkg/cache/v2"
	xds "github.com/envoyproxy/go-control-plane/pkg/server/v2"
	oasParser "github.com/wso2/micro-gw/internal/pkg/oasparser"

	"google.golang.org/grpc"

	//accesslog "github.com/envoyproxy/go-control-plane/envoy/service/accesslog/v2"
	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v2"
)

var (
	debug       bool
	onlyLogging bool

	localhost = "0.0.0.0"

	port        uint
	gatewayPort uint
	alsPort     uint

	mode string

	version int32

	cache cachev2.SnapshotCache

)

const (
	XdsCluster = "xds_cluster"
	Ads        = "ads"
	Xds        = "xds"
	Rest       = "rest"
)

func init() {
	flag.BoolVar(&debug, "debug", true, "Use debug logging")
	flag.BoolVar(&onlyLogging, "onlyLogging", false, "Only demo AccessLogging Service")
	flag.UintVar(&port, "port", 18000, "Management server port")
	flag.UintVar(&gatewayPort, "gateway", 18001, "Management server port for HTTP gateway")
	flag.UintVar(&alsPort, "als", 18090, "Accesslog server port")
	flag.StringVar(&mode, "ads", Ads, "Management server type (ads, xds, rest)")
}

// Hasher returns node ID as an ID
type Hasher struct {
}

// ID function
func (h Hasher) ID(node *core.Node) string {
	if node == nil {
		return "unknown"
	}
	return node.Id
}


const grpcMaxConcurrentStreams = 1000000

// RunManagementServer starts an xDS server at the given port.
func RunManagementServer(ctx context.Context, server xds.Server, port uint) {
	var grpcOptions []grpc.ServerOption
	grpcOptions = append(grpcOptions, grpc.MaxConcurrentStreams(grpcMaxConcurrentStreams))
	grpcServer := grpc.NewServer()

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		Logger.Fatal("failed to listen: ", err)
	}

	// register services
	discovery.RegisterAggregatedDiscoveryServiceServer(grpcServer, server)
	v2.RegisterEndpointDiscoveryServiceServer(grpcServer, server)
	v2.RegisterClusterDiscoveryServiceServer(grpcServer, server)
	v2.RegisterRouteDiscoveryServiceServer(grpcServer, server)
	v2.RegisterListenerDiscoveryServiceServer(grpcServer, server)

	Logger.Info("port: ",port, " management server listening")
	//log.Fatalf("", Serve(lis))
	//go func() {
	go func() {
		if err = grpcServer.Serve(lis); err != nil {
			Logger.Error(err)
		}
	}()
	//<-ctx.Done()
	//grpcServer.GracefulStop()
	//}()

}

func updateEnvoy(location string) {
	var nodeId string
	if len(cache.GetStatusKeys()) > 0 {
		nodeId = cache.GetStatusKeys()[0]
	}

	listeners, clusters, routes, endpoints := oasParser.GetProductionSources(location)

	atomic.AddInt32(&version, 1)
	Logger.Infof(">>>>>>>>>>>>>>>>>>> creating snapshot Version " + fmt.Sprint(version))
	snap := cachev2.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil)
	snap.Consistent()

	err := cache.SetSnapshot(nodeId, snap)
	if err != nil {
		Logger.Error(err)
	}
}

// Run the management grpc server.
func Run(conf *mgwconfig.Config) {
	sig := make(chan os.Signal)
	signal.Notify(sig, os.Interrupt)
	watcher, _ := fsnotify.NewWatcher()
	err := watcher.Add(conf.Apis.Location)

	if err != nil {
		Logger.Panic("Error reading the api definitions.", err)
	}

	flag.Parse()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	Logger.Info("Starting control plane")

	cache = cachev2.NewSnapshotCache(mode != Ads, Hasher{}, nil)

	srv := xds.NewServer(ctx, cache, nil)

	//als := &myals.AccessLogService{}
	//go RunAccessLogServer(ctx, als, alsPort)

	// start the xDS server
	RunManagementServer(ctx, srv, port)
	go apiserver.Start(conf)

	updateEnvoy(conf.Apis.Location)
OUTER:
	for {
		select {
		case c := <-watcher.Events:
			switch c.Op.String() {
			case "WRITE":
				Logger.Info("Loading updated swagger definition...")
				updateEnvoy(conf.Apis.Location)
			}
		case s := <-sig:
			switch s {
			case os.Interrupt:
				Logger.Info("Shutting down...")
				break OUTER
			}
		}
	}
	Logger.Info("Bye!")
}
