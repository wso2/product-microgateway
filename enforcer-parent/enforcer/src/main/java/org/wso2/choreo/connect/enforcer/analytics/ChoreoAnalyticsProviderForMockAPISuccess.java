/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.envoyproxy.envoy.data.accesslog.v3.AccessLogCommon;
import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Error;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Latencies;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;

import java.util.concurrent.TimeUnit;

/**
 * Analytics Data Provider for MockAPIs Successful requests.
 * <p>
 * Note: MockAPI call failures is no different compared to regular api call failures. Hence, they are also handled
 * via the {@code ChoreoFaultAnalyticsProvider}.
 */
public class ChoreoAnalyticsProviderForMockAPISuccess extends ChoreoAnalyticsProvider {
    private static final Logger logger = LogManager.getLogger(ChoreoAnalyticsProviderForMockAPISuccess.class);

    public ChoreoAnalyticsProviderForMockAPISuccess(HTTPAccessLogEntry logEntry) {
        super(logEntry);
    }

    @Override
    public EventCategory getEventCategory() {
        return EventCategory.SUCCESS;
    }

    @Override
    public FaultCategory getFaultType() {
        // Unreachable condition as this provider is only handling successful mock api events.
        return null;
    }

    @Override
    public Latencies getLatencies() {
        // This method is only invoked for success requests. Hence all these properties will be available.
        // The cors requests responded from the CORS filter are already filtered at this point.
        AccessLogCommon properties = logEntry.getCommonProperties();
        Latencies latencies = new Latencies();

        long downstreamResponseSendTimeFromStart =
                TimeUnit.SECONDS.toMillis(properties.getTimeToLastDownstreamTxByte().getSeconds()) +
                        TimeUnit.NANOSECONDS.toMillis(properties.getTimeToLastDownstreamTxByte().getNanos());
        // Mock APIs does not have a backend. Hence, backend latency remains 0.
        latencies.setResponseLatency(downstreamResponseSendTimeFromStart);
        latencies.setBackendLatency(0);
        latencies.setRequestMediationLatency(downstreamResponseSendTimeFromStart);
        latencies.setResponseMediationLatency(0);
        return latencies;

    }

    @Override
    public Error getError(FaultCategory faultCategory) {
        // Since this provider only handles success requests, getError method does not get called.
        return null;
    }
}
