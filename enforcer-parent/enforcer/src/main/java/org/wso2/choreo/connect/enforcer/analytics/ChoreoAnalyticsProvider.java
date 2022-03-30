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

import com.google.protobuf.Value;
import io.envoyproxy.envoy.data.accesslog.v3.AccessLogCommon;
import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.API;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Application;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Error;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.ExtendedAPI;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Latencies;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.MetaInfo;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Operation;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Target;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;
import org.wso2.choreo.connect.enforcer.constants.MetadataConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Analytics Data Provider of Microgateway
 */
public class ChoreoAnalyticsProvider implements AnalyticsDataProvider {
    private static final Logger logger = LogManager.getLogger(ChoreoAnalyticsProvider.class);
    protected final HTTPAccessLogEntry logEntry;

    public ChoreoAnalyticsProvider(HTTPAccessLogEntry logEntry) {
        this.logEntry = logEntry;
    }

    @Override
    public EventCategory getEventCategory() {
        if (logEntry.getResponse() != null && AnalyticsConstants.UPSTREAM_SUCCESS_RESPONSE_DETAIL.equals(
                logEntry.getResponse().getResponseCodeDetails())) {
            logger.debug("Is success event");
            return EventCategory.SUCCESS;
        } else if (logEntry.hasResponse()
                && logEntry.getResponse().hasResponseCode()
                && logEntry.getResponse().getResponseCode().getValue() != 200
                && logEntry.getResponse().getResponseCode().getValue() != 204) {
            logger.debug("Is fault event");
            return EventCategory.FAULT;
        } else {
            logger.debug("Is invalid event");
            return EventCategory.INVALID;
        }
    }

    @Override
    public boolean isAnonymous() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        // If appId is unknown, subscriptions are not validated.
        return AnalyticsConstants.DEFAULT_FOR_UNASSIGNED
                .equals(getValueAsString(fieldsMap, MetadataConstants.APP_ID_KEY));
    }

    @Override
    public boolean isAuthenticated() {
        // Authentication failed requests are already published.
        return true;
    }

    @Override
    public FaultCategory getFaultType() {
        if (isTargetFaultRequest()) {
            return FaultCategory.TARGET_CONNECTIVITY;
        } else {
            return FaultCategory.OTHER;
        }
    }

    public boolean isTargetFaultRequest() {
        String responseCodeDetail = logEntry.getResponse().getResponseCodeDetails();
        return (!AnalyticsConstants.UPSTREAM_SUCCESS_RESPONSE_DETAIL.equals(responseCodeDetail))
                && (!AnalyticsConstants.EXT_AUTH_DENIED_RESPONSE_DETAIL.equals(responseCodeDetail))
                && (!AnalyticsConstants.EXT_AUTH_ERROR_RESPONSE_DETAIL.equals(responseCodeDetail))
                && (!AnalyticsConstants.ROUTE_NOT_FOUND_RESPONSE_DETAIL.equals(responseCodeDetail));
    }

    @Override
    public API getApi() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        ExtendedAPI api = new ExtendedAPI();
        api.setApiType(getValueAsString(fieldsMap, MetadataConstants.API_TYPE_KEY));
        api.setApiId(getValueAsString(fieldsMap, MetadataConstants.API_ID_KEY));
        api.setApiCreator(getValueAsString(fieldsMap, MetadataConstants.API_CREATOR_KEY));
        api.setApiName(getValueAsString(fieldsMap, MetadataConstants.API_NAME_KEY));
        api.setApiVersion(getValueAsString(fieldsMap, MetadataConstants.API_VERSION_KEY));
        api.setApiCreatorTenantDomain(getValueAsString(fieldsMap, MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY));
        api.setOrganizationId(getValueAsString(fieldsMap, MetadataConstants.API_ORGANIZATION_ID));

        return api;
    }

    @Override
    public Application getApplication() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        Application application = new Application();
        application.setApplicationOwner(getValueAsString(fieldsMap, MetadataConstants.APP_OWNER_KEY));
        application.setApplicationName(getValueAsString(fieldsMap, MetadataConstants.APP_NAME_KEY));
        application.setKeyType(getValueAsString(fieldsMap, MetadataConstants.APP_KEY_TYPE_KEY));
        application.setApplicationId(getValueAsString(fieldsMap, MetadataConstants.APP_UUID_KEY));
        return application;
    }

    @Override
    public Operation getOperation() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        Operation operation = new Operation();
        operation.setApiResourceTemplate(getValueAsString(fieldsMap, MetadataConstants.API_RESOURCE_TEMPLATE_KEY));
        operation.setApiMethod(logEntry.getRequest().getRequestMethod().name());
        return operation;
    }

    @Override
    public Target getTarget() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        Target target = new Target();
        // As response caching is not configured at the moment.
        target.setResponseCacheHit(false);
        target.setTargetResponseCode(logEntry.getResponse().getResponseCode().getValue());
        target.setDestination(getValueAsString(fieldsMap, MetadataConstants.DESTINATION));
        return target;
    }

    @Override
    public Latencies getLatencies() {
        // This method is only invoked for success requests. Hence all these properties will be available.
        // The cors requests responded from the CORS filter are already filtered at this point.
        AccessLogCommon properties = logEntry.getCommonProperties();
        long backendResponseRecvTimestamp = properties.getTimeToLastUpstreamRxByte().getSeconds() * 1000 +
                properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000;
        long backendRequestSendTimestamp = properties.getTimeToFirstUpstreamTxByte().getSeconds() * 1000 +
                properties.getTimeToFirstUpstreamTxByte().getNanos() / 1000000;
        long downstreamResponseSendTimestamp = properties.getTimeToLastDownstreamTxByte().getSeconds() * 1000 +
                properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000;

        Latencies latencies = new Latencies();
        latencies.setBackendLatency(backendResponseRecvTimestamp - backendRequestSendTimestamp);
        latencies.setRequestMediationLatency(backendRequestSendTimestamp);
        latencies.setResponseLatency(downstreamResponseSendTimestamp);
        latencies.setResponseMediationLatency(downstreamResponseSendTimestamp - backendResponseRecvTimestamp);
        return latencies;
    }

    @Override
    public MetaInfo getMetaInfo() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setCorrelationId(getValueAsString(fieldsMap, MetadataConstants.CORRELATION_ID_KEY));
        metaInfo.setGatewayType(AnalyticsConstants.GATEWAY_LABEL);
        metaInfo.setRegionId(getValueAsString(fieldsMap, MetadataConstants.REGION_KEY));
        return metaInfo;
    }

    @Override
    public int getProxyResponseCode() {
        // As the response is not modified
        return getTargetResponseCode();
    }

    @Override
    public int getTargetResponseCode() {
        return logEntry.getResponse().getResponseCode().getValue();
    }

    @Override
    public long getRequestTime() {
        return logEntry.getCommonProperties().getStartTime().getSeconds() * 1000 +
                logEntry.getCommonProperties().getStartTime().getNanos() / 1000000;
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
        if (fieldsMap == null || !fieldsMap.containsKey(key)) {
            return null;
        }
        return fieldsMap.get(key).getStringValue();
    }

    private Map<String, Value> getFieldsMapFromLogEntry() {
        if (logEntry.getCommonProperties() == null
                || logEntry.getCommonProperties().getMetadata() == null
                || logEntry.getCommonProperties().getMetadata().getFilterMetadataMap() == null
                || !logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .containsKey(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY)) {
            return new HashMap<>(0);
        }
        return logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap();
    }
}
