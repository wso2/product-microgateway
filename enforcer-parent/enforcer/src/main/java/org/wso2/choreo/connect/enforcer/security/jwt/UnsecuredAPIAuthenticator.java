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

package org.wso2.choreo.connect.enforcer.security.jwt;

import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;
import org.wso2.choreo.connect.enforcer.models.API;
import org.wso2.choreo.connect.enforcer.security.Authenticator;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataHolder;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataStore;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

/**
 * Implements the authenticator interface to authenticate non-secured APIs.
 */

public class UnsecuredAPIAuthenticator implements Authenticator {

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        // Retrieve the disable security value. If security is disabled, then you can proceed directly with the
        // authentication.
        if (isDisableSecurity(requestContext.getMatchedResourcePath())) {
            return true;
        }
        return false;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        TracingSpan unsecuredApiAuthenticatorSpan = null;
        Scope unsecuredApiAuthenticatorSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                unsecuredApiAuthenticatorSpan = Utils
                        .startSpan(TracingConstants.UNSECURED_API_AUTHENTICATOR_SPAN, tracer);
                unsecuredApiAuthenticatorSpanScope = unsecuredApiAuthenticatorSpan.getSpan().makeCurrent();
                Utils.setTag(unsecuredApiAuthenticatorSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }
            String uuid = requestContext.getMatchedAPI().getUuid();
            String context = requestContext.getMatchedAPI().getBasePath();
            String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(context);
            SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                    .getTenantSubscriptionStore(apiTenantDomain);
            API api = datastore.getApiByContextAndVersion(uuid);
            if (api != null && APIConstants.LifecycleStatus.BLOCKED.equals(api.getLcState())) {
                FilterUtils.setErrorToContext(requestContext,
                        GeneralErrorCodeConstants.API_BLOCKED_CODE,
                        APIConstants.StatusCodes.SERVICE_UNAVAILABLE.getCode(),
                        GeneralErrorCodeConstants.API_BLOCKED_MESSAGE,
                        GeneralErrorCodeConstants.API_BLOCKED_DESCRIPTION);
                throw new APISecurityException(APIConstants.StatusCodes.SERVICE_UNAVAILABLE.getCode(),
                        GeneralErrorCodeConstants.API_BLOCKED_CODE, GeneralErrorCodeConstants.API_BLOCKED_MESSAGE);
            }
            return FilterUtils.generateAuthenticationContextForUnsecured(requestContext);
        } finally {
            if (Utils.tracingEnabled()) {
                unsecuredApiAuthenticatorSpanScope.close();
                Utils.finishSpan(unsecuredApiAuthenticatorSpan);
            }
        }
    }

    @Override
    public String getChallengeString() {
        return "";
    }

    @Override
    public String getName() {
        return "Unsecured";
    }

    @Override public int getPriority() {
        return -20;
    }

    /**
     * This method retrieve the proper auth type for the given request context.
     * AuthType can be deduced from API level and resource level. If both are defined,
     * resource level gets the precedence.
     * If nothing declared, it will return the authType as "default".
     * @param matchingResource matching resource related configurations
     * @return value of the authType from API definition. If not present "default"
     */
    private boolean isDisableSecurity(ResourceConfig matchingResource) {

        return matchingResource.isDisableSecurity();
    }
}
