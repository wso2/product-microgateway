package svcdiscovery

import (
	"context"
	"fmt"
	"github.com/hashicorp/consul/api"
	"github.com/wso2/micro-gw/config"
	"time"
)

//todo replace fmt with logger
//todo profile memory usage
var (
	conf                    *config.Consul
	healthChecksPassingOnly bool
	requestTimeout          time.Duration
	pollInterval            time.Duration

	ConsulWatcherInstance *consulWatcher
	// Cluster Name -> consul syntax key
	ClusterConsulKeyMap map[string]string
	//Cluster Name -> QueryResult
	//saves the last result with respected to a cluster
	ClusterConsulResultMap map[string][]QueryResult
	//Cluster Name -> doneChan for respective go routine
	//when the cluster is removed we can stop the respective go routine to release resources
	ClusterConsulDoneChanMap map[string]chan bool
)

func init() {
	conf, _ = config.ReadConsulConfig()
	healthChecksPassingOnly = conf.HealthChecksPassingOnly
	requestTimeout = time.Duration(int32(conf.RequestTimeout)) * time.Second
	pollInterval = time.Duration(int32(conf.PollInterval)) * time.Second

	ClusterConsulKeyMap = make(map[string]string)
	ClusterConsulResultMap = make(map[string][]QueryResult)
	ClusterConsulDoneChanMap = make(map[string]chan bool)

	// config for consul client
	con := api.DefaultConfig()
	con.Address = conf.Address
	con.Scheme = conf.Scheme
	con.TokenFile = conf.TokenFile
	cl, errInit := api.NewClient(con) // initialize consul client
	if errInit != nil {               // Errors in Address,schema,token file result in errInit
		cl, _ = api.NewClient(api.DefaultConfig())
	}
	he := cl.Health() // we are using health checks
	client := NewConsulClient(he)
	ConsulWatcherInstance = NewConsulWatcher(client)
}

type consulWatcher struct {
	client *consulClient
}

//Watcher interface
type Watcher interface {
	//Watch poll consul server for a given single query
	Watch(query Query, doneChan <-chan bool) <-chan []QueryResult
}

func (c consulWatcher) Watch(query Query, doneChan <-chan bool) <-chan []QueryResult {
	resultChan := make(chan []QueryResult)

	//this routine will live until doneChan is closed
	go func() {
		ticker := time.NewTicker(pollInterval)
		intervalChan := ticker.C //emits a signal every pollInterval(5 seconds)

		//handle panics
		defer func() {
			if r := recover(); r != nil {
				fmt.Println("Recovered from panic: ", r)
			}
		}()
		// release resources when this go routine exits
		defer close(resultChan)
		defer ticker.Stop()

		//go c.client.GetUpstreams(context.Background(), query, resultChan) //eliminate the first 5 sec delay
		for {
			// timeouts the current request
			// garbage collected after the timer fires
			timeout := time.After(requestTimeout)
			ctx, cancel := context.WithCancel(context.Background())
			select {
			case <-doneChan:
				cancel()
				//sending a signal through doneChan will cause this go routine to exit
				//logger.LoggerSvcDiscovery.Info("consul query stopped polling for:", query.QString)
				return
			case <-timeout:
				cancel() //propagates to http request
				//logger.LoggerSvcDiscovery.Error("consul query timeout for query:", query.QString)
			case <-intervalChan:
				go c.client.GetUpstreams(ctx, query, resultChan)
				// possible panic:
			}
		}
	}()

	return resultChan
}

//NewConsulWatcher constructor for consulWatcher
func NewConsulWatcher(client *consulClient) *consulWatcher {
	return &consulWatcher{client: client}
}
