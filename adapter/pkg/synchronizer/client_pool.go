/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

/*
 * Package "synchronizer" contains artifacts relate to fetching APIs and
 * API related updates from the control plane event-hub.
 * This file contains functions to retrieve APIs and API updates.
 */

package synchronizer

import (
	"crypto/tls"
	"net/http"
	"sync"
	"time"

	"github.com/wso2/product-microgateway/adapter/pkg/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/tlsutils"
)

type worker struct {
	id              int
	internalQueue   <-chan workerRequest
	processFunc     processHTTPRequest
	delayAfterFault time.Duration
}

// workerRequest is the task which can be submitted to the pool.
type workerRequest struct {
	Req                http.Request
	APIUUID            *string
	labels             []string
	SyncAPIRespChannel chan SyncAPIResponse
}

// pool is the worker pool which is handling
type pool struct {
	internalQueue      chan workerRequest
	workers            []*worker
	client             http.Client
	controlPlaneParams controlPlaneParameters
}

type controlPlaneParameters struct {
	serviceURL    string
	username      string
	password      string
	retryInterval time.Duration
}

type processHTTPRequest func(*http.Request, *string, []string, chan SyncAPIResponse, *http.Client) bool

func (w *worker) ProcessFunction() {
	for workerReq := range w.internalQueue {
		responseReceived := w.processFunc(&workerReq.Req, workerReq.APIUUID, workerReq.labels, workerReq.SyncAPIRespChannel,
			&workerPool.client)
		if !responseReceived {
			time.Sleep(w.delayAfterFault)
		}
	}
}

var (
	// WorkerPool is the thread pool responsible for sending the control plane request to fetch APIs
	workerPool        *pool
	oncePoolInitiated sync.Once
)

// InitializeWorkerPool creates the Worker Pool used for the Control Plane Rest API invocations.
// maxWorkers indicate the maximum number of parallel workers sending requests to the control plane.
// jobQueueCapacity indicate the maximum number of requests can kept inside a single worker's queue.
// delayForFaultRequests indicate the delay a worker enforce (in seconds) when a fault response is received.
func InitializeWorkerPool(maxWorkers, jobQueueCapacity int, delayForFaultRequests time.Duration, trustStoreLocation string,
	skipSSL bool, requestTimeout, retryInterval time.Duration, serviceURL, username, password string) {
	oncePoolInitiated.Do(func() {
		workerPool = newWorkerPool(maxWorkers, jobQueueCapacity, delayForFaultRequests)
		workerPool.controlPlaneParams = controlPlaneParameters{
			serviceURL:    serviceURL,
			username:      username,
			password:      password,
			retryInterval: retryInterval,
		}
		var tr *http.Transport
		if !skipSSL {
			caCertPool := tlsutils.GetTrustedCertPool(trustStoreLocation)
			tr = &http.Transport{
				TLSClientConfig: &tls.Config{RootCAs: caCertPool},
			}
		} else {
			tr = &http.Transport{
				TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			}
		}
		// Configure Connection Level Parameters since it is reused over and over
		tr.MaxConnsPerHost = maxWorkers * 2
		tr.MaxIdleConns = maxWorkers * 2
		tr.MaxIdleConnsPerHost = maxWorkers * 2
		workerPool.client = http.Client{
			Transport: tr,
			Timeout:   requestTimeout * time.Second,
		}
	})
}

func newWorkerPool(maxWorkers, jobQueueCapacity int, delayForFaultRequests time.Duration) *pool {
	if jobQueueCapacity <= 0 {
		jobQueueCapacity = 100
	}
	requestChannel := make(chan workerRequest, jobQueueCapacity)
	workers := make([]*worker, maxWorkers)

	// create workers
	for i := 0; i < maxWorkers; i++ {
		workers[i] = &worker{
			id:              i,
			internalQueue:   requestChannel,
			processFunc:     SendRequestToControlPlane,
			delayAfterFault: delayForFaultRequests,
		}
		go workers[i].ProcessFunction()
		loggers.LoggerSync.Infof("ControlPlane processing worker %d spawned.", i)
	}

	return &pool{
		internalQueue: requestChannel,
		workers:       workers,
	}
}

// Enqueue Tries to enqueue but fails if queue is full
func (q *pool) Enqueue(req workerRequest) bool {
	select {
	case q.internalQueue <- req:
		return true
	default:
		return false
	}
}
