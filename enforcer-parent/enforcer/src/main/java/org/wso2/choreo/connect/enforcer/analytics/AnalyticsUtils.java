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

import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import org.apache.commons.lang3.StringUtils;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.AnalyticsConstants;
import org.wso2.choreo.connect.enforcer.constants.MetadataConstants;
import org.wso2.choreo.connect.enforcer.models.API;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataHolder;

/**
 * Common Utility functions
 */
public class AnalyticsUtils {

    public static String getAPIId(RequestContext requestContext) {
        return requestContext.getMatchedAPI().getUuid();
    }

    public static String setDefaultIfNull(String value) {
        return value == null ? AnalyticsConstants.DEFAULT_FOR_UNASSIGNED : value;
    }

    public static String getAPIProvider(String uuid) {
        API api = SubscriptionDataHolder.getInstance().getTenantSubscriptionStore().getApiByContextAndVersion(uuid);
        if (api == null) {
            return AnalyticsConstants.DEFAULT_FOR_UNASSIGNED;
        }
        return setDefaultIfNull(api.getApiProvider());
    }

    /**
     * Extracts Authentication Context from the request Context. If Authentication Context is not available,
     * new Authentication Context object will be created with authenticated property is set to false.
     *
     * @param requestContext {@code RequestContext} object
     * @return {@code AuthenticationContext} object
     */
    public static AuthenticationContext getAuthenticationContext(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        // When authentication failure happens authContext remains null
        if (authContext == null) {
            authContext = new AuthenticationContext();
            authContext.setAuthenticated(false);
        }
        return authContext;
    }

    /**
     * Decides if the logEntry corresponds to a mock API. The "x-wso2-is-mock-api" is only set when
     * handling mock-api-request.
     *
     * @param logEntry Access Log Entry
     * @return true if the logEntry has the metadata called "x-wso2-is-mock-api" and its value is true
     */
    public static boolean isMockAPISuccessRequest(HTTPAccessLogEntry logEntry) {

        return (!StringUtils.isEmpty(logEntry.getResponse().getResponseCodeDetails())) &&
                logEntry.getResponse().getResponseCodeDetails()
                        .equals(AnalyticsConstants.EXT_AUTH_DENIED_RESPONSE_DETAIL) &&
                logEntry.hasCommonProperties() &&
                logEntry.getCommonProperties().hasMetadata() &&
                logEntry.getCommonProperties().getMetadata().getFilterMetadataMap()
                        .get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY) != null &&
                logEntry.getCommonProperties().getMetadata()
                        .getFilterMetadataMap().get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
                        .containsKey(MetadataConstants.IS_MOCK_API) &&
                Boolean.parseBoolean(logEntry.getCommonProperties().getMetadata()
                        .getFilterMetadataMap().get(MetadataConstants.EXT_AUTH_METADATA_CONTEXT_KEY).getFieldsMap()
                        .get(MetadataConstants.IS_MOCK_API).getStringValue());
    }
}
