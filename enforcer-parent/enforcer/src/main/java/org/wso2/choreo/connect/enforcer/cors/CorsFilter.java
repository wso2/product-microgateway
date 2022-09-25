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

package org.wso2.choreo.connect.enforcer.cors;

import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.HttpConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;

/**
 * Cors Filter for failed preflight requests.
 */
public class CorsFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(CorsFilter.class);

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        TracingSpan corsSpan = null;
        Scope corsSpanScope = null;
        try {
            if (Utils.tracingEnabled()) {
                TracingTracer tracer = Utils.getGlobalTracer();
                corsSpan = Utils.startSpan(TracingConstants.CORS_SPAN, tracer);
                corsSpanScope = corsSpan.getSpan().makeCurrent();
                Utils.setTag(corsSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));

            }
            logger.debug("Cors Filter (enforcer) is applied.");
            // Options request is served here.
            // Preflight success request does not reach here.
            if (requestContext.getRequestMethod().contains(HttpConstants.OPTIONS)) {
                // If the OPTIONS method is provided under the resource, microgateway do not respond
                if (requestContext.getMatchedResourcePaths() != null) {
                    logger.debug("OPTIONS method is listed under the resource. Hence OPTIONS request will" +
                            "be responded from the upstream");
                    return true;
                }
                StringBuilder allowedMethodsBuilder = new StringBuilder(HttpConstants.OPTIONS);
                // Handling GraphQL post requests
                // Only post method is allowed for GQL apis, hence it will be added to the allowed method list.
                if (APIConstants.ApiType.GRAPHQL.equalsIgnoreCase(requestContext.getMatchedAPI().getApiType())) {
                    allowedMethodsBuilder.append(", ").append(ResourceConfig.HttpMethods.POST);
                } else {
                    for (ResourceConfig resourceConfig : requestContext.getMatchedAPI().getResources()) {
                        if (!resourceConfig.getPath().equals(requestContext.getRequestPathTemplate())) {
                            continue;
                        }
                        allowedMethodsBuilder.append(", ").append(resourceConfig.getMethod().name());
                    }
                }
                requestContext.getProperties()
                        .put(APIConstants.MessageFormat.STATUS_CODE, HttpConstants.NO_CONTENT_STATUS_CODE);
                requestContext.addOrModifyHeaders(HttpConstants.ALLOW_HEADER, allowedMethodsBuilder.toString());
                logger.debug("OPTIONS request received for " +
                        requestContext.getMatchedAPI().getResources().get(0).getPath() +
                        ". Responded with allow header : " + allowedMethodsBuilder.toString());
                return false;
            }
            return true;
        } finally {
            if (Utils.tracingEnabled()) {
                corsSpanScope.close();
                Utils.finishSpan(corsSpan);

            }
        }
    }
}
