/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package ga

import (
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"os"
	"strings"
	"time"

	core "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	"github.com/golang/protobuf/ptypes"
	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	ga_model "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/ga"
	stub "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/ga"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"

	"google.golang.org/genproto/googleapis/rpc/status"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/keepalive"
	grpcStatus "google.golang.org/grpc/status"
)

var (
	// apiRevisionMap keeps apiUUID -> revisionUUID. This is used only for the communication between global adapter and adapter
	// The purpose here is to identify if the certain API's revision is already added to the XDS cache.
	apiRevisionMap map[string]*ga_model.Api
	// apiEnvRevisionMap keeps apiUUID -> deployedEnv -> revisionUUID mapping. This is used only for the communication
	// between global adapter and adapter.
	// The purpose here is to identify if the certain API's, certain environment's deployed revision is already added
	// to the XDS cache, when sotw response received from GA.
	apiEnvRevisionMap = make(map[string]map[string]*ga_model.Api)
	// Last Acknowledged Response from the global adapter
	lastAckedResponse *discovery.DiscoveryResponse
	// initialAPIEventArray is the array where the api events
	initialAPIEventArray []*APIEvent
	// isFirstResponse to keep track of the first discovery response received.
	isFirstResponse bool
	// gaAPIChannelStart is used to block the GAAPIChannel consuming until the startup is completed.
	gaAPIChannelStart chan bool
	// Last Received Response from the global adapter
	// Last Recieved Response is always is equal to the lastAckedResponse according to current implementation as there is no
	// validation performed on successfully recieved response.
	lastReceivedResponse *discovery.DiscoveryResponse
	// XDS stream for streaming APIs from Global Adapter
	xdsStream stub.ApiGADiscoveryService_StreamGAApisClient
	// If a connection error occurs, true event would be returned
	connectionFaultChannel chan bool
	// GAAPIChannel stores the API Events composed from XDS states
	GAAPIChannel chan APIEvent
)

const (
	// The type url for requesting API Entries from global adapter.
	apiTypeURL                   string = "type.googleapis.com/wso2.discovery.ga.Api"
	grpcGAServerKeepaliveEnabled string = "GRPC_GA_KEEPALIVE_ENABLED"
)

// APIEvent represents the event corresponding to a single API Deploy or Remove event
// based on XDS state changes
type APIEvent struct {
	APIUUID          string
	RevisionUUID     string
	IsDeployEvent    bool
	OrganizationUUID string
	DeployedEnv      string
}

func init() {
	apiRevisionMap = make(map[string]*ga_model.Api)
	lastAckedResponse = &discovery.DiscoveryResponse{}
	connectionFaultChannel = make(chan bool)
	GAAPIChannel = make(chan APIEvent, 10)
	gaAPIChannelStart = make(chan bool)
	isFirstResponse = true
}

func initConnection() (*grpc.ClientConn, error) {
	// TODO: (VirajSalaka) Bring in connection level configurations
	conn, err := getGRPCConnection()
	if err != nil {
		logger.LoggerGA.Error("Error while connecting to the Global Adapter.", err)
		return nil, err
	}

	client := stub.NewApiGADiscoveryServiceClient(conn)
	streamContext := context.Background()
	xdsStream, err = client.StreamGAApis(streamContext)

	if err != nil {
		// TODO: (VirajSalaka) handle error.
		logger.LoggerGA.Error("Error while starting client. ", err)
		return nil, err
	}
	logger.LoggerGA.Info("Connection to the global adapter is successful.")
	return conn, nil
}

func generateTLSCredentialsForXdsClient() credentials.TransportCredentials {
	conf, _ := config.ReadConfigs()
	certPool := tlsutils.GetTrustedCertPool(conf.Adapter.Truststore.Location)
	// There is a single private-public key pair for XDS server initialization, as well as for XDS client authentication
	certificate, err := tlsutils.GetServerCertificate(conf.Adapter.Keystore.CertPath,
		conf.Adapter.Keystore.KeyPath)
	if err != nil {
		logger.LoggerGA.Fatal("Error while processing the private-public key pair", err)
	}
	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{certificate},
		RootCAs:      certPool,
	}
	// This option is used if the calling IP and SAN of the certificate is different.
	if conf.GlobalAdapter.OverrideHostName != "" {
		logger.LoggerGA.Infof("ServerName used for Global Adapter Hostname verification is %s",
			conf.GlobalAdapter.OverrideHostName)
		tlsConfig.ServerName = conf.GlobalAdapter.OverrideHostName
	}
	return credentials.NewTLS(tlsConfig)
}

func watchAPIs() {
	conf, _ := config.ReadConfigs()
	for {
		discoveryResponse, err := xdsStream.Recv()
		if err == io.EOF {
			// read done.
			// TODO: (VirajSalaka) observe the behavior when grpc connection terminates
			logger.LoggerGA.Error("EOF is received from the global adapter.")
			connectionFaultChannel <- true
			return
		}
		if err != nil {
			logger.LoggerGA.Error("Failed to receive the discovery response ", err)
			errStatus, _ := grpcStatus.FromError(err)
			if errStatus.Code() == codes.Unavailable {
				logger.LoggerGA.Errorf("Connection unavailable. errorCode: %s errorMessage: %s",
					errStatus.Code().String(), errStatus.Message())
				connectionFaultChannel <- true
				return
			}
			logger.LoggerGA.Errorf("Error while XDS communication; errorCode: %s errorMessage: %s",
				errStatus.Code().String(), errStatus.Message())
			nack(errStatus.Message())
		} else {
			lastReceivedResponse = discoveryResponse
			logger.LoggerGA.Debugf("Discovery response is received : %s, size: %d", discoveryResponse.VersionInfo,
				len(discoveryResponse.Resources))
			// ToDO: (VajiraPrabuddhaka) remove this check once the dynamic environment changes are fully rolled out
			if conf.ControlPlane.DynamicEnvironments.Enabled {
				addAPIWithEnvToChannel(discoveryResponse)
			} else {
				addAPIToChannel(discoveryResponse)
			}
			ack()
		}
	}
}

func ack() {
	lastAckedResponse = lastReceivedResponse
	discoveryRequest := &discovery.DiscoveryRequest{
		Node:          getAdapterNode(),
		VersionInfo:   lastAckedResponse.VersionInfo,
		TypeUrl:       apiTypeURL,
		ResponseNonce: lastReceivedResponse.Nonce,
	}
	xdsStream.Send(discoveryRequest)
}

func nack(errorMessage string) {
	if lastAckedResponse == nil {
		return
	}
	discoveryRequest := &discovery.DiscoveryRequest{
		Node:        getAdapterNode(),
		VersionInfo: lastAckedResponse.VersionInfo,
		TypeUrl:     apiTypeURL,
		ErrorDetail: &status.Status{
			Message: errorMessage,
		},
	}
	if lastReceivedResponse != nil {
		discoveryRequest.ResponseNonce = lastReceivedResponse.Nonce
	}
	xdsStream.Send(discoveryRequest)
}

func getAdapterNode() *core.Node {
	config, _ := config.ReadConfigs()
	if config.ControlPlane.DynamicEnvironments.Enabled {
		return &core.Node{
			Id: fmt.Sprintf("%s-%s-%s",
				config.ControlPlane.DynamicEnvironments.DataPlaneID,
				config.ControlPlane.DynamicEnvironments.GatewayAccessibilityType,
				config.GlobalAdapter.LocalLabel),
		}
	}
	return &core.Node{
		Id: config.GlobalAdapter.LocalLabel,
	}
}

// InitGAClient initializes the connection to the global adapter.
func InitGAClient() {
	config, _ := config.ReadConfigs()
	if config.ControlPlane.DynamicEnvironments.Enabled {
		logger.LoggerGA.Infof("Starting the XDS Client connection for %s-%s-%s to Global Adapter.",
			config.ControlPlane.DynamicEnvironments.DataPlaneID,
			config.ControlPlane.DynamicEnvironments.GatewayAccessibilityType,
			config.GlobalAdapter.LocalLabel)
	} else {
		logger.LoggerGA.Infof("Starting the XDS Client connection to Global Adapter.")
	}
	go handleAPIEventsFromGA(GAAPIChannel)
	conn := initializeAndWatch()
	for retryTrueReceived := range connectionFaultChannel {
		// event is always true
		if !retryTrueReceived {
			continue
		}
		if conn != nil {
			conn.Close()
		}
		conn = initializeAndWatch()
	}
}

// StartConsumeGAAPIChannel starts consuming API events from GAAPIChannel, once the conditions are met
func StartConsumeGAAPIChannel() {
	gaAPIChannelStart <- true
}

func addAPIToChannel(resp *discovery.DiscoveryResponse) {
	// To keep track of the APIs needs to be deleted.

	removedAPIMap := make(map[string]*ga_model.Api)
	if !isFirstResponse {
		for k, v := range apiRevisionMap {
			removedAPIMap[k] = v
		}
	}

	var startupAPIEventArray []*APIEvent
	// Even if there are no resources available within ga, an empty but non-nil array would be set
	startupAPIEventArray = make([]*APIEvent, 0)
	for _, res := range resp.Resources {
		api := &ga_model.Api{}
		err := ptypes.UnmarshalAny(res, api)

		if err != nil {
			logger.LoggerGA.Errorf("Error while unmarshalling: %s\n", err.Error())
			continue
		}

		currentGAAPI, apiFound := apiRevisionMap[api.ApiUUID]
		if apiFound {
			if currentGAAPI.RevisionUUID == api.RevisionUUID {
				logger.LoggerGA.Debugf("Current GA API revision ID and API event revision ID is equal: %v\n",
					currentGAAPI.RevisionUUID)
				delete(removedAPIMap, api.ApiUUID)
				continue
			}
		}
		event := APIEvent{
			APIUUID:          api.ApiUUID,
			RevisionUUID:     api.RevisionUUID,
			IsDeployEvent:    true,
			OrganizationUUID: api.OrganizationUUID,
			DeployedEnv:      api.DeployedEnv,
		}

		// If it is the first response, the GA would not send it via the channel. Rather
		// it appends to an array and let the apis_fetcher collect those data.
		if isFirstResponse {
			startupAPIEventArray = append(startupAPIEventArray, &event)
		} else {
			GAAPIChannel <- event
		}
		apiRevisionMap[api.ApiUUID] = api
		logger.LoggerGA.Infof("API Deploy event is added to the channel. %s : %s", api.ApiUUID, api.RevisionUUID)
	}

	// If it is the first response, it does not contain any remove events.
	if isFirstResponse {
		initialAPIEventArray = startupAPIEventArray
		isFirstResponse = false
		return
	}

	for apiEntry, gaAPI := range removedAPIMap {
		event := APIEvent{
			APIUUID:          apiEntry,
			IsDeployEvent:    false,
			OrganizationUUID: gaAPI.OrganizationUUID,
			RevisionUUID:     gaAPI.RevisionUUID,
			DeployedEnv:      gaAPI.DeployedEnv,
		}
		GAAPIChannel <- event
		delete(apiRevisionMap, apiEntry)
		logger.LoggerGA.Infof("API Undeploy event is added to the channel. : %s", apiEntry)
	}
}

func addAPIWithEnvToChannel(resp *discovery.DiscoveryResponse) {

	// removedAPIEnvMap is used To keep track of the APIs needs to be deleted.
	var removedAPIEnvMap = make(map[string]map[string]*ga_model.Api)

	if !isFirstResponse {
		for apiUUID, envToRevisionMap := range apiEnvRevisionMap {
			cp := make(map[string]*ga_model.Api)
			for l, w := range envToRevisionMap {
				cp[l] = w
			}
			removedAPIEnvMap[apiUUID] = cp
		}
	}

	var startupAPIEventArray []*APIEvent
	// Even if there are no resources available within ga, an empty but non-nil array would be set
	startupAPIEventArray = make([]*APIEvent, 0)
	for _, res := range resp.Resources {
		api := &ga_model.Api{}
		err := ptypes.UnmarshalAny(res, api)

		if err != nil {
			logger.LoggerGA.Errorf("Error while unmarshalling: %s\n", err.Error())
			continue
		}

		currentGAAPI, apiFound := apiEnvRevisionMap[api.ApiUUID][api.DeployedEnv]
		if apiFound {
			if currentGAAPI.RevisionUUID == api.RevisionUUID {
				logger.LoggerGA.Debugf("Current GA API revision ID and API event revision ID: %s in "+
					"environment: %s are equal", currentGAAPI.RevisionUUID, currentGAAPI.DeployedEnv)
				if _, found := removedAPIEnvMap[api.ApiUUID]; found {
					delete(removedAPIEnvMap[api.ApiUUID], api.DeployedEnv)
				}
				continue
			}
		}
		event := APIEvent{
			APIUUID:          api.ApiUUID,
			RevisionUUID:     api.RevisionUUID,
			IsDeployEvent:    true,
			OrganizationUUID: api.OrganizationUUID,
			DeployedEnv:      api.DeployedEnv,
		}

		// If it is the first response, the GA would not send it via the channel. Rather
		// it appends to an array and let the apis_fetcher collect those data.
		if isFirstResponse {
			startupAPIEventArray = append(startupAPIEventArray, &event)
		} else {
			GAAPIChannel <- event
		}
		if _, found := apiEnvRevisionMap[api.ApiUUID]; !found {
			apiEnvRevisionMap[api.ApiUUID] = make(map[string]*ga_model.Api)
		}
		apiEnvRevisionMap[api.ApiUUID][api.DeployedEnv] = api
		logger.LoggerGA.Infof("API Deploy event is added to the channel. %s : %s : %s", api.ApiUUID,
			api.RevisionUUID, api.DeployedEnv)
	}

	// If it is the first response, it does not contain any remove events.
	if isFirstResponse {
		initialAPIEventArray = startupAPIEventArray
		isFirstResponse = false
		return
	}

	for apiID, envAPI := range removedAPIEnvMap {
		for envName, gaAPI := range envAPI {
			event := APIEvent{
				APIUUID:          apiID,
				IsDeployEvent:    false,
				OrganizationUUID: gaAPI.OrganizationUUID,
				RevisionUUID:     gaAPI.RevisionUUID,
				DeployedEnv:      envName,
			}
			GAAPIChannel <- event
			if _, found := apiEnvRevisionMap[apiID]; found {
				delete(apiEnvRevisionMap[apiID], envName)
			}
			logger.LoggerGA.Infof("API Undeploy event is added to the channel. API_ID: %s, Environment: %s",
				apiID, envName)
		}

	}
}

func initializeAndWatch() *grpc.ClientConn {
	conn, err := initConnection()
	if err != nil {
		connectionFaultChannel <- true
		return conn
	}
	go watchAPIs()
	var lastAppliedVersion string
	if lastAckedResponse != nil {
		// If the connection is interrupted in the middle, we need to apply if the version remains same
		lastAppliedVersion = lastAckedResponse.VersionInfo
	} else {
		lastAppliedVersion = ""
	}
	discoveryRequest := &discovery.DiscoveryRequest{
		Node:        getAdapterNode(),
		VersionInfo: lastAppliedVersion,
		TypeUrl:     apiTypeURL,
	}
	xdsStream.Send(discoveryRequest)
	return conn
}

func getGRPCConnection() (*grpc.ClientConn, error) {
	conf, _ := config.ReadConfigs()
	retryInterval := conf.GlobalAdapter.RetryInterval
	backOff := grpc_retry.BackoffLinearWithJitter(retryInterval*time.Second, 0.5)
	logger.LoggerGA.Infof("Dialing Global Adapter GRPC Service : %s", conf.GlobalAdapter.ServiceURL)

	grpcDialOpts := []grpc.DialOption{
		grpc.WithTransportCredentials(generateTLSCredentialsForXdsClient()),
		grpc.WithBlock(),
		grpc.WithStreamInterceptor(grpc_retry.StreamClientInterceptor(grpc_retry.WithBackoff(backOff))),
	}

	if strings.TrimSpace(os.Getenv(grpcGAServerKeepaliveEnabled)) == "true" {
		grpcDialOpts = append(grpcDialOpts, grpc.WithKeepaliveParams(keepalive.ClientParameters{
			Time: time.Duration(6 * time.Minute),
		}))
	}

	return grpc.Dial(conf.GlobalAdapter.ServiceURL, grpcDialOpts...)
}

// FetchAPIsFromGA returns the initial state of GA APIs within Adapter
func FetchAPIsFromGA() []*APIEvent {
	for {
		if initialAPIEventArray != nil {
			return initialAPIEventArray
		}
		time.Sleep(1 * time.Second)
	}
}
