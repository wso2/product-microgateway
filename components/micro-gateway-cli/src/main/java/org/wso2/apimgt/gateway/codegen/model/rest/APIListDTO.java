package org.wso2.apimgt.gateway.codegen.service.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.wso2.apimgt.gateway.codegen.service.bean.ext.ExtendedAPI;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class APIListDTO  {
  
  
  
  private Integer count = null;
  
  
  private String next = null;
  
  
  private String previous = null;
  
  
  private List<ExtendedAPI> list = new ArrayList<ExtendedAPI>();
  
  
  private APIListPaginationDTO pagination = null;

  
  /**
   * Number of APIs returned.\n
   **/
  @ApiModelProperty(value = "Number of APIs returned.\n")
  @JsonProperty("count")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }

  
  /**
   * Link to the next subset of resources qualified.\nEmpty if no more resources are to be returned.\n
   **/
  @ApiModelProperty(value = "Link to the next subset of resources qualified.\nEmpty if no more resources are to be returned.\n")
  @JsonProperty("next")
  public String getNext() {
    return next;
  }
  public void setNext(String next) {
    this.next = next;
  }

  
  /**
   * Link to the previous subset of resources qualified.\nEmpty if current subset is the first subset returned.\n
   **/
  @ApiModelProperty(value = "Link to the previous subset of resources qualified.\nEmpty if current subset is the first subset returned.\n")
  @JsonProperty("previous")
  public String getPrevious() {
    return previous;
  }
  public void setPrevious(String previous) {
    this.previous = previous;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("list")
  public List<ExtendedAPI> getList() {
    return list;
  }
  public void setList(List<ExtendedAPI> list) {
    this.list = list;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("pagination")
  public APIListPaginationDTO getPagination() {
    return pagination;
  }
  public void setPagination(APIListPaginationDTO pagination) {
    this.pagination = pagination;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIListDTO {\n");
    
    sb.append("  count: ").append(count).append("\n");
    sb.append("  next: ").append(next).append("\n");
    sb.append("  previous: ").append(previous).append("\n");
    sb.append("  list: ").append(list).append("\n");
    sb.append("  pagination: ").append(pagination).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
