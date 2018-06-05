package org.wso2.apimgt.gateway.codegen.service.bean;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "")
public class APIBusinessInformationDTO  {
  
  
  
  private String technicalOwnerEmail = null;
  
  
  private String businessOwnerEmail = null;
  
  
  private String businessOwner = null;
  
  
  private String technicalOwner = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("technicalOwnerEmail")
  public String getTechnicalOwnerEmail() {
    return technicalOwnerEmail;
  }
  public void setTechnicalOwnerEmail(String technicalOwnerEmail) {
    this.technicalOwnerEmail = technicalOwnerEmail;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("businessOwnerEmail")
  public String getBusinessOwnerEmail() {
    return businessOwnerEmail;
  }
  public void setBusinessOwnerEmail(String businessOwnerEmail) {
    this.businessOwnerEmail = businessOwnerEmail;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("businessOwner")
  public String getBusinessOwner() {
    return businessOwner;
  }
  public void setBusinessOwner(String businessOwner) {
    this.businessOwner = businessOwner;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("technicalOwner")
  public String getTechnicalOwner() {
    return technicalOwner;
  }
  public void setTechnicalOwner(String technicalOwner) {
    this.technicalOwner = technicalOwner;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIBusinessInformationDTO {\n");
    
    sb.append("  technicalOwnerEmail: ").append(technicalOwnerEmail).append("\n");
    sb.append("  businessOwnerEmail: ").append(businessOwnerEmail).append("\n");
    sb.append("  businessOwner: ").append(businessOwner).append("\n");
    sb.append("  technicalOwner: ").append(technicalOwner).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
