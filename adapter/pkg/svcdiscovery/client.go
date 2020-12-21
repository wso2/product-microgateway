package svcdiscovery

import (
	"context"
	"fmt"
	"github.com/hashicorp/consul/api"
	//logger "github.com/wso2/micro-gw/loggers"
)

//todo import loggers,use loggers instead of fmt.Println()

//QueryResult Data for a service instance
type QueryResult struct {
	//ServiceName string
	//DataCenter  string
	Address     string
	ServicePort int
	//ServiceTags []string
	//ModifyIndex uint64 //todo remove ModifyIndex?
}

//QueryString query structure for a consul string syntax
type QueryString struct {
	Datacenters []string
	ServiceName string
	Namespace   string
	Tags        []string
}

//Query = QueryString + QueryOptions
type Query struct {
	QString      QueryString
	QueryOptions *api.QueryOptions
}

//consulClient wraps the official go consul client
type consulClient struct {
	api *api.Health //Health checks + all other functionalities
}

type ConsulClient interface {
	//GetUpstreams get all the upstreams that matches a query
	GetUpstreams(ctx context.Context, query Query, resultChan chan []QueryResult)
	//Service wraps the consul go client's method
	Service(service string, tag string, passingOnly bool, q *api.QueryOptions) ([]*api.ServiceEntry, *api.QueryMeta, error)
	//// todo consider mesh scenario : Connect(service, tag string, q *api.QueryOptions),
	//Connect(service, tag string, q *api.QueryOptions) ([]*api.CatalogService, *api.QueryMeta, error)
}

//NewConsulClient constructor
//todo modify the constructor: receive config as a param
func NewConsulClient(a *api.Health) *consulClient {
	//logger.LoggerSvcDiscovery.Debugln("Consul client created")
	return &consulClient{api: a}
}

func (c consulClient) Service(service string, tag string, passingOnly bool, q *api.QueryOptions) ([]*api.ServiceEntry, *api.QueryMeta, error) {
	//logger.LoggerSvcDiscovery.Debugln("polling consul server:", q.Datacenter, service, tag, "passing only:",passingOnly)
	return c.api.Service(service, tag, passingOnly, q)
}

func (c consulClient) GetUpstreams(ctx context.Context, query Query, resultChan chan []QueryResult) {

	defer func() {
		if r := recover(); r != nil {
			fmt.Println("Recovered of from a panic: ", r)
			//panic: if the resultChan is closed
		}
	}()

	var result []QueryResult
	for _, dc := range query.QString.Datacenters {
		qo := query.QueryOptions.WithContext(ctx) // returns a new queryOptions obj with ctx
		qo.Datacenter = dc
		qo.Namespace = query.QString.Namespace
		for _, tag := range query.QString.Tags {

			res, _, err := c.Service(query.QString.ServiceName, tag, healthChecksPassingOnly, qo)
			if err != nil {
				//logger.LoggerSvcDiscovery.Info(err)
			} else {
				for _, r := range res {
					res := QueryResult{
						Address:     r.Node.Address,
						ServicePort: r.Service.Port,
						//ModifyIndex: r.Node.ModifyIndex,
					}
					result = append(result, res)
				}
			}
		}
	}
	if len(result) == 0 {
		fmt.Println("consul service registry query came up with empty result/ service registry unreachable")
	} else {
		resultChan <- result
	}

}
