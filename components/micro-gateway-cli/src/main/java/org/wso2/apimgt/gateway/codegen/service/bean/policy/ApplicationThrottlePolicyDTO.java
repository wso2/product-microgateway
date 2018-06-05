package org.wso2.apimgt.gateway.codegen.service.bean.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class ApplicationThrottlePolicyDTO extends ThrottlePolicyDTO {
  
  
  
  private ThrottleLimitDTO defaultLimit = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("defaultLimit")
  public ThrottleLimitDTO getDefaultLimit() {
    return defaultLimit;
  }
  public void setDefaultLimit(ThrottleLimitDTO defaultLimit) {
    this.defaultLimit = defaultLimit;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationThrottlePolicyDTO {\n");
    sb.append("  " + super.toString()).append("\n");
    sb.append("  defaultLimit: ").append(defaultLimit).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
