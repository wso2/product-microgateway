/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package health

import logger "github.com/wso2/adapter/loggers"

var (
	controlPlaneJmsStatusChan     = make(chan bool)
	controlPlaneRestAPIStatusChan = make(chan bool)
	controlPlaneStarted           = false
)

// SetControlPlaneJmsStatus sets the given status to the internal channel controlPlaneJmsStatusChan
func SetControlPlaneJmsStatus(status bool) {
	if !controlPlaneStarted {
		controlPlaneJmsStatusChan <- status
	}
}

// SetControlPlaneRestAPIStatus sets the given status to the internal channel controlPlaneRestAPIStatusChan
func SetControlPlaneRestAPIStatus(status bool) {
	if !controlPlaneStarted {
		controlPlaneRestAPIStatusChan <- status
	}
}

// WaitForControlPlane sleep the current go routine until control-plane starts
func WaitForControlPlane() {
	jmsStarted := false
	restAPIStarted := false
	for !jmsStarted || !restAPIStarted {
		select {
		case jmsStarted = <-controlPlaneJmsStatusChan:
			logger.LoggerHealth.Debugf("Connection to Control Plane JMS %v", jmsStarted)
		case restAPIStarted = <-controlPlaneRestAPIStatusChan:
			logger.LoggerHealth.Debugf("Connection to Control Plane Rest API %v", restAPIStarted)
		}
	}
	controlPlaneStarted = true
	logger.LoggerHealth.Info("Control Plane started successfully.")
}
