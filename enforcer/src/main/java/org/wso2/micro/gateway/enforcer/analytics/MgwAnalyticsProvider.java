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

package org.wso2.micro.gateway.enforcer.analytics;

import com.google.protobuf.Value;
import io.envoyproxy.envoy.data.accesslog.v3.AccessLogCommon;
import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.API;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Application;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Error;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Latencies;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.MetaInfo;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Operation;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.Target;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.gateway.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.micro.gateway.enforcer.api.APIFactory;

import java.util.Map;

/**
 * Analytics Data Provider of Microgateway
 */
public class MgwAnalyticsProvider implements AnalyticsDataProvider {
    private static final Logger logger = LogManager.getLogger(APIFactory.class);
    private final HTTPAccessLogEntry logEntry;

    public MgwAnalyticsProvider(HTTPAccessLogEntry logEntry) {
        this.logEntry = logEntry;
    }

    @Override
    public EventCategory getEventCategory() {
        if (logEntry.getResponse().getResponseCode().getValue() == 200
                && logEntry.getResponse().getResponseCodeDetails().equals("via_upstream")) {
            return EventCategory.SUCCESS;
            // TODO: (VirajSalaka) Finalize what is a fault
        } else if (logEntry.getResponse().getResponseCode().getValue() != 200
                && !logEntry.getResponse().getResponseCodeDetails().equals("via_upstream")) {
            return EventCategory.FAULT;
        } else {
            return EventCategory.INVALID;
        }
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isAuthenticated() {
        return logEntry.getResponse().getResponseCode().getValue() != 401
                && logEntry.getResponse().getResponseCode().getValue() != 403
                && !logEntry.getResponse().getResponseCodeDetails().equals("ext_authz_denied");
    }

    @Override
    public FaultCategory getFaultType() {
        if (isAuthFaultRequest()) {
            return FaultCategory.AUTH;
        } else if (isThrottledFaultRequest()) {
            return FaultCategory.THROTTLED;
        } else if (isTargetFaultRequest()) {
            return FaultCategory.TARGET_CONNECTIVITY;
        } else {
            return FaultCategory.OTHER;
        }
    }

    private boolean isAuthFaultRequest() {
        return (logEntry.getResponse().getResponseCode().getValue() == 401
                || logEntry.getResponse().getResponseCode().getValue() == 403)
                && logEntry.getResponse().getResponseCodeDetails().equals("ext_authz_denied");
    }

    public boolean isThrottledFaultRequest() {
        return logEntry.getResponse().getResponseCode().getValue() == 429
                && logEntry.getResponse().getResponseCodeDetails().equals("ext_authz_denied");
    }

    public boolean isTargetFaultRequest() {
        // TODO: (VirajSalaka) Response flags based check
        return logEntry.getResponse().getResponseCode().getValue() != 200
                && !logEntry.getResponse().getResponseCodeDetails().equals("via_upstream");
    }

    @Override
    public API getApi() {
        // TODO: (VirajSalaka) Null check (If enforcer connection is failed)
        Map<String, Value> fieldsMap = logEntry.getCommonProperties().getMetadata()
                .getFilterMetadataMap().get("envoy.filters.http.ext_authz").getFieldsMap();
        API api = new API();
        api.setApiId(getValueAsString(fieldsMap, "ApiId"));
        api.setApiCreator(getValueAsString(fieldsMap, "ApiCreator"));
        api.setApiType(getValueAsString(fieldsMap, "ApiType"));
        api.setApiName(getValueAsString(fieldsMap, "ApiName"));
        api.setApiVersion(getValueAsString(fieldsMap, "ApiVersion"));
        api.setApiCreatorTenantDomain(getValueAsString(fieldsMap, "ApiCreatorTenantDomain"));
        return api;
    }

    @Override
    public Application getApplication() {
        // TODO: (VirajSalaka) Null check (If enforcer connection is failed)
        Map<String, Value> fieldsMap = logEntry.getCommonProperties().getMetadata()
                .getFilterMetadataMap().get("envoy.filters.http.ext_authz").getFieldsMap();
        Application application = new Application();
        application.setApplicationOwner(getValueAsString(fieldsMap, "ApplicationOwner"));
        application.setApplicationId(getValueAsString(fieldsMap, "ApplicationName"));
        application.setKeyType(getValueAsString(fieldsMap, "ApplicationKeyType"));
        application.setApplicationId(getValueAsString(fieldsMap, "ApplicationId"));
        return application;
    }

    @Override
    public Operation getOperation() {
        Map<String, Value> fieldsMap = logEntry.getCommonProperties().getMetadata()
                .getFilterMetadataMap().get("envoy.filters.http.ext_authz").getFieldsMap();
        Operation operation = new Operation();
        operation.setApiResourceTemplate(getValueAsString(fieldsMap, "ApiResourceTemplate"));
        operation.setApiMethod(logEntry.getRequest().getRequestMethod().name());
        return operation;
    }

    @Override
    public Target getTarget() {
        Target target = new Target();
        // As response caching is not configured at the moment.
        target.setResponseCacheHit(false);
        target.setTargetResponseCode(logEntry.getResponse().getResponseCode().getValue());
        // TODO: (VirajSalaka) get destination in the format of url
        // TODO: (VirajSalaka) add backend basepath
        target.setDestination(logEntry.getCommonProperties().getUpstreamRemoteAddress().getSocketAddress()
                .getAddress());
        return target;
    }

    @Override
    public Latencies getLatencies() {
        AccessLogCommon properties = logEntry.getCommonProperties();
        Latencies latencies = new Latencies();
        // TODO: (VirajSalaka) If connection error happens these won't be available
        // TODO: (VirajSalaka) Finalize the correctness after discussion
        latencies.setBackendLatency(properties.getTimeToFirstUpstreamTxByte().getNanos() / 1000000 -
                properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000);
        latencies.setResponseLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000);
        latencies.setRequestMediationLatency(properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000);
        latencies.setResponseMediationLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000 -
                properties.getTimeToFirstUpstreamRxByte().getNanos() / 1000000);
        return latencies;
    }

    @Override
    public MetaInfo getMetaInfo() {
        Map<String, Value> fieldsMap = logEntry.getCommonProperties().getMetadata()
                .getFilterMetadataMap().get("envoy.filters.http.ext_authz").getFieldsMap();
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setCorrelationId(getValueAsString(fieldsMap, "CorrelationId"));
        metaInfo.setGatewayType(getValueAsString(fieldsMap, "GatewayType"));
        metaInfo.setRegionId(getValueAsString(fieldsMap, "RegionId"));
        return metaInfo;
    }

    @Override
    public int getProxyResponseCode() {
        // TODO: (VirajSalaka) Needs to bring in status code modification
        // As the response is not modified
        return logEntry.getResponse().getResponseCode().getValue();
    }

    @Override
    public int getTargetResponseCode() {
        if (logEntry.getResponse().getResponseCodeDetails().equals("via_upstream")) {
            return logEntry.getResponse().getResponseCode().getValue();
        }
        return 0;
    }

    @Override
    public long getRequestTime() {
        // TODO: (VirajSalaka) Findout if it is seconds or millies
        return logEntry.getCommonProperties().getStartTime().getSeconds();
    }

    @Override
    public Error getError(FaultCategory faultCategory) {

        FaultCodeClassifier faultCodeClassifier = new FaultCodeClassifier(logEntry);
        FaultSubCategory faultSubCategory = faultCodeClassifier.getFaultSubCategory(faultCategory);
        Error error = new Error();
        // TODO: (VirajSalaka) Check against -1 values.
        error.setErrorCode(faultCodeClassifier.getErrorCode());
        error.setErrorMessage(faultSubCategory);
        return error;
    }

    @Override
    public String getUserAgentHeader() {
        return logEntry.getRequest().getUserAgent();
    }

    @Override
    public String getEndUserIP() {
        return logEntry.getCommonProperties().getDownstreamRemoteAddress().getSocketAddress().getAddress();
    }

    private String getValueAsString(Map<String, Value> fieldsMap, String key) {
        return fieldsMap.get(key).getStringValue();
    }
}
