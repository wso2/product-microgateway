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

// Package messaging holds the implementation for event listeners functions
package messaging

import (
	"encoding/json"

	"github.com/wso2/adapter/internal/discovery/api/wso2/discovery/throttle"
	"github.com/wso2/adapter/internal/discovery/xds"
	"github.com/wso2/adapter/internal/synchronizer"

	"github.com/streadway/amqp"
	logger "github.com/wso2/adapter/loggers"
)

const (
	blockIPRange        = "IPRANGE"
	blockIP             = "IP"
	blockStateTrue      = "true"
	templateStateAdd    = "add"
	templateStateRemove = "remove"
)

// handleThrottleData handles Key template and Blocking condition in throttle data event.
func handleThrottleData(deliveries <-chan amqp.Delivery, done chan error) {
	for d := range deliveries {
		var data EventThrottleData
		var throttleData *throttle.ThrottleData
		e := json.Unmarshal([]byte(string(d.Body)), &data)
		if e != nil {
			logger.LoggerMsg.Errorf("Couldn't parse throttle data message. %v", e)
			return
		}
		logger.LoggerMsg.Debugf("Throttle Data: %s", string(d.Body))

		payload := data.Event.PayloadData
		if payload.BlockingCondition != "" {
			// control plane sends a blocking throttle data event for subscription blocking.
			// this is not required and causes issues in evaluating subscription blocking.
			if payload.BlockingCondition == "SUBSCRIPTION" {
				return
			}
			isIPCondition := payload.BlockingCondition == blockIP || payload.BlockingCondition == blockIPRange

			if isIPCondition {
				var ipCondition synchronizer.IPCondition
				ipError := json.Unmarshal([]byte(payload.ConditionValue), &ipCondition)
				if ipError != nil {
					logger.LoggerMsg.Errorf("Couldn't parse condition value as IPCondition. %v", ipError)
					return
				}
				ip := &throttle.IPCondition{
					TenantDomain: payload.TenantDomain,
					Id:           payload.ID,
					Type:         payload.BlockingCondition,
					FixedIp:      ipCondition.FixedIP,
					StartingIp:   ipCondition.StartingIP,
					EndingIp:     ipCondition.EndingIP,
					Invert:       ipCondition.Invert,
				}
				if payload.State == blockStateTrue {
					synchronizer.AddBlockingIPCondition(ip)
				} else {
					synchronizer.RemoveBlockingIPCondition(ip)
				}
			} else {
				if payload.State == blockStateTrue {
					synchronizer.AddBlockingCondition(payload.ConditionValue)
				} else {
					synchronizer.RemoveBlockingCondition(payload.ConditionValue)
				}
			}
			throttleData = &throttle.ThrottleData{
				BlockingConditions:   synchronizer.GetBlockingConditions(),
				IpBlockingConditions: synchronizer.GetBlockingIPConditions(),
			}
		} else if data.Event.PayloadData.KeyTemplateValue != "" {
			if payload.KeyTemplateState == templateStateAdd {
				synchronizer.AddKeyTemplate(payload.KeyTemplateValue)
			} else if payload.KeyTemplateState == templateStateRemove {
				synchronizer.RemoveKeyTemplate(payload.KeyTemplateValue)
			}

			throttleData = &throttle.ThrottleData{
				KeyTemplates: synchronizer.GetKeyTemplates(),
			}
		} else {
			d.Ack(false)
			continue
		}
		xds.UpdateEnforcerThrottleData(throttleData)
		d.Ack(false)
	}
	logger.LoggerMsg.Infof("handle: deliveries channel closed")
	done <- nil
}
