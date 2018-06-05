package org.wso2.apimgt.gateway.codegen.service.bean.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class RequestCountLimitDTO extends ThrottleLimitDTO {
  
  
  
  private Long requestCount = null;

  
  /**
   * Maximum number of requests allowed
   **/
  @ApiModelProperty(value = "Maximum number of requests allowed")
  @JsonProperty("requestCount")
  public Long getRequestCount() {
    return requestCount;
  }
  public void setRequestCount(Long requestCount) {
    this.requestCount = requestCount;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class RequestCountLimitDTO {\n");
    sb.append("  " + super.toString()).append("\n");
    sb.append("  requestCount: ").append(requestCount).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
