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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.wso2.apimgt.gateway.cli.model.rest.APIInfoBaseDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * DTO for APIM 4.x API model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Apim4xApiDto extends APIInfoBaseDTO {
    private String lifeCycleStatus = null;
    private WSDLInfoDTO wsdlInfo = null;
    private String wsdlUrl = null;
    private Boolean responseCachingEnabled = null;
    private Boolean hasThumbnail = null;
    private Boolean isRevision = null;
    private String revisionedApiId = null;
    private Integer revisionId = null;
    private Boolean enableSchemaValidation = null;
    private Boolean enableSubscriberVerification = null;

    /**
     * API Type Enum
     */
    @XmlType(name = "TypeEnum")
    @XmlEnum(String.class)
    public enum TypeEnum {
        HTTP("HTTP"),
        WS("WS"),
        SOAPTOREST("SOAPTOREST"),
        SOAP("SOAP"),
        GRAPHQL("GRAPHQL"),
        WEBSUB("WEBSUB"),
        SSE("SSE"),
        WEBHOOK("WEBHOOK"),
        ASYNC("ASYNC");
        private String value;

        TypeEnum(String v) {
            value = v;
        }

        @JsonCreator
        public static TypeEnum fromValue(String v) {
            for (TypeEnum b : TypeEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Audience Enum
     */
    @XmlType(name = "AudienceEnum")
    @XmlEnum(String.class)
    public enum AudienceEnum {
        PUBLIC("PUBLIC"),
        SINGLE("SINGLE");
        private String value;

        AudienceEnum (String v) {
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
        public static AudienceEnum fromValue(String v) {
            for (AudienceEnum b : AudienceEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    private TypeEnum type = TypeEnum.HTTP;
    private AudienceEnum audience = null;
    private List<String> audiences = new ArrayList<String>();
    private List<String> policies = new ArrayList<String>();
    private String apiThrottlingPolicy = null;
    private String apiKeyHeader = null;
    private List<String> securityScheme = new ArrayList<String>();

    /**
     * API Visibility Enum
     */
    @XmlType(name = "VisibilityEnum")
    @XmlEnum(String.class)
    public enum VisibilityEnum {
        PUBLIC("PUBLIC"),
        PRIVATE("PRIVATE"),
        RESTRICTED("RESTRICTED");
        private String value;

        VisibilityEnum (String v) {
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
        public static VisibilityEnum fromValue(String v) {
            for (VisibilityEnum b : VisibilityEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    private VisibilityEnum visibility = VisibilityEnum.PUBLIC;
    private List<MediationPolicyDTO> mediationPolicies = new ArrayList<MediationPolicyDTO>();
    private APIOperationPoliciesDTO apiPolicies = null;

    /**
     * Subscription Availability Enum for the Developer Portal
     */
    @XmlType(name = "SubscriptionAvailabilityEnum")
    @XmlEnum(String.class)
    public enum SubscriptionAvailabilityEnum {
        CURRENT_TENANT("CURRENT_TENANT"),
        ALL_TENANTS("ALL_TENANTS"),
        SPECIFIC_TENANTS("SPECIFIC_TENANTS");
        private String value;

        SubscriptionAvailabilityEnum (String v) {
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
        public static SubscriptionAvailabilityEnum fromValue(String v) {
            for (SubscriptionAvailabilityEnum b : SubscriptionAvailabilityEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    private SubscriptionAvailabilityEnum subscriptionAvailability = SubscriptionAvailabilityEnum.CURRENT_TENANT;
    private List<APIInfoAdditionalPropertiesDTO> additionalProperties = new ArrayList<APIInfoAdditionalPropertiesDTO>();
    private Map<String, APIInfoAdditionalPropertiesMapDTO> additionalPropertiesMap = new HashMap<String,
            APIInfoAdditionalPropertiesMapDTO>();
    private APIMonetizationInfoDTO monetization = null;

    /**
     * Access Control Enum
     */
    @XmlType(name = "AccessControlEnum")
    @XmlEnum(String.class)
    public enum AccessControlEnum {
        NONE("NONE"),
        RESTRICTED("RESTRICTED");
        private String value;
        AccessControlEnum (String v) {
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
        public static AccessControlEnum fromValue(String v) {
            for (AccessControlEnum b : AccessControlEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    private AccessControlEnum accessControl = AccessControlEnum.NONE;
    private WebsubSubscriptionConfigurationDTO websubSubscriptionConfiguration = null;
    private String workflowStatus = null;
    private String createdTime = null;
    private String lastUpdatedTimestamp = null;
    private String lastUpdatedTime = null;
    private Object endpointConfig = null;

    /**
     * Endpoint Implementation Type Enum
     */
    @XmlType(name = "EndpointImplementationTypeEnum")
    @XmlEnum(String.class)
    public enum EndpointImplementationTypeEnum {
        INLINE("INLINE"),
        ENDPOINT("ENDPOINT"),
        MOCKED_OAS("MOCKED_OAS");
        private String value;

        EndpointImplementationTypeEnum (String v) {
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
        public static EndpointImplementationTypeEnum fromValue(String v) {
            for (EndpointImplementationTypeEnum b : EndpointImplementationTypeEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

    private EndpointImplementationTypeEnum endpointImplementationType = EndpointImplementationTypeEnum.ENDPOINT;
    private List<APIScopeDTO> scopes = new ArrayList<APIScopeDTO>();
    private List<APIOperationsDTO> operations = new ArrayList<APIOperationsDTO>();
    private APIThreatProtectionPoliciesDTO threatProtectionPolicies = null;
    private List<String> categories = new ArrayList<String>();
    private Object keyManagers = null;
    private APIServiceInfoDTO serviceInfo = null;
    private AdvertiseInfoDTO advertiseInfo = null;
    private String gatewayVendor = null;
    private String gatewayType = "wso2/synapse";
    private List<String> asyncTransportProtocols = new ArrayList<String>();
    private String organizationId;

    @ApiModelProperty(example = "HTTP", value = "The api creation type to be used. Accepted values are HTTP, " +
            "WS, SOAPTOREST, GRAPHQL, WEBSUB, SSE, WEBHOOK, ASYNC")
    @JsonProperty("type")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    @ApiModelProperty(example = "CREATED", value = "")
    @JsonProperty("lifeCycleStatus")
    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("wsdlInfo")
    public WSDLInfoDTO getWsdlInfo() {
        return wsdlInfo;
    }

    public void setWsdlInfo(WSDLInfoDTO wsdlInfo) {
        this.wsdlInfo = wsdlInfo;
    }

    @ApiModelProperty(example = "/apimgt/applicationdata/wsdls/admin--soap1.wsdl", value = "")
    @JsonProperty("wsdlUrl")
    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    @ApiModelProperty(example = "true", value = "")
    @JsonProperty("responseCachingEnabled")
    public Boolean isResponseCachingEnabled() {
        return responseCachingEnabled;
    }

    public void setResponseCachingEnabled(Boolean responseCachingEnabled) {
        this.responseCachingEnabled = responseCachingEnabled;
    }

    @ApiModelProperty(example = "false", value = "")
    @JsonProperty("hasThumbnail")
    public Boolean hasThumbnail() {
        return hasThumbnail;
    }

    public void setHasThumbnail(Boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    @ApiModelProperty(example = "false", value = "")
    @JsonProperty("isRevision")
    public Boolean isRevision() {
        return isRevision;
    }

    public void setRevision(Boolean revision) {
        isRevision = revision;
    }

    @ApiModelProperty(example = "01234567-0123-0123-0123-012345678901", value = "UUID of the api registry artifact ")
    @JsonProperty("revisionedApiId")
    public String getRevisionedApiId() {
        return revisionedApiId;
    }

    public void setRevisionedApiId(String revisionedApiId) {
        this.revisionedApiId = revisionedApiId;
    }

    @ApiModelProperty(example = "1", value = "")
    @JsonProperty("revisionId")
    public Integer getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Integer revisionId) {
        this.revisionId = revisionId;
    }

    @ApiModelProperty(example = "false", value = "")
    @JsonProperty("enableSchemaValidation")
    public Boolean getEnableSchemaValidation() {
        return enableSchemaValidation;
    }

    public void setEnableSchemaValidation(Boolean enableSchemaValidation) {
        this.enableSchemaValidation = enableSchemaValidation;
    }

    @ApiModelProperty(example = "false", value = "")
    @JsonProperty("enableSubscriberVerification")
    public Boolean getEnableSubscriberVerification() {
        return enableSubscriberVerification;
    }

    public void setEnableSubscriberVerification(Boolean enableSubscriberVerification) {
        this.enableSubscriberVerification = enableSubscriberVerification;
    }

    @ApiModelProperty(example = "PUBLIC", value = "The audience of the API. Accepted values are PUBLIC, SINGLE")
    @JsonProperty("audience")
    public AudienceEnum getAudience() {
        return audience;
    }

    public void setAudience(AudienceEnum audience) {
        this.audience = audience;
    }

    @ApiModelProperty(value = "The audiences of the API for jwt validation. Accepted values are any String values")
    @JsonProperty("audiences")
    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    @ApiModelProperty(example = "[\"Unlimited\"]", value = "")
    @JsonProperty("policies")
    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

    @ApiModelProperty(example = "Unlimited", value = "The API level throttling policy selected for the particular API")
    @JsonProperty("apiThrottlingPolicy")
    public String getApiThrottlingPolicy() {
        return apiThrottlingPolicy;
    }

    public void setApiThrottlingPolicy(String apiThrottlingPolicy) {
        this.apiThrottlingPolicy = apiThrottlingPolicy;
    }

    @ApiModelProperty(example = "apiKey", value = "Name of the API key header used for invoking the API. If it " +
            "is not set, default value `apiKey` will be used. ")
    @JsonProperty("apiKeyHeader")
    @Pattern(regexp = "(^[^~!@#;:%^*()+={}|\\\\<>\"',&$\\s+]*$)")
    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    @ApiModelProperty(example = "[\"oauth2\"]", value = "Types of API security, the current API secured with. " +
            "It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security " +
            "for the current API. ")
    @JsonProperty("securityScheme")
    public List<String> getSecurityScheme() {
        return securityScheme;
    }

    public void setSecurityScheme(List<String> securityScheme) {
        this.securityScheme = securityScheme;
    }

    @ApiModelProperty(example = "PUBLIC", value = "The visibility level of the API. Accepts one of the " +
            "following. PUBLIC, PRIVATE, RESTRICTED.")
    @JsonProperty("visibility")
    public VisibilityEnum getVisibility() {
        return visibility;
    }

    public void setVisibility(VisibilityEnum visibility) {
        this.visibility = visibility;
    }

    @ApiModelProperty(example = "[{\"name\":\"json_to_xml_in_message\",\"type\":\"in\"},{\"name\":" +
            "\"xml_to_json_out_message\",\"type\":\"out\"},{\"name\":\"json_fault\",\"type\":\"fault\"}]", value = "")
    @Valid
    @JsonProperty("mediationPolicies")
    public List<MediationPolicyDTO> getMediationPolicies() {
        return mediationPolicies;
    }

    public void setMediationPolicies(List<MediationPolicyDTO> mediationPolicies) {
        this.mediationPolicies = mediationPolicies;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("apiPolicies")
    public APIOperationPoliciesDTO getApiPolicies() {
        return apiPolicies;
    }

    public void setApiPolicies(APIOperationPoliciesDTO apiPolicies) {
        this.apiPolicies = apiPolicies;
    }

    @ApiModelProperty(example = "CURRENT_TENANT", value = "The subscription availability. Accepts one of the " +
            "following. CURRENT_TENANT, ALL_TENANTS or SPECIFIC_TENANTS.")
    @JsonProperty("subscriptionAvailability")
    public SubscriptionAvailabilityEnum getSubscriptionAvailability() {
        return subscriptionAvailability;
    }

    public void setSubscriptionAvailability(SubscriptionAvailabilityEnum subscriptionAvailability) {
        this.subscriptionAvailability = subscriptionAvailability;
    }

    @ApiModelProperty(value = "Map of custom properties of API")
    @Valid
    @JsonProperty("additionalProperties")
    public List<APIInfoAdditionalPropertiesDTO> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(List<APIInfoAdditionalPropertiesDTO> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("additionalPropertiesMap")
    public Map<String, APIInfoAdditionalPropertiesMapDTO> getAdditionalPropertiesMap() {
        return additionalPropertiesMap;
    }

    public void setAdditionalPropertiesMap(Map<String, APIInfoAdditionalPropertiesMapDTO> additionalPropertiesMap) {
        this.additionalPropertiesMap = additionalPropertiesMap;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("monetization")
    public APIMonetizationInfoDTO getMonetization() {
        return monetization;
    }

    public void setMonetization(APIMonetizationInfoDTO monetization) {
        this.monetization = monetization;
    }

    @ApiModelProperty(value = "Is the API is restricted to certain set of publishers or creators or is it visible" +
            " to all the publishers and creators. If the accessControl restriction is none, this API can be modified" +
            " by all the publishers and creators, if not it can only be viewable/modifiable by certain set of" +
            " publishers and creators,  based on the restriction. ")
    @JsonProperty("accessControl")
    public AccessControlEnum getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(AccessControlEnum accessControl) {
        this.accessControl = accessControl;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("websubSubscriptionConfiguration")
    public WebsubSubscriptionConfigurationDTO getWebsubSubscriptionConfiguration() {
        return websubSubscriptionConfiguration;
    }

    public void setWebsubSubscriptionConfiguration(WebsubSubscriptionConfigurationDTO websubSubscriptionConfiguration) {
        this.websubSubscriptionConfiguration = websubSubscriptionConfiguration;
    }

    @ApiModelProperty(example = "APPROVED", value = "")
    @JsonProperty("workflowStatus")
    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("createdTime")
    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("lastUpdatedTimestamp")
    public String getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(String lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("lastUpdatedTime")
    public String getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(String lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    @ApiModelProperty(example = "{\"endpoint_type\":\"http\",\"sandbox_endpoints\":{\"url\":\"https://localhost:" +
            "9443/am/sample/pizzashack/v3/api/\"},\"production_endpoints\":{\"url\":\"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/\"}}", value = "Endpoint configuration of the API. This can be used to" +
            " provide different types of endpoints including Simple REST Endpoints, Loadbalanced and Failover.  " +
            "`Simple REST Endpoint`    {     \"endpoint_type\": \"http\",     \"sandbox_endpoints\":       {        " +
            "\"url\": \"https://localhost:9443/am/sample/pizzashack/v3/api/\"     },     \"production_endpoints\":" +
            "       {        \"url\": \"https://localhost:9443/am/sample/pizzashack/v3/api/\"     }   }  " +
            "`Loadbalanced Endpoint`    {     \"endpoint_type\": \"load_balance\",     \"algoCombo\":" +
            " \"org.apache.synapse.endpoints.algorithms.RoundRobin\",     \"sessionManagement\": \"\", " +
            "    \"sandbox_endpoints\":       [                 {           \"url\": \"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/1\"        },                 {           \"endpoint_type\": \"http\"," +
            "           \"template_not_supported\": false,           \"url\": \"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/2\"        }     ],     \"production_endpoints\":       " +
            "[                 {           \"url\": \"https://localhost:9443/am/sample/pizzashack/v3/api/3\"" +
            "        },                 {           \"endpoint_type\": \"http\",           " +
            "\"template_not_supported\": false,           \"url\": \"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/4\"        }     ],     \"sessionTimeOut\": \"\",     \"algoClassName\":" +
            " \"org.apache.synapse.endpoints.algorithms.RoundRobin\"   }  `Failover Endpoint`    " +
            "{     \"production_failovers\":[        {           \"endpoint_type\":\"http\",           " +
            "\"template_not_supported\":false,           \"url\":\"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/1\"        }     ],     \"endpoint_type\":\"failover\",     " +
            "\"sandbox_endpoints\":{        \"url\":\"https://localhost:9443/am/sample/pizzashack/v3/api/2\"     }," +
            "     \"production_endpoints\":{        \"url\":\"https://localhost:9443/am/sample/pizzashack/v3/api/3\"" +
            "     },     \"sandbox_failovers\":[        {           \"endpoint_type\":\"http\",           " +
            "\"template_not_supported\":false,           \"url\":\"https://localhost:9443" +
            "/am/sample/pizzashack/v3/api/4\"        }     ]   }  `Default Endpoint`    {     \"endpoint_type\":" +
            "\"default\",     \"sandbox_endpoints\":{        \"url\":\"default\"     },     \"production_endpoints\":" +
            "{        \"url\":\"default\"     }   }  `Endpoint from Endpoint Registry`    {     \"endpoint_type\":" +
            " \"Registry\",     \"endpoint_id\": \"{registry-name:entry-name:version}\",   }" +
            "  `AWS Lambda as Endpoint`    {     \"endpoint_type\":\"awslambda\",     \"access_method\":" +
            "\"role-supplied|stored\",     \"assume_role\":true|false,     \"amznAccessKey\":\"access_method==stored?" +
            "<accessKey>:<empty>\",     \"amznSecretKey\":\"access_method==stored?<secretKey>:<empty>\",     " +
            "\"amznRegion\":\"access_method==stored?<region>:<empty>\",     \"amznRoleArn\":" +
            "\"assume_role==true?<roleArn>:<empty>\",     \"amznRoleSessionName\":\"assume_role==true?" +
            "<roleSessionName>:<empty>\",     \"amznRoleRegion\":\"assume_role==true?<roleRegion>:<empty>\"   } ")
    @Valid
    @JsonProperty("endpointConfig")
    public Object getEndpointConfig() {
        return endpointConfig;
    }

    public void setEndpointConfig(Object endpointConfig) {
        this.endpointConfig = endpointConfig;
    }

    @ApiModelProperty(example = "INLINE", value = "")
    @JsonProperty("endpointImplementationType")
    public EndpointImplementationTypeEnum getEndpointImplementationType() {
        return endpointImplementationType;
    }

    public void setEndpointImplementationType(EndpointImplementationTypeEnum endpointImplementationType) {
        this.endpointImplementationType = endpointImplementationType;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("scopes")
    public List<APIScopeDTO> getScopes() {
        return scopes;
    }

    public void setScopes(List<APIScopeDTO> scopes) {
        this.scopes = scopes;
    }

    @ApiModelProperty(example = "[{\"target\":\"/order/{orderId}\",\"verb\":\"POST\",\"authType\":\"Application & " +
            "Application User\",\"throttlingPolicy\":\"Unlimited\"},{\"target\":\"/menu\",\"verb\":\"GET\"," +
            "\"authType\":\"Application & Application User\",\"throttlingPolicy\":\"Unlimited\"}]", value = "")
    @Valid
    @JsonProperty("operations")
    public List<APIOperationsDTO> getOperations() {
        return operations;
    }

    public void setOperations(List<APIOperationsDTO> operations) {
        this.operations = operations;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("threatProtectionPolicies")
    public APIThreatProtectionPoliciesDTO getThreatProtectionPolicies() {
        return threatProtectionPolicies;
    }

    public void setThreatProtectionPolicies(APIThreatProtectionPoliciesDTO threatProtectionPolicies) {
        this.threatProtectionPolicies = threatProtectionPolicies;
    }

    @ApiModelProperty(value = "API categories ")
    @JsonProperty("categories")
    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @ApiModelProperty(value = "API Key Managers ")
    @Valid
    @JsonProperty("keyManagers")
    public Object getKeyManagers() {
        return keyManagers;
    }

    public void setKeyManagers(Object keyManagers) {
        this.keyManagers = keyManagers;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("serviceInfo")
    public APIServiceInfoDTO getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(APIServiceInfoDTO serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @ApiModelProperty(value = "")
    @Valid
    @JsonProperty("advertiseInfo")
    public AdvertiseInfoDTO getAdvertiseInfo() {
        return advertiseInfo;
    }

    public void setAdvertiseInfo(AdvertiseInfoDTO advertiseInfo) {
        this.advertiseInfo = advertiseInfo;
    }

    @ApiModelProperty(example = "wso2", value = "")
    @JsonProperty("gatewayVendor")
    public String getGatewayVendor() {
        return gatewayVendor;
    }

    public void setGatewayVendor(String gatewayVendor) {
        this.gatewayVendor = gatewayVendor;
    }

    @ApiModelProperty(example = "wso2/synapse", value = "The gateway type selected for the API policies. Accepts" +
            " one of the following. wso2/synapse, wso2/apk.")
    @JsonProperty("gatewayType")
    public String getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    @ApiModelProperty(example = "[\"http\",\"https\"]", value = "Supported transports for the async API " +
            "(http and/or https). ")
    @JsonProperty("asyncTransportProtocols")
    public List<String> getAsyncTransportProtocols() {
        return asyncTransportProtocols;
    }

    public void setAsyncTransportProtocols(List<String> asyncTransportProtocols) {
        this.asyncTransportProtocols = asyncTransportProtocols;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public String getApiLevelPolicy() {
        return apiThrottlingPolicy;
    }

    @Override
    public void setApiLevelPolicy(String apiLevelPolicy) {
        this.apiThrottlingPolicy = apiLevelPolicy;
    }

    @Override
    public String getResponseCaching() {
        if (responseCachingEnabled) {
            return "Enabled";
        } else {
            return "Disabled";
        }
    }

    @Override
    public void setResponseCaching(String responseCaching) {
        this.responseCachingEnabled = responseCaching.equals("Enabled");
    }

    @Override
    public String getEndpointConfigStr() {
        return endpointConfig.toString();
    }

    @Override
    public void setEndpointConfigStr(String endpointConfig) {
        this.endpointConfig = endpointConfig;
    }
}
