package org.wso2.apimgt.gateway.codegen.service.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.*;

@ApiModel(description = "")
public class APIDetailedDTO extends APIInfoDTO {



  private String apiDefinition = null;


  private String wsdlUri = null;


  private String responseCaching = null;


  private Integer cacheTimeout = null;


  private String destinationStatsEnabled = null;


  private Boolean isDefaultVersion = null;

  public enum TypeEnum {
    HTTP,  WS,
  };

  private TypeEnum type = TypeEnum.HTTP;


  private List<String> transport = new ArrayList<String>();


  private List<String> tags = new ArrayList<String>();


  private List<String> tiers = new ArrayList<String>();


  private String apiLevelPolicy = null;


  private APIMaxTpsDTO maxTps = null;

  public enum VisibilityEnum {
    PUBLIC,  PRIVATE,  RESTRICTED,  CONTROLLED,
  };

  private VisibilityEnum visibility = null;


  private List<String> visibleRoles = new ArrayList<String>();


  private List<String> visibleTenants = new ArrayList<String>();


  private String endpointConfig = null;


  private APIEndpointSecurityDTO endpointSecurity = null;


  private String gatewayEnvironments = null;


  private List<LabelDTO> labels = new ArrayList<LabelDTO>();


  private List<SequenceDTO> sequences = new ArrayList<SequenceDTO>();

  public enum SubscriptionAvailabilityEnum {
    current_tenant,  all_tenants,  specific_tenants,
  };

  private SubscriptionAvailabilityEnum subscriptionAvailability = null;


  private List<String> subscriptionAvailableTenants = new ArrayList<String>();


  private Map<String, String> additionalProperties = new HashMap<String, String>();

  public enum AccessControlEnum {
    NONE,  RESTRICTED,
  };

  private AccessControlEnum accessControl = null;


  private List<String> accessControlRoles = new ArrayList<String>();


  private APIBusinessInformationDTO businessInformation = null;


  private APICorsConfigurationDTO corsConfiguration = null;

  /**
   * Swagger definition of the APIDetailedDTO which contains details about URI templates and scopes\n
   **/
  @ApiModelProperty(value = "Swagger definition of the APIDetailedDTO which contains details about URI templates and scopes\n")
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
  @ApiModelProperty(value = "WSDL URL if the APIDetailedDTO is based on a WSDL endpoint\n")
  @JsonProperty("wsdlUri")
  public String getWsdlUri() {
    return wsdlUri;
  }
  public void setWsdlUri(String wsdlUri) {
    this.wsdlUri = wsdlUri;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("responseCaching")
  public String getResponseCaching() {
    return responseCaching;
  }
  public void setResponseCaching(String responseCaching) {
    this.responseCaching = responseCaching;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("cacheTimeout")
  public Integer getCacheTimeout() {
    return cacheTimeout;
  }
  public void setCacheTimeout(Integer cacheTimeout) {
    this.cacheTimeout = cacheTimeout;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("destinationStatsEnabled")
  public String getDestinationStatsEnabled() {
    return destinationStatsEnabled;
  }
  public void setDestinationStatsEnabled(String destinationStatsEnabled) {
    this.destinationStatsEnabled = destinationStatsEnabled;
  }


  /**
   **/
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "The transport to be set. Accepted values are HTTP, WS")
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
  @ApiModelProperty(value = "Supported transports for the APIDetailedDTO (http and/or https).\n")
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
  @ApiModelProperty(value = "Search keywords related to the APIDetailedDTO")
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
  @ApiModelProperty(value = "The subscription tiers selected for the particular APIDetailedDTO")
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
  @ApiModelProperty(value = "The policy selected for the particular APIDetailedDTO")
  @JsonProperty("apiLevelPolicy")
  public String getApiLevelPolicy() {
    return apiLevelPolicy;
  }
  public void setApiLevelPolicy(String apiLevelPolicy) {
    this.apiLevelPolicy = apiLevelPolicy;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("maxTps")
  public APIMaxTpsDTO getMaxTps() {
    return maxTps;
  }
  public void setMaxTps(APIMaxTpsDTO maxTps) {
    this.maxTps = maxTps;
  }


  /**
   * The visibility level of the APIDetailedDTO. Accepts one of the following. PUBLIC, PRIVATE, RESTRICTED OR CONTROLLED.
   **/
  @ApiModelProperty(value = "The visibility level of the APIDetailedDTO. Accepts one of the following. PUBLIC, PRIVATE, RESTRICTED OR CONTROLLED.")
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
  @ApiModelProperty(value = "The user roles that are able to access the APIDetailedDTO")
  @JsonProperty("visibleRoles")
  public List<String> getVisibleRoles() {
    return visibleRoles;
  }
  public void setVisibleRoles(List<String> visibleRoles) {
    this.visibleRoles = visibleRoles;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("visibleTenants")
  public List<String> getVisibleTenants() {
    return visibleTenants;
  }
  public void setVisibleTenants(List<String> visibleTenants) {
    this.visibleTenants = visibleTenants;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("endpointConfig")
  public String getEndpointConfig() {
    return endpointConfig;
  }
  public void setEndpointConfig(String endpointConfig) {
    this.endpointConfig = endpointConfig;
  }


  /**
   **/
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "Comma separated list of gateway environments.\n")
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
  @ApiModelProperty(value = "Labels of micro-gateway environments attached to the APIDetailedDTO.\n")
  @JsonProperty("labels")
  public List<LabelDTO> getLabels() {
    return labels;
  }
  public void setLabels(List<LabelDTO> labels) {
    this.labels = labels;
  }


  /**
   **/
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "The subscription availability. Accepts one of the following. current_tenant, all_tenants or specific_tenants.")
  @JsonProperty("subscriptionAvailability")
  public SubscriptionAvailabilityEnum getSubscriptionAvailability() {
    return subscriptionAvailability;
  }
  public void setSubscriptionAvailability(SubscriptionAvailabilityEnum subscriptionAvailability) {
    this.subscriptionAvailability = subscriptionAvailability;
  }


  /**
   **/
  @ApiModelProperty(value = "")
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
  @ApiModelProperty(value = "Map of custom properties of APIDetailedDTO")
  @JsonProperty("additionalProperties")
  public Map<String, String> getAdditionalProperties() {
    return additionalProperties;
  }
  public void setAdditionalProperties(Map<String, String> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }


  /**
   * Is the APIDetailedDTO is restricted to certain set of publishers or creators or is it visible to all the\npublishers and creators. If the accessControl restriction is none, this APIDetailedDTO can be modified by all the\npublishers and creators, if not it can only be viewable/modifiable by certain set of publishers and creators,\n based on the restriction.\n
   **/
  @ApiModelProperty(value = "Is the APIDetailedDTO is restricted to certain set of publishers or creators or is it visible to all the\npublishers and creators. If the accessControl restriction is none, this APIDetailedDTO can be modified by all the\npublishers and creators, if not it can only be viewable/modifiable by certain set of publishers and creators,\n based on the restriction.\n")
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
  @ApiModelProperty(value = "The user roles that are able to view/modify as APIDetailedDTO publisher or creator.")
  @JsonProperty("accessControlRoles")
  public List<String> getAccessControlRoles() {
    return accessControlRoles;
  }
  public void setAccessControlRoles(List<String> accessControlRoles) {
    this.accessControlRoles = accessControlRoles;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("businessInformation")
  public APIBusinessInformationDTO getBusinessInformation() {
    return businessInformation;
  }
  public void setBusinessInformation(APIBusinessInformationDTO businessInformation) {
    this.businessInformation = businessInformation;
  }


  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("corsConfiguration")
  public APICorsConfigurationDTO getCorsConfiguration() {
    return corsConfiguration;
  }
  public void setCorsConfiguration(APICorsConfigurationDTO corsConfiguration) {
    this.corsConfiguration = corsConfiguration;
  }



  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIDTO {\n");
    sb.append("  " + super.toString()).append("\n");
    sb.append("  apiDefinition: ").append(apiDefinition).append("\n");
    sb.append("  wsdlUri: ").append(wsdlUri).append("\n");
    sb.append("  responseCaching: ").append(responseCaching).append("\n");
    sb.append("  cacheTimeout: ").append(cacheTimeout).append("\n");
    sb.append("  destinationStatsEnabled: ").append(destinationStatsEnabled).append("\n");
    sb.append("  isDefaultVersion: ").append(isDefaultVersion).append("\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  transport: ").append(transport).append("\n");
    sb.append("  tags: ").append(tags).append("\n");
    sb.append("  tiers: ").append(tiers).append("\n");
    sb.append("  apiLevelPolicy: ").append(apiLevelPolicy).append("\n");
    sb.append("  maxTps: ").append(maxTps).append("\n");
    sb.append("  visibility: ").append(visibility).append("\n");
    sb.append("  visibleRoles: ").append(visibleRoles).append("\n");
    sb.append("  visibleTenants: ").append(visibleTenants).append("\n");
    sb.append("  endpointConfig: ").append(endpointConfig).append("\n");
    sb.append("  endpointSecurity: ").append(endpointSecurity).append("\n");
    sb.append("  gatewayEnvironments: ").append(gatewayEnvironments).append("\n");
    sb.append("  labels: ").append(labels).append("\n");
    sb.append("  sequences: ").append(sequences).append("\n");
    sb.append("  subscriptionAvailability: ").append(subscriptionAvailability).append("\n");
    sb.append("  subscriptionAvailableTenants: ").append(subscriptionAvailableTenants).append("\n");
    sb.append("  additionalProperties: ").append(additionalProperties).append("\n");
    sb.append("  accessControl: ").append(accessControl).append("\n");
    sb.append("  accessControlRoles: ").append(accessControlRoles).append("\n");
    sb.append("  businessInformation: ").append(businessInformation).append("\n");
    sb.append("  corsConfiguration: ").append(corsConfiguration).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
