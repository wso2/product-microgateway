package filters

import (
	_ "github.com/golang/protobuf/ptypes/timestamp"
	log "github.com/sirupsen/logrus"
	"strconv"
	"strings"
	"sync"
	"time"
)

var apiLevelCounter map[string]ThrottleData
var resourceLevelCounter map[string]ThrottleData
var applicationLevelCounter map[string]ThrottleData
var subscriptionLevelCounter map[string]ThrottleData

func initiateCounters() {
	apiLevelCounter = make(map[string]ThrottleData)
	resourceLevelCounter = make(map[string]ThrottleData)
	subscriptionLevelCounter = make(map[string]ThrottleData)
	applicationLevelCounter = make(map[string]ThrottleData)
}

type ThrottleEvent struct {
	key          string
	stopOnQuota  bool
	TierCount    int64
	TierUnitTime int64
	TierTimeUnit string
	timestamp    int64
	throttleType ThrottleType
}

func (dataReferenceObject DataReference) updateCounters(mtx *sync.Mutex) {
	apiData := ThrottleEvent{
		key:          dataReferenceObject.apiKey,
		stopOnQuota:  dataReferenceObject.stopOnQuota,
		TierCount:    dataReferenceObject.apiTierCount,
		TierUnitTime: dataReferenceObject.apiTierUnitTime,
		TierTimeUnit: dataReferenceObject.apiTierTimeUnit,
		timestamp:    dataReferenceObject.timestamp,
		throttleType: ThrottleType(3),
	}
	resourceData := ThrottleEvent{
		key:          dataReferenceObject.resourceKey,
		stopOnQuota:  dataReferenceObject.stopOnQuota,
		TierCount:    dataReferenceObject.resourceTierCount,
		TierUnitTime: dataReferenceObject.resourceTierUnitTime,
		TierTimeUnit: dataReferenceObject.resourceTierTimeUnit,
		timestamp:    dataReferenceObject.timestamp,
		throttleType: ThrottleType(2),
	}
	appData := ThrottleEvent{
		key:          dataReferenceObject.appKey,
		stopOnQuota:  dataReferenceObject.stopOnQuota,
		TierCount:    dataReferenceObject.appTierCount,
		TierUnitTime: dataReferenceObject.appTierUnitTime,
		TierTimeUnit: dataReferenceObject.resourceTierTimeUnit,
		timestamp:    dataReferenceObject.timestamp,
		throttleType: ThrottleType(0),
	}
	subData := ThrottleEvent{
		key:          dataReferenceObject.subscriptionKey,
		stopOnQuota:  dataReferenceObject.stopOnQuota,
		TierCount:    dataReferenceObject.subscriptionTierCount,
		TierUnitTime: dataReferenceObject.subscriptionTierUnitTime,
		TierTimeUnit: dataReferenceObject.subscriptionTierTimeUnit,
		timestamp:    dataReferenceObject.timestamp,
		throttleType: ThrottleType(1),
	}

	mtx.Lock()
	apiLevelCounter = updateMapCounters(apiLevelCounter, apiData)
	mtx.Unlock()
	mtx.Lock()
	resourceLevelCounter = updateMapCounters(resourceLevelCounter, resourceData)
	mtx.Unlock()
	mtx.Lock()
	applicationLevelCounter = updateMapCounters(applicationLevelCounter, appData)
	mtx.Unlock()
	mtx.Lock()
	subscriptionLevelCounter = updateMapCounters(subscriptionLevelCounter, subData)
	mtx.Unlock()
}

func updateMapCounters(counterMap map[string]ThrottleData, event ThrottleEvent) map[string]ThrottleData {
	throttleKey := event.key
	stopOnQuota := event.stopOnQuota
	limit := event.TierCount
	unitTime := event.TierUnitTime
	timeUnit := event.TierTimeUnit
	timestamp := event.timestamp
	throttleType := event.throttleType

	throttleData, found := counterMap[throttleKey]
	if found {
		count := throttleData.getCount() + 1
		throttleData.setCount(count)
		if limit > 0 && count >= limit {
			throttleData.setThrottled(true)
		} else {
			throttleData.setThrottled(false)
		}
		if timestamp > (throttleData.getWindowStartTime() + throttleData.getUnitTime()) {
			throttleData.setCount(1)
			var startTime int64 = timestamp - (timestamp % getTimeInMilliSeconds(1, timeUnit))
			throttleData.setWindowStartTime(startTime)
			throttleData.setThrottled(false)
		}
		log.Infof("Throttle count for the key" + throttleKey + "is " +
			strconv.FormatInt(int64(throttleData.getCount()), 10))
		counterMap[throttleKey] = throttleData
		return counterMap
	} else {
		var throttleData ThrottleData = ThrottleData{}
		var startTime int64 = timestamp - (timestamp % getTimeInMilliSeconds(1, timeUnit))
		throttleData.setWindowStartTime(startTime)
		throttleData.setStopOnQuota(stopOnQuota)
		throttleData.setUnitTime(getTimeInMilliSeconds(unitTime, timeUnit))
		throttleData.setThrottleType(throttleType)
		throttleData.setCount(0)
		throttleData.setThrottleKey(throttleKey)
		addThrottleData(throttleData)
		counterMap[throttleKey] = throttleData
		return counterMap
	}
}

func isApiLevelThrottled(apiKey string) bool {
	return isRequestThrottled(apiLevelCounter, apiKey)
}

func isResourceThrottled(resourceKey string) bool {
	return isRequestThrottled(resourceLevelCounter, resourceKey)
}

func isAppLevelThrottled(appKey string) bool {
	return isRequestThrottled(applicationLevelCounter, appKey)
}

func isSubsLevelThrottled(subscriptionKey string) bool {
	return isRequestThrottled(subscriptionLevelCounter, subscriptionKey)
}

func removeFromResourceCounterMap(key string) {
	delete(resourceLevelCounter, key)
}

func removeFromApplicationCounterMap(key string) {
	delete(applicationLevelCounter, key)
}

func removeFromApiCounterMap(key string) {
	delete(apiLevelCounter, key)
}

func removeFromSubscriptionCounterMap(key string) {
	delete(subscriptionLevelCounter, key)
}

func isRequestThrottled(counterMap map[string]ThrottleData, throttleKey string) bool {
	if _, found := counterMap[throttleKey]; found {
		var currentTime int64 = getCurrentTimeMillis()
		var throttleData ThrottleData = counterMap[throttleKey]
		if currentTime > throttleData.getWindowStartTime()+throttleData.getUnitTime() {
			throttleData.setThrottled(false)
			counterMap[throttleKey] = throttleData
			log.Println("Throttle window has expired.")
			return false
		}
		return throttleData.isThrottled()
	}
	return false
}

func getCurrentTimeMillis() int64 {
	now := time.Now()
	unixNano := now.UnixNano()
	umillisec := unixNano / 1000000
	return umillisec
}

func getTimeInMilliSeconds(unitTime int64, timeUnit string) int64 {
	var milliSeconds int64
	if strings.EqualFold("min", timeUnit) {
		milliSeconds = time.Minute.Milliseconds() * unitTime
	} else if strings.EqualFold("hour", timeUnit) {
		milliSeconds = time.Hour.Milliseconds() * unitTime
	} else if strings.EqualFold("day", timeUnit) {
		milliSeconds = 24 * time.Hour.Milliseconds() * unitTime
	} else if strings.EqualFold("week", timeUnit) {
		milliSeconds = 7 * 24 * time.Hour.Milliseconds() * unitTime
	} else if strings.EqualFold("month", timeUnit) {
		milliSeconds = 30 * 24 * time.Hour.Milliseconds() * unitTime
	} else if strings.EqualFold("year", timeUnit) {
		milliSeconds = 365 * 24 * time.Hour.Milliseconds() * unitTime
	} else {
		log.Warnf("Unsupported time unit provided")
	}
	return milliSeconds
}
