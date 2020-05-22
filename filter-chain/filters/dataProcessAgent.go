package filters

import (
	"github.com/wso2/product-microgateway/dtos"
	"sync"
)

func setDataReference(dataReferenceObject DataReference, throttleData dtos.RequestStreamDTO) DataReference {
	dataReferenceObject.appKey = throttleData.AppKey
	dataReferenceObject.appTierCount = throttleData.AppTierCount
	dataReferenceObject.appTierUnitTime = throttleData.AppTierUnitTime
	dataReferenceObject.appTierTimeUnit = throttleData.AppTierTimeUnit
	dataReferenceObject.apiKey = throttleData.ApiKey
	dataReferenceObject.apiTierCount = throttleData.ApiTierCount
	dataReferenceObject.apiTierUnitTime = throttleData.ApiTierUnitTime
	dataReferenceObject.apiTierTimeUnit = throttleData.ApiTierTimeUnit
	dataReferenceObject.subscriptionKey = throttleData.SubscriptionKey
	dataReferenceObject.subscriptionTierCount = throttleData.SubscriptionTierCount
	dataReferenceObject.subscriptionTierUnitTime = throttleData.SubscriptionTierUnitTime
	dataReferenceObject.subscriptionTierTimeUnit = throttleData.SubscriptionTierTimeUnit
	dataReferenceObject.resourceKey = throttleData.ResourceKey
	dataReferenceObject.resourceTierCount = throttleData.ResourceTierCount
	dataReferenceObject.resourceTierUnitTime = throttleData.ResourceTierUnitTime
	dataReferenceObject.resourceTierTimeUnit = throttleData.ResourceTierTimeUnit
	dataReferenceObject.stopOnQuota = throttleData.StopOnQuota
	dataReferenceObject.timestamp = getCurrentTimeMillis()
	return dataReferenceObject
}

func run(dataReferenceObject DataReference, mtx *sync.Mutex) {
	dataReferenceObject.updateCounters(mtx)
}
