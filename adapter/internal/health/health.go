package health

import (
	"context"
	healthservice "github.com/wso2/adapter/internal/health/api/wso2/health/service"
	logger "github.com/wso2/adapter/loggers"
	"sync"
)

var (
	healthStatus         = make(map[string]bool)
	mutexForHealthUpdate sync.Mutex
)

const (
	AuthService                    service = "adapter.internal.Authorization"
	EventHubRestAPIConsumerService service = "adapter.internal.eventHub.RestAPIConsumer"
	EventHubAMQPConsumerService    service = "adapter.internal.eventHub.AMQPConsumer"
)

type service string

func (s service) SetStatus(isHealthy bool) {
	mutexForHealthUpdate.Lock()
	defer mutexForHealthUpdate.Unlock()
	healthStatus[string(s)] = isHealthy
}

type Server struct {
	healthservice.UnimplementedHealthServer
}

func (s Server) Check(ctx context.Context, request *healthservice.HealthCheckRequest) (*healthservice.HealthCheckResponse, error) {
	logger.LoggerHealth.Debugf("Querying health state for Adapter service \"%s\"", request.Service)
	logger.LoggerHealth.Debugf("Internal health state map: %v", healthStatus)

	if request.Service == "" {
		// overall health of the server
		isHealthy := true
		for _, ok := range healthStatus {
			isHealthy = isHealthy && ok
		}

		if isHealthy {
			logger.LoggerHealth.Info("Responding health state of Adapter as HEALTHY")
			return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_SERVING}, nil
		}
		logger.LoggerHealth.Info("Responding health state of Adapter as NOT_HEALTHY")
		return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_NOT_SERVING}, nil
	}

	// health of the component of a server
	if isHealthy, ok := healthStatus[request.Service]; ok {
		if isHealthy {
			logger.LoggerHealth.Infof("Responding health state of Adapter service \"%s\" as HEALTHY", request.Service)
			return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_SERVING}, nil
		}
		logger.LoggerHealth.Infof("Responding health state of Adapter service \"%s\" as NOT_HEALTHY", request.Service)
		return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_NOT_SERVING}, nil
	}

	// no component found
	logger.LoggerHealth.Infof("Responding health state of Adapter service \"%s\" as UNKNOWN", request.Service)
	return &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_UNKNOWN}, nil
}

func (s Server) Watch(request *healthservice.HealthCheckRequest, server healthservice.Health_WatchServer) error {
	response := &healthservice.HealthCheckResponse{Status: healthservice.HealthCheckResponse_SERVING}
	return server.Send(response)
}
