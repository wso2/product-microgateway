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

package enforcercallbacks

import (
	"context"

	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	"github.com/wso2/adapter/internal/discovery/protocol/resource/v3"
	xds "github.com/wso2/adapter/internal/discovery/xds"
	logger "github.com/wso2/adapter/loggers"
)

// Callbacks is used to debug the xds server related communication.
type Callbacks struct {
}

// Report logs the fetches and requests.
func (cb *Callbacks) Report() {}

// OnStreamOpen prints debug logs
func (cb *Callbacks) OnStreamOpen(_ context.Context, id int64, typ string) error {
	logger.LoggerEnforcerXdsCallbacks.Debugf("stream %d open for %s\n", id, typ)
	return nil
}

// OnStreamClosed prints debug logs
func (cb *Callbacks) OnStreamClosed(id int64) {
	logger.LoggerEnforcerXdsCallbacks.Debugf("stream %d closed\n", id)
}

// OnStreamRequest prints debug logs
func (cb *Callbacks) OnStreamRequest(id int64, request *discovery.DiscoveryRequest) error {
	logger.LoggerEnforcerXdsCallbacks.Debugf("stream request on stream id: %d, from node: %s, version: %s, for type: %s",
		id, request.GetNode(), request.GetVersionInfo(), request.GetTypeUrl())
	requestEventChannel := xds.GetRequestEventChannel()
	if resource.APIType == request.GetTypeUrl() {
		requestEvent := xds.NewRequestEvent()
		if request.ErrorDetail != nil {
			logger.LoggerEnforcerXdsCallbacks.Errorf("stream request on stream id: %d Error: %s", id, request.ErrorDetail.Message)
			requestEvent.IsError = true
		}
		requestEvent.Node = request.GetNode().GetId()
		requestEvent.Version = request.VersionInfo
		requestEventChannel <- requestEvent
	}
	return nil
}

// OnStreamResponse prints debug logs
func (cb *Callbacks) OnStreamResponse(id int64, request *discovery.DiscoveryRequest, response *discovery.DiscoveryResponse) {
	logger.LoggerEnforcerXdsCallbacks.Debugf("stream request on stream id: %d node: %s for type: %s version: %s",
		id, request.GetNode(), request.GetTypeUrl(), response.GetVersionInfo())
}

// OnFetchRequest prints debug logs
func (cb *Callbacks) OnFetchRequest(_ context.Context, req *discovery.DiscoveryRequest) error {
	logger.LoggerEnforcerXdsCallbacks.Debugf("fetch request from node: %s, version: %s, for type: %s", req.Node.Id, req.VersionInfo, req.TypeUrl)
	return nil
}

// OnFetchResponse prints debug logs
func (cb *Callbacks) OnFetchResponse(req *discovery.DiscoveryRequest, res *discovery.DiscoveryResponse) {
	logger.LoggerEnforcerXdsCallbacks.Debugf("fetch response to node: %s, version: %s, for type: %s", req.Node.Id, req.VersionInfo, res.TypeUrl)
}
