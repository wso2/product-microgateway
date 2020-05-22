package filters

import (
	"github.com/wso2/product-microgateway/dtos"
	"sync"
)

var mtx *sync.Mutex

type DataReference struct {
	apiKey                   string
	appKey                   string
	stopOnQuota              bool
	subscriptionKey          string
	appTierCount             int64
	appTierUnitTime          int64
	appTierTimeUnit          string
	apiTierCount             int64
	apiTierUnitTime          int64
	apiTierTimeUnit          string
	subscriptionTierCount    int64
	subscriptionTierUnitTime int64
	subscriptionTierTimeUnit string
	resourceKey              string
	resourceTierCount        int64
	resourceTierUnitTime     int64
	resourceTierTimeUnit     string
	timestamp                int64
}

func InitThrottleDataReceiver() {
	initiateCounters()
	InitiateCleanUpTask()
	mtx = &sync.Mutex{}
}

func PublishNonThrottleEvent(throttleEvent dtos.RequestStreamDTO) {
	//Publish throttle event to internal policies
	if !enabledGlobalTMEventPublishing {
		ProcessNonThrottledEvent(throttleEvent)
	}
}

//This method used to pass throttle data and let it run within separate goroutine
func ProcessNonThrottledEvent(throttleEvent dtos.RequestStreamDTO) {
	dataReferenceObject := DataReference{}
	dataReferenceObject = setDataReference(dataReferenceObject, throttleEvent)
	go run(dataReferenceObject, mtx)
}

