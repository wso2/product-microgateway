package org.wso2.apimgt.gateway.codegen.service.bean.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RequestCountLimitDTO.class, name = "RequestCountLimit"),
    @JsonSubTypes.Type(value = BandwidthLimitDTO.class, name = "BandwidthLimit"),
})
@ApiModel(description = "")
public class ThrottleLimitDTO {
  
  
  public enum TypeEnum {
     RequestCountLimit,  BandwidthLimit, 
  };
  @NotNull
  private TypeEnum type = null;
  
  @NotNull
  private String timeUnit = null;
  
  @NotNull
  private Integer unitTime = null;

  
  /**
   * Type of the throttling limit. Allowed values are \"RequestCountLimit\" and \"BandwidthLimit\".\nPlease see schemas of each of those throttling limit types in Definitions section.\n
   **/
  @ApiModelProperty(required = true, value = "Type of the throttling limit. Allowed values are \"RequestCountLimit\" and \"BandwidthLimit\".\nPlease see schemas of each of those throttling limit types in Definitions section.\n")
  @JsonProperty("type")
  public TypeEnum getType() {
    return type;
  }
  public void setType(TypeEnum type) {
    this.type = type;
  }

  
  /**
   * Unit of the time. Allowed values are \"sec\", \"min\", \"hour\", \"day\"
   **/
  @ApiModelProperty(required = true, value = "Unit of the time. Allowed values are \"sec\", \"min\", \"hour\", \"day\"")
  @JsonProperty("timeUnit")
  public String getTimeUnit() {
    return timeUnit;
  }
  public void setTimeUnit(String timeUnit) {
    this.timeUnit = timeUnit;
  }

  
  /**
   * Time limit that the throttling limit applies.
   **/
  @ApiModelProperty(required = true, value = "Time limit that the throttling limit applies.")
  @JsonProperty("unitTime")
  public Integer getUnitTime() {
    return unitTime;
  }
  public void setUnitTime(Integer unitTime) {
    this.unitTime = unitTime;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ThrottleLimitDTO {\n");
    
    sb.append("  type: ").append(type).append("\n");
    sb.append("  timeUnit: ").append(timeUnit).append("\n");
    sb.append("  unitTime: ").append(unitTime).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
