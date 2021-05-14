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

// Package messaging holds the implementation for event listeners functions
package messaging

// EventNotification for struct event notifications
type EventNotification struct {
	Event struct {
		PayloadData struct {
			EventType string  `json:"eventType"`
			Timstamp  float64 `json:"timeStamp"`
			Event     string  `json:"event"`
		} `json:"payloadData"`
	} `json:"event"`
}

// EventThrottleData for struct throttle events
type EventThrottleData struct {
	Event struct {
		PayloadData struct {
			KeyTemplateValue  string `json:"keyTemplateValue"`
			KeyTemplateState  string `json:"keyTemplateState"`
			ID                int32  `json:"id"`
			BlockingCondition string `json:"blockingCondition"`
			ConditionValue    string `json:"conditionValue"`
			State             string `json:"state"`
			TenantDomain      string `json:"tenantDomain"`
		} `json:"payloadData"`
	} `json:"event"`
}

// EventTokenRevocationNotification for struct
type EventTokenRevocationNotification struct {
	Event struct {
		PayloadData struct {
			EventID      string `json:"eventId"`
			RevokedToken string `json:"revokedToken"`
			TTL          string `json:"ttl"`
			ExpiryTime   int64  `json:"expiryTime"`
			Type         string `json:"type"`
			TenantID     int    `json:"tenantId"`
		} `json:"payloadData"`
	} `json:"event"`
}

// EventKeyManagerNotification for struct
type EventKeyManagerNotification struct {
	Event struct {
		PayloadData struct {
			EventType    string `json:"event_type"`
			Name         string `json:"name"`
			Action       string `json:"action"`
			Type         string `json:"type"`
			Enabled      bool   `json:"enabled"`
			Value        string `json:"value"`
			TenantID     string `json:"tenantId,omitempty"`
			TenantDomain string `json:"tenantDomain,omitempty"`
		} `json:"payloadData"`
	} `json:"event"`
}

// Event for struct abstract event
type Event struct {
	EventID      string `json:"eventId"`
	TimeStamp    int64  `json:"timeStamp"`
	Type         string `json:"type"`
	TenantID     int32  `json:"tenantId"`
	TenantDomain string `json:"tenantDomain"`
}

// APIEvent for struct API events
type APIEvent struct {
	APIID         int      `json:"apiId"`
	UUID          string   `json:"uuid"`
	GatewayLabels []string `json:"gatewayLabels"`
	APIVersion    string   `json:"apiVersion"`
	APIContext    string   `json:"apiContext"`
	APIName       string   `json:"apiName"`
	APIProvider   string   `json:"apiProvider"`
	APIStatus     string   `json:"apiStatus"`
	APIType       string   `json:"apiType"`
	Event
	// TODO: (VirajSalaka) Remove this when the event is fixed from APIM side
	Version string `json:"version"`
	Context string `json:"context"`
	Name    string `json:"name"`
}

// ApplicationRegistrationEvent for struct application registration events
type ApplicationRegistrationEvent struct {
	ApplicationID   int32  `json:"applicationId"`
	ApplicationUUID string `json:"applicationUUID"`
	ConsumerKey     string `json:"consumerKey"`
	KeyType         string `json:"keyType"`
	KeyManager      string `json:"keyManager"`
	Event
}

// ApplicationEvent for struct application events
type ApplicationEvent struct {
	UUID              string      `json:"uuid"`
	ApplicationID     int32       `json:"applicationId"`
	ApplicationName   string      `json:"applicationName"`
	TokenType         string      `json:"tokenType"`
	ApplicationPolicy string      `json:"applicationPolicy"`
	Attributes        interface{} `json:"attributes"`
	Subscriber        string      `json:"subscriber"`
	GroupID           []string    `json:"groupIds"`
	Event
}

// SubscriptionEvent for struct subscription events
type SubscriptionEvent struct {
	SubscriptionID    int32  `json:"subscriptionId"`
	SubscriptionUUID  string `json:"subscriptionUUID"`
	APIID             int32  `json:"apiId"`
	APIUUID           string `json:"apiUUID"`
	ApplicationID     int32  `json:"applicationId"`
	ApplicationUUID   string `json:"applicationUUID"`
	PolicyID          string `json:"policyId"`
	SubscriptionState string `json:"subscriptionState"`
	Event
}

// ScopeEvent for struct scope events
type ScopeEvent struct {
	Name            string `json:"name"`
	DisplayName     string `json:"displayName"`
	ApplicationName string `json:"description"`
	Event
}

// PolicyInfo for struct policy Info events
type PolicyInfo struct {
	PolicyID   int32  `json:"policyId"`
	PolicyName string `json:"policyName"`
	QuotaType  string `json:"quotaType"`
	PolicyType string `json:"policyType"`
	Event
}

// APIPolicyEvent for struct API policy events
type APIPolicyEvent struct {
	PolicyInfo
	AddedConditionGroupIds   string `json:"addedConditionGroupIds"`
	DeletedConditionGroupIds string `json:"deletedConditionGroupIds"`
}

// SubscriptionPolicyEvent for struct subscriptionPolicy events
type SubscriptionPolicyEvent struct {
	PolicyInfo
	RateLimitCount       int32  `json:"rateLimitCount"`
	RateLimitTimeUnit    string `json:"rateLimitTimeUnit"`
	StopOnQuotaReach     bool   `json:"stopOnQuotaReach"`
	GraphQLMaxComplexity int32  `json:"graphQLMaxComplexity"`
	GraphQLMaxDepth      int32  `json:"graphQLMaxDepth"`
}

// KeyManagerEvent for struct
type KeyManagerEvent struct {
	ServerURL                  string   `json:"ServerURL"`
	ValidationEnable           bool     `json:"validation_enable"`
	ClaimMappings              []Claim  `json:"Claim"`
	GrantTypes                 []string `json:"grant_types"`
	EncryptPersistedTokens     bool     `json:"OAuthConfigurations.EncryptPersistedTokens"`
	EnableOauthAppCreation     bool     `json:"enable_oauth_app_creation"`
	ValidityPeriod             string   `json:"VALIDITY_PERIOD"`
	CertificateValue           string   `json:"certificate_value"`
	EnableTokenGeneration      bool     `json:"enable_token_generation"`
	Issuer                     string   `json:"issuer"`
	EnableMapOauthConsumerApps bool     `json:"enable_map_oauth_consumer_apps"`
	EnableTokenHash            bool     `json:"enable_token_hash"`
	SelfValidateJwt            bool     `json:"self_validate_jwt"`
	RevokeEndpoint             string   `json:"revoke_endpoint"`
	EnableTokenEncryption      bool     `json:"enable_token_encryption"`
	RevokeURL                  string   `json:"RevokeURL"`
	TokenURL                   string   `json:"TokenURL"`
	TokenFormatString          string   `json:"token_format_string"`
	CertificateType            string   `json:"certificate_type"`
	TokenEndpoint              string   `json:"token_endpoint"`
}

// Claim for struct
type Claim struct {
	remoteClaim string
	localClaim  string
}
