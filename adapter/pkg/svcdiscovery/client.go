/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package svcdiscovery

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	logger "github.com/wso2/micro-gw/loggers"
	"io/ioutil"
	"net/http"
	"time"
	//logger "github.com/wso2/micro-gw/loggers"
)

var (
	apiPath = "v1/health/service/"
)

type proxy struct {
	MeshGateway interface{}
	Expose      interface{}
}

type connect struct {
}

type node struct {
	ID              string
	Node            string
	Address         string
	Datacenter      string
	TaggedAddresses map[string]string
	Meta            map[string]string
	CreateIndex     int
	ModifyIndex     int
}

type service struct {
	ID                string
	Service           string
	Tags              []string
	Address           string
	TaggedAddresses   interface{}
	Meta              interface{}
	Port              int
	Weights           map[string]int
	EnableTagOverride bool
	Proxy             proxy
	Connect           connect
}

type check struct {
	Node        string
	CheckID     string
	Name        string
	Status      string
	Notes       string
	Output      string
	ServiceID   string
	ServiceName string
	ServiceTags []string
	Type        string
	Definition  interface{}
	CreateIndex int
	ModifyIndex int
}

type result struct {
	Node    node
	Service service
	Checks  []check
}

//Upstream Data for a service instance
type Upstream struct {
	Address     string
	ServicePort int
}

//Query query structure for a consul string syntax
type Query struct {
	Datacenters []string
	ServiceName string
	Namespace   string
	Tags        []string
}

func newHTTPClient(transport *http.Transport, timeout time.Duration) http.Client {
	client := http.Client{
		Transport: transport,
		Timeout:   timeout,
	}
	return client
}

func newTLSConfig(rootCAs *x509.CertPool, certs []tls.Certificate, insecureSkipVerify bool) tls.Config {
	return tls.Config{
		RootCAs:            rootCAs,
		Certificates:       certs,
		InsecureSkipVerify: insecureSkipVerify,
	}
}

func newTransport(config *tls.Config) http.Transport {
	return http.Transport{
		TLSClientConfig: config,
	}
}

//ConsulClient wraps the HTTP API
type ConsulClient struct {
	client                  http.Client //Health checks + all other functionalities
	healthChecksPassingOnly bool
	scheme                  string
	host                    string
	//requestTimeout          time.Duration
	pollInterval time.Duration
}

//NewConsulClient constructor for ConsulClient
func NewConsulClient(api http.Client, healthChecksPassingOnly bool, scheme string, host string) ConsulClient {
	//logger.LoggerSvcDiscovery.Debugln("Consul client created")
	return ConsulClient{
		client:                  api,
		healthChecksPassingOnly: healthChecksPassingOnly,
		scheme:                  scheme,
		host:                    host,
		//requestTimeout:          requestTimeout,
		pollInterval: api.Timeout,
	}
}

//whether source list contains one of elements list
func contains(source []string, elements []string) bool {
	for _, a := range source {
		for _, b := range elements {
			if a == b {
				return true
			}
		}
	}
	return false
}

//sends a get request to a consul-client
//parses the response into []Upstream
func (c ConsulClient) get(path string, dc string, passingOnly bool, namespace string, tags []string) ([]Upstream, error) {
	url := c.scheme + "://" + c.host
	if c.host[len(c.host)-1:] != "/" {
		url += "/"
	}
	url += apiPath
	url += path

	req, _ := http.NewRequest("GET", url, nil)

	//add query parameters
	q := req.URL.Query()
	q.Add("dc", dc)
	if passingOnly {
		q.Add("passing", "1")
	}
	if namespace != "" {
		q.Add("nc", namespace)
	}

	req.URL.RawQuery = q.Encode()
	response, errHTTP := c.client.Do(req)
	if errHTTP != nil {
		return []Upstream{}, errHTTP
	}
	var result []result
	body, errRead := ioutil.ReadAll(response.Body)
	if errRead != nil {
		return []Upstream{}, errRead
	}
	errUnmarshal := json.Unmarshal(body, &result)
	var out []Upstream
	for _, r := range result {
		address := r.Service.Address
		if address == "" {
			address = r.Node.Address
		}
		res := Upstream{
			Address:     address,
			ServicePort: r.Service.Port,
		}
		if contains(r.Service.Tags, tags) {
			out = append(out, res)
			//tags haven't been defined
		} else if len(tags) == 1 && tags[0] == "" {
			out = append(out, res)
		}
	}
	return out, errUnmarshal
}

//gets the upstreams for single query(ex: [dc1,dc2].namespace.serviceA.[tag1,tag2])
//there can be many upstreams per query
//sends the respective upstreams through resultChan
func (c ConsulClient) getUpstreams(query Query, resultChan chan []Upstream) {

	defer func() {
		if r := recover(); r != nil {
			logger.LoggerSvcDiscovery.Error("Recovered of from a panic: ", r)
			//panic: if the resultChan is closed
		}
	}()

	var result []Upstream
	for _, dc := range query.Datacenters {
		res, errGet := c.get(query.ServiceName, dc, c.healthChecksPassingOnly, query.Namespace, query.Tags)
		if errGet == nil {
			result = append(result, res...)
		}
	}
	if len(result) == 0 {
		logger.LoggerSvcDiscovery.Error("consul service registry query came up with empty result/ service registry unreachable")
	} else {
		resultChan <- result
	}

}

//Poll periodically poll consul for updates using getUpstreams() func.
//doneChan is there to release resources
//closing the doneChan will stop polling
func (c ConsulClient) Poll(query Query, doneChan <-chan bool) <-chan []Upstream {
	resultChan := make(chan []Upstream)

	//do not start polling consul if there are config errors
	if errConfLoad != nil {
		logger.LoggerSvcDiscovery.Error("Config errors found in consul config. ", query, " wil not be polled")
		return resultChan
	}

	//this routine will live until doneChan is closed
	go func() {
		ticker := time.NewTicker(pollInterval)
		intervalChan := ticker.C //emits a signal every pollInterval(5 seconds)

		//handle panics
		defer func() {
			if r := recover(); r != nil {
				logger.LoggerSvcDiscovery.Info("Recovered from panic: ", r)
			}
		}()
		// release resources when this go routine exits
		defer close(resultChan)
		defer ticker.Stop()

		for {
			select {
			case <-doneChan:
				//sending a signal through doneChan will cause this go routine to exit
				//logger.LoggerSvcDiscovery.Info("consul query stopped polling for:", query.QString)
				return
			case <-intervalChan:
				c.getUpstreams(query, resultChan)
			}
		}
	}()

	return resultChan
}
