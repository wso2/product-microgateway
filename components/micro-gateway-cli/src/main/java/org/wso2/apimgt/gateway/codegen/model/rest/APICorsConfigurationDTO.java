package org.wso2.apimgt.gateway.codegen.service.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;


/**
 * CORS configuration for the APIDetailedDTO\n
 **/


@ApiModel(description = "CORS configuration for the APIDetailedDTO\n")
public class APICorsConfigurationDTO  {
  
  
  
  private List<String> accessControlAllowOrigins = new ArrayList<String>();
  
  
  private Boolean accessControlAllowCredentials = false;
  
  
  private Boolean corsConfigurationEnabled = false;
  
  
  private List<String> accessControlAllowHeaders = new ArrayList<String>();
  
  
  private List<String> accessControlAllowMethods = new ArrayList<String>();

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("accessControlAllowOrigins")
  public List<String> getAccessControlAllowOrigins() {
    return accessControlAllowOrigins;
  }
  public void setAccessControlAllowOrigins(List<String> accessControlAllowOrigins) {
    this.accessControlAllowOrigins = accessControlAllowOrigins;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("accessControlAllowCredentials")
  public Boolean getAccessControlAllowCredentials() {
    return accessControlAllowCredentials;
  }
  public void setAccessControlAllowCredentials(Boolean accessControlAllowCredentials) {
    this.accessControlAllowCredentials = accessControlAllowCredentials;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("corsConfigurationEnabled")
  public Boolean getCorsConfigurationEnabled() {
    return corsConfigurationEnabled;
  }
  public void setCorsConfigurationEnabled(Boolean corsConfigurationEnabled) {
    this.corsConfigurationEnabled = corsConfigurationEnabled;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("accessControlAllowHeaders")
  public List<String> getAccessControlAllowHeaders() {
    return accessControlAllowHeaders;
  }
  public void setAccessControlAllowHeaders(List<String> accessControlAllowHeaders) {
    this.accessControlAllowHeaders = accessControlAllowHeaders;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("accessControlAllowMethods")
  public List<String> getAccessControlAllowMethods() {
    return accessControlAllowMethods;
  }
  public void setAccessControlAllowMethods(List<String> accessControlAllowMethods) {
    this.accessControlAllowMethods = accessControlAllowMethods;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APICorsConfigurationDTO {\n");
    
    sb.append("  accessControlAllowOrigins: ").append(accessControlAllowOrigins).append("\n");
    sb.append("  accessControlAllowCredentials: ").append(accessControlAllowCredentials).append("\n");
    sb.append("  corsConfigurationEnabled: ").append(corsConfigurationEnabled).append("\n");
    sb.append("  accessControlAllowHeaders: ").append(accessControlAllowHeaders).append("\n");
    sb.append("  accessControlAllowMethods: ").append(accessControlAllowMethods).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
