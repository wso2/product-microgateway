package xds

import (
	"fmt"
	"strconv"

	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/config/enforcer"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/micro-gw/internal/eventhub/types"
)

// MarshalConfig will marshal a Config struct - read from the config toml - to
// enfocer's CDS resource representation.
func MarshalConfig(config *config.Config) *enforcer.Config {
	issuers := []*enforcer.Issuer{}
	urlGroups := []*enforcer.TMURLGroup{}

	for _, issuer := range config.Enforcer.JwtTokenConfig {
		jwtConfig := &enforcer.Issuer{
			CertificateAlias:     issuer.CertificateAlias,
			ConsumerKeyClaim:     issuer.ConsumerKeyClaim,
			Issuer:               issuer.Issuer,
			Name:                 issuer.Name,
			ValidateSubscription: issuer.ValidateSubscription,
			JwksURL:              issuer.JwksURL,
			CertificateFilePath:  issuer.CertificateFilePath,
		}
		issuers = append(issuers, jwtConfig)
	}

	for _, urlGroup := range config.Enforcer.ThrottlingConfig.Binary.URLGroup {
		group := &enforcer.TMURLGroup{
			AuthURLs:     urlGroup.AuthURLs,
			ReceiverURLs: urlGroup.ReceiverURLs,
			Type:         urlGroup.Type,
		}
		urlGroups = append(urlGroups, group)
	}

	authService := &enforcer.AuthService{
		KeepAliveTime:  config.Enforcer.AuthService.KeepAliveTime,
		MaxHeaderLimit: config.Enforcer.AuthService.MaxHeaderLimit,
		MaxMessageSize: config.Enforcer.AuthService.MaxMessageSize,
		Port:           config.Enforcer.AuthService.Port,
		ThreadPool: &enforcer.ThreadPool{
			CoreSize:      config.Enforcer.AuthService.ThreadPool.CoreSize,
			KeepAliveTime: config.Enforcer.AuthService.ThreadPool.KeepAliveTime,
			MaxSize:       config.Enforcer.AuthService.ThreadPool.MaxSize,
			QueueSize:     config.Enforcer.AuthService.ThreadPool.QueueSize,
		},
	}

	cache := &enforcer.Cache{
		Enable:      config.Enforcer.Cache.Enabled,
		MaximumSize: config.Enforcer.Cache.MaximumSize,
		ExpiryTime:  config.Enforcer.Cache.ExpiryTime,
	}

	return &enforcer.Config{
		ApimCredentials: &enforcer.AmCredentials{
			Username: config.Enforcer.ApimCredentials.Username,
			Password: config.Enforcer.ApimCredentials.Password,
		},
		JwtGenerator: &enforcer.JWTGenerator{
			Enable:                config.Enforcer.JwtGenerator.Enable,
			Encoding:              config.Enforcer.JwtGenerator.Encoding,
			ClaimDialect:          config.Enforcer.JwtGenerator.ClaimDialect,
			ConvertDialect:        config.Enforcer.JwtGenerator.ConvertDialect,
			Header:                config.Enforcer.JwtGenerator.Header,
			SigningAlgorithm:      config.Enforcer.JwtGenerator.SigningAlgorithm,
			EnableUserClaims:      config.Enforcer.JwtGenerator.EnableUserClaims,
			GatewayGeneratorImpl:  config.Enforcer.JwtGenerator.GatewayGeneratorImpl,
			ClaimsExtractorImpl:   config.Enforcer.JwtGenerator.ClaimsExtractorImpl,
			PublicCertificatePath: config.Enforcer.JwtGenerator.PublicCertificatePath,
			PrivateKeyPath:        config.Enforcer.JwtGenerator.PrivateKeyPath,
		},
		AuthService:    authService,
		JwtTokenConfig: issuers,
		Cache:          cache,
		Eventhub: &enforcer.EventHub{
			Enabled:    config.ControlPlane.EventHub.Enabled,
			ServiceUrl: config.ControlPlane.EventHub.ServiceURL,
			JmsConnectionParameters: &enforcer.JmsConnectionParameters{
				EventListeningEndpoints: config.ControlPlane.EventHub.JmsConnectionParameters.EventListeningEndpoints,
			},
		},
		ThrottlingConfig: &enforcer.Throttling{
			Binary: &enforcer.BinaryThrottling{
				Enabled:  config.Enforcer.ThrottlingConfig.Binary.Enabled,
				Username: config.Enforcer.ThrottlingConfig.Binary.Username,
				Password: config.Enforcer.ThrottlingConfig.Binary.Password,
				UrlGroup: urlGroups,
				Publisher: &enforcer.ThrottlePublisher{
					InitIdleObjectDataPublishingAgents: config.Enforcer.ThrottlingConfig.Binary.Publisher.InitIdleObjectDataPublishingAgents,
					MaxIdleDataPublishingAgents:        config.Enforcer.ThrottlingConfig.Binary.Publisher.MaxIdleDataPublishingAgents,
					PublisherThreadPoolCoreSize:        config.Enforcer.ThrottlingConfig.Binary.Publisher.PublisherThreadPoolCoreSize,
					PublisherThreadPoolKeepAliveTime:   config.Enforcer.ThrottlingConfig.Binary.Publisher.PublisherThreadPoolKeepAliveTime,
					PublisherThreadPoolMaximumSize:     config.Enforcer.ThrottlingConfig.Binary.Publisher.PublisherThreadPoolMaximumSize,
				},
				Agent: &enforcer.ThrottleAgent{
					BatchSize:                  config.Enforcer.ThrottlingConfig.Binary.Agent.BatchSize,
					Ciphers:                    config.Enforcer.ThrottlingConfig.Binary.Agent.Ciphers,
					CorePoolSize:               config.Enforcer.ThrottlingConfig.Binary.Agent.CorePoolSize,
					EvictionTimePeriod:         config.Enforcer.ThrottlingConfig.Binary.Agent.EvictionTimePeriod,
					KeepAliveTimeInPool:        config.Enforcer.ThrottlingConfig.Binary.Agent.KeepAliveTimeInPool,
					MaxIdleConnections:         config.Enforcer.ThrottlingConfig.Binary.Agent.MaxIdleConnections,
					MaxPoolSize:                config.Enforcer.ThrottlingConfig.Binary.Agent.MaxPoolSize,
					MaxTransportPoolSize:       config.Enforcer.ThrottlingConfig.Binary.Agent.MaxTransportPoolSize,
					MinIdleTimeInPool:          config.Enforcer.ThrottlingConfig.Binary.Agent.MinIdleTimeInPool,
					QueueSize:                  config.Enforcer.ThrottlingConfig.Binary.Agent.QueueSize,
					ReconnectionInterval:       config.Enforcer.ThrottlingConfig.Binary.Agent.ReconnectionInterval,
					SecureEvictionTimePeriod:   config.Enforcer.ThrottlingConfig.Binary.Agent.SecureEvictionTimePeriod,
					SecureMaxIdleConnections:   config.Enforcer.ThrottlingConfig.Binary.Agent.SecureMaxIdleConnections,
					SecureMaxTransportPoolSize: config.Enforcer.ThrottlingConfig.Binary.Agent.SecureMaxTransportPoolSize,
					SecureMinIdleTimeInPool:    config.Enforcer.ThrottlingConfig.Binary.Agent.SecureMinIdleTimeInPool,
					SocketTimeoutMS:            config.Enforcer.ThrottlingConfig.Binary.Agent.SocketTimeoutMS,
					SslEnabledProtocols:        config.Enforcer.ThrottlingConfig.Binary.Agent.SslEnabledProtocols,
				},
			},
		},
	}
}

// MarshalSubscriptionList converts the data into SubscriptionList proto type
func MarshalSubscriptionList(subList *types.SubscriptionList) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}

	for _, sb := range subList.List {
		sub := &subscription.Subscription{
			SubscriptionId:    fmt.Sprint(sb.SubscriptionID),
			PolicyId:          sb.PolicyID,
			ApiId:             sb.APIID,
			AppId:             sb.AppID,
			SubscriptionState: sb.SubscriptionState,
			TimeStamp:         sb.TimeStamp,
			TenantId:          sb.TenantID,
			TenantDomain:      sb.TenantDomain,
		}
		subscriptions = append(subscriptions, sub)
	}

	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

// MarshalApplicationList converts the data into ApplicationList proto type
func MarshalApplicationList(appList *types.ApplicationList) *subscription.ApplicationList {
	applications := []*subscription.Application{}

	for _, app := range appList.List {
		application := &subscription.Application{
			Uuid:         app.UUID,
			Id:           app.ID,
			Name:         app.Name,
			SubId:        app.ID,
			SubName:      app.SubName,
			Policy:       app.Policy,
			TokenType:    app.TokenType,
			GroupIds:     app.GroupIds,
			Attributes:   app.Attributes,
			TenantId:     app.TenantID,
			TenantDomain: app.TenantDomain,
			Timestamp:    app.TimeStamp,
		}
		applications = append(applications, application)
	}

	return &subscription.ApplicationList{
		List: applications,
	}
}

// MarshalAPIList converts the data into APIList proto type
func MarshalAPIList(apiList *types.APIList) *subscription.APIList {
	apis := []*subscription.APIs{}

	for _, api := range apiList.List {
		newAPI := &subscription.APIs{
			ApiId:            strconv.Itoa(api.APIID),
			Name:             api.Name,
			Provider:         api.Provider,
			Version:          api.Version,
			Context:          api.Context,
			Policy:           api.Policy,
			ApiType:          api.APIType,
			IsDefaultVersion: api.IsDefaultVersion,
		}
		apis = append(apis, newAPI)
	}

	return &subscription.APIList{
		List: apis,
	}
}

// MarshalApplicationPolicyList converts the data into ApplicationPolicyList proto type
func MarshalApplicationPolicyList(appPolicyList *types.ApplicationPolicyList) *subscription.ApplicationPolicyList {
	applicationPolicies := []*subscription.ApplicationPolicy{}

	for _, policy := range appPolicyList.List {
		appPolicy := &subscription.ApplicationPolicy{
			Id:        policy.ID,
			TenantId:  policy.TenantID,
			Name:      policy.Name,
			QuotaType: policy.QuotaType,
		}
		applicationPolicies = append(applicationPolicies, appPolicy)
	}

	return &subscription.ApplicationPolicyList{
		List: applicationPolicies,
	}
}

// MarshalSubscriptionPolicyList converts the data into SubscriptionPolicyList proto type
func MarshalSubscriptionPolicyList(subPolicyList *types.SubscriptionPolicyList) *subscription.SubscriptionPolicyList {
	subscriptionPolicies := []*subscription.SubscriptionPolicy{}

	for _, policy := range subPolicyList.List {
		subPolicy := &subscription.SubscriptionPolicy{
			Id:                   policy.ID,
			Name:                 policy.Name,
			QuotaType:            policy.QuotaType,
			GraphQLMaxComplexity: policy.GraphQLMaxComplexity,
			GraphQLMaxDepth:      policy.GraphQLMaxDepth,
			RateLimitCount:       policy.RateLimitCount,
			RateLimitTimeUnit:    policy.RateLimitTimeUnit,
			StopOnQuotaReach:     policy.StopOnQuotaReach,
			TenantId:             policy.TenantID,
			TenantDomain:         policy.TenantDomain,
			Timestamp:            policy.TimeStamp,
		}
		subscriptionPolicies = append(subscriptionPolicies, subPolicy)
	}

	return &subscription.SubscriptionPolicyList{
		List: subscriptionPolicies,
	}
}

// MarshalKeyMappingList converts the data into ApplicationKeyMappingList proto type
func MarshalKeyMappingList(keyMappingList *types.ApplicationKeyMappingList) *subscription.ApplicationKeyMappingList {
	applicationKeyMappings := []*subscription.ApplicationKeyMapping{}

	for _, mapping := range keyMappingList.List {
		keyMapping := &subscription.ApplicationKeyMapping{
			ConsumerKey:   mapping.ConsumerKey,
			KeyType:       mapping.KeyType,
			KeyManager:    mapping.KeyManager,
			ApplicationId: mapping.ApplicationID,
			TenantId:      mapping.TenantID,
			TenantDomain:  mapping.TenantDomain,
			Timestamp:     mapping.TimeStamp,
		}

		applicationKeyMappings = append(applicationKeyMappings, keyMapping)
	}

	return &subscription.ApplicationKeyMappingList{
		List: applicationKeyMappings,
	}
}
