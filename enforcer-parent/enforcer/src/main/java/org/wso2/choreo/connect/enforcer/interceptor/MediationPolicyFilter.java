/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.interceptor;

import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.model.Policy;
import org.wso2.choreo.connect.enforcer.commons.model.PolicyConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;

import java.util.Map;

/**
 * Apply mediation policies.
 */
public class MediationPolicyFilter implements Filter {
    private static final Logger log = LogManager.getLogger(MediationPolicyFilter.class);
    @Override
    public boolean handleRequest(RequestContext requestContext) {
        // get operation policies
        PolicyConfig policyConfig = requestContext.getMatchedResourcePath().getPolicyConfig();
        // apply in policies
        if (policyConfig.getIn() != null && policyConfig.getIn().size() > 0) {
            for (Policy policy : policyConfig.getIn()) {
                applyPolicy(requestContext, policy);
            }
        }
        return true;
    }

    private void applyPolicy(RequestContext requestContext, Policy policy) {
        //todo(amali)
        // 1. handle fault in out
        // 2. parameter sanitization, set constants
        // 3. check order
        // 4. per operation interceptor
        // 5. check interceptor extension and policy as and/or?
        switch (policy.getTemplateName()) {
            case "SET_HEADER":
            case "RENAME_HEADER": {
                addOrModifyHeader(requestContext, policy.getParameters());
                break;
            }
            case "REMOVE_HEADER": {
                removeHeader(requestContext, policy.getParameters());
                break;
            }
            case "ADD_QUERY": {
                addOrModifyQuery(requestContext, policy.getParameters());
                break;
            }
            case "REMOVE_QUERY": {
                removeQuery(requestContext, policy.getParameters());
                break;
            }
            case "REWRITE_RESOURCE_PATH": {
                removeQueries(requestContext, policy.getParameters());
                break;
            }
            case "REWRITE_RESOURCE_METHOD": {
                modifyMethod(requestContext, policy.getParameters());
                break;
            }
        }
    }

    private void addOrModifyHeader(RequestContext requestContext, Map<String, String> policyAttrib) {
        String headerName = policyAttrib.get("headerName");
        String headerValue = policyAttrib.get("headerValue");
        requestContext.addOrModifyHeaders(headerName, headerValue);
    }

    private void removeHeader(RequestContext requestContext, Map<String, String> policyAttrib) {
        String headerName = policyAttrib.get("headerName");
        requestContext.getRemoveHeaders().add(headerName);
    }

    private void removeQuery(RequestContext requestContext, Map<String, String> policyAttrib) {
        String queryName = policyAttrib.get("queryParamName");
        requestContext.getQueryParamsToRemove().add(queryName);
    }

    private void removeQueries(RequestContext requestContext, Map<String, String> policyAttrib) {
        if (policyAttrib.containsKey("includeQueryParams")) {
            boolean removeQuery = !Boolean.parseBoolean(policyAttrib.get("includeQueryParams"));
            requestContext.setRemoveAllQueryParams(removeQuery);
        }
    }

    private void addOrModifyQuery(RequestContext requestContext, Map<String, String> policyAttrib) {
        String queryName = policyAttrib.get("queryParamName");
        String queryValue = policyAttrib.get("queryParamValue");
        requestContext.getQueryParamsToAdd().put(queryName, queryValue);
    }

    private void modifyMethod(RequestContext requestContext, Map<String, String> policyAttrib) {
        String currentMethod = policyAttrib.get("currentMethod");
        try {
            HttpMethod updatedMethod = HttpMethod.valueOf(policyAttrib.get("updatedMethod"));

            if (currentMethod.equalsIgnoreCase(requestContext.getHeaders().get(":method"))) {
                requestContext.addOrModifyHeaders(":method", updatedMethod.toString().toUpperCase());
            }
        } catch (IllegalArgumentException ex) {
            log.error("Error while getting mediation policy rewrite method", ex);
        }
    }
}
