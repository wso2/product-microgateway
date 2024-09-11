package org.wso2.apimgt.gateway.cli.model.rest.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.hashing.Hash;
import org.wso2.apimgt.gateway.cli.model.config.ApplicationSecurity;
import org.wso2.apimgt.gateway.cli.model.mgwcodegen.MgwEndpointConfigDTO;
import org.wso2.apimgt.gateway.cli.model.rest.apim3x.APIEndpointSecurityDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APIBusinessInformationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APICorsConfigurationDTO;
import org.wso2.apimgt.gateway.cli.model.rest.common.APIMaxTpsDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper object for ExtendedAPI to maintain backward compatibility with previous versions
 */
public class ExtendedAPIWrapper {
    private String id = null;
    private String name = null;
    private String description = null;
    private String context = null;
    private String version = null;
    private String provider = null;
    private Integer cacheTimeout = null;
    private Boolean isDefaultVersion = null;
    private List<String> transport = new ArrayList<String>();
    private List<String> tags = new ArrayList<String>();
    private String authorizationHeader = null;
    private APIMaxTpsDTO maxTps = null;
    private List<String> visibleRoles = new ArrayList<String>();
    private List<String> visibleTenants = new ArrayList<String>();
    private List<String> subscriptionAvailableTenants = new ArrayList<String>();
    private List<String> accessControlRoles = new ArrayList<String>();
    private APIBusinessInformationDTO businessInformation = null;
    private APICorsConfigurationDTO corsConfiguration = null;
    private String apiDefinition = null;
    //API Level endpoint configuration
    private MgwEndpointConfigDTO endpointConfigRepresentation = null;
    //Basepath
    private String specificBasepath = null;
    //Security
    private String mgwApiSecurity = null;
    //Scopes
    private String mgwApiScope = null;
    //isGrpc
    private boolean isGrpc = false;

    //support apim application level security
    private ApplicationSecurity applicationSecurity = null;
    //support apim transport level security
    private String mutualSSL = null;

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
    }

    /**
     * UUID of the api registry artifact.
     **/
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Name of the APIDetailedDTO.
     **/
    @Hash
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A brief description about the APIDetailedDTO.
     **/
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * A string that represents the context of the user's request.
     **/
    @Hash
    @JsonProperty("context")
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    /**
     * The version of the APIDetailedDTO.
     **/
    @Hash
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * If the provider value is not given, the user invoking the APIDetailedDTO will be used as the provider.
     **/
    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getCacheTimeout() {
        return cacheTimeout;
    }

    @Hash
    @JsonProperty("cacheTimeout")
    public void setCacheTimeout(Integer cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    @Hash
    @JsonProperty("isDefaultVersion")
    public Boolean getIsDefaultVersion() {
        return isDefaultVersion;
    }

    public void setDefaultVersion(Boolean defaultVersion) {
        isDefaultVersion = defaultVersion;
    }

    /**
     * Supported transports for the APIDetailedDTO (http and/or https).
     **/
    @Hash
    @JsonProperty("transport")
    public List<String> getTransport() {
        return transport;
    }

    public void setTransport(List<String> transport) {
        this.transport = transport;
    }

    /**
     * Search keywords related to the APIDetailedDTO.
     **/
    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * * The authorization header of the API.
     **/
    @Hash
    @JsonProperty("authorizationHeader")
    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    @JsonProperty("maxTps")
    public APIMaxTpsDTO getMaxTps() {
        return maxTps;
    }

    public void setMaxTps(APIMaxTpsDTO maxTps) {
        this.maxTps = maxTps;
    }

    /**
     * The user roles that are able to access the APIDetailedDTO.
     **/
    @JsonProperty("visibleRoles")
    public List<String> getVisibleRoles() {
        return visibleRoles;
    }

    public void setVisibleRoles(List<String> visibleRoles) {
        this.visibleRoles = visibleRoles;
    }

    @JsonProperty("visibleTenants")
    public List<String> getVisibleTenants() {
        return visibleTenants;
    }

    public void setVisibleTenants(List<String> visibleTenants) {
        this.visibleTenants = visibleTenants;
    }

    @JsonProperty("subscriptionAvailableTenants")
    public List<String> getSubscriptionAvailableTenants() {
        return subscriptionAvailableTenants;
    }

    public void setSubscriptionAvailableTenants(List<String> subscriptionAvailableTenants) {
        this.subscriptionAvailableTenants = subscriptionAvailableTenants;
    }

    /**
     * The user roles that are able to view/modify as APIDetailedDTO publisher or creator.
     **/
    @JsonProperty("accessControlRoles")
    public List<String> getAccessControlRoles() {
        return accessControlRoles;
    }

    public void setAccessControlRoles(List<String> accessControlRoles) {
        this.accessControlRoles = accessControlRoles;
    }

    @JsonProperty("businessInformation")
    public APIBusinessInformationDTO getBusinessInformation() {
        return businessInformation;
    }

    public void setBusinessInformation(APIBusinessInformationDTO businessInformation) {
        this.businessInformation = businessInformation;
    }

    @Hash
    @JsonProperty("corsConfiguration")
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
    @Hash
    @JsonProperty("apiDefinition")
    public String getApiDefinition() {
        return apiDefinition;
    }

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }

    public String getApiLevelPolicy() {
        return null;
    }

    public void setApiLevelPolicy(String apiLevelPolicy) {
    }

    public String getResponseCaching() {
        return null;
    }

    public void setResponseCaching(String responseCaching) {
    }

    //TODO: Fix the endpoint config and endpoint security to be compatible with both 3.x and 4.x models
    public String getEndpointConfigStr() {
        return null;
    }

    public void setEndpointConfigStr(String endpointConfig) {
    }

    public APIEndpointSecurityDTO getEndpointSecurity() {
        return null;
    }

    public void setEndpointSecurity(APIEndpointSecurityDTO endpointSecurity) {
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
