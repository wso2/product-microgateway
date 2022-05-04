package xds

import (
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/config/enforcer"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/keymgt"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
)

var (
	// APIListMap has the following mapping label -> apiUUID -> API (Metadata)
	APIListMap map[string]map[string]*subscription.APIs
	// SubscriptionMap contains the subscriptions recieved from API Manager Control Plane
	SubscriptionMap map[int32]*subscription.Subscription
	// ApplicationMap contains the applications recieved from API Manager Control Plane
	ApplicationMap map[string]*subscription.Application
	// ApplicationKeyMappingMap contains the application key mappings recieved from API Manager Control Plane
	ApplicationKeyMappingMap map[string]*subscription.ApplicationKeyMapping
	// ApplicationPolicyMap contains the application policies recieved from API Manager Control Plane
	ApplicationPolicyMap map[int32]*subscription.ApplicationPolicy
	// SubscriptionPolicyMap contains the subscription policies recieved from API Manager Control Plane
	SubscriptionPolicyMap map[int32]*subscription.SubscriptionPolicy
)

// EventType is a enum to distinguish Create, Update and Delete Events
type EventType int

const (
	// CreateEvent : enum
	CreateEvent EventType = iota
	// UpdateEvent : enum
	UpdateEvent
	// DeleteEvent : enum
	DeleteEvent
)

const blockedStatus string = "BLOCKED"

// MarshalConfig will marshal a Config struct - read from the config toml - to
// enfocer's CDS resource representation.
func MarshalConfig(config *config.Config) *enforcer.Config {
	issuers := []*enforcer.Issuer{}
	urlGroups := []*enforcer.TMURLGroup{}

	for _, issuer := range config.Enforcer.Security.TokenService {
		claimMaps := []*enforcer.ClaimMapping{}
		for _, claimMap := range issuer.ClaimMapping {
			claim := &enforcer.ClaimMapping{
				RemoteClaim: claimMap.RemoteClaim,
				LocalClaim:  claimMap.LocalClaim,
			}
			claimMaps = append(claimMaps, claim)
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
	for _, user := range config.Enforcer.JwtIssuer.JwtUser {
		jwtUser := &enforcer.JWTUser{
			Username: user.Username,
			Password: user.Password,
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

	tracing := &enforcer.Tracing{
		Enabled:          config.Tracing.Enabled,
		Type:             config.Tracing.Type,
		ConfigProperties: config.Tracing.ConfigProperties,
	}
	metrics := &enforcer.Metrics{
		Enabled: config.Enforcer.Metrics.Enabled,
		Type:    config.Enforcer.Metrics.Type,
	}
	analytics := &enforcer.Analytics{
		Enabled:          config.Analytics.Enabled,
		ConfigProperties: config.Analytics.Enforcer.ConfigProperties,
		Service: &enforcer.Service{
			Port:           config.Analytics.Enforcer.LogReceiver.Port,
			MaxHeaderLimit: config.Analytics.Enforcer.LogReceiver.MaxHeaderLimit,
			KeepAliveTime:  config.Analytics.Enforcer.LogReceiver.KeepAliveTime,
			MaxMessageSize: config.Analytics.Enforcer.LogReceiver.MaxMessageSize,
			ThreadPool: &enforcer.ThreadPool{
				CoreSize:      config.Analytics.Enforcer.LogReceiver.ThreadPool.CoreSize,
				MaxSize:       config.Analytics.Enforcer.LogReceiver.ThreadPool.MaxSize,
				QueueSize:     config.Analytics.Enforcer.LogReceiver.ThreadPool.QueueSize,
				KeepAliveTime: config.Analytics.Enforcer.LogReceiver.ThreadPool.KeepAliveTime,
			},
		},
	}

	management := &enforcer.Management{
		Username: config.Enforcer.Management.Username,
		Password: config.Enforcer.Management.Password,
	}

	restServer := &enforcer.RestServer{
		Enable: config.Enforcer.RestServer.Enabled,
	}

	filters := []*enforcer.Filter{}

	for _, filterConfig := range config.Enforcer.Filters {
		filter := &enforcer.Filter{
			ClassName:        filterConfig.ClassName,
			Position:         filterConfig.Position,
			ConfigProperties: filterConfig.ConfigProperties,
		}
		filters = append(filters, filter)
	}

	return &enforcer.Config{
		JwtGenerator: &enforcer.JWTGenerator{
			Enable:                config.Enforcer.JwtGenerator.Enabled,
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
			JwtUsers:              jwtUsers,
		},
		AuthService: authService,
		Security: &enforcer.Security{
			TokenService: issuers,
			AuthHeader: &enforcer.AuthHeader{
				EnableOutboundAuthHeader: config.Enforcer.Security.AuthHeader.EnableOutboundAuthHeader,
				AuthorizationHeader:      config.Enforcer.Security.AuthHeader.AuthorizationHeader,
				TestConsoleHeaderName:    config.Enforcer.Security.AuthHeader.TestConsoleHeaderName,
			},
		},
		Cache:     cache,
		Tracing:   tracing,
		Metrics:   metrics,
		Analytics: analytics,
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
		Management: management,
		RestServer: restServer,
		Filters:    filters,
	}
}

// marshalSubscriptionMapToList converts the data into SubscriptionList proto type
func marshalSubscriptionMapToList(subscriptionMap map[int32]*subscription.Subscription) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}
	for _, sub := range subscriptionMap {
		subscriptions = append(subscriptions, sub)
	}

	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

// marshalApplicationMapToList converts the data into ApplicationList proto type
func marshalApplicationMapToList(appMap map[string]*subscription.Application) *subscription.ApplicationList {
	applications := []*subscription.Application{}
	for _, app := range appMap {
		applications = append(applications, app)
	}

	return &subscription.ApplicationList{
		List: applications,
	}
}

// marshalAPIListMapToList converts the data into APIList proto type
func marshalAPIListMapToList(apiMap map[string]*subscription.APIs) *subscription.APIList {
	apis := []*subscription.APIs{}
	for _, api := range apiMap {
		apis = append(apis, api)
	}

	return &subscription.APIList{
		List: apis,
	}
}

// marshalApplicationPolicyMapToList converts the data into ApplicationPolicyList proto type
func marshalApplicationPolicyMapToList(appPolicyMap map[int32]*subscription.ApplicationPolicy) *subscription.ApplicationPolicyList {
	applicationPolicies := []*subscription.ApplicationPolicy{}
	for _, policy := range appPolicyMap {
		applicationPolicies = append(applicationPolicies, policy)
	}

	return &subscription.ApplicationPolicyList{
		List: applicationPolicies,
	}
}

// marshalSubscriptionPolicyMapToList converts the data into SubscriptionPolicyList proto type
func marshalSubscriptionPolicyMapToList(subPolicyMap map[int32]*subscription.SubscriptionPolicy) *subscription.SubscriptionPolicyList {
	subscriptionPolicies := []*subscription.SubscriptionPolicy{}

	for _, policy := range subPolicyMap {
		subscriptionPolicies = append(subscriptionPolicies, policy)
	}

	return &subscription.SubscriptionPolicyList{
		List: subscriptionPolicies,
	}
}

// marshalKeyMappingMapToList converts the data into ApplicationKeyMappingList proto type
func marshalKeyMappingMapToList(keyMappingMap map[string]*subscription.ApplicationKeyMapping) *subscription.ApplicationKeyMappingList {
	applicationKeyMappings := []*subscription.ApplicationKeyMapping{}

	for _, keyMapping := range keyMappingMap {
		// TODO: (VirajSalaka) tenant domain check missing
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

// MarshalMultipleApplications is used to update the applicationList during the startup where
// multiple applications are pulled at once. And then it returns the ApplicationList.
func MarshalMultipleApplications(appList *types.ApplicationList) *subscription.ApplicationList {
	resourceMap := make(map[string]*subscription.Application)
	for _, application := range appList.List {
		applicationSub := marshalApplication(&application)
		resourceMap[application.UUID] = applicationSub
	}
	ApplicationMap = resourceMap
	return marshalApplicationMapToList(ApplicationMap)
}

// MarshalApplicationEventAndReturnList handles the Application Event corresponding to the event received
// from message broker. And then it returns the ApplicationList.
func MarshalApplicationEventAndReturnList(application *types.Application,
	eventType EventType) *subscription.ApplicationList {
	if eventType == DeleteEvent {
		delete(ApplicationMap, application.UUID)
		logger.LoggerXds.Infof("Application %s is deleted.", application.UUID)
	} else {
		applicationSub := marshalApplication(application)
		ApplicationMap[application.UUID] = applicationSub
		if eventType == CreateEvent {
			logger.LoggerXds.Infof("Application %s is added.", application.UUID)
		} else {
			logger.LoggerXds.Infof("Application %s is updated.", application.UUID)
		}
	}
	return marshalApplicationMapToList(ApplicationMap)
}

// MarshalMultipleApplicationKeyMappings is used to update the application key mappings during the startup where
// multiple key mappings are pulled at once. And then it returns the ApplicationKeyMappingList.
func MarshalMultipleApplicationKeyMappings(keymappingList *types.ApplicationKeyMappingList) *subscription.ApplicationKeyMappingList {
	resourceMap := make(map[string]*subscription.ApplicationKeyMapping)
	for _, keyMapping := range keymappingList.List {
		applicationKeyMappingReference := GetApplicationKeyMappingReference(&keyMapping)
		keyMappingSub := marshalKeyMapping(&keyMapping)
		resourceMap[applicationKeyMappingReference] = keyMappingSub
	}
	ApplicationKeyMappingMap = resourceMap
	return marshalKeyMappingMapToList(ApplicationKeyMappingMap)
}

// MarshalApplicationKeyMappingEventAndReturnList handles the Application Key Mapping Event corresponding to the event received
// from message broker. And then it returns the ApplicationKeyMappingList.
func MarshalApplicationKeyMappingEventAndReturnList(keyMapping *types.ApplicationKeyMapping,
	eventType EventType) *subscription.ApplicationKeyMappingList {
	applicationKeyMappingReference := GetApplicationKeyMappingReference(keyMapping)
	if eventType == DeleteEvent {
		delete(ApplicationKeyMappingMap, applicationKeyMappingReference)
		logger.LoggerXds.Infof("Application Key Mapping for the applicationKeyMappingReference %s is removed.",
			applicationKeyMappingReference)
	} else {
		keyMappingSub := marshalKeyMapping(keyMapping)
		ApplicationKeyMappingMap[applicationKeyMappingReference] = keyMappingSub
		logger.LoggerXds.Infof("Application Key Mapping for the applicationKeyMappingReference %s is added.",
			applicationKeyMappingReference)
	}
	return marshalKeyMappingMapToList(ApplicationKeyMappingMap)
}

// MarshalMultipleSubscriptions is used to update the subscriptions during the startup where
// multiple subscriptions are pulled at once. And then it returns the SubscriptionList.
func MarshalMultipleSubscriptions(subscriptionsList *types.SubscriptionList) *subscription.SubscriptionList {
	resourceMap := make(map[int32]*subscription.Subscription)
	for _, sb := range subscriptionsList.List {
		resourceMap[sb.SubscriptionID] = marshalSubscription(&sb)
	}
	SubscriptionMap = resourceMap
	return marshalSubscriptionMapToList(SubscriptionMap)
}

// MarshalSubscriptionEventAndReturnList handles the Subscription Event corresponding to the event received
// from message broker. And then it returns the SubscriptionList.
func MarshalSubscriptionEventAndReturnList(sub *types.Subscription, eventType EventType) *subscription.SubscriptionList {
	if eventType == DeleteEvent {
		delete(SubscriptionMap, sub.SubscriptionID)
		logger.LoggerXds.Infof("Subscription for %s:%s is deleted.", sub.APIUUID, sub.ApplicationUUID)
	} else {
		subscriptionSub := marshalSubscription(sub)
		SubscriptionMap[sub.SubscriptionID] = subscriptionSub
		if eventType == UpdateEvent {
			logger.LoggerXds.Infof("Subscription for %s:%s is updated.", sub.APIUUID, sub.ApplicationUUID)
		} else {
			logger.LoggerXds.Infof("Subscription for %s:%s is added.", sub.APIUUID, sub.ApplicationUUID)
		}
	}
	return marshalSubscriptionMapToList(SubscriptionMap)
}

// MarshalMultipleApplicationPolicies is used to update the applicationPolicies during the startup where
// multiple application policies are pulled at once. And then it returns the ApplicationPolicyList.
func MarshalMultipleApplicationPolicies(policies *types.ApplicationPolicyList) *subscription.ApplicationPolicyList {
	resourceMap := make(map[int32]*subscription.ApplicationPolicy)
	for _, policy := range policies.List {
		appPolicy := marshalApplicationPolicy(&policy)
		resourceMap[policy.ID] = appPolicy
		logger.LoggerXds.Infof("appPolicy Entry is added : %v", appPolicy)
	}
	ApplicationPolicyMap = resourceMap
	return marshalApplicationPolicyMapToList(ApplicationPolicyMap)
}

// MarshalApplicationPolicyEventAndReturnList handles the Application Policy Event corresponding to the event received
// from message broker. And then it returns the ApplicationPolicyList.
func MarshalApplicationPolicyEventAndReturnList(policy *types.ApplicationPolicy, eventType EventType) *subscription.ApplicationPolicyList {
	if eventType == DeleteEvent {
		delete(ApplicationPolicyMap, policy.ID)
		logger.LoggerXds.Infof("Application Policy: %s is deleted.", policy.Name)
	} else {
		appPolicy := marshalApplicationPolicy(policy)
		ApplicationPolicyMap[policy.ID] = appPolicy
		if eventType == UpdateEvent {
			logger.LoggerInternalMsg.Infof("Application Policy: %s is updated.", appPolicy.Name)
		} else {
			logger.LoggerInternalMsg.Infof("Application Policy: %s is added.", appPolicy.Name)
		}
	}
	return marshalApplicationPolicyMapToList(ApplicationPolicyMap)
}

// MarshalMultipleSubscriptionPolicies is used to update the subscriptionPolicies during the startup where
// multiple subscription policies are pulled at once. And then it returns the SubscriptionPolicyList.
func MarshalMultipleSubscriptionPolicies(policies *types.SubscriptionPolicyList) *subscription.SubscriptionPolicyList {
	resourceMap := make(map[int32]*subscription.SubscriptionPolicy)
	for _, policy := range policies.List {
		resourceMap[policy.ID] = marshalSubscriptionPolicy(&policy)
	}
	SubscriptionPolicyMap = resourceMap
	return marshalSubscriptionPolicyMapToList(SubscriptionPolicyMap)
}

// MarshalSubscriptionPolicyEventAndReturnList handles the Subscription Policy Event corresponding to the event received
// from message broker. And then it returns the subscriptionPolicyList.
func MarshalSubscriptionPolicyEventAndReturnList(policy *types.SubscriptionPolicy, eventType EventType) *subscription.SubscriptionPolicyList {
	if eventType == DeleteEvent {
		delete(ApplicationPolicyMap, policy.ID)
		logger.LoggerXds.Infof("Application Policy: %s is deleted.", policy.Name)
	} else {
		subPolicy := marshalSubscriptionPolicy(policy)
		SubscriptionPolicyMap[policy.ID] = subPolicy
		if eventType == UpdateEvent {
			logger.LoggerInternalMsg.Infof("Subscription Policy: %s is updated.", subPolicy.Name)
		} else {
			logger.LoggerInternalMsg.Infof("Subscription Policy: %s is added.", subPolicy.Name)
		}
	}
	return marshalSubscriptionPolicyMapToList(SubscriptionPolicyMap)
}

// MarshalAPIMetataAndReturnList updates the internal APIListMap and returns the XDS compatible APIList.
// apiList is the internal APIList object (For single API, this would contain a List with just one API)
// initialAPIUUIDListMap is assigned during startup when global adapter is associated. This would be empty otherwise.
// gatewayLabel is the environment.
func MarshalAPIMetataAndReturnList(apiList *types.APIList, initialAPIUUIDListMap map[string]int, gatewayLabel string) *subscription.APIList {

	if APIListMap == nil {
		APIListMap = make(map[string]map[string]*subscription.APIs)
	}
	// var resourceMapForLabel map[string]*subscription.APIs
	if _, ok := APIListMap[gatewayLabel]; !ok {
		APIListMap[gatewayLabel] = make(map[string]*subscription.APIs)
	}
	resourceMapForLabel := APIListMap[gatewayLabel]
	for _, api := range apiList.List {
		// initialAPIUUIDListMap is not null if the adapter is running with global adapter enabled, and it is
		// the first method invocation.
		if initialAPIUUIDListMap != nil {
			if _, ok := initialAPIUUIDListMap[api.UUID]; !ok {
				continue
			}
		}
		newAPI := marshalAPIMetadata(&api)
		resourceMapForLabel[api.UUID] = newAPI
	}
	return marshalAPIListMapToList(resourceMapForLabel)
}

// DeleteAPIAndReturnList removes the API from internal maps and returns the marshalled API List.
// If the apiUUID is not found in the internal map under the provided environment, then it would return a
// nil value. Hence it is required to check if the return value is nil, prior to updating the XDS cache.
func DeleteAPIAndReturnList(apiUUID, organizationUUID string, gatewayLabel string) *subscription.APIList {
	if _, ok := APIListMap[gatewayLabel]; !ok {
		logger.LoggerXds.Debugf("No API Metadata is available under gateway Environment : %s", gatewayLabel)
		return nil
	}
	delete(APIListMap[gatewayLabel], apiUUID)
	return marshalAPIListMapToList(APIListMap[gatewayLabel])
}

// MarshalAPIForLifeCycleChangeEventAndReturnList updates the internal map's API instances lifecycle state only if
// stored API Instance's or input status event is a blocked event.
// If no change is applied, it would return nil. Hence the XDS cache should not be updated.
func MarshalAPIForLifeCycleChangeEventAndReturnList(apiUUID, status, gatewayLabel string) *subscription.APIList {
	if _, ok := APIListMap[gatewayLabel]; !ok {
		logger.LoggerXds.Debugf("No API Metadata is available under gateway Environment : %s", gatewayLabel)
		return nil
	}
	if _, ok := APIListMap[gatewayLabel][apiUUID]; !ok {
		// If the Lifecycle statue is not blocked, and the API is not included in the map, the APIListMap need not to be updated.
		if status != blockedStatus {
			return nil
		}
		logger.LoggerXds.Debugf("No API Metadata for API ID: %s is available under gateway Environment : %s. Hence"+
			"a new record is added.", apiUUID, gatewayLabel)
		APIListMap[gatewayLabel][apiUUID] = &subscription.APIs{
			Uuid:    apiUUID,
			LcState: status,
		}
	}
	storedAPILCState := APIListMap[gatewayLabel][apiUUID].LcState

	// Because the adapter only required to update the XDS if it is related to blocked state.
	if !(storedAPILCState == blockedStatus || status == blockedStatus) {
		return nil
	}
	APIListMap[gatewayLabel][apiUUID].LcState = status
	return marshalAPIListMapToList(APIListMap[gatewayLabel])
}

func marshalSubscription(subscriptionInternal *types.Subscription) *subscription.Subscription {
	sub := &subscription.Subscription{
		SubscriptionId:    fmt.Sprint(subscriptionInternal.SubscriptionID),
		PolicyId:          subscriptionInternal.PolicyID,
		ApiId:             subscriptionInternal.APIID,
		AppId:             subscriptionInternal.AppID,
		SubscriptionState: subscriptionInternal.SubscriptionState,
		TimeStamp:         subscriptionInternal.TimeStamp,
		TenantId:          subscriptionInternal.TenantID,
		TenantDomain:      subscriptionInternal.TenantDomain,
		SubscriptionUUID:  subscriptionInternal.SubscriptionUUID,
		ApiUUID:           subscriptionInternal.APIUUID,
		AppUUID:           subscriptionInternal.ApplicationUUID,
	}
	if sub.TenantDomain == "" {
		sub.TenantDomain = config.GetControlPlaneConnectedTenantDomain()
	}
	return sub
}

func marshalApplication(appInternal *types.Application) *subscription.Application {
	app := &subscription.Application{
		Uuid:         appInternal.UUID,
		Id:           appInternal.ID,
		Name:         appInternal.Name,
		SubId:        appInternal.ID,
		SubName:      appInternal.SubName,
		Policy:       appInternal.Policy,
		TokenType:    appInternal.TokenType,
		GroupIds:     appInternal.GroupIds,
		Attributes:   appInternal.Attributes,
		TenantId:     appInternal.TenantID,
		TenantDomain: appInternal.TenantDomain,
		Timestamp:    appInternal.TimeStamp,
	}
	if app.TenantDomain == "" {
		app.TenantDomain = config.GetControlPlaneConnectedTenantDomain()
	}
	return app
}

func marshalKeyMapping(keyMappingInternal *types.ApplicationKeyMapping) *subscription.ApplicationKeyMapping {
	return &subscription.ApplicationKeyMapping{
		ConsumerKey:     keyMappingInternal.ConsumerKey,
		KeyType:         keyMappingInternal.KeyType,
		KeyManager:      keyMappingInternal.KeyManager,
		ApplicationId:   keyMappingInternal.ApplicationID,
		ApplicationUUID: keyMappingInternal.ApplicationUUID,
		TenantId:        keyMappingInternal.TenantID,
		TenantDomain:    keyMappingInternal.TenantDomain,
		Timestamp:       keyMappingInternal.TimeStamp,
	}
}

func marshalAPIMetadata(api *types.API) *subscription.APIs {
	return &subscription.APIs{
		ApiId:            strconv.Itoa(api.APIID),
		Name:             api.Name,
		Provider:         api.Provider,
		Version:          api.Version,
		Context:          api.Context,
		Policy:           api.Policy,
		ApiType:          api.APIType,
		Uuid:             api.UUID,
		IsDefaultVersion: api.IsDefaultVersion,
		LcState:          api.APIStatus,
	}
}

func marshalApplicationPolicy(policy *types.ApplicationPolicy) *subscription.ApplicationPolicy {
	return &subscription.ApplicationPolicy{
		Id:        policy.ID,
		TenantId:  policy.TenantID,
		Name:      policy.Name,
		QuotaType: policy.QuotaType,
	}
}

func marshalSubscriptionPolicy(policy *types.SubscriptionPolicy) *subscription.SubscriptionPolicy {
	return &subscription.SubscriptionPolicy{
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
}

// GetApplicationKeyMappingReference returns unique reference for each key Mapping event.
// It is the combination of consumerKey:keyManager
func GetApplicationKeyMappingReference(keyMapping *types.ApplicationKeyMapping) string {
	return keyMapping.ConsumerKey + ":" + keyMapping.KeyManager
}

// CheckIfAPIMetadataIsAlreadyAvailable returns true only if the API Metadata for the given API UUID
// is already available
func CheckIfAPIMetadataIsAlreadyAvailable(apiUUID, label string) bool {
	if _, labelAvailable := APIListMap[label]; labelAvailable {
		if _, apiAvailale := APIListMap[label][apiUUID]; apiAvailale {
			return true
		}
	}
	return false
}
