package org.wso2.apimgt.gateway.codegen.service.bean;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class APIMaxTpsDTO {
  
  
  
  private Long production = null;
  
  
  private Long sandbox = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("production")
  public Long getProduction() {
    return production;
  }
  public void setProduction(Long production) {
    this.production = production;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("sandbox")
  public Long getSandbox() {
    return sandbox;
  }
  public void setSandbox(Long sandbox) {
    this.sandbox = sandbox;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIMaxTpsDTO {\n");
    
    sb.append("  production: ").append(production).append("\n");
    sb.append("  sandbox: ").append(sandbox).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
