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
 */

package config

import (
	"sync"
	"time"
)

// Experimenting asynchronous communication between go routines using channels
// This uses singleton pattern where creating a single channel for communication
//
// To get a instance of the channel for a data publisher go routine
//  `publisher := NewSender()`
//
// Create a receiver channel in worker go routine
// receiver := NewReceiver()
//
// From publisher go routine, feed string value to the channel
// publisher<- "some value"
//
// In worker go routine, read the value sent by the publisher
// message := <-receiver
var once sync.Once

// C represents the channel to identify modifications added to the configuration file
// TODO: (VirajSalaka) remove this as unused.
var (
	C chan string // better to be interface{} type which could send any type of data.
)

// NewSender initializes the channel if it is not created an returns
func NewSender() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

// NewReceiver initializes the channel if it is not created an returns
func NewReceiver() chan string {
	once.Do(func() {
		C = make(chan string)
	})
	return C
}

// Config represents the adapter configuration.
// It is created directly from the configuration toml file.
type Config struct {
	//Adapter related Configurations
	Adapter struct {
		// Server represents the configuration related to REST API (to which the apictl requests)
		Server struct {
			// Host name of the server
			Host string
			// Port of the server
			Port string
			// APICTL Users
			Users []APICtlUser `toml:"users"`
			// Access token validity duration. Valid time units are "ns", "us" (or "µs"), "ms", "s", "m", "h". eg: "2h45m"
			TokenTTL string
			// Private key to sign the token
			TokenPrivateKeyPath string
		}

		//Consul represents the configuration required to connect to consul service discovery
		Consul struct {
			//Enable whether consul service discovery should be enabled
			Enable bool
			//URL url of the consul client in format: http(s)://host:port
			URL string
			//PollInterval how frequently consul API should be polled to get updates (in seconds)
			PollInterval int
			//ACLToken Access Control Token required to invoke HTTP API
			ACLToken string
			//MgwServiceName service name that Microgateway registered in Consul Service Mesh
			MgwServiceName string
			//ServiceMeshEnabled whether Consul service mesh is enabled
			ServiceMeshEnabled bool
			//CaCertFile path to the CA cert file(PEM encoded) required for tls connection between adapter and a consul client
			CaCertFile string
			//CertFile path to the cert file(PEM encoded) required for tls connection between adapter and a consul client
			CertFile string
			//KeyFile path to the key file(PEM encoded) required for tls connection between adapter and a consul client
			KeyFile string
		}
		// Keystore contains the keyFile and Cert File of the adapter
		Keystore keystore
		//Trusted Certificates
		Truststore truststore
	}

	// Envoy Listener Component related configurations.
	Envoy struct {
		ListenerHost            string
		ListenerPort            uint32
		SecuredListenerHost     string
		SecuredListenerPort     uint32
		ClusterTimeoutInSeconds time.Duration
		KeyStore                keystore

		// Global CORS configurations.
		Cors struct {
			Enabled          bool
			AllowOrigins     []string
			AllowMethods     []string
			AllowHeaders     []string
			AllowCredentials bool
			ExposeHeaders    []string
		}

		// Envoy Upstream Related Configurations
		Upstream struct {
			//UpstreamTLS related Configuration
			TLS struct {
				MinVersion             string `toml:"minimumProtocolVersion"`
				MaxVersion             string `toml:"maximumProtocolVersion"`
				Ciphers                string `toml:"ciphers"`
				CACrtPath              string `toml:"trustedCertPath"`
				VerifyHostName         bool   `toml:"verifyHostName"`
				DisableSSLVerification bool   `toml:"disableSslVerification"`
			}
		}
	} `toml:"router"`

	Enforcer struct {
		EventHub        eventHub
		ApimCredentials apimCredentials
		AuthService     authService
		JwtGenerator    jwtGenerator
		Cache           cache
		Throttling      throttlingConfig
		JwtIssuer       jwtIssuer
	}

	Security struct {
		Adapter struct {
			EnableOutboundAuthHeader bool   `toml:"enableOutboundAuthHeader"`
			AuthorizationHeader      string `toml:"authorizationHeader"`
		}
		Enforcer struct {
			TokenService []tokenService
		}
	}

	ControlPlane controlPlane `toml:"controlPlane"`

	Analytics analytics `toml:"analytics"`
}

type apimCredentials struct {
	Username string
	Password string
}

type authService struct {
	Port           int32
	MaxMessageSize int32
	MaxHeaderLimit int32
	KeepAliveTime  int32
	ThreadPool     threadPool
}

type threadPool struct {
	CoreSize      int32
	MaxSize       int32
	KeepAliveTime int32
	QueueSize     int32
}

type keystore struct {
	PrivateKeyLocation string `toml:"keyPath"`
	PublicKeyLocation  string `toml:"certPath"`
}

type truststore struct {
	Location string
}

type tokenService struct {
	Name                 string
	Issuer               string
	CertificateAlias     string
	JwksURL              string
	ValidateSubscription bool
	ConsumerKeyClaim     string
	CertificateFilePath  string
	ClaimMapping         []claimMapping
}

type eventHub struct {
	Enabled                 bool
	ServiceURL              string
	JmsConnectionParameters struct {
		EventListeningEndpoints string `toml:"eventListeningEndpoints"`
	} `toml:"jmsConnectionParameters"`
}

type throttlingConfig struct {
	EnableGlobalEventPublishing        bool   `toml:"enableGlobalEventPublishing"`
	EnableHeaderConditions             bool   `toml:"enableHeaderConditions"`
	EnableQueryParamConditions         bool   `toml:"enableQueryParamConditions"`
	EnableJwtClaimConditions           bool   `toml:"enableJwtClaimConditions"`
	JmsConnectionInitialContextFactory string `toml:"jmsConnectionInitialContextFactory"`
	JmsConnectionProviderURL           string `toml:"jmsConnectionProviderUrl"`
	Publisher                          binaryPublisher
}

type binaryPublisher struct {
	Username string
	Password string
	URLGroup []urlGroup `toml:"urlGroup"`
	Pool     publisherPool
	Agent    binaryAgent
}

type urlGroup struct {
	ReceiverURLs []string `toml:"receiverURLs"`
	AuthURLs     []string `toml:"authURLs"`
	Type         string   `toml:"type"`
}

type publisherPool struct {
	MaxIdleDataPublishingAgents        int32
	InitIdleObjectDataPublishingAgents int32
	PublisherThreadPoolCoreSize        int32
	PublisherThreadPoolMaximumSize     int32
	PublisherThreadPoolKeepAliveTime   int32
}

type binaryAgent struct {
	SslEnabledProtocols        string
	Ciphers                    string
	QueueSize                  int32
	BatchSize                  int32
	CorePoolSize               int32
	SocketTimeoutMS            int32
	MaxPoolSize                int32
	KeepAliveTimeInPool        int32
	ReconnectionInterval       int32
	MaxTransportPoolSize       int32
	MaxIdleConnections         int32
	EvictionTimePeriod         int32
	MinIdleTimeInPool          int32
	SecureMaxTransportPoolSize int32
	SecureMaxIdleConnections   int32
	SecureEvictionTimePeriod   int32
	SecureMinIdleTimeInPool    int32
}

type jwtGenerator struct {
	Enable                bool   `toml:"enable"`
	Encoding              string `toml:"encoding"`
	ClaimDialect          string `toml:"claimDialect"`
	ConvertDialect        bool   `toml:"convertDialect"`
	Header                string `toml:"header"`
	SigningAlgorithm      string `toml:"signingAlgorithm"`
	EnableUserClaims      bool   `toml:"enableUserClaims"`
	GatewayGeneratorImpl  string `toml:"gatewayGeneratorImpl"`
	ClaimsExtractorImpl   string `toml:"claimsExtractorImpl"`
	PublicCertificatePath string `toml:"publicCertificatePath"`
	PrivateKeyPath        string `toml:"privateKeyPath"`
}

type claimMapping struct {
	RemoteClaim string
	LocalClaim  string
}

type cache struct {
	Enabled     bool  `toml:"enabled"`
	MaximumSize int32 `toml:"maximumSize"`
	ExpiryTime  int32 `toml:"expiryTime"`
}

type analytics struct {
	Enabled bool `toml:"enabled"`

	Adapter struct {
		BufferFlushInterval time.Duration `toml:"bufferFlushInterval"`
		BufferSizeBytes     uint32        `toml:"bufferSizeBytes"`
		GRPCRequestTimeout  time.Duration `toml:"gRPCRequestTimeout"`
	}

	Enforcer struct {
		ConfigProperties    map[string]string
		EnforcerLogReceiver authService `toml:"LogReceiver"`
	}
}

type routerLogPublisher struct {
	BufferFlushInterval time.Duration `toml:"bufferFlushInterval"`
	BufferSizeBytes     uint32        `toml:"bufferSizeBytes"`
	GRPCRequestTimeout  time.Duration `toml:"gRPCRequestTimeout"`
}

type jwtIssuer struct {
	Enabled               bool      `toml:"enabled"`
	Issuer                string    `toml:"issuer"`
	Encoding              string    `toml:"encoding"`
	ClaimDialect          string    `toml:"claimDialect"`
	SigningAlgorithm      string    `toml:"signingAlgorithm"`
	PublicCertificatePath string    `toml:"publicCertificatePath"`
	PrivateKeyPath        string    `toml:"privateKeyPath"`
	ValidityPeriod        int32     `toml:"validityPeriod"`
	JwtUsers              []JwtUser `toml:"jwtUser"`
}

// JwtUser represents allowed users to generate JWT tokens
type JwtUser struct {
	Username string `toml:"username"`
	Password string `toml:"password"`
}

// APICtlUser represents registered APICtl Users
type APICtlUser struct {
	Username string
	Password string
}

// ControlPlane struct contains configurations related to the API Manager
type controlPlane struct {
	EventHub struct {
		Enabled                 bool          `toml:"enabled"`
		ServiceURL              string        `toml:"serviceUrl"`
		Username                string        `toml:"username"`
		Password                string        `toml:"password"`
		SyncApisOnStartUp       bool          `toml:"syncApisOnStartUp"`
		EnvironmentLabels       []string      `toml:"environmentLabels"`
		RetryInterval           time.Duration `toml:"retryInterval"`
		SkipSSLVerification     bool          `toml:"skipSSLVerification"`
		JmsConnectionParameters struct {
			EventListeningEndpoints []string `toml:"eventListeningEndpoints"`
		} `toml:"jmsConnectionParameters"`
	} `toml:"eventHub"`
}

// APIContent contains everything necessary to create an API
type APIContent struct {
	VHost              string
	Name               string
	Version            string
	APIType            string
	LifeCycleStatus    string
	APIDefinition      []byte
	UpstreamCerts      []byte
	Environments       []string
	ProductionEndpoint string
	SandboxEndpoint    string
	SecurityScheme     []string
	EndpointSecurity   EndpointSecurity
	AuthHeader         string
}

// APIJsonData contains everything necessary to extract api.json/api.yaml file
type APIJsonData struct {
	Data struct {
		APIName                    string   `json:"name,omitempty"`
		APIContext                 string   `json:"context,omitempty"`
		APIVersion                 string   `json:"version,omitempty"`
		APIType                    string   `json:"type,omitempty"`
		LifeCycleStatus            string   `json:"lifeCycleStatus,omitempty"`
		EndpointImplementationType string   `json:"endpointImplementationType,omitempty"`
		AuthorizationHeader        string   `json:"authorizationHeader,omitempty"`
		SecurityScheme             []string `json:"securityScheme,omitempty"`
		EndpointConfig             struct {
			EndpointType     string `json:"endpoint_type,omitempty"`
			EndpointSecurity struct {
				Production struct {
					Password string `json:"password,omitempty"`
					Type     string `json:"type,omitempty"`
					Enabled  bool   `json:"enabled,omitempty"`
					Username string `json:"username,omitempty"`
				} `json:"production,omitempty"`
				Sandbox struct {
					Password string `json:"password,omitempty"`
					Type     string `json:"type,omitempty"`
					Enabled  bool   `json:"enabled,omitempty"`
					Username string `json:"username,omitempty"`
				} `json:"sandbox,omitempty"`
			} `json:"endpoint_security,omitempty"`
			ProductionEndpoints struct {
				Endpoint string `json:"url,omitempty"`
			} `json:"production_endpoints,omitempty"`
			SandBoxEndpoints struct {
				Endpoint string `json:"url,omitempty"`
			} `json:"sandbox_endpoints,omitempty"`
		} `json:"endpointConfig,omitempty"`
	} `json:"data"`
}

// EpSecurity contains parameters of endpoint security at api.json
type EpSecurity struct {
	Password string `json:"password,omitempty"`
	Type     string `json:"type,omitempty"`
	Enabled  bool   `json:"enabled,omitempty"`
	Username string `json:"username,omitempty"`
}

// EndpointSecurity contains the SandBox/Production endpoint security
type EndpointSecurity struct {
	SandBox    SecurityInfo `json:"SandBox,omitempty"`
	Production SecurityInfo `json:"Production,omitempty"`
}

// SecurityInfo contains the parameters of endpoint security
type SecurityInfo struct {
	Password         string `json:"password,omitempty"`
	CustomParameters string `json:"customparameters,omitempty"`
	SecurityType     string `json:"Type,omitempty"`
	Enabled          bool   `json:"enabled,omitempty"`
	Username         string `json:"username,omitempty"`
}
