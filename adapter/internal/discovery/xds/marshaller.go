package xds

import (
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/wso2/micro-gw/config"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/config/enforcer"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/keymgt"
	"github.com/wso2/micro-gw/internal/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/micro-gw/internal/eventhub/types"
	logger "github.com/wso2/micro-gw/loggers"
)

// MarshalConfig will marshal a Config struct - read from the config toml - to
// enfocer's CDS resource representation.
func MarshalConfig(config *config.Config) *enforcer.Config {
	issuers := []*enforcer.Issuer{}
	urlGroups := []*enforcer.TMURLGroup{}

	for _, issuer := range config.Enforcer.JwtTokenConfig {
	    claimMaps := []*enforcer.ClaimMapping{}
	    for _, claimMap := range issuer.ClaimMapping{
	        claim := &enforcer.ClaimMapping{
	            RemoteClaim:    claimMap.RemoteClaim,
	            LocalClaim:     claimMap.LocalClaim,
        	}
        	claimMaps = append(claimMaps,claim)
        }
    	jwtConfig := &enforcer.Issuer{
            CertificateAlias:     issuer.CertificateAlias,
    		ConsumerKeyClaim:     issuer.ConsumerKeyClaim,
    		Issuer:               issuer.Issuer,
    		Name:                 issuer.Name,
    		ValidateSubscription: issuer.ValidateSubscription,
    		JwksURL:              issuer.JwksURL,
    		CertificateFilePath:  issuer.CertificateFilePath,
    		ClaimMapping:         claimMaps,
    	}
    	issuers = append(issuers, jwtConfig)
    }

	jwtUsers := []*enforcer.JWTUser{}
	for _, user := range config.Enforcer.JwtIssuer.JwtUsers {
		jwtUser := &enforcer.JWTUser{
			Username:  user.Username,
			Password:  user.Password,
		}
		jwtUsers = append(jwtUsers, jwtUser)
	}

	for _, urlGroup := range config.Enforcer.Throttling.Publisher.URLGroup {
		group := &enforcer.TMURLGroup{
			AuthURLs:     urlGroup.AuthURLs,
			ReceiverURLs: urlGroup.ReceiverURLs,
			Type:         urlGroup.Type,
		}
		urlGroups = append(urlGroups, group)
	}

	authService := &enforcer.Service{
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

	analytics := &enforcer.Analytics{
		Enabled:   config.ControlPlane.Analytics.Enabled,
		AuthUrl:   config.ControlPlane.Analytics.AuthURL,
		AuthToken: config.ControlPlane.Analytics.AuthToken,
		Service: &enforcer.Service{
			Port:           config.Enforcer.AnalyticsReceiver.Port,
			MaxHeaderLimit: config.Enforcer.AnalyticsReceiver.MaxHeaderLimit,
			KeepAliveTime:  config.Enforcer.AnalyticsReceiver.KeepAliveTime,
			MaxMessageSize: config.Enforcer.AnalyticsReceiver.MaxMessageSize,
			ThreadPool: &enforcer.ThreadPool{
				CoreSize:      config.Enforcer.AnalyticsReceiver.ThreadPool.CoreSize,
				MaxSize:       config.Enforcer.AnalyticsReceiver.ThreadPool.MaxSize,
				QueueSize:     config.Enforcer.AnalyticsReceiver.ThreadPool.QueueSize,
				KeepAliveTime: config.Enforcer.AnalyticsReceiver.ThreadPool.KeepAliveTime,
			},
		},
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
		JwtIssuer: &enforcer.JWTIssuer{
			Enabled:               config.Enforcer.JwtIssuer.Enabled,
			Issuer:                config.Enforcer.JwtIssuer.Issuer,
			Encoding:              config.Enforcer.JwtIssuer.Encoding,
			ClaimDialect:          config.Enforcer.JwtIssuer.ClaimDialect,
			SigningAlgorithm:      config.Enforcer.JwtIssuer.SigningAlgorithm,
			PublicCertificatePath: config.Enforcer.JwtIssuer.PublicCertificatePath,
			PrivateKeyPath:        config.Enforcer.JwtIssuer.PrivateKeyPath,
			ValidityPeriod:        config.Enforcer.JwtIssuer.ValidityPeriod,
			JwtUsers: jwtUsers,
		},
		AuthService:    authService,
		JwtTokenConfig: issuers,
		Cache:          cache,
		Analytics:      analytics,
		Eventhub: &enforcer.EventHub{
			Enabled:    config.ControlPlane.EventHub.Enabled,
			ServiceUrl: config.ControlPlane.EventHub.ServiceURL,
			JmsConnectionParameters: &enforcer.JmsConnectionParameters{
				EventListeningEndpoints: config.ControlPlane.EventHub.JmsConnectionParameters.EventListeningEndpoints,
			},
		},
		Throttling: &enforcer.Throttling{
			EnableGlobalEventPublishing:        config.Enforcer.Throttling.EnableGlobalEventPublishing,
			EnableHeaderConditions:             config.Enforcer.Throttling.EnableHeaderConditions,
			EnableQueryParamConditions:         config.Enforcer.Throttling.EnableQueryParamConditions,
			EnableJwtClaimConditions:           config.Enforcer.Throttling.EnableJwtClaimConditions,
			JmsConnectionInitialContextFactory: config.Enforcer.Throttling.JmsConnectionInitialContextFactory,
			JmsConnectionProviderUrl:           config.Enforcer.Throttling.JmsConnectionProviderURL,
			Publisher: &enforcer.BinaryPublisher{
				Username: config.Enforcer.Throttling.Publisher.Username,
				Password: config.Enforcer.Throttling.Publisher.Password,
				UrlGroup: urlGroups,
				Pool: &enforcer.PublisherPool{
					InitIdleObjectDataPublishingAgents: config.Enforcer.Throttling.Publisher.Pool.InitIdleObjectDataPublishingAgents,
					MaxIdleDataPublishingAgents:        config.Enforcer.Throttling.Publisher.Pool.MaxIdleDataPublishingAgents,
					PublisherThreadPoolCoreSize:        config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolCoreSize,
					PublisherThreadPoolKeepAliveTime:   config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolKeepAliveTime,
					PublisherThreadPoolMaximumSize:     config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolMaximumSize,
				},
				Agent: &enforcer.ThrottleAgent{
					BatchSize:                  config.Enforcer.Throttling.Publisher.Agent.BatchSize,
					Ciphers:                    config.Enforcer.Throttling.Publisher.Agent.Ciphers,
					CorePoolSize:               config.Enforcer.Throttling.Publisher.Agent.CorePoolSize,
					EvictionTimePeriod:         config.Enforcer.Throttling.Publisher.Agent.EvictionTimePeriod,
					KeepAliveTimeInPool:        config.Enforcer.Throttling.Publisher.Agent.KeepAliveTimeInPool,
					MaxIdleConnections:         config.Enforcer.Throttling.Publisher.Agent.MaxIdleConnections,
					MaxPoolSize:                config.Enforcer.Throttling.Publisher.Agent.MaxPoolSize,
					MaxTransportPoolSize:       config.Enforcer.Throttling.Publisher.Agent.MaxTransportPoolSize,
					MinIdleTimeInPool:          config.Enforcer.Throttling.Publisher.Agent.MinIdleTimeInPool,
					QueueSize:                  config.Enforcer.Throttling.Publisher.Agent.QueueSize,
					ReconnectionInterval:       config.Enforcer.Throttling.Publisher.Agent.ReconnectionInterval,
					SecureEvictionTimePeriod:   config.Enforcer.Throttling.Publisher.Agent.SecureEvictionTimePeriod,
					SecureMaxIdleConnections:   config.Enforcer.Throttling.Publisher.Agent.SecureMaxIdleConnections,
					SecureMaxTransportPoolSize: config.Enforcer.Throttling.Publisher.Agent.SecureMaxTransportPoolSize,
					SecureMinIdleTimeInPool:    config.Enforcer.Throttling.Publisher.Agent.SecureMinIdleTimeInPool,
					SocketTimeoutMS:            config.Enforcer.Throttling.Publisher.Agent.SocketTimeoutMS,
					SslEnabledProtocols:        config.Enforcer.Throttling.Publisher.Agent.SslEnabledProtocols,
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
			Uuid:             api.UUID,
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

// MarshalKeyManager converts the data into KeyManager proto type
func MarshalKeyManager(keyManager *types.KeyManager) *keymgt.KeyManagerConfig {
	configList, err := json.Marshal(keyManager.Configuration)
	configuration := string(configList)
	if err == nil {
		newKeyManager := &keymgt.KeyManagerConfig{
			Name:          keyManager.Name,
			Type:          keyManager.Type,
			Enabled:       keyManager.Enabled,
			TenantDomain:  keyManager.TenantDomain,
			Configuration: configuration,
		}
		return newKeyManager
	}
	logger.LoggerXds.Debugf("Error happens while marshaling key manager data for " + fmt.Sprint(keyManager.Name))
	return nil
}
