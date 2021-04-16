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

package xds

import (
	"context"

	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	logger "github.com/wso2/adapter/loggers"
)

// Callbacks is used to debug the xds server related communication.
type Callbacks struct {
}

// Report logs the fetches and requests.
func (cb *Callbacks) Report() {}

// OnStreamOpen prints debug logs
func (cb *Callbacks) OnStreamOpen(_ context.Context, id int64, typ string) error {
	logger.LoggerXdsCallbacks.Debugf("stream %d open for %s\n", id, typ)
	return nil
}

// OnStreamClosed prints debug logs
func (cb *Callbacks) OnStreamClosed(id int64) {
	logger.LoggerXdsCallbacks.Debugf("stream %d closed\n", id)
}

// OnStreamRequest prints debug logs
func (cb *Callbacks) OnStreamRequest(id int64, request *discovery.DiscoveryRequest) error {
	logger.LoggerXdsCallbacks.Debugf("stream request on stream id: %d Request: %v", id, request)
	requestEventChannel := GetRequestEventChannel()
	if "type.googleapis.com/wso2.discovery.api.Api" == request.GetTypeUrl() {
		logger.LoggerXdsCallbacks.Debugf("stream request on stream id: %d Request: %v", id, request)
		requestEvent := NewRequestEvent()
		if request.ErrorDetail != nil {
			requestEvent.IsError = true
		}
		requestEvent.Node = request.GetNode().GetId()
		requestEvent.Version = request.VersionInfo
		requestEvent.IsResponse = false
		requestEventChannel <- requestEvent
	}
	return nil
}

// OnStreamResponse prints debug logs
func (cb *Callbacks) OnStreamResponse(id int64, request *discovery.DiscoveryRequest, response *discovery.DiscoveryResponse) {
	logger.LoggerXdsCallbacks.Debugf("stream request on stream id: %d Response: %v", id, response)
}

// OnFetchRequest prints debug logs
func (cb *Callbacks) OnFetchRequest(_ context.Context, req *discovery.DiscoveryRequest) error {
	logger.LoggerXdsCallbacks.Debugf("fetch request : %v", req)
	return nil
}

// OnFetchResponse prints debug logs
func (cb *Callbacks) OnFetchResponse(req *discovery.DiscoveryRequest, res *discovery.DiscoveryResponse) {
	logger.LoggerXdsCallbacks.Debugf("fetch response : %v", res)
}
