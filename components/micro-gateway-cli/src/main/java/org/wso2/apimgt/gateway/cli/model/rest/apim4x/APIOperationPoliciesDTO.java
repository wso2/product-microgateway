/*
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.model.rest.apim4x;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

/**
 * DTO for API Operation Policies
 */
public class APIOperationPoliciesDTO {
  
    private List<OperationPolicyDTO> request = new ArrayList<OperationPolicyDTO>();
    private List<OperationPolicyDTO> response = new ArrayList<OperationPolicyDTO>();
    private List<OperationPolicyDTO> fault = new ArrayList<OperationPolicyDTO>();

  /**
   **/
  public APIOperationPoliciesDTO request(List<OperationPolicyDTO> request) {
    this.request = request;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("request")
  public List<OperationPolicyDTO> getRequest() {
    return request;
  }
  public void setRequest(List<OperationPolicyDTO> request) {
    this.request = request;
  }

  /**
   **/
  public APIOperationPoliciesDTO response(List<OperationPolicyDTO> response) {
    this.response = response;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("response")
  public List<OperationPolicyDTO> getResponse() {
    return response;
  }
  public void setResponse(List<OperationPolicyDTO> response) {
    this.response = response;
  }

  /**
   **/
  public APIOperationPoliciesDTO fault(List<OperationPolicyDTO> fault) {
    this.fault = fault;
    return this;
  }

  
  @ApiModelProperty(value = "")
      @Valid
  @JsonProperty("fault")
  public List<OperationPolicyDTO> getFault() {
    return fault;
  }
  public void setFault(List<OperationPolicyDTO> fault) {
    this.fault = fault;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    APIOperationPoliciesDTO apIOperationPolicies = (APIOperationPoliciesDTO) o;
    return Objects.equals(request, apIOperationPolicies.request) &&
        Objects.equals(response, apIOperationPolicies.response) &&
        Objects.equals(fault, apIOperationPolicies.fault);
  }

  @Override
  public int hashCode() {
    return Objects.hash(request, response, fault);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIOperationPoliciesDTO {\n");
    
    sb.append("    request: ").append(toIndentedString(request)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
    sb.append("    fault: ").append(toIndentedString(fault)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

