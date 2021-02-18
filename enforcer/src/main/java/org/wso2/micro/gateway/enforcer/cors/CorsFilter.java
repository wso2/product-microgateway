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

package org.wso2.micro.gateway.enforcer.cors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.Filter;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.api.config.APIConfig;
import org.wso2.micro.gateway.enforcer.api.config.ResourceConfig;
import org.wso2.micro.gateway.enforcer.constants.APIConstants;
import org.wso2.micro.gateway.enforcer.constants.HttpConstants;

/**
 * Cors Filter for failed preflight requests.
 */
public class CorsFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(CorsFilter.class);

    @Override
    public void init(APIConfig apiConfig) {
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        logger.debug("Cors Filter (enforcer) is applied.");
        // Options request is served here.
        // Preflight success request does not reach here.
        if (requestContext.getRequestMethod().contains(HttpConstants.OPTIONS)) {
            StringBuilder allowedMethodsBuilder = new StringBuilder(HttpConstants.OPTIONS);
            for (ResourceConfig resourceConfig : requestContext.getMathedAPI().getAPIConfig().getResources()) {
                if (resourceConfig.getMethod() == ResourceConfig.HttpMethods.OPTIONS) {
                    logger.debug("OPTIONS method is listed under the resource. Hence OPTIONS request will" +
                            "be responded from the upstream");
                    return true;
                } else {
                    allowedMethodsBuilder.append(", ").append(resourceConfig.getMethod().name());
                }
            }
            requestContext.getProperties()
                    .put(APIConstants.MessageFormat.STATUS_CODE, HttpConstants.NO_CONTENT_STATUS_CODE);
            requestContext.addResponseHeaders(HttpConstants.ALLOW_HEADER, allowedMethodsBuilder.toString());
            logger.debug("OPTIONS request received for " +
                    requestContext.getMathedAPI().getAPIConfig().getResources().get(0).getPath() +
                    ". Responded with allow header : " + allowedMethodsBuilder.toString());
            return false;
        }
        return true;
    }
}
