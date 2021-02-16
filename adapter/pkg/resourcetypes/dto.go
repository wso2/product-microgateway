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

package resourcetypes

// Subscription for struct subscription
type Subscription struct {
	SubscriptionID    int32  `json:"subscriptionId"`
	PolicyID          string `json:"policyId"`
	APIID             int32  `json:"apiId"`
	AppID             int32  `json:"appId" json:"applicationId"`
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
	ApplicationID int32  `json:"applicationId"`
	ConsumerKey   string `json:"consumerKey"`
	KeyType       string `json:"keyType"`
	KeyManager    string `json:"keyManager"`
	TenantID      int32  `json:"tenanId,omitempty"`
	TenantDomain  string `json:"tenanDomain,omitempty"`
	TimeStamp     int64  `json:"timeStamp,omitempty"`
}

// ApplicationKeyMappingList for struct list of applicationKeyMapping
type ApplicationKeyMappingList struct {
	List []ApplicationKeyMapping `json:"list"`
}

// API for struct Api
type API struct {
	APIID            string `json:"apiId"`
	UUID             string `json:"uuid"`
	Provider         string `json:"provider" json:"apiProvider"`
	Name             string `json:"name" json:"apiName"`
	Version          string `json:"version" json:"apiVersion"`
	Context          string `json:"context" json:"apiContext"`
	Policy           string `json:"policy"`
	APIType          string `json:"apiType"`
	IsDefaultVersion bool   `json:"isDefaultVersion"`
	APIStatus        string `json:"apiStatus"`
	TenantID         int32  `json:"tenanId,omitempty"`
	TenantDomain     string `json:"tenanDomain,omitempty"`
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
