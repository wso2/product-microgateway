/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class APIListDTO  {

  private Integer count = null;
  private String next = null;
  private String previous = null;
  private List<ExtendedAPI> list = new ArrayList<ExtendedAPI>();
  private APIListPaginationDTO pagination = null;

  
  /**
   * Number of APIs returned.\n
   **/
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
  @JsonProperty("previous")
  public String getPrevious() {
    return previous;
  }
  public void setPrevious(String previous) {
    this.previous = previous;
  }

  @JsonProperty("list")
  public List<ExtendedAPI> getList() {
    return list;
  }
  public void setList(List<ExtendedAPI> list) {
    this.list = list;
  }

  @JsonProperty("pagination")
  public APIListPaginationDTO getPagination() {
    return pagination;
  }
  public void setPagination(APIListPaginationDTO pagination) {
    this.pagination = pagination;
  }

}
