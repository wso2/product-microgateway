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

package svcdiscovery

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	logger "github.com/wso2/micro-gw/loggers"
	"io/ioutil"
	"net/http"
	"time"
)

const (
	apiPath           = "v1/health/service/"
	datacenter        = "dc"
	passing           = "passing"
	passingVal        = "1"
	namespace         = "nc"
	consulTokenHeader = "X-Consul-Token"
)

type node struct {
	Address         string
	Datacenter      string
	TaggedAddresses map[string]string
}

type service struct {
	Tags            []string
	Address         string
	TaggedAddresses interface{}
	Port            int
}

//result is used to unmarshal the required components from the consul server's response
type result struct {
	Node    node
	Service service
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

//newHTTPClient is a golang http client with request timeout
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

func newHTTPSTransport(config *tls.Config) http.Transport {
	return http.Transport{
		TLSClientConfig: config,
	}
}
func newHTTPTransport() http.Transport {
	return http.Transport{}
}

//ConsulClient wraps the HTTP API
type ConsulClient struct {
	client       http.Client //Health checks + all other functionalities
	scheme       string
	host         string
	aclToken     string
	pollInterval time.Duration
}

//NewConsulClient constructor for ConsulClient
func NewConsulClient(api http.Client, scheme string, host string, aclToken string) ConsulClient {
	return ConsulClient{
		client:       api,
		scheme:       scheme,
		host:         host,
		pollInterval: api.Timeout,
		aclToken:     aclToken,
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
func (c ConsulClient) get(path string, dc string, nc string, tags []string) ([]Upstream, error) {
	url := c.scheme + "://" + c.host
	if c.host[len(c.host)-1:] != "/" {
		url += "/"
	}
	url += apiPath
	url += path

	req, _ := http.NewRequest("GET", url, nil)

	//add query parameters
	q := req.URL.Query()
	q.Add(datacenter, dc)      //datacenter
	q.Add(passing, passingVal) // health checks passing only
	if nc != "" {              //namespace, an enterprise feature
		q.Add(namespace, nc)
	}

	req.URL.RawQuery = q.Encode()
	response, errHTTP := c.client.Do(req)
	if errHTTP != nil {
		return []Upstream{}, errHTTP
	}
	//set headers
	req.Header.Set(consulTokenHeader, c.aclToken)
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
			logger.LoggerSvcDiscovery.Error("Recovered from a panic: ", r)
			//panic: if the resultChan is closed
		}
	}()

	var result []Upstream
	for _, dc := range query.Datacenters {
		res, errGet := c.get(query.ServiceName, dc, query.Namespace, query.Tags)
		if errGet == nil {
			result = append(result, res...)
		} else {
			logger.LoggerSvcDiscovery.Error("service registry unreachable ", errGet)
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
		logger.LoggerSvcDiscovery.Error("Config errors found in consul config. ", errConfLoad, " query: ", query, " wil not be polled")
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
				logger.LoggerSvcDiscovery.Info("consul query stopped polling for:", query)
				return
			case <-intervalChan:
				c.getUpstreams(query, resultChan)
			}
		}
	}()

	return resultChan
}
