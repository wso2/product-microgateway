package org.wso2.apimgt.gateway.cli.model.rest.ext;

import org.wso2.apimgt.gateway.cli.model.config.ApplicationSecurity;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.apim3x.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APIBusinessInformationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APIMaxTpsDTO;

import java.util.List;

/**
 * Wrapper object for ExtendedAPI to maintain backward compatibility with previous versions
 */
public class ExtendedAPIWrapper {
    private String id;
    private String name;
    private String description;
    private String context;
    private String version;
    private String provider;
    private Integer cacheTimeout;
    private Boolean isDefaultVersion;
    private List<String> transport;
    private List<String> tags;
    private String authorizationHeader;
    private APIMaxTpsDTO maxTps;
    private List<String> visibleRoles;
    private List<String> visibleTenants;
    private List<String> subscriptionAvailableTenants;
    private List<String> accessControlRoles;
    private APIBusinessInformationDTO businessInformation;
    private APICorsConfigurationDTO corsConfiguration;
    private String apiDefinition;
    //API Level endpoint configuration
    private MgwEndpointConfigDTO endpointConfigRepresentation;
    //Basepath
    private String specificBasepath;
    //Security
    private String mgwApiSecurity;
    //Scopes
    private String mgwApiScope;
    //isGrpc
    private boolean isGrpc;

    //support APIM application level security
    private ApplicationSecurity applicationSecurity;
    //support APIM transport level security
    private String mutualSSL;
    private String apiLevelPolicy;
    private String endpointConfigStr;
    private String responseCaching;
    private APIEndpointSecurityDTO endpointSecurity;

    public ExtendedAPIWrapper(ExtendedAPI extendedAPI) {
        this.id = extendedAPI.getApiInfo().getId();
        this.name = extendedAPI.getApiInfo().getName();
        this.description = extendedAPI.getApiInfo().getDescription();
        this.context = extendedAPI.getApiInfo().getContext();
        this.version = extendedAPI.getApiInfo().getVersion();
        this.provider = extendedAPI.getApiInfo().getProvider();
        this.cacheTimeout = extendedAPI.getApiInfo().getCacheTimeout();
        this.isDefaultVersion = extendedAPI.getApiInfo().getIsDefaultVersion();
        this.transport = extendedAPI.getApiInfo().getTransport();
        this.tags = extendedAPI.getApiInfo().getTags();
        this.authorizationHeader = extendedAPI.getApiInfo().getAuthorizationHeader();
        this.maxTps = extendedAPI.getApiInfo().getMaxTps();
        this.visibleRoles = extendedAPI.getApiInfo().getVisibleRoles();
        this.visibleTenants = extendedAPI.getApiInfo().getVisibleTenants();
        this.subscriptionAvailableTenants = extendedAPI.getApiInfo().getSubscriptionAvailableTenants();
        this.accessControlRoles = extendedAPI.getApiInfo().getAccessControlRoles();
        this.businessInformation = extendedAPI.getApiInfo().getBusinessInformation();
        this.corsConfiguration = extendedAPI.getApiInfo().getCorsConfiguration();
        this.apiDefinition = extendedAPI.getApiInfo().getApiDefinition();
        this.endpointConfigRepresentation = extendedAPI.getEndpointConfigRepresentation();
        this.specificBasepath = extendedAPI.getSpecificBasepath();
        this.mgwApiSecurity = extendedAPI.getMgwApiSecurity();
        this.mgwApiScope = extendedAPI.getMgwApiScope();
        this.isGrpc = extendedAPI.isGrpc();
        this.applicationSecurity = extendedAPI.getApplicationSecurity();
        this.mutualSSL = extendedAPI.getMutualSSL();
        this.apiLevelPolicy = extendedAPI.getApiInfo().getApiLevelPolicy();
        this.endpointConfigStr = extendedAPI.getApiInfo().getEndpointConfigStr();
        this.responseCaching = extendedAPI.getApiInfo().getResponseCaching();
        this.endpointSecurity = extendedAPI.getApiInfo().getEndpointSecurity();
    }

    /**
     * UUID of the api registry artifact.
     **/
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Name of the APIDetailedDTO.
     **/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A brief description about the APIDetailedDTO.
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * A string that represents the context of the user's request.
     **/
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * The version of the APIDetailedDTO.
     **/
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * If the provider value is not given, the user invoking the APIDetailedDTO will be used as the provider.
     **/
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(Integer cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public Boolean getIsDefaultVersion() {
        return isDefaultVersion;
    }

    public void setDefaultVersion(Boolean defaultVersion) {
        isDefaultVersion = defaultVersion;
    }

    public List<String> getTransport() {
        return transport;
    }

    public void setTransport(List<String> transport) {
        this.transport = transport;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * * The authorization header of the API.
     **/
    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public APIMaxTpsDTO getMaxTps() {
        return maxTps;
    }

    public void setMaxTps(APIMaxTpsDTO maxTps) {
        this.maxTps = maxTps;
    }

    /**
     * The user roles that are able to access the APIDetailedDTO.
     **/
    public List<String> getVisibleRoles() {
        return visibleRoles;
    }

    public void setVisibleRoles(List<String> visibleRoles) {
        this.visibleRoles = visibleRoles;
    }

    public List<String> getVisibleTenants() {
        return visibleTenants;
    }

    public void setVisibleTenants(List<String> visibleTenants) {
        this.visibleTenants = visibleTenants;
    }

    public List<String> getSubscriptionAvailableTenants() {
        return subscriptionAvailableTenants;
    }

    public void setSubscriptionAvailableTenants(List<String> subscriptionAvailableTenants) {
        this.subscriptionAvailableTenants = subscriptionAvailableTenants;
    }

    /**
     * The user roles that are able to view/modify as APIDetailedDTO publisher or creator.
     **/
    public List<String> getAccessControlRoles() {
        return accessControlRoles;
    }

    public void setAccessControlRoles(List<String> accessControlRoles) {
        this.accessControlRoles = accessControlRoles;
    }

    public APIBusinessInformationDTO getBusinessInformation() {
        return businessInformation;
    }

    public void setBusinessInformation(APIBusinessInformationDTO businessInformation) {
        this.businessInformation = businessInformation;
    }

    public APICorsConfigurationDTO getCorsConfiguration() {
        return corsConfiguration;
    }

    public void setCorsConfiguration(APICorsConfigurationDTO corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    // These following methods are added to make the 4.x models compatible with the 3.x models

    /**
     * Swagger definition of the APIDetailedDTO which contains details about URI templates and scopes.
     **/
    public String getApiDefinition() {
        return apiDefinition;
    }

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getApiLevelPolicy() {
        return apiLevelPolicy;
    }

    public void setApiLevelPolicy(String apiLevelPolicy) {
        this.apiLevelPolicy = apiLevelPolicy;
    }

    public String getResponseCaching() {
        return responseCaching;
    }

    public void setResponseCaching(String responseCaching) {
        this.responseCaching = responseCaching;
    }

    //TODO: Fix the endpoint config and endpoint security to be compatible with both 3.x and 4.x models
    public String getEndpointConfigStr() {
        return endpointConfigStr;
    }

    public void setEndpointConfigStr(String endpointConfig) {
        this.endpointConfigStr = endpointConfig;
    }

    public APIEndpointSecurityDTO getEndpointSecurity() {
        return endpointSecurity;
    }

    public void setEndpointSecurity(APIEndpointSecurityDTO endpointSecurity) {
        this.endpointSecurity = endpointSecurity;
    }

    public MgwEndpointConfigDTO getEndpointConfigRepresentation() {
        return endpointConfigRepresentation;
    }

    public void setEndpointConfigRepresentation(MgwEndpointConfigDTO endpointConfigRepresentation) {
        this.endpointConfigRepresentation = endpointConfigRepresentation;
    }

    public String getSpecificBasepath() {
        return specificBasepath;
    }

    public void setSpecificBasepath(String specificBasepath) {
        this.specificBasepath = specificBasepath;
    }

    public String getMgwApiSecurity() {
        return mgwApiSecurity;
    }

    public void setMgwApiSecurity(String mgwApiSecurity) {
        this.mgwApiSecurity = mgwApiSecurity;
    }

    public void setMgwApiScope(String mgwApiScope) {
        this.mgwApiScope = mgwApiScope;
    }

    public String getMgwApiScope() {
        return mgwApiScope;
    }

    public boolean isGrpc() {
        return isGrpc;
    }

    public void setGrpc(boolean grpc) {
        isGrpc = grpc;
    }

    public void setApplicationSecurity(ApplicationSecurity applicationSecurity) {
        this.applicationSecurity = applicationSecurity;
    }

    public ApplicationSecurity getApplicationSecurity() {
        return applicationSecurity;
    }

    public String getMutualSSL() {
        return mutualSSL;
    }

    public void setMutualSSL(String mutualSSL) {
        this.mutualSSL = mutualSSL;
    }
}
