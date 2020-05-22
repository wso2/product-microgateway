package filters

import (
	_ "github.com/cactus/go-statsd-client/statsd"
)

type ThrottleType int

const (
	APP ThrottleType = iota
	SUBSCRIPTION
	RESOURCE
	API
)

func (t ThrottleType) String() string {
	return [...]string{"APP", "SUBSCRIPTION", "RESOURCE", "API"}[t]
}

type ThrottleData struct {
	windowStartTime int64
	unitTime        int64
	count           int64
	remainingQuota  int64
	stopOnQuota     bool
	throttled       bool
	throttleKey     string
	throttleType    ThrottleType
}

func (t ThrottleData) getWindowStartTime() int64 {
	return t.windowStartTime
}

func (t *ThrottleData) setWindowStartTime(windowStartTime int64) {
	t.windowStartTime = windowStartTime
}

func (t ThrottleData) getUnitTime() int64 {
	return t.unitTime
}

func (t *ThrottleData) setUnitTime(unitTime int64) {
	t.unitTime = unitTime
}

func (t ThrottleData) getCount() int64 {
	return t.count
}

func (t *ThrottleData) setCount(count int64) {
	t.count = count
}

func (t ThrottleData) getRemainingQuota() int64 {
	return t.remainingQuota
}

func (t *ThrottleData) setRemainingQuota(remainingQuota int64) {
	t.remainingQuota = remainingQuota
}

func (t ThrottleData) isStopOnQuota() bool {
	return t.stopOnQuota
}

func (t *ThrottleData) setStopOnQuota(stopOnQuota bool) {
	t.stopOnQuota = stopOnQuota
}

func (t ThrottleData) isThrottled() bool {
	return t.throttled
}

func (t *ThrottleData) setThrottled(throttled bool) {
	t.throttled = throttled
}

func (t ThrottleData) getThrottleType() ThrottleType {
	return t.throttleType
}

func (t *ThrottleData) setThrottleType(throttleType ThrottleType) {
	t.throttleType = throttleType
}

func (t *ThrottleData) setThrottleKey(throttleKey string) {
	t.throttleKey = throttleKey
}

func cleanThrottleData(t ThrottleData, timeStamp int64) bool {
	if t.windowStartTime+t.unitTime < timeStamp {
		switch throttleType := t.getThrottleType(); throttleType {
		case APP:
			{
				removeFromApplicationCounterMap(t.throttleKey)
				break
			}
		case API:
			{
				removeFromApiCounterMap(t.throttleKey)
				break
			}
		case RESOURCE:
			{
				removeFromResourceCounterMap(t.throttleKey)
				break
			}
		case SUBSCRIPTION:
			{
				removeFromSubscriptionCounterMap(t.throttleKey)
				break
			}
		}
		return true
	}
	return false
}
