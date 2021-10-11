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

// Package tlsutils contains the utility functions related to tls communication of the adapter
package tlsutils

import (
	"crypto/tls"
	"crypto/x509"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"sync"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/pkg/loggers"
)

var (
	onceTrustedCertsRead sync.Once
	onceKeyCertsRead     sync.Once
	certificate          tls.Certificate
	certReadErr          error
	caCertPool           *x509.CertPool
)

const (
	pemExtension  string = ".pem"
	crtExtension  string = ".crt"
	authorization string = "Authorization"
)

// GetServerCertificate returns the certificate (used for the restAPI server and xds server) created based on configuration values.
// Move to pkg. remove config and read from a file path
func GetServerCertificate(tlsCertificate string, tlsCertificateKey string) (tls.Certificate, error) {
	certReadErr = nil
	onceKeyCertsRead.Do(func() {
		cert, err := tls.LoadX509KeyPair(string(tlsCertificate), string(tlsCertificateKey))
		if err != nil {
			logger.LoggerTLSUtils.Fatal("Error while loading the tls keypair.", err)
			certReadErr = err
		}
		certificate = cert
	})
	return certificate, certReadErr
}

// GetTrustedCertPool returns the trusted certificate (used for the restAPI server and xds server) created based on
// the provided directory/file path.
// Move to pkg
func GetTrustedCertPool(truststoreLocation string) *x509.CertPool {
	onceTrustedCertsRead.Do(func() {
		caCertPool = x509.NewCertPool()
		filepath.Walk(truststoreLocation, func(path string, info os.FileInfo, err error) error {
			if err != nil {
				logger.LoggerTLSUtils.Warn("Error while reading the trusted certificates directory/file.", err)
			} else {
				if !info.IsDir() && (filepath.Ext(info.Name()) == pemExtension ||
					filepath.Ext(info.Name()) == crtExtension) {
					caCert, caCertErr := ioutil.ReadFile(path)
					if caCertErr != nil {
						logger.LoggerTLSUtils.Warn("Error while reading the certificate file.", info.Name())
					}
					if IsPublicCertificate(caCert) {
						caCertPool.AppendCertsFromPEM(caCert)
						logger.LoggerTLSUtils.Debugf("%v : Certificate is added as a trusted certificate.", info.Name())
					}
				}
			}
			return nil
		})
	})
	return caCertPool
}

// IsPublicCertificate checks if the file content represents valid public certificate in PEM format.
// Move to pkg
func IsPublicCertificate(certContent []byte) bool {
	certContentPattern := `\-\-\-\-\-BEGIN\sCERTIFICATE\-\-\-\-\-((.|\n)*)\-\-\-\-\-END\sCERTIFICATE\-\-\-\-\-`
	regex := regexp.MustCompile(certContentPattern)
	if regex.Match(certContent) {
		return true
	}
	return false
}

// InvokeControlPlane sends request to the control plane and returns the response
func InvokeControlPlane(req *http.Request, skipSSL bool) (*http.Response, error) {
	tr := &http.Transport{}
	if !skipSSL {
		_, _, truststoreLocation := GetKeyLocations()
		caCertPool := GetTrustedCertPool(truststoreLocation)
		tr = &http.Transport{
			TLSClientConfig: &tls.Config{RootCAs: caCertPool},
		}
	} else {
		tr = &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
		}
	}

	// Configuring the http client
	client := &http.Client{
		Transport: tr,
	}
	return client.Do(req)
}

// GetKeyLocations function returns the public key path and private key path
func GetKeyLocations() (string, string, string) {
	conf, _ := config.ReadConfigs()
	publicKeyLocation := conf.Adapter.Keystore.CertPath
	privateKeyLocation := conf.Adapter.Keystore.KeyPath
	truststoreLocation := conf.Adapter.Truststore.Location
	return publicKeyLocation, privateKeyLocation, truststoreLocation
}
