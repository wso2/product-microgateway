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

import (
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

var (
	controlPlaneBrokerStatusChan  = make(chan bool)
	controlPlaneRestAPIStatusChan = make(chan bool)
	controlPlaneStarted           = false
	controlPlaneUnhealthy         = false
)

// SetControlPlaneBrokerStatus sets the given status to the internal channel controlPlaneBrokerStatusChan
func SetControlPlaneBrokerStatus(status bool) {
	// check for controlPlaneStarted, to non block call
	// if called again (somehow) after startup, for extra safe check this value
	if !controlPlaneStarted {
		controlPlaneBrokerStatusChan <- status
	}
}

// SetControlPlaneRestAPIStatus sets the given status to the internal channel controlPlaneRestAPIStatusChan
func SetControlPlaneRestAPIStatus(status bool) {
	// check for controlPlaneStarted, to non block call
	if !controlPlaneStarted && !controlPlaneUnhealthy {
		controlPlaneRestAPIStatusChan <- status
	}
}

// WaitForControlPlane sleep the current go routine until control-plane starts
func WaitForControlPlane() {
	brokerStarted := false
	restAPIStarted := false
	// if wait for both jmsStarted and restAPIStarted becomes true
	for !brokerStarted || !restAPIStarted {
		logger.LoggerHealth.Debugf("Waiting for connecting to control plane.. brokerStarted:%v restAPIStarted:%v", brokerStarted, restAPIStarted)
		select {
		case brokerStarted = <-controlPlaneBrokerStatusChan:
			logger.LoggerHealth.Debugf("Connection to Control Plane Broker %v", brokerStarted)
		case restAPIStarted = <-controlPlaneRestAPIStatusChan:
			logger.LoggerHealth.Debugf("Connection to Control Plane Rest API %v", restAPIStarted)
		}
	}
	controlPlaneStarted = true
	logger.LoggerHealth.Info("Successfully connected to the control plane.")
}
