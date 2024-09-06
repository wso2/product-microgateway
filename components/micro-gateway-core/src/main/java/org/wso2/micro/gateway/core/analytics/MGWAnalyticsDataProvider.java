/*
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.micro.gateway.core.analytics;

import org.ballerinalang.jvm.values.api.BMap;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Data provider interface to extract request data.
 */
public class MGWAnalyticsDataProvider implements AnalyticsDataProvider {
    private final Boolean isFault;
    private final boolean isAnonymous;
    private final boolean isAuthenticated;
    private final int responseCode;
    private final String apiUUID;
    private final String apiType;
    private final String apiName;
    private final String apiVersion;
    private final String apiContext;
    private final String apiCreator;
    private final String apiCreatorTenantDomain;
    private final String organizationId;
    private final String applicationUUID;
    private final String applicationName;
    private final String applicationOwner;
    private final String applicationKeyType;
    private final String httpMethod;
    private final String apiResourceTemplate;
    private final int targetResponseCode;
    private final boolean responseCacheHit;
    private final String destination;
    private final long requestTime;
    private final String correlationId;
    private final String regionId;
    private final String userAgentHeader;
    private final String userName;
    private final String endUserIP;
    private final long backendLatency;
    private final long responseLatency;
    private final int errorCode;

    public MGWAnalyticsDataProvider(BMap<String, Object> eventData) {
        this.isFault = Boolean.parseBoolean(String.valueOf(eventData.get("isFault")));
        this.isAnonymous = Boolean.parseBoolean(String.valueOf(eventData.get("isAnonymous")));
        this.isAuthenticated = Boolean.parseBoolean(String.valueOf(eventData.get("isAuthenticated")));
        this.responseCode = Integer.parseInt(String.valueOf(eventData.get("responseCode")));
        this.apiUUID = String.valueOf(eventData.get("apiUUID"));
        this.apiType = String.valueOf(eventData.get("apiType"));
        this.apiName = String.valueOf(eventData.get("apiName"));
        this.apiVersion = String.valueOf(eventData.get("apiVersion"));
        this.apiContext = String.valueOf(eventData.get("apiContext"));
        this.apiCreator = String.valueOf(eventData.get("apiCreator"));
        this.apiCreatorTenantDomain = String.valueOf(eventData.get("apiCreatorTenantDomain"));
        this.organizationId = String.valueOf(eventData.get("organizationId"));
        this.applicationUUID = String.valueOf(eventData.get("applicationUUID"));
        this.applicationName = String.valueOf(eventData.get("applicationName"));
        this.applicationOwner = String.valueOf(eventData.get("applicationOwner"));
        this.applicationKeyType = String.valueOf(eventData.get("applicationKeyType"));
        this.httpMethod = String.valueOf(eventData.get("httpMethod"));
        this.apiResourceTemplate = String.valueOf(eventData.get("apiResourceTemplate"));
        this.targetResponseCode = Integer.parseInt(String.valueOf(eventData.get("targetResponseCode")));
        this.responseCacheHit = Boolean.parseBoolean(String.valueOf(eventData.get("responseCacheHit")));
        this.destination = String.valueOf(eventData.get("destination"));
        this.requestTime = Long.parseLong(String.valueOf(eventData.get("requestTime")));
        this.correlationId = String.valueOf(eventData.get("correlationId"));
        this.regionId = String.valueOf(eventData.get("regionId"));
        this.userAgentHeader = String.valueOf(eventData.get("userAgentHeader"));
        this.userName = String.valueOf(eventData.get("userName"));
        this.endUserIP = String.valueOf(eventData.get("endUserIP"));
        this.backendLatency = Long.parseLong(String.valueOf(eventData.get("backendLatency")));
        this.responseLatency = Long.parseLong(String.valueOf(eventData.get("responseLatency")));
        this.errorCode = Integer.parseInt(String.valueOf(eventData.get("errorCode")));
    }

    @Override
    public EventCategory getEventCategory() {
        if (isFault != null && !isFault) {
            return EventCategory.SUCCESS;
        } else if (isFault != null) {
            return EventCategory.FAULT;
        } else {
            return EventCategory.INVALID;
        }
    }

    @Override
    public boolean isAnonymous() {
        return isAnonymous;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public FaultCategory getFaultType() {
        switch (responseCode) {
            case 401:
            case 403:
            case 503:
                // TODO: need to handle further
                return FaultCategory.AUTH;
            case 429:
                return FaultCategory.THROTTLED;
            default:
                return FaultCategory.OTHER;
        }
    }

    @Override
    public API getApi() {
        ExtendedAPI api = new ExtendedAPI();
        api.setApiId(apiUUID);
        api.setApiType(apiType);
        api.setApiName(apiName);
        api.setApiVersion(apiVersion);
        api.setApiContext(apiContext);
        api.setApiCreator(apiCreator);
        api.setApiCreatorTenantDomain(apiCreatorTenantDomain);
        api.setOrganizationId(organizationId);
        return api;
    }

    @Override
    public Application getApplication() {
        Application application = new Application();
        application.setApplicationId(applicationUUID);
        application.setApplicationName(applicationName);
        application.setApplicationOwner(applicationOwner);
        application.setKeyType(applicationKeyType);
        return application;
    }

    @Override
    public Operation getOperation() {
        Operation operation = new Operation();
        operation.setApiMethod(httpMethod);
        operation.setApiResourceTemplate(apiResourceTemplate);
        return operation;
    }

    @Override
    public Target getTarget() {
        Target target = new Target();
        target.setTargetResponseCode(targetResponseCode);
        target.setResponseCacheHit(responseCacheHit);
        target.setDestination(destination);
        return target;
    }

    @Override
    public Latencies getLatencies() {
        Latencies latencies = new Latencies();
        latencies.setBackendLatency(backendLatency);
        latencies.setResponseLatency(responseLatency);
        return latencies;
    }

    @Override
    public MetaInfo getMetaInfo() {
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setCorrelationId(correlationId);
        metaInfo.setGatewayType("BALLERINA");
        metaInfo.setRegionId(regionId);
        return metaInfo;
    }

    @Override
    public int getProxyResponseCode() {
        return 0;
    }

    @Override
    public int getTargetResponseCode() {
        return targetResponseCode;
    }

    @Override
    public long getRequestTime() {
        return requestTime;
    }

    @Override
    public Error getError(FaultCategory faultCategory) {
        FaultCodeClassifier faultCodeClassifier = new FaultCodeClassifier(errorCode);
        FaultSubCategory faultSubCategory = faultCodeClassifier.getFaultSubCategory(faultCategory);
        Error error = new Error();
        error.setErrorCode(errorCode);
        error.setErrorMessage(faultSubCategory);
        return error;
    }

    @Override
    public String getUserAgentHeader() {
        return userAgentHeader;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getEndUserIP() {
        return endUserIP;
    }

    @Override
    public Map<String, String> getMaskProperties() {
        return new HashMap<>();
    }
}
