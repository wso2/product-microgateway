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
	"context"
	healthservice "github.com/wso2/adapter/internal/health/api/wso2/health/service"
	logger "github.com/wso2/adapter/loggers"
	"sync"
)

var (
	serviceHealthStatus = make(map[string]bool)
	healthStatuses      = map[bool]string{
		true:  "HEALTHY",
		false: "UNHEALTHY",
	}
	mutexForHealthUpdate sync.Mutex
)

// Service components to be set health status
const (
	RestService service = "adapter.internal.RestService"
)

type service string

// SetStatus sets the health state of the service
func (s service) SetStatus(isHealthy bool) {
	mutexForHealthUpdate.Lock()
	defer mutexForHealthUpdate.Unlock()
	logger.LoggerHealth.Infof("Update health status of service \"%s\" as %s", s, healthStatuses[isHealthy])
	serviceHealthStatus[string(s)] = isHealthy
}

// Server represents the Health GRPC server
type Server struct {
	healthservice.UnimplementedHealthServer
}

// Check responds the health check client with health status of the Adapter
func (s Server) Check(ctx context.Context, request *healthservice.HealthCheckRequest) (*healthservice.HealthCheckResponse, error) {
	logger.LoggerHealth.Debugf("Querying health state for Adapter service \"%s\"", request.Service)
	logger.LoggerHealth.Debugf("Internal health state map: %v", serviceHealthStatus)

	if request.Service == "" {
		// overall health of the server
		isHealthy := true
		for _, ok := range serviceHealthStatus {
			isHealthy = isHealthy && ok
		}

		if isHealthy {
			logger.LoggerHealth.Debug("Responding health state of Adapter as HEALTHY")
			return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_SERVING}, nil
		}
		logger.LoggerHealth.Debug("Responding health state of Adapter as NOT_HEALTHY")
		return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_NOT_SERVING}, nil
	}

	// health of the component of a server
	if isHealthy, ok := serviceHealthStatus[request.Service]; ok {
		if isHealthy {
			logger.LoggerHealth.Debugf("Responding health state of Adapter service \"%s\" as HEALTHY", request.Service)
			return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_SERVING}, nil
		}
		logger.LoggerHealth.Debugf("Responding health state of Adapter service \"%s\" as NOT_HEALTHY", request.Service)
		return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_NOT_SERVING}, nil
	}

	// no component found
	logger.LoggerHealth.Debugf("Responding health state of Adapter service \"%s\" as UNKNOWN", request.Service)
	return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_UNKNOWN}, nil
}
