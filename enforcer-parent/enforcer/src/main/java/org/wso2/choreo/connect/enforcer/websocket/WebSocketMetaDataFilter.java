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
package org.wso2.choreo.connect.enforcer.websocket;

import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.api.WebSocketAPI;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocketMetaDataFilter adds metadata from AuthenticationContext and ApiConfig, to metadata map in the
 * RequestContext. These metadata values are added to envoy connection properties as dynamic metadata and will be
 * sent back to enforcer through the WebSocketFrame service as Metadata.
 */
public class WebSocketMetaDataFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(WebSocketAPI.class);

    private APIConfig apiConfig;

    @Override
    public void init(APIConfig apiConfig, Map<String, String> configProperties) {
        this.apiConfig = apiConfig;
    }

    @Override public boolean handleRequest(RequestContext requestContext) {
        TracingSpan wsSpan = null;
        Scope wsSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                wsSpan = Utils.startSpan(TracingConstants.WS_METADATA_SPAN, tracer);
                wsSpanScope = wsSpan.getSpan().makeCurrent();
                Utils.setTag(wsSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));

            }
            String apiTier = !requestContext.getMatchedAPI().getTier().isBlank()
                    ? requestContext.getMatchedAPI().getTier() : ThrottleConstants.UNLIMITED_TIER;
            AuthenticationContext authenticationContext = requestContext.getAuthenticationContext();
            requestContext.addMetadataToMap(MetadataConstants.GRPC_STREAM_ID, UUID.randomUUID().toString());
            requestContext.addMetadataToMap(MetadataConstants.REQUEST_ID,
                    getNullableStringValue(requestContext.getRequestID()));
            requestContext.addMetadataToMap(MetadataConstants.USERNAME,
                    getNullableStringValue(authenticationContext.getUsername()));
            requestContext.addMetadataToMap(MetadataConstants.APP_TIER,
                    getNullableStringValue(authenticationContext.getApplicationTier()));
            requestContext.addMetadataToMap(MetadataConstants.TIER,
                    getNullableStringValue(authenticationContext.getTier()));
            requestContext.addMetadataToMap(MetadataConstants.API_TIER,
                    getNullableStringValue(apiTier));
            requestContext.addMetadataToMap(MetadataConstants.CONTENT_AWARE_TIER_PRESENT,
                    getNullableStringValue(String.valueOf(authenticationContext.isContentAwareTierPresent())));
            requestContext.addMetadataToMap(MetadataConstants.API_KEY,
                    getNullableStringValue(authenticationContext.getApiKey()));
            requestContext.addMetadataToMap(MetadataConstants.KEY_TYPE,
                    getNullableStringValue(authenticationContext.getKeyType()));
            requestContext.addMetadataToMap(MetadataConstants.CALLER_TOKEN,
                    getNullableStringValue(authenticationContext.getCallerToken()));
            requestContext.addMetadataToMap(MetadataConstants.APP_ID,
                    String.valueOf(authenticationContext.getApplicationId()));
            requestContext.addMetadataToMap(MetadataConstants.API_TYPE_KEY, apiConfig.getApiType());
            // Unused but added to maintain the consistancy
            requestContext.addMetadataToMap(MetadataConstants.APP_UUID,
                    String.valueOf(authenticationContext.getApplicationUUID()));
            requestContext.addMetadataToMap(MetadataConstants.APP_NAME,
                    getNullableStringValue(authenticationContext.getApplicationName()));
            requestContext.addMetadataToMap(MetadataConstants.CONSUMER_KEY,
                    getNullableStringValue(authenticationContext.getConsumerKey()));
            requestContext.addMetadataToMap(MetadataConstants.SUBSCRIBER,
                    getNullableStringValue(authenticationContext.getSubscriber()));
            requestContext.addMetadataToMap(MetadataConstants.SPIKE_ARREST_LIMIT,
                    getNullableStringValue(String.valueOf(authenticationContext.getSpikeArrestLimit())));
            requestContext.addMetadataToMap(MetadataConstants.SUBSCRIBER_TENANT_DOMAIN,
                    getNullableStringValue(authenticationContext.getSubscriberTenantDomain()));
            requestContext.addMetadataToMap(MetadataConstants.SPIKE_ARREST_UNIT,
                    getNullableStringValue(authenticationContext.getSpikeArrestUnit()));
            requestContext.addMetadataToMap(MetadataConstants.STOP_ON_QUOTA,
                    getNullableStringValue(String.valueOf(authenticationContext.isStopOnQuotaReach())));
            requestContext.addMetadataToMap(MetadataConstants.PRODUCT_NAME,
                    getNullableStringValue(authenticationContext.getProductName()));
            requestContext.addMetadataToMap(MetadataConstants.PRODUCT_PROVIDER,
                    getNullableStringValue(authenticationContext.getProductProvider()));
            requestContext.addMetadataToMap(MetadataConstants.API_PUBLISHER,
                    getNullableStringValue(authenticationContext.getApiPublisher()));
            requestContext.addMetadataToMap(APIConstants.GW_API_NAME_PARAM,
                    getNullableStringValue(apiConfig.getName()));
            requestContext.addMetadataToMap(APIConstants.GW_BASE_PATH_PARAM,
                    getNullableStringValue(apiConfig.getBasePath()));
            requestContext.addMetadataToMap(APIConstants.GW_VHOST_PARAM,
                    getNullableStringValue(apiConfig.getVhost()));
            requestContext.addMetadataToMap(APIConstants.GW_VERSION_PARAM,
                    getNullableStringValue(apiConfig.getVersion()));
            return true;
        } finally {
            if (Utils.tracingEnabled()) {
                wsSpanScope.close();
                Utils.finishSpan(wsSpan);
            }
        }
    }

    private String getNullableStringValue(String value) {
        if (value != null) {
            return value;
        } else {
            return "";
        }
    }
}
