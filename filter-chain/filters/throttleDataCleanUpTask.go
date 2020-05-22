package filters

import (
	"time"
)

var throttleDataList []ThrottleData

func InitiateCleanUpTask() {
	ticker := time.NewTicker(time.Second)
	done := make(chan bool)
	go func() {
		for {
			select {
			case <-done:
				return
			case <-ticker.C:
				cleanUpTask()

			}
		}
	}()
}

func cleanUpTask() {
	currentTimeStamp := getCurrentTimeMillis()
	for i := 0; i < len(throttleDataList); i++ {
		if cleanThrottleData(throttleDataList[i], currentTimeStamp) {
			//remove throttle data from the list
			throttleDataList[i] = throttleDataList[len(throttleDataList)-1]
			throttleDataList[len(throttleDataList)-1] = ThrottleData{}
			throttleDataList = throttleDataList[:len(throttleDataList)-1]
		}
	}
}

func addThrottleData(throttleData ThrottleData) {
	throttleDataList = append(throttleDataList, throttleData)
}
