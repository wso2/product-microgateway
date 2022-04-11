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
	// done          *sync.WaitGroup
	// work          chan Work
	// quit          chan bool
}

// WorkerRequest is the task which can be submitted to the pool.
type workerRequest struct {
	Req                http.Request
	APIUUID            *string
	labels             []string
	SyncAPIRespChannel chan SyncAPIResponse
}

// pool is the worker pool which is handling
type pool struct {
	// TODO: (VirajSalaka) remove timeout
	internalQueue      chan workerRequest
	workers            []*worker
	quit               chan bool
	timeout            time.Duration
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
// TODO: (VirajSalaka) comment
func InitializeWorkerPool(maxWorkers, jobQueueCapacity int, delayForFaultRequests time.Duration, trustStoreLocation string,
	skipSSL bool, requestTimeout, retryInterval time.Duration, serviceURL, username, password string) {
	// TODO: (VirajSalaka) Think on whether this could be moved to global adapter seamlessly.
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
		quit:          make(chan bool),
	}
}

// Enqueue Tries to enqueue but fails if queue is full
func (q *pool) Enqueue(req workerRequest) bool {
	if len(q.internalQueue) > cap(q.internalQueue)/10*8 {
		loggers.LoggerSync.Errorf("Queue size for worker pool is at %d."+
			"Please check the reason for control plane request failures", len(q.internalQueue))
	} else if len(q.internalQueue) > cap(q.internalQueue)/10*5 {
		loggers.LoggerSync.Warnf("Queue size for worker pool is at %d."+
			"Please check the reason for control plane request failures", len(q.internalQueue))
	}
	select {
	case q.internalQueue <- req:
		return true
	default:
		return false
	}
}

// EnqueueWithTimeout Tries to enqueue but fails if queue becomes not vacant within the defined period of time.
func (q *pool) EnqueueWithTimeout(req workerRequest) bool {
	// TODO: (VirajSalaka) Remove this
	timeout := q.timeout
	if timeout <= 0 {
		timeout = 1 * time.Second
	}

	ch := make(chan bool, 1)
	t := time.AfterFunc(timeout, func() {
		ch <- false
	})
	defer func() {
		t.Stop()
	}()

	select {
	case q.internalQueue <- req:
		return true
	case <-ch:
		return false
	}
}
