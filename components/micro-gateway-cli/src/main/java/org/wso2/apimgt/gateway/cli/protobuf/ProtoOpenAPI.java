/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.protobuf;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.util.Arrays;
import java.util.UUID;

/**
 * custom OpenAPI object for mapping the grpc service definition (protobuf).
 */
public class ProtoOpenAPI {
    private static final Logger logger = LoggerFactory.getLogger(ProtoOpenAPI.class);

    private static final String OAUTH2_SCHEME = "grpc-oauth2-scheme";
    private static final String BASIC_SCHEME = "grpc-basic-scheme";
    private static final String APIKEY_SCHEME = "grpc-apikey-scheme";
    private boolean isBasicAuthEnabled = false;
    private boolean isOauth2Enabled = false;
    private boolean isAPIKeyEnabled = false;
    private boolean isSecurityDisabled = false;
    private boolean endpointsAvailable = false;
    private OpenAPI openAPI;

    ProtoOpenAPI() {
        openAPI = new OpenAPI();
    }

    /**
     * Add the minimal information required for OpenAPI Info segment.
     * The same name is assigned as the basePath.
     *
     * @param name API name
     */
    void addOpenAPIInfo(String name) {
        Info info = new Info();
        info.setTitle(name);
        //todo: decide if we bring versioning into this.
        //version is set to 1.0.0 as default.
        info.setVersion("1.0.0");
        openAPI.setInfo(info);
        //todo: remove if basePath is not mandatory
        openAPI.addExtension(OpenAPIConstants.BASEPATH, "/" + name);
    }

    /**
     * Add openAPI path item to the the openAPI object.
     *
     * @param path            name of the pathItem
     * @param scopes          array of operation scopes
     * @param throttlingTier throttling tier
     */
    void addOpenAPIPath(String path, String[] scopes, String throttlingTier) {
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        addOauth2SecurityRequirement(operation, scopes);
        if (StringUtils.isNotEmpty(throttlingTier)) {
            operation.addExtension(OpenAPIConstants.THROTTLING_TIER, throttlingTier);
        }
        //For each path, the only available http method is "post" according to the grpc mapping.
        pathItem.setPost(operation);
        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new Paths());
        }
        openAPI.getPaths().addPathItem(path, pathItem);
    }

    /**
     * Add API Level production endpoints to the openAPI object.
     * If the provided {@link EndpointListRouteDTO} object is null, nothing will be added to the openAPI object.
     *
     * @param endpointListRouteDTO {@link EndpointListRouteDTO} object representing the endpoint configuration.
     */
    void addAPIProdEpExtension(EndpointListRouteDTO endpointListRouteDTO) {
        if (endpointListRouteDTO == null) {
            return;
        }
        openAPI.addExtension(OpenAPIConstants.PRODUCTION_ENDPOINTS, endpointListRouteDTO);
        endpointsAvailable = true;
    }

    /**
     * Add API Level sandbox endpoints to the openAPI object.
     * If the provided {@link EndpointListRouteDTO} object is null, nothing will be added to the openAPI object.
     *
     * @param endpointListRouteDTO {@link EndpointListRouteDTO} object representing the endpoint configuration.
     */
    void addAPISandEpExtension(EndpointListRouteDTO endpointListRouteDTO) {
        if (endpointListRouteDTO == null) {
            return;
        }
        openAPI.addExtension(OpenAPIConstants.SANDBOX_ENDPOINTS, endpointListRouteDTO);
        endpointsAvailable = true;
    }

    /**
     * Add Oauth2 security scheme to the openAPI object.
     */
    private void addOauth2SecurityScheme() {
        OAuthFlow flowObj = new OAuthFlow();
        //todo: fix this dummy value to something meaningful
        flowObj.setAuthorizationUrl("http://dummmyVal.com");
        flowObj.setScopes(new Scopes());
        SecurityScheme scheme;
        scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.OAUTH2);
        scheme.setFlows(new OAuthFlows().implicit(flowObj));
        openAPI.setComponents(new Components().addSecuritySchemes(OAUTH2_SCHEME, scheme));
        isOauth2Enabled = true;
    }

    /**
     * Add Basic security scheme to the openAPI object.
     */
    private void addBasicSecurityScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.HTTP);
        scheme.setScheme("basic");
        openAPI.setComponents(new Components().addSecuritySchemes(BASIC_SCHEME, scheme));
        isBasicAuthEnabled = true;
    }

    /*
     * Add APIKey security scheme to the openAPI object.
     */
    private void addAPIKeySecurityScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.APIKEY);
        scheme.setName("apikey");
        scheme.setIn(SecurityScheme.In.HEADER);
        openAPI.setComponents(new Components().addSecuritySchemes(APIKEY_SCHEME, scheme));
        isAPIKeyEnabled = true;
    }

    /**
     * Add scopes to the security schema.
     *
     * @param scope scope
     */
    private void addScopeToSchema(String scope) {
        if (StringUtils.isEmpty(scope)) {
            return;
        }
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OAUTH2_SCHEME);
        if (!scheme.getFlows().getImplicit().getScopes().containsKey(scope)) {
            //scopes description is set as a null string
            scheme.getFlows().getImplicit().setScopes(new Scopes().addString(scope, ""));
        }
    }

    /**
     * Add Oauth2 security requirement to the operation/API.
     * If {@link Operation} object is null, security requirement is added to the API.
     *
     * @param operation {@link Operation} object
     * @param scopes    array of scopes
     */
    private void addOauth2SecurityRequirement(Operation operation, String[] scopes) {
        //if Oauth2 is not available as a security scheme, adding scopes would be meaningless.
        if (!isOauth2Enabled) {
            if (scopes != null && scopes.length > 0 && !scopes[0].isEmpty()) {
                throw new CLIRuntimeException("Scopes cannot be added if \"oauth2\" is not provided as security type.");
            }
        }
        SecurityRequirement oauth2Req = new SecurityRequirement();
        //Since the scopes are not known at the start, the security scheme should be updated with newly identified
        //scopes as proceed
        if (scopes != null) {
            for (String scope : scopes) {
                addScopeToSchema(scope);
            }
            oauth2Req.addList(OAUTH2_SCHEME, Arrays.asList(scopes));
        } else {
            oauth2Req.addList(OAUTH2_SCHEME);
        }
        if (operation == null) {
            openAPI.addSecurityItem(oauth2Req);
        } else {
            operation.addSecurityItem(oauth2Req);
        }
    }

    //todo: operation level security is not considered. Hence remove the unnecessary code
    /**
     * Add Basic Auth security requirement to the operation/API.
     * If {@link Operation} object is null, security requirement is added to the API.
     *
     * @param operation {@link Operation} object
     */
    private void addBasicAuthSecurityRequirement(Operation operation) {
        if (!isBasicAuthEnabled) {
            return;
        }
        if (openAPI.getComponents().getSecuritySchemes().get(BASIC_SCHEME) != null) {
            SecurityRequirement basicAuthReq = new SecurityRequirement();
            basicAuthReq.addList(BASIC_SCHEME);

            if (operation == null) {
                openAPI.addSecurityItem(basicAuthReq);
            } else {
                operation.addSecurityItem(basicAuthReq);
            }
        }
    }

    //todo: remove the redundant work as operation level security is not considered
    /**
     * Add APIKey security requirement to the operation/API.
     * If {@link Operation} object is null, security requirement is added to the API.
     *
     * @param operation {@link Operation} object
     */
    private void addAPIKeySecurityRequirement(Operation operation) {
        if (!isAPIKeyEnabled) {
            return;
        }
        if (openAPI.getComponents().getSecuritySchemes().get(APIKEY_SCHEME) != null) {
            SecurityRequirement apikeyReq = new SecurityRequirement();
            apikeyReq.addList(APIKEY_SCHEME);

            if (operation == null) {
                openAPI.addSecurityItem(apikeyReq);
            } else {
                operation.addSecurityItem(apikeyReq);
            }
        }
    }

    /**
     * Add Oauth2 security requirement to the API level.
     */
    void addAPIOauth2SecurityRequirement() {
        addOauth2SecurityScheme();
        addOauth2SecurityRequirement(null, null);
    }

    /**
     * Add Basic Auth security requirement to the API level.
     */
    void addAPIBasicSecurityRequirement() {
        addBasicSecurityScheme();
        addBasicAuthSecurityRequirement(null);
    }

    /**
     * Add API-KEY security requirement to the API level
     */
    void addAPIKeySecurityRequirement() {
        addAPIKeySecurityScheme();
        addAPIKeySecurityRequirement(null);
    }

    /**
     * Disable API security.
     */
    void disableAPISecurity() {
        openAPI.addExtension(OpenAPIConstants.DISABLE_SECURITY, true);
        isSecurityDisabled = true;
    }

    private void checkSecurityTypeIncompatibility(String service) {
        //if security types are defined with disabled security option, throw an error to indicate incompatibility.
        if ((isOauth2Enabled || isBasicAuthEnabled || isAPIKeyEnabled) && isSecurityDisabled) {
            throw new CLIRuntimeException("\"None\" security type is incompatible with other security types. " +
                    "service : \"" + service + "\".");
        }
    }

    /**
     * Set API level throttling tier.
     *
     * @param throttlingTier throttling-tier as mentioned in the policies.yaml
     */
    void setAPIThrottlingTier(String throttlingTier) {
        if (StringUtils.isEmpty(throttlingTier)) {
            return;
        }
        openAPI.addExtension(OpenAPIConstants.THROTTLING_TIER, throttlingTier);
    }

    private void checkEndpointAvailability(String service) {
        //if no endpoints are available, throw an error.
        if (!endpointsAvailable) {
            throw new CLIRuntimeException("No endpoints provided in the service : \"" + service + "\".");
        }
    }

    /**
     * Return the validated openAPI object.
     *
     * @param service gRPC service name
     * @return {@link OpenAPI} object
     */
    OpenAPI getOpenAPI(String service) {
        checkEndpointAvailability(service);
        checkSecurityTypeIncompatibility(service);
        logger.debug("Corresponding openAPI is successfully generated. \n" + openAPI.toString());
        return openAPI;
    }
}
