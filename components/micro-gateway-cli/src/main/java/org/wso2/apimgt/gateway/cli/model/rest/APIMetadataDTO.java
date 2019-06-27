package org.wso2.apimgt.gateway.cli.model.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data holder/mapper for API Metadata of WSO2 APIM APIs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIMetadataDTO {
    private String security = null;
    private APICorsConfigurationDTO corsConfigurationDTO = null;

    @JsonProperty("apiSecurity")
    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    @JsonProperty("corsConfiguration")
    public APICorsConfigurationDTO getCorsConfigurationDTO() {
        return corsConfigurationDTO;
    }

    public void setCorsConfigurationDTO(APICorsConfigurationDTO corsConfigurationDTO) {
        this.corsConfigurationDTO = corsConfigurationDTO;
    }
}
