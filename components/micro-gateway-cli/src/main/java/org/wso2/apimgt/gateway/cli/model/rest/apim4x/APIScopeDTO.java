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

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * DTO for API bound scopes
 */
public class APIScopeDTO   {
  
    private ScopeDTO scope = null;
    private Boolean shared = null;

  /**
   **/
  public APIScopeDTO scope(ScopeDTO scope) {
    this.scope = scope;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
      @Valid
  @JsonProperty("scope")
  @NotNull
  public ScopeDTO getScope() {
    return scope;
  }
  public void setScope(ScopeDTO scope) {
    this.scope = scope;
  }

  /**
   * States whether scope is shared. This will not be honored when updating/adding scopes to APIs or when
   * adding/updating Shared Scopes.
   **/
  public APIScopeDTO shared(Boolean shared) {
    this.shared = shared;
    return this;
  }

  
  @ApiModelProperty(example = "true", value = "States whether scope is shared. This will not be honored" +
          " when updating/adding scopes to APIs or when adding/updating Shared Scopes. ")
  @JsonProperty("shared")
  public Boolean isShared() {
    return shared;
  }
  public void setShared(Boolean shared) {
    this.shared = shared;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    APIScopeDTO apIScope = (APIScopeDTO) o;
    return Objects.equals(scope, apIScope.scope) &&
        Objects.equals(shared, apIScope.shared);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scope, shared);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class APIScopeDTO {\n");
    
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    shared: ").append(toIndentedString(shared)).append("\n");
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

