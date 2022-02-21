/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.interceptor.opa.OPAClient;
import org.wso2.choreo.connect.enforcer.interceptor.opa.OPASecurityException;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.Map;

/**
 * Apply mediation policies.
 */
public class MediationPolicyFilter implements Filter {
    private static final Logger log = LogManager.getLogger(MediationPolicyFilter.class);

    public MediationPolicyFilter() {
        OPAClient.init();
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        // get operation policies
        PolicyConfig policyConfig = requestContext.getMatchedResourcePath().getPolicyConfig();
        // apply in policies
        if (policyConfig.getIn() != null && policyConfig.getIn().size() > 0) {
            for (Policy policy : policyConfig.getIn()) {
                if (!applyPolicy(requestContext, policy)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean applyPolicy(RequestContext requestContext, Policy policy) {
        //todo(amali) check policy order
        switch (policy.getAction()) {
            case "SET_HEADER": {
                addOrModifyHeader(requestContext, policy.getParameters());
                return true;
            }
            case "RENAME_HEADER": {
                renameHeader(requestContext, policy.getParameters());
                return true;
            }
            case "REMOVE_HEADER": {
                removeHeader(requestContext, policy.getParameters());
                return true;
            }
            case "ADD_QUERY": {
                addOrModifyQuery(requestContext, policy.getParameters());
                return true;
            }
            case "REMOVE_QUERY": {
                removeQuery(requestContext, policy.getParameters());
                return true;
            }
            case "REWRITE_RESOURCE_PATH": {
                removeAllQueries(requestContext, policy.getParameters());
                return true;
            }
            case "REWRITE_RESOURCE_METHOD": {
                modifyMethod(requestContext, policy.getParameters());
                return true;
            }
            case "OPA": {
                return opaAuthValidation(requestContext, policy.getParameters());
            }
        }

        log.error("Operation policy action \"{}\" is not supported", policy.getAction());
        return false;
    }

    private void addOrModifyHeader(RequestContext requestContext, Map<String, String> policyAttrib) {
        String headerName = policyAttrib.get("headerName");
        String headerValue = policyAttrib.get("headerValue");
        requestContext.addOrModifyHeaders(headerName, headerValue);
    }

    private void renameHeader(RequestContext requestContext, Map<String, String> policyAttrib) {
        String currentHeaderName = policyAttrib.get("currentHeaderName");
        String updatedHeaderValue = policyAttrib.get("updatedHeaderName");
        if (requestContext.getHeaders().containsKey(currentHeaderName)) {
            String headerValue = requestContext.getHeaders().get(currentHeaderName);
            requestContext.getRemoveHeaders().add(currentHeaderName);
            requestContext.addOrModifyHeaders(updatedHeaderValue, headerValue);
        }
    }

    private void removeHeader(RequestContext requestContext, Map<String, String> policyAttrib) {
        String headerName = policyAttrib.get("headerName");
        requestContext.getRemoveHeaders().add(headerName);
    }

    private void removeQuery(RequestContext requestContext, Map<String, String> policyAttrib) {
        String queryName = policyAttrib.get("queryParamName");
        requestContext.getQueryParamsToRemove().add(queryName);
    }

    private void removeAllQueries(RequestContext requestContext, Map<String, String> policyAttrib) {
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

    private boolean opaAuthValidation(RequestContext requestContext, Map<String, String> policyAttrib) {
        try {
            boolean isValid = OPAClient.getInstance().validateRequest(requestContext, policyAttrib);
            if (!isValid) {
                log.error("OPA validation failed for the request: " + requestContext.getRequestPath());
                FilterUtils.setErrorToContext(requestContext,
                        APISecurityConstants.REMOTE_AUTHORIZATION_AUTH_FORBIDDEN,
                        APIConstants.StatusCodes.UNAUTHORIZED.getCode(),
                        "Request authorization failure at the remote authorization server");
            }
            return isValid;
        } catch (OPASecurityException e) {
            log.error("Error while validating the OPA policy for the request: {}", requestContext.getRequestPath(), e);
            FilterUtils.setErrorToContext(requestContext, e);
            return false;
        }
    }
}
