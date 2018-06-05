package org.wso2.apimgt.gateway.codegen.service.bean;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class APIEndpointSecurityDTO {
  
  
  
  private String password = null;
  
  public enum TypeEnum {
     basic,  digest, 
  };
  
  private TypeEnum type = null;
  
  
  private String username = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("type")
  public TypeEnum getType() {
    return type;
  }
  public void setType(TypeEnum type) {
    this.type = type;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIEndpointSecurityDTO {\n");
    
    sb.append("  password: ").append(password).append("\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  username: ").append(username).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
