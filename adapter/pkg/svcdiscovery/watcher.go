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
	"fmt"
	"github.com/wso2/micro-gw/config"
	"io/ioutil"
	"net/url"
	"time"
)

//todo replace fmt with logger
var (
	conf                    *config.Config
	healthChecksPassingOnly bool
	pollInterval            time.Duration
	errConfLoad             error
	//ssl certs
	caCert []byte
	cert   []byte
	key    []byte

	ConsulWatcherInstance ConsulClient
	// Cluster Name -> consul syntax key
	ClusterConsulKeyMap map[string]string
	//Cluster Name -> Upstream
	//saves the last result with respected to a cluster
	ClusterConsulResultMap map[string][]Upstream
	//Cluster Name -> doneChan for respective go routine
	//when the cluster is removed we can stop the respective go routine to release resources
	ClusterConsulDoneChanMap map[string]chan bool
)

func init() {
	conf, errConfLoad = config.ReadConfigs()
	healthChecksPassingOnly = conf.Consul.HealthChecksPassingOnly
	pollInterval = time.Duration(int32(conf.Consul.PollInterval)) * time.Second

	ClusterConsulKeyMap = make(map[string]string)
	ClusterConsulResultMap = make(map[string][]Upstream)
	ClusterConsulDoneChanMap = make(map[string]chan bool)

	r, _ := url.Parse(conf.Consul.Url)

	//caCert, er1 := ioutil.ReadFile("certs/consul-agent-ca.pem")
	//fmt.Println(er1,"err1")
	_ = readCerts()
	pool := x509.NewCertPool()
	//caCert = []byte("-----BEGIN CERTIFICATE-----\n" +
	//	"MIIC7TCCApOgAwIBAgIQTJSoYy6c1twuT4+sqb3pGDAKBggqhkjOPQQDAjCBuTEL\n" +
	//	"MAkGA1UEBhMCVVMxCzAJBgNVBAgTAkNBMRYwFAYDVQQHEw1TYW4gRnJhbmNpc2Nv\n" +
	//	"MRowGAYDVQQJExExMDEgU2Vjb25kIFN0cmVldDEOMAwGA1UEERMFOTQxMDUxFzAV\n" +
	//	"BgNVBAoTDkhhc2hpQ29ycCBJbmMuMUAwPgYDVQQDEzdDb25zdWwgQWdlbnQgQ0Eg\n" +
	//	"MTAxNzkzMjAyOTE3NTQ0MTgwNzUwMjAyMzI0MzkwNTE4ODQzNjcyMB4XDTIxMDEw\n" +
	//	"NjAxNTUyNVoXDTI2MDEwNTAxNTUyNVowgbkxCzAJBgNVBAYTAlVTMQswCQYDVQQI\n" +
	//	"EwJDQTEWMBQGA1UEBxMNU2FuIEZyYW5jaXNjbzEaMBgGA1UECRMRMTAxIFNlY29u\n" +
	//	"ZCBTdHJlZXQxDjAMBgNVBBETBTk0MTA1MRcwFQYDVQQKEw5IYXNoaUNvcnAgSW5j\n" +
	//	"LjFAMD4GA1UEAxM3Q29uc3VsIEFnZW50IENBIDEwMTc5MzIwMjkxNzU0NDE4MDc1\n" +
	//	"MDIwMjMyNDM5MDUxODg0MzY3MjBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJHy\n" +
	//	"DhBHMQ14UB2flhUfMceC2fnmhNQ6yu+BCA14XGaA4dvuL5qa5KVB1n4o9qYvT6oc\n" +
	//	"yqX11oGtVEzdShSS30WjezB5MA4GA1UdDwEB/wQEAwIBhjAPBgNVHRMBAf8EBTAD\n" +
	//	"AQH/MCkGA1UdDgQiBCC/6kSjhEwHvONTVNLHlXSNgRTYpPaXCROeLjgQ00lBaTAr\n" +
	//	"BgNVHSMEJDAigCC/6kSjhEwHvONTVNLHlXSNgRTYpPaXCROeLjgQ00lBaTAKBggq\n" +
	//	"hkjOPQQDAgNIADBFAiEAtYhLzf7K00z5buwCovPe3gwKbQzJzjvLg/xwfUAukdgC\n" +
	//	"IHUvDrvMdAMpdEeXfJr0H1CsSUt2uwJ6LQr1xDmmPf7e\n" +
	//	"-----END CERTIFICATE-----")

	pool.AppendCertsFromPEM(caCert)
	//clientCert, _ := tls.LoadX509KeyPair("certs/local-dc-client-consul-0.pem", "certs/local-dc-client-consul-0-key.pem")
	//cert = []byte("-----BEGIN CERTIFICATE-----\nMIICpjCCAkygAwIBAgIQIzl4Fy" +
	//	"bvY6NN945INgLYrTAKBggqhkjOPQQDAjCBuTEL\nMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkNBMRYwF" +
	//	"AYDVQQHEw1TYW4gRnJhbmNpc2Nv\nMRowGAYDVQQJExExMDEgU2Vjb25kIFN0cmVldDEOMAwGA1UEE" +
	//	"RMFOTQxMDUxFzAV\nBgNVBAoTDkhhc2hpQ29ycCBJbmMuMUAwPgYDVQQDEzdDb25zdWwgQWdlbnQ" +
	//	"gQ0Eg\nMTAxNzkzMjAyOTE3NTQ0MTgwNzUwMjAyMzI0MzkwNTE4ODQzNjcyMB4XDTIxMDEw\nNjA" +
	//	"5NDEyN1oXDTIyMDEwNjA5NDEyN1owITEfMB0GA1UEAxMWY2xpZW50LmxvY2Fs\nLWRjLmNvbn" +
	//	"N1bDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHBiL1uK3uqCtue3\nU1o+f/V+JPgmu8ixz" +
	//	"ioIUFHM+V5xaVgy3k9Kn+BKKehy4Gg6b1jyJzq1rKFwlzRQ\nEMtkvWWjgcwwgckwDgYDVR0P" +
	//	"AQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMC\nBggrBgEFBQcDATAMBgNVHRMBAf8EAjA" +
	//	"AMCkGA1UdDgQiBCCH5kmYI9qlaRHactVC\nXVSpMgKG0HPQwV6rixT2kOpgXzArBgNVHSMEJD" +
	//	"AigCC/6kSjhEwHvONTVNLHlXSN\ngRTYpPaXCROeLjgQ00lBaTAyBgNVHREEKzApghZjbG" +
	//	"llbnQubG9jYWwtZGMuY29u\nc3Vsgglsb2NhbGhvc3SHBH8AAAEwCgYIKoZIzj0EAwIDSAAwRQIhALgI1Ae6omC/\nYa8KvA" +
	//	"vUM8HVE/iBB3fXnNM4UJZYxFg1AiBCOYpgDa23VUT8x9VJVuCmCZ02uEzC\nluNAt4BWlvADHA==\n-----END CERTIFICATE-----")
	//key = []byte("-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIPBO0X3kFQxjX" +
	//	"1QPO58OzLPVG4to4tBHNorS2SWiLyN1oAoGCCqGSM49\nAwEHoUQDQgAEcGIvW4re6oK257dT" +
	//	"Wj5/9X4k+Ca7yLHOKghQUcz5XnFpWDLeT0qf\n4Eop6HLgaDpvWPInOrWsoXCXNFAQy2S9ZQ" +
	//	"==\n-----END EC PRIVATE KEY-----")
	clientCert, errr := tls.X509KeyPair(cert, key)
	fmt.Println("Key pair error", errr)

	tlsConfig := NewTLSConfig(pool, []tls.Certificate{clientCert}, false)
	transport := NewTransport(&tlsConfig)
	client := NewHttpClient(&transport, 5*time.Second)
	ConsulWatcherInstance = NewConsulClient(client, healthChecksPassingOnly, r.Scheme, r.Host)
	//ConsulWatcherInstance.host = r.Host

}

func readCerts() error {
	var readErr error
	caCert = []byte("")
	cert = []byte("")
	key = []byte("")
	caFileContent, readErr := ioutil.ReadFile(conf.Consul.CaCertPath)
	if readErr != nil {
		fmt.Println("CA cert reading error", readErr)
		return readErr
	}
	if validateCert(caFileContent) {
		caCert = caFileContent
	}

	certFileContent, readErr := ioutil.ReadFile(conf.Consul.CertPath)
	if readErr != nil {
		fmt.Println("cert reading error", readErr)
		return readErr
	}
	if validateCert(certFileContent) {
		cert = certFileContent
	}

	keyFileContent, readErr := ioutil.ReadFile(conf.Consul.KeyPath)
	if readErr != nil {
		fmt.Println("key reading error", readErr)
		return readErr
	}
	if validateCert(keyFileContent) {
		key = keyFileContent
	}
	return readErr
}

func validateCert(content []byte) bool {
	return true
}
