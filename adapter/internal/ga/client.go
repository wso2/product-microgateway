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
	"io"
	"time"

	core "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	ga_model "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/ga"
	stub "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/service/ga"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"

	"google.golang.org/genproto/googleapis/rpc/status"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials"
	grpcStatus "google.golang.org/grpc/status"
)

var (
	// apiRevision Map keeps apiUUID -> revisionUUID. This is used only for the communication between global adapter and adapter
	// The purpose here is to identify if the certain API's revision is already added to the XDS cache.
	apiRevisionMap map[string]string
	// Last Acknowledged Response from the global adapter
	lastAckedResponse *discovery.DiscoveryResponse
	// initialAPIEventArray is the array where the api events
	initialAPIEventArray []*APIEvent
	// isFirstResponse to keep track of the first discovery response received.
	isFirstResponse bool
	// Last Received Response from the global adapter
	// Last Recieved Response is always is equal to the lastAckedResponse according to current implementation as there is no
	// validation performed on successfully recieved response.
	lastReceivedResponse *discovery.DiscoveryResponse
	// XDS stream for streaming APIs from Global Adapter
	xdsStream stub.ApiGADiscoveryService_StreamGAApisClient
	// GAAPIChannel stores the API Events composed from XDS states
	GAAPIChannel chan APIEvent
)

const (
	// The type url for requesting API Entries from global adapter.
	apiTypeURL string = "type.googleapis.com/wso2.discovery.ga.Api"
)

// APIEvent represents the event corresponding to a single API Deploy or Remove event
// based on XDS state changes
type APIEvent struct {
	APIUUID       string
	RevisionUUID  string
	IsDeployEvent bool
}

func init() {
	apiRevisionMap = make(map[string]string)
	lastAckedResponse = &discovery.DiscoveryResponse{}
	GAAPIChannel = make(chan APIEvent, 10)
	isFirstResponse = true
}

func initConnection(xdsURL string) error {
	config, _ := config.ReadConfigs()
	certPool := tlsutils.GetTrustedCertPool(config.Adapter.Truststore.Location)
	tlsOption := grpc.WithTransportCredentials(credentials.NewClientTLSFromCert(certPool, config.GlobalAdapter.HostName))
	// TODO: (VirajSalaka) Bring in connection level configurations
	conn, err := grpc.Dial(xdsURL, tlsOption, grpc.WithBlock())
	if err != nil {
		// TODO: (VirajSalaka) retries
		logger.LoggerGA.Error("Error while connecting to the Global Adapter.", err)
		return err
	}

	client := stub.NewApiGADiscoveryServiceClient(conn)
	streamContext := context.Background()
	xdsStream, err = client.StreamGAApis(streamContext)

	if err != nil {
		// TODO: (VirajSalaka) handle error.
		logger.LoggerGA.Error("Error while starting client. ", err)
		return err
	}
	logger.LoggerGA.Infof("Connection to the global adapter: %s is successful.", xdsURL)
	return nil
}

func watchAPIs() {
	for {
		discoveryResponse, err := xdsStream.Recv()
		if err == io.EOF {
			// read done.
			// TODO: (VirajSalaka) observe the behavior when grpc connection terminates
			logger.LoggerGA.Error("EOF is received from the global adapter.")
			return
		}
		if err != nil {
			logger.LoggerGA.Error("Failed to receive the discovery response ", err)
			errStatus, _ := grpcStatus.FromError(err)
			// TODO: (VirajSalaka) implement retries.
			if errStatus.Code() == codes.Unavailable {
				logger.LoggerGA.Fatal("Connection stopped. ")
			}
			nack(err.Error())
		} else {
			lastReceivedResponse = discoveryResponse
			logger.LoggerGA.Debugf("Discovery response is received : %s", discoveryResponse.VersionInfo)
			addAPIToChannel(discoveryResponse)
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
		Node:          getAdapterNode(),
		VersionInfo:   lastAckedResponse.VersionInfo,
		TypeUrl:       apiTypeURL,
		ResponseNonce: lastReceivedResponse.Nonce,
		ErrorDetail: &status.Status{
			Message: errorMessage,
		},
	}
	xdsStream.Send(discoveryRequest)
}

func getAdapterNode() *core.Node {
	config, _ := config.ReadConfigs()
	return &core.Node{
		Id: config.GlobalAdapter.LocalLabel,
	}
}

// InitGAClient initializes the connection to the global adapter.
func InitGAClient(xdsURL string) {
	logger.LoggerGA.Info("Starting the XDS Client connection to Global Adapter.")
	err := initConnection(xdsURL)
	if err == nil {
		go watchAPIs()
		discoveryRequest := &discovery.DiscoveryRequest{
			Node:        getAdapterNode(),
			VersionInfo: "",
			TypeUrl:     apiTypeURL,
		}
		xdsStream.Send(discoveryRequest)
		consumeAPIChannel()
	}
	select {}
}

func addAPIToChannel(resp *discovery.DiscoveryResponse) {
	// To keep track of the APIs needs to be deleted.

	removedAPIMap := make(map[string]string)
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

		currentRevision, apiFound := apiRevisionMap[api.ApiUUID]
		if apiFound {
			delete(removedAPIMap, api.ApiUUID)
			if currentRevision == api.RevisionUUID {
				continue
			}
		}
		event := APIEvent{
			APIUUID:       api.ApiUUID,
			RevisionUUID:  api.RevisionUUID,
			IsDeployEvent: true,
		}

		// If it is the first response, the GA would not send it via the channel. Rather
		// it appends to an array and let the apis_fetcher collect those data.
		if isFirstResponse {
			startupAPIEventArray = append(startupAPIEventArray, &event)
		} else {
			GAAPIChannel <- event
		}
		apiRevisionMap[api.ApiUUID] = api.RevisionUUID
		logger.LoggerGA.Infof("API Deploy event is added to the channel. %s : %s", api.ApiUUID, api.RevisionUUID)
	}

	// If it is the first response, it does not contain any remove events.
	if isFirstResponse {
		initialAPIEventArray = startupAPIEventArray
		isFirstResponse = false
		return
	}

	for apiEntry := range removedAPIMap {
		event := APIEvent{
			APIUUID:       apiEntry,
			IsDeployEvent: false,
		}
		GAAPIChannel <- event
		delete(apiRevisionMap, apiEntry)
		logger.LoggerGA.Infof("API Undeploy event is added to the channel. : %s", apiEntry)
	}
}

// TODO: (VirajSalaka) Remove this method once the channel consume logic is implemented.
func consumeAPIChannel() {
	for event := range GAAPIChannel {
		logger.LoggerGA.Infof("Event : %v", event)
	}
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
