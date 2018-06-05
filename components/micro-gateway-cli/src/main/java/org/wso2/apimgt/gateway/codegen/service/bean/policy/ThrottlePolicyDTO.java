package org.wso2.apimgt.gateway.codegen.service.bean.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel(description = "")
public class ThrottlePolicyDTO {
  
  
  
  private String policyId = null;
  
  @NotNull
  private String policyName = null;
  
  
  private String displayName = null;
  
  
  private String description = null;
  
  
  private Boolean isDeployed = false;

  
  /**
   * Id of policy
   **/
  @ApiModelProperty(value = "Id of policy")
  @JsonProperty("policyId")
  public String getPolicyId() {
    return policyId;
  }
  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  
  /**
   * Name of policy
   **/
  @ApiModelProperty(required = true, value = "Name of policy")
  @JsonProperty("policyName")
  public String getPolicyName() {
    return policyName;
  }
  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  
  /**
   * Display name of the policy
   **/
  @ApiModelProperty(value = "Display name of the policy")
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  
  /**
   * Description of the policy
   **/
  @ApiModelProperty(value = "Description of the policy")
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  
  /**
   * Indicates whether the policy is deployed successfully or not.
   **/
  @ApiModelProperty(value = "Indicates whether the policy is deployed successfully or not.")
  @JsonProperty("isDeployed")
  public Boolean getIsDeployed() {
    return isDeployed;
  }
  public void setIsDeployed(Boolean isDeployed) {
    this.isDeployed = isDeployed;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ThrottlePolicyDTO {\n");
    
    sb.append("  policyId: ").append(policyId).append("\n");
    sb.append("  policyName: ").append(policyName).append("\n");
    sb.append("  displayName: ").append(displayName).append("\n");
    sb.append("  description: ").append(description).append("\n");
    sb.append("  isDeployed: ").append(isDeployed).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
