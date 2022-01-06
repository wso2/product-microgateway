/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.analytics;

import io.envoyproxy.envoy.service.accesslog.v3.StreamAccessLogsMessage;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;

import java.util.Map;

/**
 * AnalyticsEventPublisher interface should be used to write a custom analytics publisher.
 */
public interface AnalyticsEventPublisher {

    /**
     * The method body should include about how to handle the gRPC log message received from the router.
     *
     * In addition to the access log entry content, following information would be available within
     * dynamic metadata which is populated within router (envoy.filters.http.ext_authz).
     *
     * APIID
     * APIName
     * APIVersion
     * APICreator
     * APIType
     * APICreatorTenantDomain
     *
     * ApplicationKeyType
     * ApplicationID
     * ApplicationName
     * ApplicationOwner
     *
     * CorrelationID (fetched from requestID)
     * Region
     * APIResourceTemplate
     * Destination (The upstream endpoint of the request)
     *
     * @param message gRPC Stream access log message from router.
     */
    void handleGRPCLogMsg(StreamAccessLogsMessage message);

    /**
     * The method body should include about how to handle the websocket framereqeust received from the router's
     * wasm filter for websockets.
     *
     * Following information would be available within
     * dynamic metadata which is populated within router (envoy.filters.http.ext_authz).
     *
     * APIID
     * APIName
     * APIVersion
     * APICreator
     * APIType
     * APICreatorTenantDomain
     *
     * ApplicationKeyType
     * ApplicationID
     * ApplicationName
     * ApplicationOwner
     *
     * CorrelationID (fetched from requestID)
     * Region
     * APIResourceTemplate
     * Destination (The upstream endpoint of the request)
     *
     * @param webSocketFrameRequest
     */
    void handleWebsocketFrameRequest(WebSocketFrameRequest webSocketFrameRequest);

    /**
     * Initialize the analytics publisher with configurations map.
     *
     * @param configurationMap Analytics Configurations
     */
    void init(Map<String, String> configurationMap);
}
