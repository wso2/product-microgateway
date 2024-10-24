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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO for Third party API information
 */
public class AdvertiseInfoDTO {
  
    private Boolean advertised = null;
    private String apiExternalProductionEndpoint = null;
    private String apiExternalSandboxEndpoint = null;
    private String originalDevPortalUrl = null;
    private String apiOwner = null;

    /**
     * Vendor enum for the third party APIs
     */
    @XmlType(name = "VendorEnum")
    @XmlEnum(String.class)
    public enum VendorEnum {
        WSO2("WSO2"),
        AWS("AWS");
        private String value;

        VendorEnum (String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static VendorEnum fromValue(String v) {
            for (VendorEnum b : VendorEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
return null;
        }
    }
    private VendorEnum vendor = VendorEnum.WSO2;

  /**
   **/
  public AdvertiseInfoDTO advertised(Boolean advertised) {
    this.advertised = advertised;
    return this;
  }

  
  @ApiModelProperty(example = "true", value = "")
  @JsonProperty("advertised")
  public Boolean isAdvertised() {
    return advertised;
  }
  public void setAdvertised(Boolean advertised) {
    this.advertised = advertised;
  }

  /**
   **/
  public AdvertiseInfoDTO apiExternalProductionEndpoint(String apiExternalProductionEndpoint) {
    this.apiExternalProductionEndpoint = apiExternalProductionEndpoint;
    return this;
  }

  
  @ApiModelProperty(example = "https://localhost:9443/devportal", value = "")
  @JsonProperty("apiExternalProductionEndpoint")
  public String getApiExternalProductionEndpoint() {
    return apiExternalProductionEndpoint;
  }
  public void setApiExternalProductionEndpoint(String apiExternalProductionEndpoint) {
    this.apiExternalProductionEndpoint = apiExternalProductionEndpoint;
  }

  /**
   **/
  public AdvertiseInfoDTO apiExternalSandboxEndpoint(String apiExternalSandboxEndpoint) {
    this.apiExternalSandboxEndpoint = apiExternalSandboxEndpoint;
    return this;
  }

  
  @ApiModelProperty(example = "https://localhost:9443/devportal", value = "")
  @JsonProperty("apiExternalSandboxEndpoint")
  public String getApiExternalSandboxEndpoint() {
    return apiExternalSandboxEndpoint;
  }
  public void setApiExternalSandboxEndpoint(String apiExternalSandboxEndpoint) {
    this.apiExternalSandboxEndpoint = apiExternalSandboxEndpoint;
  }

  /**
   **/
  public AdvertiseInfoDTO originalDevPortalUrl(String originalDevPortalUrl) {
    this.originalDevPortalUrl = originalDevPortalUrl;
    return this;
  }

  
  @ApiModelProperty(example = "https://localhost:9443/devportal", value = "")
  @JsonProperty("originalDevPortalUrl")
  public String getOriginalDevPortalUrl() {
    return originalDevPortalUrl;
  }
  public void setOriginalDevPortalUrl(String originalDevPortalUrl) {
    this.originalDevPortalUrl = originalDevPortalUrl;
  }

  /**
   **/
  public AdvertiseInfoDTO apiOwner(String apiOwner) {
    this.apiOwner = apiOwner;
    return this;
  }

  
  @ApiModelProperty(example = "admin", value = "")
  @JsonProperty("apiOwner")
  public String getApiOwner() {
    return apiOwner;
  }
  public void setApiOwner(String apiOwner) {
    this.apiOwner = apiOwner;
  }

  /**
   **/
  public AdvertiseInfoDTO vendor(VendorEnum vendor) {
    this.vendor = vendor;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("vendor")
  public VendorEnum getVendor() {
    return vendor;
  }
  public void setVendor(VendorEnum vendor) {
    this.vendor = vendor;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdvertiseInfoDTO advertiseInfo = (AdvertiseInfoDTO) o;
    return Objects.equals(advertised, advertiseInfo.advertised) &&
        Objects.equals(apiExternalProductionEndpoint, advertiseInfo.apiExternalProductionEndpoint) &&
        Objects.equals(apiExternalSandboxEndpoint, advertiseInfo.apiExternalSandboxEndpoint) &&
        Objects.equals(originalDevPortalUrl, advertiseInfo.originalDevPortalUrl) &&
        Objects.equals(apiOwner, advertiseInfo.apiOwner) &&
        Objects.equals(vendor, advertiseInfo.vendor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(advertised, apiExternalProductionEndpoint, apiExternalSandboxEndpoint, originalDevPortalUrl,
            apiOwner, vendor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdvertiseInfoDTO {\n");
    
    sb.append("    advertised: ").append(toIndentedString(advertised)).append("\n");
    sb.append("    apiExternalProductionEndpoint: ").append(toIndentedString(apiExternalProductionEndpoint))
            .append("\n");
    sb.append("    apiExternalSandboxEndpoint: ").append(toIndentedString(apiExternalSandboxEndpoint)).append("\n");
    sb.append("    originalDevPortalUrl: ").append(toIndentedString(originalDevPortalUrl)).append("\n");
    sb.append("    apiOwner: ").append(toIndentedString(apiOwner)).append("\n");
    sb.append("    vendor: ").append(toIndentedString(vendor)).append("\n");
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

