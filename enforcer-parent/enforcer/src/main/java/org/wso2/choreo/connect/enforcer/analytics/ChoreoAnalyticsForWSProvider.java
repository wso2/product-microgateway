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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.analytics.Constants;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsDataProvider;
import org.wso2.carbon.apimgt.common.analytics.exceptions.DataNotFoundException;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.API;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Application;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Error;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Latencies;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.MetaInfo;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Operation;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.Target;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.EventCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultCategory;
import org.wso2.carbon.apimgt.common.analytics.publishers.dto.enums.FaultSubCategory;
import org.wso2.choreo.connect.discovery.service.websocket.WebSocketFrameRequest;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;
import org.wso2.choreo.connect.enforcer.constants.MetadataConstants;

import java.util.Map;

/**
 * Analytics Data Provider for Websocket frame requests.
 */
public class ChoreoAnalyticsForWSProvider implements AnalyticsDataProvider {

    private static final Logger logger = LogManager.getLogger(ChoreoAnalyticsForWSProvider.class);

    private WebSocketFrameRequest webSocketFrameRequest;
    private Map<String, String> extAuthMetadata;

    public ChoreoAnalyticsForWSProvider(WebSocketFrameRequest webSocketFrameRequest) {
        this.webSocketFrameRequest = webSocketFrameRequest;
        extAuthMetadata = webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap();
    }

    @Override
    public EventCategory getEventCategory() {
        // TODO: (VirajSalaka) Fix
        if (isSuccessRequest()) {
            return EventCategory.SUCCESS;
        } else if (isFaultRequest()) {
            return EventCategory.FAULT;
        } else {
            return EventCategory.INVALID;
        }
    }

    private boolean isSuccessRequest() {
        return 0 == webSocketFrameRequest.getApimErrorCode();
    }

    private boolean isFaultRequest() {
        // TODO: (VirajSalaka) Fix
        return !isSuccessRequest();
    }

    @Override
    public boolean isAnonymous() {
        return AnalyticsConstants.DEFAULT_FOR_UNASSIGNED
                .equals(webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap()
                        .get(MetadataConstants.APP_UUID_KEY));
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public FaultCategory getFaultType() {
        if (webSocketFrameRequest.getApimErrorCode() >= 900800
                && webSocketFrameRequest.getApimErrorCode() < 900900) {
            return FaultCategory.THROTTLED;
        } else if (webSocketFrameRequest.getApimErrorCode() == 101503) {
            return FaultCategory.TARGET_CONNECTIVITY;
        }
        return FaultCategory.OTHER;
    }

    @Override
    public API getApi() throws DataNotFoundException {

        API api = new API();

        String apiVersion = extAuthMetadata.get(MetadataConstants.API_VERSION_KEY);
        String apiName = extAuthMetadata.get(MetadataConstants.API_NAME_KEY);
        String apiId = extAuthMetadata.get(MetadataConstants.API_ID_KEY);
        String apiCreator = extAuthMetadata.get(MetadataConstants.API_CREATOR_KEY);;
        String apiCreatorTenantDomain = extAuthMetadata.get(MetadataConstants.API_CREATOR_TENANT_DOMAIN_KEY);
        api.setApiType(APIConstants.ApiType.WEB_SOCKET);
        api.setApiId(apiId);
        api.setApiName(apiName);
        api.setApiVersion(apiVersion);
        api.setApiCreatorTenantDomain(apiCreatorTenantDomain);
        api.setApiCreator(apiCreator);
        return api;
    }

    @Override
    public Application getApplication() throws DataNotFoundException {
        Application application = new Application();
        application.setApplicationId(extAuthMetadata.get(MetadataConstants.APP_UUID_KEY));
        application.setApplicationName(extAuthMetadata.get(MetadataConstants.APP_NAME_KEY));
        application.setApplicationOwner(extAuthMetadata.get(MetadataConstants.APP_OWNER_KEY));
        application.setKeyType(extAuthMetadata.get(MetadataConstants.APP_KEY_TYPE_KEY));
        return application;
    }

    @Override
    public Operation getOperation() throws DataNotFoundException {
        Operation operation = new Operation();
        String method = webSocketFrameRequest.getDirection().name();
        operation.setApiMethod(method);
        String matchingResource = extAuthMetadata.get(MetadataConstants.API_RESOURCE_TEMPLATE_KEY);
        if ("HANDSHAKE".equals(webSocketFrameRequest.getDirection().name())) {
            matchingResource = "init-request:" + matchingResource;
        }
        operation.setApiResourceTemplate(matchingResource);
        return operation;
    }

    @Override
    public Target getTarget() {
        Target target = new Target();

        // These properties are not applicable for WS API
        target.setResponseCacheHit(false);
        target.setTargetResponseCode(0);
        String endpointAddress = extAuthMetadata.get(MetadataConstants.DESTINATION);
        target.setDestination(endpointAddress);
        return target;
    }

    @Override
    public Latencies getLatencies() {
        return new Latencies();
    }

    @Override
    public MetaInfo getMetaInfo() {
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setCorrelationId(extAuthMetadata.get(MetadataConstants.CORRELATION_ID_KEY));
        metaInfo.setGatewayType("ENVOY");
        metaInfo.setRegionId(extAuthMetadata.get(MetadataConstants.REGION_KEY));
        return metaInfo;
    }

    @Override
    public int getProxyResponseCode() {
        if (isSuccessRequest()) {
            return 200;
        }
        // TODO: (VirajSalaka) bring in constants
        // This is required by the analytics endpoint in order to display the errors properly.
        if (webSocketFrameRequest.getApimErrorCode() >= 900800
                && webSocketFrameRequest.getApimErrorCode() < 900900) {
            return 429;
        }
        if (webSocketFrameRequest.getApimErrorCode() == 101503) {
            return 503;
        }
        return Constants.UNKNOWN_INT_VALUE;
    }

    @Override
    public int getTargetResponseCode() {
        if (isSuccessRequest()) {
            return 200;
        }
        return Constants.UNKNOWN_INT_VALUE;
    }

    @Override
    public long getRequestTime() {
        return 0;
    }

    @Override
    public Error getError(FaultCategory faultCategory) {
        int errorCode = webSocketFrameRequest.getApimErrorCode();
        FaultCodeClassifier faultCodeClassifier = new FaultCodeClassifier(errorCode);
        FaultSubCategory faultSubCategory = faultCodeClassifier.getFaultSubCategory(faultCategory);
        Error error = new Error();
        error.setErrorCode(errorCode);
        error.setErrorMessage(faultSubCategory);
        return error;
    }

    @Override
    public String getUserAgentHeader() {
        return extAuthMetadata.get(MetadataConstants.USER_AGENT_KEY);
    }

    @Override
    public String getEndUserIP() {
        return extAuthMetadata.get(MetadataConstants.CLIENT_IP_KEY);
    }

    private String getExtAuthzMetadata(String key) {
        if (!webSocketFrameRequest.hasMetadata() ||
                !webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap().containsKey(key)) {
            return null;
        }
        return webSocketFrameRequest.getMetadata().getExtAuthzMetadataMap().get(key);
    }
}
