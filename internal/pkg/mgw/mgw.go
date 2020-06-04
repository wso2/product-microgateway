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
	myals "github.com/wso2/micro-gw/internal/pkg/logging"
	apiserver "github.com/wso2/micro-gw/internal/pkg/api"
	mgwconfig "github.com/wso2/micro-gw/internal/pkg/confTypes"
	"net"
	"os"
	"os/signal"
	"sync/atomic"

	cachev2 "github.com/envoyproxy/go-control-plane/pkg/cache/v2"
	xds "github.com/envoyproxy/go-control-plane/pkg/server/v2"
	oasParser "github.com/wso2/micro-gw/internal/pkg/oasparser"

	logrus "github.com/sirupsen/logrus"
	"google.golang.org/grpc"

	accesslog "github.com/envoyproxy/go-control-plane/envoy/service/accesslog/v2"
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

type logger struct {
	*logrus.Logger
}

var log = &logger{
	Logger: logrus.StandardLogger(),
}

// OnStreamOpen is called once an xDS stream is open with a stream ID and the type URL (or "" for ADS).
func (l logger) OnStreamOpen(_ context.Context, sid int64, stype string) error {
	l.Infof("Stream open[%v]: %v", sid, stype)
	return nil
}

// OnStreamClosed is called immediately prior to closing an xDS stream with a stream ID.
func (l logger) OnStreamClosed(sid int64) {
	l.Infof("Stream closed[%v]", sid)
}

// OnStreamRequest is called once a request is received on a stream.
func (l logger) OnStreamRequest(sid int64, req *v2.DiscoveryRequest) error {
	l.Infof("Stream request[%v]: %v", sid, req)
	return nil
}

// OnStreamResponse is called immediately prior to sending a response on a stream.
func (l logger) OnStreamResponse(sid int64, req *v2.DiscoveryRequest, res *v2.DiscoveryResponse) {
	l.Infof("Stream response[%v]: %v -> %v", sid, req, res)
}

// OnFetchRequest is called for each Fetch request
func (l logger) OnFetchRequest(_ context.Context, r *v2.DiscoveryRequest) error {
	l.Infof("Fetch request: %v", r)
	return nil
}

// OnFetchResponse is called immediately prior to sending a response.
func (l logger) OnFetchResponse(req *v2.DiscoveryRequest, res *v2.DiscoveryResponse) {
	l.Infof("Fetch response: %v -> %v", req, res)
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

//RunAccessLogServer starts an accesslog service. TODO: Remove
func RunAccessLogServer(ctx context.Context, als *myals.AccessLogService, port uint) {
	grpcServer := grpc.NewServer()
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logrus.WithError(err).Fatal("failed to listen")
	}

	accesslog.RegisterAccessLogServiceServer(grpcServer, als)
	logrus.WithFields(logrus.Fields{"port": port}).Info("access log server listening")
	//log.Fatalf("", Serve(lis))
	go func() {
		if err = grpcServer.Serve(lis); err != nil {
			logrus.Error(err)
		}
	}()
	<-ctx.Done()

	grpcServer.GracefulStop()
}

const grpcMaxConcurrentStreams = 1000000

// RunManagementServer starts an xDS server at the given port.
func RunManagementServer(ctx context.Context, server xds.Server, port uint) {
	var grpcOptions []grpc.ServerOption
	grpcOptions = append(grpcOptions, grpc.MaxConcurrentStreams(grpcMaxConcurrentStreams))
	grpcServer := grpc.NewServer()

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		logrus.WithError(err).Fatal("failed to listen")
	}

	// register services
	discovery.RegisterAggregatedDiscoveryServiceServer(grpcServer, server)
	v2.RegisterEndpointDiscoveryServiceServer(grpcServer, server)
	v2.RegisterClusterDiscoveryServiceServer(grpcServer, server)
	v2.RegisterRouteDiscoveryServiceServer(grpcServer, server)
	v2.RegisterListenerDiscoveryServiceServer(grpcServer, server)

	logrus.WithFields(logrus.Fields{"port": port}).Info("management server listening")
	//log.Fatalf("", Serve(lis))
	//go func() {
	go func() {
		if err = grpcServer.Serve(lis); err != nil {
			logrus.Error(err)
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
	log.Infof(">>>>>>>>>>>>>>>>>>> creating snapshot Version " + fmt.Sprint(version))
	snap := cachev2.NewSnapshot(fmt.Sprint(version), endpoints, clusters, routes, listeners, nil)
	snap.Consistent()

	err := cache.SetSnapshot(nodeId, snap)
	if err != nil {
		logrus.Error(err)
	}
}

// Run the management grpc server.
func Run(conf *mgwconfig.Config) {
	sig := make(chan os.Signal)
	signal.Notify(sig, os.Interrupt)
	watcher, _ := fsnotify.NewWatcher()
	err := watcher.Add(conf.Apis.Location)

	if err != nil {
		logrus.Panic("Error reading the api definitions.", err)
	}

	flag.Parse()
	if debug {
		logrus.SetLevel(logrus.DebugLevel)
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	logrus.Printf("Starting control plane")

	cache = cachev2.NewSnapshotCache(mode != Ads, Hasher{}, nil)

	srv := xds.NewServer(ctx, cache, nil)

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
				logrus.Info("Loading updated swagger definition...")
				updateEnvoy(conf.Apis.Location)
			}
		case s := <-sig:
			switch s {
			case os.Interrupt:
				logrus.Info("Shutting down...")
				break OUTER
			}
		}
	}
	logrus.Info("Bye!")
}
