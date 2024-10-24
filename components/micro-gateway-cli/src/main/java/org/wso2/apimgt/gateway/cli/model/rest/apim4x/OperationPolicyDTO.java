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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;

/**
 * DTO for Operation Policy
 */
public class OperationPolicyDTO {
  
    private String policyName = null;
    private String policyVersion = "v1";
    private String policyId = null;
    private Map<String, Object> parameters = new HashMap<String, Object>();

  /**
   **/
  public OperationPolicyDTO policyName(String policyName) {
    this.policyName = policyName;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("policyName")
  @NotNull
  public String getPolicyName() {
    return policyName;
  }
  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  /**
   **/
  public OperationPolicyDTO policyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("policyVersion")
  public String getPolicyVersion() {
    return policyVersion;
  }
  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  /**
   **/
  public OperationPolicyDTO policyId(String policyId) {
    this.policyId = policyId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("policyId")
  public String getPolicyId() {
    return policyId;
  }
  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  /**
   **/
  public OperationPolicyDTO parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperationPolicyDTO operationPolicy = (OperationPolicyDTO) o;
    return Objects.equals(policyName, operationPolicy.policyName) &&
        Objects.equals(policyVersion, operationPolicy.policyVersion) &&
        Objects.equals(policyId, operationPolicy.policyId) &&
        Objects.equals(parameters, operationPolicy.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(policyName, policyVersion, policyId, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OperationPolicyDTO {\n");
    
    sb.append("    policyName: ").append(toIndentedString(policyName)).append("\n");
    sb.append("    policyVersion: ").append(toIndentedString(policyVersion)).append("\n");
    sb.append("    policyId: ").append(toIndentedString(policyId)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

