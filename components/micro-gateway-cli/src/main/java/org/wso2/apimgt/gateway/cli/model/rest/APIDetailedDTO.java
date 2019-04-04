/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonSyntaxException;
import io.swagger.annotations.ApiModel;
import io.swagger.util.Json;
import org.wso2.apimgt.gateway.cli.hashing.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "")
public class APIDetailedDTO extends APIInfoDTO {

    private String apiDefinition = null;
    private String wsdlUri = null;
    private String responseCaching = null;
    private Integer cacheTimeout = null;
    private String destinationStatsEnabled = null;
    private Boolean isDefaultVersion = null;
    private Json apiSwagger = null;

    public enum TypeEnum {
        HTTP, WS, SOAPTOREST,
    }

    ;

    private TypeEnum type = TypeEnum.HTTP;
    private List<String> transport = new ArrayList<String>();
    private List<String> tags = new ArrayList<String>();
    private List<String> tiers = new ArrayList<String>();
    private String apiLevelPolicy = null;
    private String authorizationHeader = null;
    private String apiSecurity = null;
    private APIMaxTpsDTO maxTps = null;

    public enum VisibilityEnum {
        PUBLIC, PRIVATE, RESTRICTED, CONTROLLED,
    }

    ;

    private VisibilityEnum visibility = null;
    private List<String> visibleRoles = new ArrayList<String>();
    private List<String> visibleTenants = new ArrayList<String>();
    private String endpointConfig = null;
    private APIEndpointSecurityDTO endpointSecurity = null;
    private String gatewayEnvironments = null;
    private List<LabelDTO> labels = new ArrayList<LabelDTO>();
    private List<SequenceDTO> sequences = new ArrayList<SequenceDTO>();

    public enum SubscriptionAvailabilityEnum {
        current_tenant, all_tenants, specific_tenants,
    }

    ;

    private SubscriptionAvailabilityEnum subscriptionAvailability = null;
    private List<String> subscriptionAvailableTenants = new ArrayList<String>();
    private Map<String, String> additionalProperties = new HashMap<String, String>();

    public enum AccessControlEnum {
        NONE, RESTRICTED,
    }

    ;

    private AccessControlEnum accessControl = null;
    private List<String> accessControlRoles = new ArrayList<String>();
    private APIBusinessInformationDTO businessInformation = null;
    private APICorsConfigurationDTO corsConfiguration = null;

    /**
     * Swagger definition of the APIDetailedDTO which contains details about URI templates and scopes\n
     **/
    @Hash
    @JsonProperty("apiDefinition")
    public String getApiDefinition() {
        return apiDefinition;
    }

    public void setApiDefinition(String apiDefinition) {
        this.apiDefinition = apiDefinition;
    }


    /**
     * WSDL URL if the APIDetailedDTO is based on a WSDL endpoint\n
     **/
    @JsonProperty("wsdlUri")
    public String getWsdlUri() {
        return wsdlUri;
    }

    public void setWsdlUri(String wsdlUri) {
        this.wsdlUri = wsdlUri;
    }

    @Hash
    @JsonProperty("responseCaching")
    public String getResponseCaching() {
        return responseCaching;
    }

    public void setResponseCaching(String responseCaching) {
        this.responseCaching = responseCaching;
    }

    @Hash
    @JsonProperty("cacheTimeout")
    public Integer getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(Integer cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }


    @JsonProperty("destinationStatsEnabled")
    public String getDestinationStatsEnabled() {
        return destinationStatsEnabled;
    }

    public void setDestinationStatsEnabled(String destinationStatsEnabled) {
        this.destinationStatsEnabled = destinationStatsEnabled;
    }

    @Hash
    @JsonProperty("isDefaultVersion")
    public Boolean getIsDefaultVersion() {
        return isDefaultVersion;
    }

    public void setIsDefaultVersion(Boolean isDefaultVersion) {
        this.isDefaultVersion = isDefaultVersion;
    }


    /**
     * The transport to be set. Accepted values are HTTP, WS
     **/
    @Hash
    @JsonProperty("type")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }


    /**
     * Supported transports for the APIDetailedDTO (http and/or https).\n
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
     * Search keywords related to the APIDetailedDTO
     **/
    @JsonProperty("tags")
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }


    /**
     * The subscription tiers selected for the particular APIDetailedDTO
     **/
    @Hash
    @JsonProperty("tiers")
    public List<String> getTiers() {
        return tiers;
    }

    public void setTiers(List<String> tiers) {
        this.tiers = tiers;
    }


    /**
     * The policy selected for the particular APIDetailedDTO
     **/
    @JsonProperty("apiLevelPolicy")
    public String getApiLevelPolicy() {
        return apiLevelPolicy;
    }

    public void setApiLevelPolicy(String apiLevelPolicy) {
        this.apiLevelPolicy = apiLevelPolicy;
    }

    /**
     * * The authorization header of the API
     **/
    @Hash
    @JsonProperty("authorizationHeader")
    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    /**
     * Type of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both.
     * If it is not set OAuth2 will be set as the security for the current API.\n
     **/
    @JsonProperty("apiSecurity")
    public String getApiSecurity() {
        return apiSecurity;
    }

    public void setApiSecurity(String apiSecurity) {
        this.apiSecurity = apiSecurity;
    }

    @JsonProperty("maxTps")
    public APIMaxTpsDTO getMaxTps() {
        return maxTps;
    }

    public void setMaxTps(APIMaxTpsDTO maxTps) {
        this.maxTps = maxTps;
    }

    /**
     * The visibility level of the APIDetailedDTO. Accepts one of the following. PUBLIC, PRIVATE,
     * RESTRICTED OR CONTROLLED.
     **/
    @JsonProperty("visibility")
    public VisibilityEnum getVisibility() {
        return visibility;
    }

    public void setVisibility(VisibilityEnum visibility) {
        this.visibility = visibility;
    }


    /**
     * The user roles that are able to access the APIDetailedDTO
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

    @Hash
    @JsonProperty("endpointConfig")
    public String getEndpointConfig() {
        return endpointConfig;
    }

    public void setEndpointConfig(String endpointConfig) {
        this.endpointConfig = endpointConfig;
    }

    @Hash
    @JsonProperty("endpointSecurity")
    public APIEndpointSecurityDTO getEndpointSecurity() {
        return endpointSecurity;
    }

    public void setEndpointSecurity(APIEndpointSecurityDTO endpointSecurity) {
        this.endpointSecurity = endpointSecurity;
    }

    /**
     * Comma separated list of gateway environments.\n
     **/
    @JsonProperty("gatewayEnvironments")
    public String getGatewayEnvironments() {
        return gatewayEnvironments;
    }

    public void setGatewayEnvironments(String gatewayEnvironments) {
        this.gatewayEnvironments = gatewayEnvironments;
    }

    /**
     * Labels of micro-gateway environments attached to the APIDetailedDTO.\n
     **/
    @JsonProperty("labels")
    public List<LabelDTO> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelDTO> labels) {
        this.labels = labels;
    }

    @JsonProperty("sequences")
    public List<SequenceDTO> getSequences() {
        return sequences;
    }

    public void setSequences(List<SequenceDTO> sequences) {
        this.sequences = sequences;
    }

    /**
     * The subscription availability. Accepts one of the following. current_tenant, all_tenants or specific_tenants.
     **/
    @JsonProperty("subscriptionAvailability")
    public SubscriptionAvailabilityEnum getSubscriptionAvailability() {
        return subscriptionAvailability;
    }

    public void setSubscriptionAvailability(SubscriptionAvailabilityEnum subscriptionAvailability) {
        this.subscriptionAvailability = subscriptionAvailability;
    }

    @JsonProperty("subscriptionAvailableTenants")
    public List<String> getSubscriptionAvailableTenants() {
        return subscriptionAvailableTenants;
    }

    public void setSubscriptionAvailableTenants(List<String> subscriptionAvailableTenants) {
        this.subscriptionAvailableTenants = subscriptionAvailableTenants;
    }

    /**
     * Map of custom properties of APIDetailedDTO
     **/
    @JsonProperty("additionalProperties")
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    /**
     * Is the API is restricted to certain set of publishers or creators or is it visible to all the\npublishers and
     * creators. If the accessControl restriction is none, this API can be modified by all the\npublishers and creators,
     * if not it can only be viewable/modifiable by certain set of publishers and creators,\n based on the restriction.
     * \n
     **/
    @JsonProperty("accessControl")
    public AccessControlEnum getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControlEnum accessControl) {
        this.accessControl = accessControl;
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

    public JsonObject getApiSwagger() {
        String swagger = getApiDefinition();
        JsonParser parser = new JsonParser();
        JsonObject jsonSwagger;
        try {
            jsonSwagger = parser.parse(swagger).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new JsonSyntaxException("Error occured while parsing the swagger to a JsonObject", e);
        }
        return jsonSwagger;
    }

    public void setApiSwagger(Json apiSwagger) {
        this.apiSwagger = apiSwagger;
    }

    public void setCorsConfiguration(APICorsConfigurationDTO corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }
}
