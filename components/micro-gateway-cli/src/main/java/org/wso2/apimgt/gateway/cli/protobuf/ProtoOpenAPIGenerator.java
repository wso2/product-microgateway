package org.wso2.apimgt.gateway.cli.protobuf;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.apache.commons.lang3.StringUtils;
import org.wso2.apimgt.gateway.cli.constants.OpenAPIConstants;
import org.wso2.apimgt.gateway.cli.exception.CLIRuntimeException;
import org.wso2.apimgt.gateway.cli.model.route.EndpointListRouteDTO;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ProtoOpenAPIGenerator {
    public static final String OAUTH2_SCHEME = "grpc-oauth2-scheme";
    public static final String BASIC_SCHEME = "grpc-basic-scheme";
    private boolean isBasicAuthEnabled = false;
    private boolean isOauth2Enabled = false;
    private boolean isSecurityDisabled = false;
    private boolean endpointsAvailable = false;
    private OpenAPI openAPI;

    public ProtoOpenAPIGenerator(){
        openAPI = new OpenAPI();
    }

    //todo: bring enum for security, if so needs to modify in the openAPI related process as well.
    public void addOpenAPIInfo(String name, String version) {
        Info info = new Info();
        info.setTitle(name);
        info.setVersion(version);
        openAPI.setInfo(info);
        openAPI.addExtension(OpenAPIConstants.BASEPATH, name);
    }

    public void addOpenAPIPath(String path, String[] scopes, String throttling_tier) {
        PathItem pathItem = new PathItem();
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        addOauth2SecurityRequirement(operation, scopes);
        addBasicAuthSecurityRequirement(operation);

        if(StringUtils.isNotEmpty(throttling_tier)){
            operation.addExtension(OpenAPIConstants.THROTTLING_TIER, throttling_tier);
        }

        pathItem.setPost(operation);
        openAPI.setPaths(new Paths().addPathItem(path, pathItem));
    }

    public void addAPIProdEpExtension(EndpointListRouteDTO endpointListRouteDTO) {
        if(endpointListRouteDTO == null){
            return;
        }
        openAPI.addExtension(OpenAPIConstants.PRODUCTION_ENDPOINTS, endpointListRouteDTO);
        endpointsAvailable = true;
    }

    public void addAPISandEpExtension(EndpointListRouteDTO endpointListRouteDTO) {
        if(endpointListRouteDTO == null){
            return;
        }
        openAPI.addExtension(OpenAPIConstants.SANDBOX_ENDPOINTS, endpointListRouteDTO);
        endpointsAvailable = true;
    }

    private void addOauth2SecurityScheme() {
        OAuthFlow flowObj = new OAuthFlow();
        //todo: fix this dummy value to something meaningful
        flowObj.setAuthorizationUrl("http://dummmyVal.com");
        flowObj.setScopes(new Scopes());
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.OAUTH2);
        //todo: the dummy flow object is added as an "implicit" object
        scheme.setFlows(new OAuthFlows().implicit(flowObj));
        openAPI.setComponents(new Components().addSecuritySchemes(OAUTH2_SCHEME, scheme));
        isOauth2Enabled = true;
    }

    private void addBasicSecurityScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.setType(SecurityScheme.Type.HTTP);
        scheme.setScheme("basic");
        openAPI.setComponents(new Components().addSecuritySchemes(BASIC_SCHEME, scheme));
        isBasicAuthEnabled = false;
    }

    private void addScopeToSchema(String scope) {
        if(StringUtils.isEmpty(scope)){
            return;
        }
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(OAUTH2_SCHEME);
        scheme.getFlows().getImplicit().setScopes(new Scopes().addString(scope, ""));
    }

    //it is required to happen updating the security schema and creating the security requirement at the same moment
    //as we do not know about the available scopes until the end
    private void addOauth2SecurityRequirement(Operation operation, String[] scopes) {
        if(!isOauth2Enabled){
            throw new RuntimeException("Scopes cannot be added if \"oauth2\" is not provided as security type.");
        }
        SecurityRequirement oauth2Req = new SecurityRequirement();
        if(scopes != null){
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

    private void addBasicAuthSecurityRequirement(Operation operation) {
        if(!isBasicAuthEnabled){
            return;
        }
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

    public void addAPIOauth2SecurityRequirement(){
        addOauth2SecurityScheme();
        addOauth2SecurityRequirement(null, null);
    }

    public void addAPIBasicSecurityRequirement(){
        addBasicSecurityScheme();
        addBasicAuthSecurityRequirement(null);
    }

    public void disableAPISecurity(){
        openAPI.addExtension(OpenAPIConstants.DISABLE_SECURITY, true);
        isSecurityDisabled = true;
        checkSecurityTypeIncompatibility();
    }

    private void checkSecurityTypeIncompatibility(){
        if((isOauth2Enabled || isBasicAuthEnabled) && isSecurityDisabled) {
            throw new RuntimeException("\"None\" security type is incompatible with other security types.");
        }
    }

    public void setAPIThrottlingTier(String throttlingTier){
        if(StringUtils.isEmpty(throttlingTier)){
            return;
        }
        openAPI.addExtension(OpenAPIConstants.THROTTLING_TIER, throttlingTier);
    }

    private void checkEndpointAvailability(){
        if(!endpointsAvailable){
            throw new CLIRuntimeException("No endpoints provided for the service");
        }
    }

    public OpenAPI getOpenAPI(){
        checkEndpointAvailability();
        checkSecurityTypeIncompatibility();
        //todo: do the validation here
        return openAPI;
    }
}
