package org.wso2.apimgt.gateway.cli.codegen;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.util.Arrays;
import java.util.UUID;

public class ProtoOpenAPIGenerator {
    public static final String OAUTH2_SCHEME = "grpc-oauth2-scheme";
    public static final String BASIC_SCHEME = "grpc-basic-scheme";

    //todo: bring enum for security, if so needs to modify in the openAPI related process as well.
    private void addOpenAPIInfo(OpenAPI openAPI, String name, String version) {
        Info info = new Info();
        info.setTitle(name);
        info.setVersion(version);
        openAPI.setInfo(info);
    }

    private void addOpenAPIPath(OpenAPI openAPI, String path, String[] scopes, String throttling_tier) {
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        addOauth2SecurityRequirement(openAPI, operation, scopes);
        addBasicAuthSecurityRequirement(openAPI, operation);
        operation.addExtension(OpenAPIConstants.THROTTLING_TIER, throttling_tier);
        pathItem.setPost(operation);
        openAPI.setPaths(new Paths().addPathItem(path, pathItem));
    }

    private void addAPIProdEpExtension(OpenAPI openAPI, EndpointListRouteDTO endpointListRouteDTO) {
        openAPI.addExtension(OpenAPIConstants.PRODUCTION_ENDPOINTS, endpointListRouteDTO);
    }

    private void addAPISandEpExtension(OpenAPI openAPI, EndpointListRouteDTO endpointListRouteDTO) {
        openAPI.addExtension(OpenAPIConstants.SANDBOX_ENDPOINTS, endpointListRouteDTO);
    }

    private void addAPISecurity(OpenAPI openAPI, String[] security) {
        //todo:
    }

    private void addOauth2SecurityScheme(OpenAPI openAPI) {
        OAuthFlow flowObj = new OAuthFlow();
        //todo: fix this dummy value to something meaningful
        flowObj.setAuthorizationUrl("http://dummmyVal.com");
        flowObj.setScopes(new Scopes());
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.OAUTH2);
        //todo: the dummy flow object is added as an "implicit" object
        scheme.setFlows(new OAuthFlows().implicit(flowObj));
        openAPI.setComponents(new Components().addSecuritySchemes(OAUTH2_SCHEME, scheme));
    }

    private void addBasicSecurityScheme(OpenAPI openAPI) {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.HTTP);
        scheme.setScheme("basic");
        openAPI.setComponents(new Components().addSecuritySchemes(BASIC_SCHEME, scheme));
    }

    private void addScopeToSchema(OpenAPI openAPI, String scope) {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OAUTH2_SCHEME);
        if (scheme != null) {
            scheme.getFlows().getImplicit().setScopes(new Scopes().addString(scope, ""));
        }
        //todo: proper the error message
        throw new RuntimeException("micro-gw: Scopes cannot be added if \"oauth2\" is not provided as security type.");
    }

    //it is required to happen updating the security schema and creating the security requirement at the same moment
    //as we do not know about the available scopes until the end
    private void addOauth2SecurityRequirement(OpenAPI openAPI, Operation operation, String[] scopes) {
        for (String scope : scopes) {
            addScopeToSchema(openAPI, scope);
        }
        SecurityRequirement oauth2Req = new SecurityRequirement();
        oauth2Req.addList(OAUTH2_SCHEME, Arrays.asList(scopes));

        if (operation == null) {
            openAPI.addSecurityItem(oauth2Req);
        } else {
            operation.addSecurityItem(oauth2Req);
        }
    }

    private void addBasicAuthSecurityRequirement(OpenAPI openAPI, Operation operation) {
        if(openAPI.getComponents().getSecuritySchemes().get(BASIC_SCHEME) != null){
            SecurityRequirement basicAuthReq = new SecurityRequirement();
            basicAuthReq.addList(BASIC_SCHEME);

            if (operation == null){
                openAPI.addSecurityItem(basicAuthReq);
            } else {
                operation.addSecurityItem(basicAuthReq);
            }
        }
    }
}
