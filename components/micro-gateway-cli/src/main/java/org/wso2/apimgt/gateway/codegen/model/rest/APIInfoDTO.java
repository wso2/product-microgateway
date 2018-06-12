package org.wso2.apimgt.gateway.codegen.service.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class APIInfoDTO  {
  
  
  
  private String id = null;
  
  
  private String name = null;
  
  
  private String description = null;
  
  
  private String context = null;
  
  
  private String version = null;
  
  
  private String provider = null;
  
  
  private String status = null;
  
  
  private String thumbnailUri = null;

  
  /**
   * UUID of the api registry artifact\n
   **/
  @ApiModelProperty(value = "UUID of the api registry artifact\n")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  
  /**
   * Name of the APIDetailedDTO
   **/
  @ApiModelProperty(value = "Name of the APIDetailedDTO")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   * A brief description about the APIDetailedDTO
   **/
  @ApiModelProperty(value = "A brief description about the APIDetailedDTO")
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  
  /**
   * A string that represents the context of the user's request
   **/
  @ApiModelProperty(value = "A string that represents the context of the user's request")
  @JsonProperty("context")
  public String getContext() {
    return context;
  }
  public void setContext(String context) {
    this.context = context;
  }

  
  /**
   * The version of the APIDetailedDTO
   **/
  @ApiModelProperty(value = "The version of the APIDetailedDTO")
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }

  
  /**
   * If the provider value is not given, the user invoking the APIDetailedDTO will be used as the provider.\n
   **/
  @ApiModelProperty(value = "If the provider value is not given, the user invoking the APIDetailedDTO will be used as the provider.\n")
  @JsonProperty("provider")
  public String getProvider() {
    return provider;
  }
  public void setProvider(String provider) {
    this.provider = provider;
  }

  
  /**
   * This describes in which status of the lifecycle the APIDetailedDTO is
   **/
  @ApiModelProperty(value = "This describes in which status of the lifecycle the APIDetailedDTO is")
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("thumbnailUri")
  public String getThumbnailUri() {
    return thumbnailUri;
  }
  public void setThumbnailUri(String thumbnailUri) {
    this.thumbnailUri = thumbnailUri;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIInfoDTO {\n");
    
    sb.append("  id: ").append(id).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("  description: ").append(description).append("\n");
    sb.append("  context: ").append(context).append("\n");
    sb.append("  version: ").append(version).append("\n");
    sb.append("  provider: ").append(provider).append("\n");
    sb.append("  status: ").append(status).append("\n");
    sb.append("  thumbnailUri: ").append(thumbnailUri).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
