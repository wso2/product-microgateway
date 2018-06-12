package org.wso2.apimgt.gateway.codegen.service.bean;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;


@ApiModel(description = "")
public class SequenceDTO {
  
  
  @NotNull
  private String name = null;
  
  
  private String type = null;
  
  
  private String id = null;
  
  
  private Boolean shared = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("type")
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("shared")
  public Boolean getShared() {
    return shared;
  }
  public void setShared(Boolean shared) {
    this.shared = shared;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class SequenceDTO {\n");
    
    sb.append("  name: ").append(name).append("\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  id: ").append(id).append("\n");
    sb.append("  shared: ").append(shared).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
