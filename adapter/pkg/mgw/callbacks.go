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

package mgw

import (
	"context"

	discovery "github.com/envoyproxy/go-control-plane/envoy/service/discovery/v3"
	logger "github.com/wso2/micro-gw/loggers"
)

// callbacks is used to debug the xds server related communication.
type callbacks struct {
	Fetches  int
	Requests int
}

func (cb *callbacks) Report() {
	logger.LoggerMgw.Debugf("server callbacks fetches=%d requests=%d\n", cb.Fetches, cb.Requests)
}

func (cb *callbacks) OnStreamOpen(_ context.Context, id int64, typ string) error {
	logger.LoggerMgw.Debugf("stream %d open for %s\n", id, typ)
	return nil
}

func (cb *callbacks) OnStreamClosed(id int64) {
	logger.LoggerMgw.Debugf("stream %d closed\n", id)
}

func (cb *callbacks) OnStreamRequest(id int64, request *discovery.DiscoveryRequest) error {
	logger.LoggerMgw.Debugf("stream request on stream id: %d Request: %v", id, request)
	return nil
}

func (cb *callbacks) OnStreamResponse(id int64, request *discovery.DiscoveryRequest, response *discovery.DiscoveryResponse) {
	logger.LoggerMgw.Debugf("stream request on stream id: %d Response: %v", id, response)
}

func (cb *callbacks) OnFetchRequest(_ context.Context, req *discovery.DiscoveryRequest) error {
	logger.LoggerMgw.Debugf("fetch request : %v", req)
	return nil
}
func (cb *callbacks) OnFetchResponse(req *discovery.DiscoveryRequest, res *discovery.DiscoveryResponse) {
	logger.LoggerMgw.Debugf("fetch response : %v", res)
}
