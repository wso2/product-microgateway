// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package cache

import (
	"fmt"

	envoy_types "github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/api"
	apikmgt "github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/apkmgt"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/config/enforcer"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/ga"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/keymgt"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/throttle"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/cache/types"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/protocol/resource/v3"
)

// GetResponseType returns the enumeration for a valid xDS type URL
func GetResponseType(typeURL string) types.ResponseType {
	switch typeURL {
	case resource.ConfigType:
		return types.Config
	case resource.APIType:
		return types.API
	case resource.SubscriptionListType:
		return types.SubscriptionList
	case resource.APIListType:
		return types.APIList
	case resource.ApplicationListType:
		return types.ApplicationList
	case resource.ApplicationPolicyListType:
		return types.ApplicationPolicyList
	case resource.SubscriptionPolicyListType:
		return types.SubscriptionPolicyList
	case resource.ApplicationKeyMappingListType:
		return types.ApplicationKeyMappingList
	case resource.KeyManagerType:
		return types.KeyManagerConfig
	case resource.RevokedTokensType:
		return types.RevokedTokens
	case resource.ThrottleDataType:
		return types.ThrottleData
	case resource.GAAPIType:
		return types.GAAPI
	case resource.APKMgtApplicationType:
		return types.APKMgtApplicationList
	}
	return types.UnknownType
}

// GetResourceName returns the resource name for a valid xDS response type.
func GetResourceName(res envoy_types.Resource) string {
	// Since Applications, Subscriptions, API-Metadata, Application Policies and Subscription Policies
	// are always maintained under a single list, there is no need to have separate key.
	// (Compared to GAAPI and API)
	switch v := res.(type) {
	case *api.Api:
		return fmt.Sprint(v.Vhost, v.BasePath, v.Version)
	case *enforcer.Config:
		return "Config"
	case *subscription.SubscriptionList:
		return "Subscription"
	case *subscription.ApplicationList:
		return "Application"
	case *subscription.ApplicationKeyMappingList:
		return "ApplicationKeyMapping"
	case *subscription.APIList:
		return "APIList"
	case *subscription.ApplicationPolicyList:
		return "ApplicationPolicyList"
	case *subscription.SubscriptionPolicyList:
		return "SubscriptionPolicyList"
	case *keymgt.KeyManagerConfig:
		return fmt.Sprint(v.Name)
	case *throttle.ThrottleData:
		return "ThrottleData"
	case *ga.Api:
		return fmt.Sprint(v.ApiUUID)
	case *apikmgt.Application:
		return fmt.Sprint(v.Uuid)
	case *keymgt.RevokedToken:
		return fmt.Sprint(v.Jti)
	default:
		return ""
	}
}
