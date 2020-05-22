package dtos

type RequestStreamDTO struct {
	ResetTimestamp           int
	RemainingQuota           int
	IsThrottled              bool
	messageID                string
	ApiKey                   string
	AppKey                   string
	StopOnQuota              bool
	SubscriptionKey          string
	policyKey                string
	AppTier                  string
	ApiTier                  string
	SubscriptionTier         string
	ResourceKey              string
	ResourceTier             string
	userId                   string
	apiContext               string
	ApiVersion               string
	appTenant                string
	apiTenant                string
	appId                    string
	apiName                  string
	properties               string
	ResourceTierCount        int64
	ResourceTierUnitTime     int64
	ResourceTierTimeUnit     string
	AppTierCount             int64
	AppTierUnitTime          int64
	AppTierTimeUnit          string
	ApiTierCount             int64
	ApiTierUnitTime          int64
	ApiTierTimeUnit          string
	SubscriptionTierCount    int64
	SubscriptionTierUnitTime int64
	SubscriptionTierTimeUnit string
}
