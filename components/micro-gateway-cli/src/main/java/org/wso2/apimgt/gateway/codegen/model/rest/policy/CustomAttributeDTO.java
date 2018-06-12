package org.wso2.apimgt.gateway.codegen.service.bean.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel(description = "")
public class CustomAttributeDTO {
  
  
  @NotNull
  private String name = null;
  
  @NotNull
  private String value = null;

  
  /**
   * Name of the custom attribute
   **/
  @ApiModelProperty(required = true, value = "Name of the custom attribute")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   * Value of the custom attribute
   **/
  @ApiModelProperty(required = true, value = "Value of the custom attribute")
  @JsonProperty("value")
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomAttributeDTO {\n");
    
    sb.append("  name: ").append(name).append("\n");
    sb.append("  value: ").append(value).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
