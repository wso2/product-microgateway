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
	"io/ioutil"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
)

var (
	//IsServiceDiscoveryEnabled whether Consul service discovery should be enabled
	IsServiceDiscoveryEnabled bool
	onceConfigLoad            sync.Once
	conf                      *config.Config
	pollInterval              time.Duration
	errConfLoad               error
	//ssl certs
	caCert   []byte
	cert     []byte
	key      []byte
	aclToken string

	//ConsulClientInstance instance for consul client
	ConsulClientInstance ConsulClient
	//ClusterConsulKeyMap Cluster Name -> consul syntax key
	ClusterConsulKeyMap map[string]string
	//ClusterConsulResultMap Cluster Name -> Upstream
	//saves the last result with respected to a cluster
	ClusterConsulResultMap map[string][]Upstream
	//ClusterConsulDoneChanMap Cluster Name -> doneChan for respective go routine
	//when the cluster is removed we can stop the respective go routine to stop polling and release resources
	ClusterConsulDoneChanMap map[string]chan bool
)

func init() {
	ClusterConsulKeyMap = make(map[string]string)
	ClusterConsulResultMap = make(map[string][]Upstream)
	ClusterConsulDoneChanMap = make(map[string]chan bool)
	//Read config
	conf, errConfLoad = config.ReadConfigs()
	IsServiceDiscoveryEnabled = conf.Adapter.Consul.Enable
}

//read the certs and access token required for tls into respective global variables
func readCerts() error {
	// TODO: (VirajSalaka) Replace with common CA cert pool
	caFileContent, readErr := ioutil.ReadFile(conf.Adapter.Consul.CaCertPath)
	if readErr != nil {
		return readErr
	}

	certFileContent, readErr := ioutil.ReadFile(conf.Adapter.Consul.CertPath)
	if readErr != nil {
		return readErr
	}

	keyFileContent, readErr := ioutil.ReadFile(conf.Adapter.Consul.KeyPath)
	if readErr != nil {
		return readErr
	}

	aclTokenContent, readErr := ioutil.ReadFile(conf.Adapter.Consul.ACLTokenFilePath)
	if readErr != nil && conf.Adapter.Consul.ACLTokenFilePath != "" {
		return readErr
	}

	caCert = caFileContent
	cert = certFileContent
	key = keyFileContent
	aclToken = strings.TrimSpace(string(aclTokenContent))

	return nil
}

//InitConsul loads certs and initialize a ConsulClient
//lazy loading
func InitConsul() {
	onceConfigLoad.Do(func() {
		conf, errConfLoad = config.ReadConfigs()
		if errConfLoad != nil {
			logger.LoggerSvcDiscovery.Error("Consul Config loading error ", errConfLoad)
			return
		}
		pollInterval = time.Duration(conf.Adapter.Consul.PollInterval) * time.Second
		urlStructure, errURLParse := url.Parse(conf.Adapter.Consul.URL)
		if errURLParse != nil {
			errConfLoad = errURLParse
			logger.LoggerSvcDiscovery.Error("Invalid URL to Consul Client ", errURLParse)
			return
		}
		if urlStructure.Scheme == "https" { //communicate to consul through https
			errCertRead := readCerts()
			if errCertRead != nil {
				errConfLoad = errCertRead
				logger.LoggerSvcDiscovery.Error("Consul Certs read error ", errCertRead)
				return
			}
			pool := x509.NewCertPool()
			pool.AppendCertsFromPEM(caCert)
			clientCert, errKeyPairLoad := tls.X509KeyPair(cert, key)
			if errKeyPairLoad != nil {
				errConfLoad = errKeyPairLoad
				logger.LoggerSvcDiscovery.Error("Key pair error", errKeyPairLoad)
				return
			}
			tlsConfig := newTLSConfig(pool, []tls.Certificate{clientCert}, false)
			transport := newHTTPSTransport(&tlsConfig)
			client := newHTTPClient(&transport, pollInterval)
			ConsulClientInstance = NewConsulClient(client, urlStructure.Scheme, urlStructure.Host, aclToken)
		} else {
			//communicate to consul through http
			transport := newHTTPTransport()
			client := newHTTPClient(&transport, pollInterval)
			ConsulClientInstance = NewConsulClient(client, urlStructure.Scheme, urlStructure.Host, aclToken)
		}

	})
}
