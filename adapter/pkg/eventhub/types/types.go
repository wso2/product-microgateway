/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package types

// Subscription for struct subscription
type Subscription struct {
	SubscriptionID    int32  `json:"subscriptionId"`
	SubscriptionUUID  string `json:"subscriptionUUID"`
	PolicyID          string `json:"policyId"`
	APIID             int32  `json:"apiId"`
	APIUUID           string `json:"apiUUID"`
	AppID             int32  `json:"appId" json:"applicationId"`
	ApplicationUUID   string `json:"applicationUUID"`
	SubscriptionState string `json:"subscriptionState"`
	TenantID          int32  `json:"tenanId,omitempty"`
	TenantDomain      string `json:"tenanDomain,omitempty"`
	TimeStamp         int64  `json:"timeStamp,omitempty"`
}

// SubscriptionList for struct list of applications
type SubscriptionList struct {
	List []Subscription `json:"list"`
}

// Application for struct application
type Application struct {
	UUID         string            `json:"uuid"`
	ID           int32             `json:"id" json:"applicationId"`
	Name         string            `json:"name" json:"applicationName"`
	SubName      string            `json:"subName" json:"subscriber"`
	Policy       string            `json:"policy" json:"applicationPolicy"`
	TokenType    string            `json:"tokenType"`
	GroupIds     []string          `json:"groupIds"`
	Attributes   map[string]string `json:"attributes"`
	TenantID     int32             `json:"tenanId,omitempty"`
	TenantDomain string            `json:"tenanDomain,omitempty"`
	TimeStamp    int64             `json:"timeStamp,omitempty"`
}

// ApplicationList for struct list of application
type ApplicationList struct {
	List []Application `json:"list"`
}

// ApplicationKeyMapping for struct applicationKeyMapping
type ApplicationKeyMapping struct {
	ApplicationID   int32  `json:"applicationId"`
	ApplicationUUID string `json:"applicationUUID"`
	ConsumerKey     string `json:"consumerKey"`
	KeyType         string `json:"keyType"`
	KeyManager      string `json:"keyManager"`
	TenantID        int32  `json:"tenanId,omitempty"`
	TenantDomain    string `json:"tenanDomain,omitempty"`
	TimeStamp       int64  `json:"timeStamp,omitempty"`
}

// ApplicationKeyMappingList for struct list of applicationKeyMapping
type ApplicationKeyMappingList struct {
	List []ApplicationKeyMapping `json:"list"`
}

// API for struct Api
type API struct {
	UUID             string `json:"uuid"`
	IsDefaultVersion bool   `json:"isDefaultVersion"`
	APIStatus        string `json:"status"`
	TimeStamp        int64  `json:"timeStamp,omitempty"`
}

// APIList for struct ApiList
type APIList struct {
	List []API `json:"list"`
}

// ApplicationPolicy for struct ApplicationPolicy
type ApplicationPolicy struct {
	ID        int32  `json:"id"`
	TenantID  int32  `json:"tenantId"`
	Name      string `json:"name"`
	QuotaType string `json:"quotaType"`
}

// ApplicationPolicyList for struct list of ApplicationPolicy
type ApplicationPolicyList struct {
	List []ApplicationPolicy `json:"list"`
}

// SubscriptionPolicy for struct list of SubscriptionPolicy
type SubscriptionPolicy struct {
	ID                   int32  `json:"id" json:"policyId"`
	TenantID             int32  `json:"tenantId"`
	Name                 string `json:"name"`
	QuotaType            string `json:"quotaType"`
	GraphQLMaxComplexity int32  `json:"graphQLMaxComplexity"`
	GraphQLMaxDepth      int32  `json:"graphQLMaxDepth"`
	RateLimitCount       int32  `json:"rateLimitCount"`
	RateLimitTimeUnit    string `json:"rateLimitTimeUnit"`
	StopOnQuotaReach     bool   `json:"stopOnQuotaReach"`
	TenantDomain         string `json:"tenanDomain,omitempty"`
	TimeStamp            int64  `json:"timeStamp,omitempty"`
}

// SubscriptionPolicyList for struct list of SubscriptionPolicy
type SubscriptionPolicyList struct {
	List []SubscriptionPolicy `json:"list"`
}

// APIPolicy for struct policy Info events
type APIPolicy struct {
	PolicyID                 string `json:"policyId"`
	PolicyName               string `json:"policyName"`
	QuotaType                string `json:"quotaType"`
	PolicyType               string `json:"policyType"`
	AddedConditionGroupIds   string `json:"addedConditionGroupIds"`
	DeletedConditionGroupIds string `json:"deletedConditionGroupIds"`
	TimeStamp                int64  `json:"timeStamp,omitempty"`
}

// Scope for struct Scope
type Scope struct {
	Name            string `json:"name"`
	DisplayName     string `json:"displayName"`
	ApplicationName string `json:"description"`
}

// ScopeList for struct list of Scope
type ScopeList struct {
	List []Scope `json:"list"`
}

// KeyManagerList for struct list of KeyManager
type KeyManagerList struct {
	KeyManagers []KeyManager `json:"KeyManager"`
}

// KeyManager for struct
type KeyManager struct {
	Name          string `json:"name"`
	Type          string `json:"type"`
	Enabled       bool   `json:"enabled"`
	TenantDomain  string `json:"tenantDomain,omitempty"`
	Configuration map[string]interface{}
	// Configuration KeyManagerConfig `json:"configuration"`
}

// KeyManagerConfig for struct Configuration map[string]interface{} `json:"value"`
type KeyManagerConfig struct {
	TokenFormatString          string   `json:"token_format_string"`
	ServerURL                  string   `json:"ServerURL"`
	ValidationEnable           bool     `json:"validation_enable"`
	ClaimMappings              []Claim  `json:"Claim"`
	GrantTypes                 []string `json:"grant_types"`
	EncryptPersistedTokens     bool     `json:"OAuthConfigurations.EncryptPersistedTokens"`
	EnableOauthAppCreation     bool     `json:"enable_oauth_app_creation"`
	ValidityPeriod             string   `json:"VALIDITY_PERIOD"`
	EnableTokenGeneration      bool     `json:"enable_token_generation"`
	Issuer                     string   `json:"issuer"`
	EnableMapOauthConsumerApps bool     `json:"enable_map_oauth_consumer_apps"`
	EnableTokenHash            bool     `json:"enable_token_hash"`
	SelfValidateJwt            bool     `json:"self_validate_jwt"`
	RevokeEndpoint             string   `json:"revoke_endpoint"`
	EnableTokenEncryption      bool     `json:"enable_token_encryption"`
	RevokeURL                  string   `json:"RevokeURL"`
	TokenURL                   string   `json:"TokenURL,token_endpoint"`
	CertificateType            string   `json:"certificate_type"`
	CertificateValue           string   `json:"certificate_value"`
}

// Claim for struct
type Claim struct {
	RemoteClaim string `json:"remoteClaim"`
	LocalClaim  string `json:"localClaim"`
}
