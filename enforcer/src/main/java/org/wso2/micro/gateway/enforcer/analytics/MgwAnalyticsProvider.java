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
import org.wso2.micro.gateway.enforcer.constants.MetadataConstants;

import java.util.HashMap;
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
        // TODO: (VirajSalaka) Decide if options call should be neglected.
        // TODO: (VirajSalaka) Backend returns some error ?
        if (logEntry.getResponse().getResponseCode().getValue() == 200
                && logEntry.getResponse().getResponseCodeDetails().equals("via_upstream")) {
            logger.info("Is success event");
            return EventCategory.SUCCESS;
            // TODO: (VirajSalaka) Finalize what is a fault
        } else if (logEntry.getResponse().getResponseCode().getValue() != 200
                && !logEntry.getResponse().getResponseCodeDetails().equals("via_upstream")) {
            logger.info("Is fault event");
            return EventCategory.FAULT;
        } else {
            logger.info("Is invalid event");
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
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        API api = new API();
        api.setApiId(getValueAsString(fieldsMap, MetadataConstants.API_ID_KEY));
        api.setApiCreator(getValueAsString(fieldsMap, MetadataConstants.API_CREATOR_KEY));
        api.setApiType(getValueAsString(fieldsMap, MetadataConstants.API_TYPE_KEY));
        api.setApiName(getValueAsString(fieldsMap, MetadataConstants.API_NAME_KEY));
        api.setApiVersion(getValueAsString(fieldsMap, MetadataConstants.API_VERSION_KEY));
        api.setApiCreatorTenantDomain(getValueAsString(fieldsMap, MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY));
        return api;
    }

    @Override
    public Application getApplication() {
        // TODO: (VirajSalaka) Null check (If enforcer connection is failed)
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        Application application = new Application();
        application.setApplicationOwner(getValueAsString(fieldsMap, MetadataConstants.APP_OWNER_KEY));
        application.setApplicationName(getValueAsString(fieldsMap, MetadataConstants.APP_NAME_KEY));
        application.setKeyType(getValueAsString(fieldsMap, MetadataConstants.APP_KEY_TYPE_KEY));
        application.setApplicationId(getValueAsString(fieldsMap, MetadataConstants.APP_ID_KEY));
        return application;
    }

    @Override
    public Operation getOperation() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        Operation operation = new Operation();
        operation.setApiResourceTemplate(getValueAsString(fieldsMap, MetadataConstants.API_RESOURCE_TEMPLATE_KEY));
        // TODO: (VirajSalaka) read from context
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
        logger.info(" Target Event: " + target.toString());
        return target;
    }

    @Override
    public Latencies getLatencies() {
        AccessLogCommon properties = logEntry.getCommonProperties();
        Latencies latencies = new Latencies();
        // TODO: (VirajSalaka) If connection error happens these won't be available
        // TODO: (VirajSalaka) Finalize the correctness after discussion
        latencies.setBackendLatency(properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000 -
                properties.getTimeToFirstUpstreamTxByte().getNanos() / 1000000);
        latencies.setResponseLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000);
        latencies.setRequestMediationLatency(properties.getTimeToLastUpstreamRxByte().getNanos() / 1000000);
        latencies.setResponseMediationLatency(properties.getTimeToLastDownstreamTxByte().getNanos() / 1000000 -
                properties.getTimeToFirstUpstreamRxByte().getNanos() / 1000000);
        return latencies;
    }

    @Override
    public MetaInfo getMetaInfo() {
        Map<String, Value> fieldsMap = getFieldsMapFromLogEntry();
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setCorrelationId(getValueAsString(fieldsMap, MetadataConstants.CORRELATION_ID_KEY));
        // TODO: (VirajSalaka) Introduce Constant
        metaInfo.setGatewayType("ENVOY");
        metaInfo.setRegionId(getValueAsString(fieldsMap, MetadataConstants.REGION_KEY));
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
        return logEntry.getCommonProperties().getStartTime().getSeconds() * 1000;
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
        if (fieldsMap == null || fieldsMap.get(key) == null) {
            return null;
        }
        return fieldsMap.get(key).getStringValue();
    }

    private Map<String, Value> getFieldsMapFromLogEntry() {
        if ((logEntry.getCommonProperties().getMetadata() == null
                && logEntry.getCommonProperties().getMetadata().getFilterMetadataMap() == null)
                 || !logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .containsKey(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY)) {
            return new HashMap<>(0);
        }
        return logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap();
    }
}
