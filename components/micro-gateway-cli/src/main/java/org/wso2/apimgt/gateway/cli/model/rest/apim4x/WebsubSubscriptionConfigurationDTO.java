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


/**
 * DTO for Web-Sub Subscription Configurations
 */
public class WebsubSubscriptionConfigurationDTO {
  
    private Boolean enable = false;
    private String secret = null;
    private String signingAlgorithm = null;
    private String signatureHeader = null;

  /**
   * Toggle enable WebSub subscription configuration
   **/
  public WebsubSubscriptionConfigurationDTO enable(Boolean enable) {
    this.enable = enable;
    return this;
  }

  
  @ApiModelProperty(value = "Toggle enable WebSub subscription configuration")
  @JsonProperty("enable")
  public Boolean isEnable() {
    return enable;
  }
  public void setEnable(Boolean enable) {
    this.enable = enable;
  }

  /**
   * Secret key to be used for subscription
   **/
  public WebsubSubscriptionConfigurationDTO secret(String secret) {
    this.secret = secret;
    return this;
  }

  
  @ApiModelProperty(value = "Secret key to be used for subscription")
  @JsonProperty("secret")
  public String getSecret() {
    return secret;
  }
  public void setSecret(String secret) {
    this.secret = secret;
  }

  /**
   * The algorithm used for signing
   **/
  public WebsubSubscriptionConfigurationDTO signingAlgorithm(String signingAlgorithm) {
    this.signingAlgorithm = signingAlgorithm;
    return this;
  }

  
  @ApiModelProperty(value = "The algorithm used for signing")
  @JsonProperty("signingAlgorithm")
  public String getSigningAlgorithm() {
    return signingAlgorithm;
  }
  public void setSigningAlgorithm(String signingAlgorithm) {
    this.signingAlgorithm = signingAlgorithm;
  }

  /**
   * The header uses to send the signature
   **/
  public WebsubSubscriptionConfigurationDTO signatureHeader(String signatureHeader) {
    this.signatureHeader = signatureHeader;
    return this;
  }

  
  @ApiModelProperty(value = "The header uses to send the signature")
  @JsonProperty("signatureHeader")
  public String getSignatureHeader() {
    return signatureHeader;
  }
  public void setSignatureHeader(String signatureHeader) {
    this.signatureHeader = signatureHeader;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebsubSubscriptionConfigurationDTO websubSubscriptionConfiguration = (WebsubSubscriptionConfigurationDTO) o;
    return Objects.equals(enable, websubSubscriptionConfiguration.enable) &&
        Objects.equals(secret, websubSubscriptionConfiguration.secret) &&
        Objects.equals(signingAlgorithm, websubSubscriptionConfiguration.signingAlgorithm) &&
        Objects.equals(signatureHeader, websubSubscriptionConfiguration.signatureHeader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enable, secret, signingAlgorithm, signatureHeader);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebsubSubscriptionConfigurationDTO {\n");
    
    sb.append("    enable: ").append(toIndentedString(enable)).append("\n");
    sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
    sb.append("    signingAlgorithm: ").append(toIndentedString(signingAlgorithm)).append("\n");
    sb.append("    signatureHeader: ").append(toIndentedString(signatureHeader)).append("\n");
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

