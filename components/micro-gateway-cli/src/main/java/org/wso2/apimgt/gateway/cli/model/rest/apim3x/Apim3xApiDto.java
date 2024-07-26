/*
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.apimgt.gateway.cli.model.rest.apim3x;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.util.Json;
import org.wso2.apimgt.gateway.cli.hashing.Hash;
import org.wso2.apimgt.gateway.cli.model.rest.APIInfoBaseDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for APIM 3.x API model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Apim3xApiDto extends APIInfoBaseDTO {

    private String status = null;
    private String thumbnailUri = null;
    private TypeEnum type = TypeEnum.HTTP;

    /**
     * API Type.
     */
    public enum TypeEnum {
        HTTP, WS, SOAPTOREST,
    }

    /**
     * API Visibility level. Not used in MGW as of 3.0.1.
     */
    public enum VisibilityEnum {
        PUBLIC, PRIVATE, RESTRICTED, CONTROLLED,
    }

    /**
     * Subscription availability in WSO2 APIM.
     */
    public enum SubscriptionAvailabilityEnum {
        current_tenant, all_tenants, specific_tenants,
    }

    /**
     * WSO2 APIM access control level.
     */
    public enum AccessControlEnum {
        NONE, RESTRICTED,
    }

    private String wsdlUri = null;
    private String responseCaching = "Disabled";
    private String destinationStatsEnabled = null;

    @SuppressFBWarnings(value = "URF_UNREAD_FIELD")
    private Json apiSwagger = null;

    private List<String> tiers = new ArrayList<String>();
    private String apiLevelPolicy = null;
    private String apiSecurity = null;
    private VisibilityEnum visibility = null;
    private String endpointConfig = null;
    private APIEndpointSecurityDTO endpointSecurity = null;
    private String gatewayEnvironments = null;
    private List<LabelDTO> labels = new ArrayList<LabelDTO>();
    private List<SequenceDTO> sequences = new ArrayList<SequenceDTO>();
    private SubscriptionAvailabilityEnum subscriptionAvailability = null;
    private Map<String, String> additionalProperties = new HashMap<String, String>();
    private AccessControlEnum accessControl = null;

    /**
     * This describes in which status of the lifecycle the APIDetailedDTO is.
     **/
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("thumbnailUri")
    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
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
     * WSDL URL if the APIDetailedDTO is based on a WSDL endpoint.
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
    @Override
    public String getResponseCaching() {
        return responseCaching;
    }

    @Override
    public void setResponseCaching(String responseCaching) {
        this.responseCaching = responseCaching;
    }

    @JsonProperty("destinationStatsEnabled")
    public String getDestinationStatsEnabled() {
        return destinationStatsEnabled;
    }

    public void setDestinationStatsEnabled(String destinationStatsEnabled) {
        this.destinationStatsEnabled = destinationStatsEnabled;
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

    /**
     * The subscription tiers selected for the particular APIDetailedDTO.
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
     * The policy selected for the particular APIDetailedDTO.
     **/
    @JsonProperty("apiLevelPolicy")
    @Override
    public String getApiLevelPolicy() {
        return apiLevelPolicy;
    }

    @Override
    public void setApiLevelPolicy(String apiLevelPolicy) {
        this.apiLevelPolicy = apiLevelPolicy;
    }

    /**
     * Type of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both.
     * If it is not set OAuth2 will be set as the security for the current API.
     **/
    @JsonProperty("apiSecurity")
    public String getApiSecurity() {
        return apiSecurity;
    }

    public void setApiSecurity(String apiSecurity) {
        this.apiSecurity = apiSecurity;
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

    @Hash
    @JsonProperty("endpointConfig")
    public String getEndpointConfigStr() {
        return endpointConfig;
    }

    public void setEndpointConfigStr(String endpointConfig) {
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
     * Comma separated list of gateway environments.
     **/
    @JsonProperty("gatewayEnvironments")
    public String getGatewayEnvironments() {
        return gatewayEnvironments;
    }

    public void setGatewayEnvironments(String gatewayEnvironments) {
        this.gatewayEnvironments = gatewayEnvironments;
    }

    /**
     * Labels of micro-gateway environments attached to the APIDetailedDTO.
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

    /**
     * Map of custom properties of APIDetailedDTO.
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
}
