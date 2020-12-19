package svcdiscovery

import (
	"context"
	"fmt"
	"github.com/hashicorp/consul/api"
	"time"
)

//todo get from config from global config
//todo replace fmt with logger
var (
	RequestTimeout        = 5 * time.Second
	RepeatInterval        = 5 * time.Second
	ConsulWatcherInstance *consulWatcher
	// Cluster Name -> consul syntax key
	ClusterConsulKeyMap map[string]string
	//Cluster Name -> QueryResult
	//saves the last result with respected to a cluster
	ClusterConsulResultMap map[string][]QueryResult
	//Cluster Name -> doneChan for respective go routine
	//when the cluster is removed we can stop the respective gon routine too
	ClusterConsulDoneChanMap map[string]chan bool
)

func init() {
	ClusterConsulKeyMap = make(map[string]string)
	ClusterConsulResultMap = make(map[string][]QueryResult)
	ClusterConsulDoneChanMap = make(map[string]chan bool)
	config := api.DefaultConfig()
	config.Address = "169.254.1.1:8500"
	cl, _ := api.NewClient(config)
	he := cl.Health()
	client := NewConsulClient(he)
	ConsulWatcherInstance, _ = NewConsulWatcher(client)
}

type consulWatcher struct {
	client *consulClient
}

type Watcher interface {
	//Watch poll consul server for a given query
	Watch(query Query, doneChan <-chan bool) <-chan []QueryResult
}

func (c consulWatcher) Watch(query Query, doneChan <-chan bool) <-chan []QueryResult {
	resultChan := make(chan []QueryResult)

	//long lived go routine
	go func() {
		ticker := time.NewTicker(RepeatInterval)
		intervalChan := ticker.C

		//cleanup when this go routine exits
		defer func() {
			if r := recover(); r != nil {
				fmt.Println("Recovered of from a panic line36 watcher.go", r)
			}
		}()
		defer close(resultChan)
		defer ticker.Stop()

		//go c.client.GetUpstreams(context.Background(), query, resultChan) //eliminate the first 5 sec delay
		for {
			timeout := time.After(RequestTimeout)
			ctx, cancel := context.WithCancel(context.Background())
			select {
			case <-doneChan:
				cancel()
				//logger.LoggerSvcDiscovery.Info("consul query stopped polling for:", query.QString)
				return
			case <-timeout:
				cancel() //propagates to http request
				//logger.LoggerSvcDiscovery.Info("consul query timeout for query:", query.QString)
			case <-intervalChan:
				go c.client.GetUpstreams(ctx, query, resultChan)
			}
		}
	}()

	return resultChan
}

//NewConsulWatcher constructor for consulWatcher
func NewConsulWatcher(client *consulClient) (*consulWatcher, error) {
	return &consulWatcher{client: client}, nil
}
