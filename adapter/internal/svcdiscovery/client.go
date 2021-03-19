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
	"strconv"
	"time"
)

const (
	apiVersion        = "v1"
	apiCatalogPath    = "/health/service/"
	apiMeshPath       = "/health/connect/"
	apiRootCertPath   = "/agent/connect/ca/roots"
	apiLeafCertPath   = "/agent/connect/ca/leaf/"
	consulTokenHeader = "X-Consul-Token"
	consulIndexHeader = "X-Consul-Index"
	datacenter        = "dc"
	passing           = "passing"
	passingVal        = "1"
	namespace         = "nc"
	indexQueryParam   = "index"
	waitQueryParam    = "wait"
	get               = "GET"
	longPollInterval  = 60 //in seconds, default = 300s
)

var (
	caReqLastIndex   = 0
	leafReqLastIndex = 0
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
	ID              string
	Proxy           Proxy
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
	ID          string
	InMesh      bool
}

//Proxy side car proxy information
type Proxy struct {
	DestinationServiceID string
	LocalServiceAddress  string
	LocalServicePort     int
}

//Query query structure for a consul string syntax
type Query struct {
	Datacenters []string
	ServiceName string
	Namespace   string
	Tags        []string
}

//Root contains root certificates
type Root struct {
	RootCert          string
	IntermediateCerts []string
	Active            bool
}

//RootCertResp structure of a response to a request to get the root certificates
type RootCertResp struct {
	Roots []Root
}

//ServiceCertResp structure of a response to get a service's certificate and private key
type ServiceCertResp struct {
	CertPEM       string
	PrivateKeyPEM string
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
	client         http.Client //for upstreams
	longPollClient http.Client //for certs
	scheme         string
	host           string
	aclToken       string
	pollInterval   time.Duration
}

//NewConsulClient constructor for ConsulClient
func NewConsulClient(api http.Client, longPollClient http.Client, scheme string, host string, aclToken string) ConsulClient {
	return ConsulClient{
		client:         api,
		longPollClient: longPollClient,
		scheme:         scheme,
		host:           host,
		pollInterval:   api.Timeout,
		aclToken:       aclToken,
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

// construct a URL from it's elements
func constructURL(scheme, host, apiV string, otherPaths ...string) string {
	url := scheme + "://" + host
	if host[len(host)-1:] != "/" {
		url += "/"
	}
	url += apiV
	for _, path := range otherPaths {
		url += path
	}
	return url
}

//sends a get request to a consul-client
//parses the response into []Upstream
func (c ConsulClient) get(path string, dc string, nc string, tags []string) ([]Upstream, error) {
	url := constructURL(c.scheme, c.host, apiVersion, apiCatalogPath, path)
	req, _ := http.NewRequest(get, url, nil)

	//add query parameters
	q := req.URL.Query()
	q.Add(datacenter, dc) //datacenter
	//q.Add(passing, passingVal) // health checks passing only todo remove comment
	if nc != "" { //namespace, an enterprise feature
		q.Add(namespace, nc)
	}
	req.URL.RawQuery = q.Encode()
	response, errHTTP := c.client.Do(req)
	if errHTTP != nil {
		return []Upstream{}, errHTTP
	}
	//set headers
	req.Header.Set(consulTokenHeader, c.aclToken)
	var results []result
	body, errRead := ioutil.ReadAll(response.Body)
	if errRead != nil {
		return []Upstream{}, errRead
	}
	errUnmarshal := json.Unmarshal(body, &results)
	var out []Upstream
	for _, r := range results {
		address := r.Service.Address
		if address == "" {
			address = r.Node.Address
		}
		res := Upstream{
			Address:     address,
			ServicePort: r.Service.Port,
			ID:          r.Service.ID,
			InMesh:      false,
		}

		logger.LoggerSvcDiscovery.Println("Result: ", res)
		if contains(r.Service.Tags, tags) {
			out = append(out, res)
			//tags haven't been defined
		} else if len(tags) == 1 && tags[0] == "" {
			out = append(out, res)
		}
	}
	return out, errUnmarshal
}

//sends a get request to a consul-client
//parses the response into []Upstream
//gets upstreams that only belongs to service mesh
func (c ConsulClient) getMeshUpstreams(path string) ([]Upstream, error) {
	url := constructURL(c.scheme, c.host, apiVersion, apiMeshPath, path)
	req, _ := http.NewRequest(get, url, nil)

	//add query parameters
	q := req.URL.Query()
	//q.Add(passing, passingVal) // health checks passing only
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
			address = r.Service.Proxy.LocalServiceAddress
		}
		res := Upstream{
			Address:     address,
			ServicePort: r.Service.Port,
			ID:          r.Service.Proxy.DestinationServiceID,
			InMesh:      true,
		}
		out = append(out, res)
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
			logger.LoggerSvcDiscovery.Error("Service registry unreachable ", errGet)
		}
	}

	if len(result) == 0 {
		logger.LoggerSvcDiscovery.Debugln("Consul service registry query came up with empty result")
	} else {
		if MeshEnabled { //replace the actual address and port with proxy's address and port
			resMesh, errGet := c.getMeshUpstreams(query.ServiceName)
			if errGet == nil {
				for i := range resMesh {
					for j := range result {
						if resMesh[i].ID == result[j].ID {
							result[j].Address = resMesh[i].Address
							result[j].ServicePort = resMesh[i].ServicePort
						}
					}
				}
			} else {
				logger.LoggerSvcDiscovery.Error("Service registry unreachable ", errGet)
			}
		}
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
				logger.LoggerSvcDiscovery.Info("Consul stopped polling for query :", query)
				return
			case <-intervalChan:
				c.getUpstreams(query, resultChan)
			}
		}
	}()

	return resultChan
}

func updateCAIndex(currentIndex int) bool {
	if caReqLastIndex > currentIndex {
		caReqLastIndex = 0 //Reset in case Consul's indexing messes up
		return true
	} else if caReqLastIndex < currentIndex {
		caReqLastIndex = currentIndex
		return true
	}
	return false
}

func updateLeafIndex(currentIndex int) bool {
	if leafReqLastIndex > currentIndex {
		leafReqLastIndex = 0 //Reset
		return true
	} else if leafReqLastIndex < currentIndex {
		leafReqLastIndex = currentIndex
		return true
	}
	return false
}

func (c ConsulClient) getCertRequest(url string, lastIndex int) (*http.Response, error) {

	request, errReq := http.NewRequest(get, url, nil)
	if errReq != nil {
		return nil, errReq
	}
	request.Header.Set(consulTokenHeader, aclToken)
	//send the last index to activate long polling from serverside
	query := request.URL.Query()
	query.Add(indexQueryParam, strconv.Itoa(lastIndex))
	query.Add(waitQueryParam, strconv.Itoa(longPollInterval/60)+"s")
	request.URL.RawQuery = query.Encode()
	response, errClient := c.longPollClient.Do(request)
	if errClient != nil {
		return nil, errClient
	}
	return response, nil
}

func (c ConsulClient) getRootCert(signal chan bool) {
	url := constructURL(c.scheme, c.host, apiVersion, apiRootCertPath)
	result := RootCertResp{}
	response, errReq := c.getCertRequest(url, caReqLastIndex)
	if errReq != nil {
		logger.LoggerSvcDiscovery.Error("Error getting root cert: ", errReq)
		return
	}
	body, errRead := ioutil.ReadAll(response.Body)
	if errRead != nil {
		logger.LoggerSvcDiscovery.Error("Error reading root cert request: ", errRead)
		return
	}
	errUnmarshal := json.Unmarshal(body, &result)
	if errUnmarshal != nil {
		logger.LoggerSvcDiscovery.Error("Malformed response: ", errUnmarshal)
		return
	}
	index, errStrConv := strconv.Atoi(response.Header.Get(consulIndexHeader))
	if errStrConv != nil {
		logger.LoggerSvcDiscovery.Error("Index header not sent")
		return
	}
	shouldUpdateRouter := updateCAIndex(index)
	if shouldUpdateRouter {
		//there is only one root CA cert except while rotation in progress
		for _, root := range result.Roots {
			if root.Active && root.RootCert != "" { //only select the active root
				MeshCACert = root.RootCert
			}
		}
		signal <- true
	}
}

func (c ConsulClient) getServiceCertAndKey(signal chan bool) {
	url := constructURL(c.scheme, c.host, apiVersion, apiLeafCertPath, mgwServiceName)
	result := ServiceCertResp{}
	response, errReq := c.getCertRequest(url, leafReqLastIndex)
	if errReq != nil {
		logger.LoggerSvcDiscovery.Error("Error getting leaf cert and key: ", errReq)
		return
	}
	body, errRead := ioutil.ReadAll(response.Body)
	if errRead != nil {
		logger.LoggerSvcDiscovery.Error("Error reading leaf cert and key: ", errRead)
		return
	}
	errUnmarshal := json.Unmarshal(body, &result)
	if errUnmarshal != nil {
		logger.LoggerSvcDiscovery.Error("Malformed response: ", errUnmarshal)
		return
	}

	index, errStrConv := strconv.Atoi(response.Header.Get(consulIndexHeader))
	if errStrConv != nil {
		logger.LoggerSvcDiscovery.Error("Index header not sent")
		return
	}
	shouldUpdateRouter := updateLeafIndex(index)
	if shouldUpdateRouter {
		MeshServiceCert = result.CertPEM
		MeshServiceKey = result.PrivateKeyPEM
		signal <- true
	}
}

//LongPollRootCert starts long polling root certificate
func (c ConsulClient) LongPollRootCert(signal chan bool) {
	go func(signal chan bool) {
		c.getRootCert(signal)
		ticker := time.NewTicker(longPollInterval * time.Second)
		intervalChan := ticker.C //emits a signal every longPollInterval
		defer ticker.Stop()
		for {
			select {
			case <-intervalChan:
				c.getRootCert(signal)
			}
		}
	}(signal)
}

//LongPollServiceCertAndKey starts long polling for service cert and key
func (c ConsulClient) LongPollServiceCertAndKey(signal chan bool) {
	go func(signal chan bool) {
		c.getServiceCertAndKey(signal)
		ticker := time.NewTicker(longPollInterval * time.Second)
		intervalChan := ticker.C //emits a signal every longPollInterval
		defer ticker.Stop()
		for {
			select {
			case <-intervalChan:
				c.getServiceCertAndKey(signal)
			}
		}
	}(signal)
}
