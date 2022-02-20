/*
 * Package "synchronizer" contains artifacts relate to fetching APIs and
 * API related updates from the control plane event-hub.
 * This file contains functions to retrieve APIs and API updates.
 */

package synchronizer

import (
	"net/http"
	"time"
)

type worker struct {
	id            int
	internalQueue <-chan WorkerRequest
	processFunc   processHTTPRequest
	// done          *sync.WaitGroup
	// work          chan Work
	// quit          chan bool
}

// WorkerRequest is the task which can be submitted to the pool.
type WorkerRequest struct {
	Req                http.Request
	APIUUID            *string
	labels             []string
	SyncAPIRespChannel chan SyncAPIResponse
}

// Pool is the worker pool which is handling
type Pool struct {
	internalQueue chan WorkerRequest
	workers       []*worker
	quit          chan bool
	// TODO: (VirajSalaka) remove
	processFunc processHTTPRequest
	timeout     time.Duration
}

type processHTTPRequest func(*http.Request, *string, []string, chan SyncAPIResponse) bool

func (w *worker) ProcessFunction() {
	for workerReq := range w.internalQueue {
		responseReceived := w.processFunc(&workerReq.Req, workerReq.APIUUID, workerReq.labels, workerReq.SyncAPIRespChannel)
		if !responseReceived {
			// TODO: (VirajSalaka) make it configurable
			time.Sleep(5 * time.Second)
		}

	}
}

// WorkerPool is the thread pool responsible for sending the control plane request to fetch APIs
var WorkerPool *Pool

func init() {
	WorkerPool = newWorkerPool(4, 100)
}

func newWorkerPool(maxWorkers int, jobQueueCapacity int) *Pool {
	if jobQueueCapacity <= 0 {
		jobQueueCapacity = 100
	}
	requestChannel := make(chan WorkerRequest, jobQueueCapacity)
	workers := make([]*worker, maxWorkers)

	// create workers
	for i := 0; i < maxWorkers; i++ {
		workers[i] = &worker{
			id:            i,
			internalQueue: requestChannel,
			processFunc:   SendRequestToControlPlane,
		}
		go workers[i].ProcessFunction()
		// NewWorker(i+1, readyPool, &workersStopped)
	}

	return &Pool{
		internalQueue: requestChannel,
		workers:       workers,
		quit:          make(chan bool),
	}
}

// Enqueue Tries to enqueue but fails if queue is full
func (q *Pool) Enqueue(req WorkerRequest) bool {
	select {
	case q.internalQueue <- req:
		return true
	default:
		return false
	}
}

// EnqueueWithTimeout Tries to enqueue but fails if queue becomes not vacant within the defined period of time.
func (q *Pool) EnqueueWithTimeout(req WorkerRequest) bool {
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
