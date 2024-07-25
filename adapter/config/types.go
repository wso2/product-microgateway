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
	"fmt"
	"sync"
	"time"
)

// Experimenting asynchronous communication between go routines using channels
// This uses singleton pattern where creating a single channel for communication
//
// To get a instance of the channel for a data publisher go routine
//
//	`publisher := NewSender()`
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

const (
	//UnassignedAsDeprecated is used by the configurations which are deprecated.
	UnassignedAsDeprecated string = "unassigned-as-deprecated"
)

// Config represents the adapter configuration.
// It is created directly from the configuration toml file.
// Note :
//
//	Don't use toml tag for configuration properties as it may affect environment variable based
//	config resolution.
type Config struct {
	Adapter       adapter
	Enforcer      enforcer
	Envoy         envoy         `toml:"router"`
	ControlPlane  controlPlane  `toml:"controlPlane"`
	GlobalAdapter globalAdapter `toml:"globalAdapter"`
	Analytics     analytics     `toml:"analytics"`
	Tracing       tracing
}

// Adapter related Configurations
type adapter struct {
	// Server represents the configuration related to REST API (to which the apictl requests)
	Server server
	// VhostMapping represents default vhost of gateway environments
	VhostMapping []vhostMapping
	// Consul represents the configuration required to connect to consul service discovery
	Consul consul
	// XdsPayloadFormatter represents the configuration to format the xds payload
	XdsPayloadFormatter xdsPayloadFormatter
	// Keystore contains the keyFile and Cert File of the adapter
	Keystore keystore
	// Trusted Certificates
	Truststore truststore
	// ArtifactsDirectory is the FilePath where the api artifacts are mounted
	ArtifactsDirectory string
	// Sandbox vhost for Choreo that exposes the sandbox endpoint
	SandboxVhost string
	// Sandbox environment name for Choreo that exposes the sandbox endpoint
	SandboxEnvName string
	// Feature flag to enable semantic version based intelligent routing
	IsIntelligentRoutingEnabled bool
}

// Envoy Listener Component related configurations.
type envoy struct {
	ListenerHost                     string
	ListenerPort                     uint32
	SecuredListenerHost              string
	SecuredListenerPort              uint32
	ClusterTimeoutInSeconds          time.Duration
	EnforcerResponseTimeoutInSeconds time.Duration `default:"20"`
	KeyStore                         keystore
	SystemHost                       string `default:"localhost"`
	InternalServiceHost              string
	Cors                             globalCors
	Upstream                         envoyUpstream
	Connection                       connection
	RateLimit                        rateLimit
	MaximumResourcePathLengthInKB    int16
}

type connectionTimeouts struct {
	RequestTimeoutInSeconds        time.Duration
	RequestHeadersTimeoutInSeconds time.Duration // default disabled
	StreamIdleTimeoutInSeconds     time.Duration // Default 5 mins
	IdleTimeoutInSeconds           time.Duration // default 1hr
}

type connection struct {
	Timeouts connectionTimeouts
}

type rateLimit struct {
	Enabled                bool
	Host                   string
	Port                   uint32
	XRateLimitHeaders      xRateLimitHeaders
	FailureModeDeny        bool
	RequestTimeoutInMillis int64
	KeyFilePath            string
	CertFilePath           string
	CaCertFilePath         string
	SSLCertSANHostname     string
}

type xRateLimitHeaders struct {
	Enabled    bool
	RFCVersion string
}

type enforcer struct {
	Security     security
	AuthService  authService
	JwtGenerator jwtGenerator
	Cache        cache
	Throttling   throttlingConfig
	JwtIssuer    jwtIssuer
	Management   management
	RestServer   restServer
	Filters      []filter
	Metrics      metrics
}

type server struct {
	// Enabled the serving the REST API
	Enabled bool `default:"true"`
	// Host name of the server
	Host string
	// Port of the server
	Port string
	// APICTL Users
	Users []APICtlUser
	// Access token validity duration. Valid time units are "ns", "us" (or "µs"), "ms", "s", "m", "h". eg: "2h45m"
	TokenTTL string
	// Private key to sign the token
	TokenPrivateKeyPath string
}

type vhostMapping struct {
	// Environment name of the gateway
	Environment string
	// Vhost to be default of the environment
	Vhost string
}

type consul struct {
	// Deprecated: Use Enabled instead
	Enable bool
	// Enabled whether consul service discovery should be enabled
	Enabled bool
	// URL url of the consul client in format: http(s)://host:port
	URL string
	// PollInterval how frequently consul API should be polled to get updates (in seconds)
	PollInterval int
	// ACLToken Access Control Token required to invoke HTTP API
	ACLToken string
	// MgwServiceName service name that Microgateway registered in Consul Service Mesh
	MgwServiceName string
	// ServiceMeshEnabled whether Consul service mesh is enabled
	ServiceMeshEnabled bool
	// CaCertFile path to the CA cert file(PEM encoded) required for tls connection between adapter and a consul client
	CaCertFile string
	// CertFile path to the cert file(PEM encoded) required for tls connection between adapter and a consul client
	CertFile string
	// KeyFile path to the key file(PEM encoded) required for tls connection between adapter and a consul client
	KeyFile string
}

type xdsPayloadFormatter struct {
	// KeyManagerConfigs contains format configurations related to key manager configuration
	KeyManagerConfigs keyManagerConfigs
}

type keyManagerConfigs struct {
	// RetainKeys contains the keys that should be retained in the xds payload
	RetainKeys []string
}

// Global CORS configurations
type globalCors struct {
	Enabled          bool
	AllowOrigins     []string
	AllowMethods     []string
	AllowHeaders     []string
	AllowCredentials bool
	ExposeHeaders    []string
	MandatoryHeaders []string
}

// Envoy Upstream Related Configurations
type envoyUpstream struct {
	// UpstreamTLS related Configuration
	TLS      upstreamTLS
	Timeouts upstreamTimeout
	Health   upstreamHealth
	DNS      upstreamDNS
	Retry    upstreamRetry
}

type upstreamTLS struct {
	MinimumProtocolVersion string
	MaximumProtocolVersion string
	Ciphers                string
	TrustedCertPath        string
	VerifyHostName         bool
	DisableSslVerification bool
}

type upstreamTimeout struct {
	RouteTimeoutInSeconds     uint32
	MaxRouteTimeoutInSeconds  uint32
	RouteIdleTimeoutInSeconds uint32
}

type upstreamHealth struct {
	Timeout            int32
	Interval           int32
	UnhealthyThreshold int32
	HealthyThreshold   int32
}

type upstreamDNS struct {
	DNSRefreshRate int32
	RespectDNSTtl  bool
	DNSResolver    dnsResolverConfig `toml:"DNSResolver"`
}

type dnsResolverConfig struct {
	ResolverType dnsResolverType
	CAres        cAres
}

type dnsResolverType string

const (
	// DNSResolverCAres is the c-ares DNS resolver type
	DNSResolverCAres dnsResolverType = "c-ares"
)

func (r dnsResolverType) isValid() error {
	switch r {
	case DNSResolverCAres: // if required we can include DNS_RESOLVER_APPLE here
		return nil
	case "": // for Envoy default settings
		return nil
	}
	return fmt.Errorf("invalid DNS resolver type: %q, supported types [%q]", r, DNSResolverCAres)
}

type cAres struct {
	Resolvers                []socketAddress
	UseResolversAsFallback   bool
	FilterUnroutableFamilies bool
	UseTCPForDNSLookups      bool
	NoDefaultSearchDomain    bool
}

type socketAddress struct {
	Protocol string
	Address  string
	Port     uint32
}

type upstreamRetry struct {
	MaxRetryCount uint32
	BaseInterval  time.Duration
	MaxInterval   time.Duration
	RetryOn       string
	StatusCodes   []uint32
}

type security struct {
	TokenService []tokenService
	AuthHeader   authHeader
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
	KeyPath  string
	CertPath string
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

type throttlingConfig struct {
	EnableGlobalEventPublishing        bool
	EnableHeaderConditions             bool
	EnableQueryParamConditions         bool
	EnableJwtClaimConditions           bool
	JmsConnectionInitialContextFactory string
	JmsConnectionProviderURL           string
	// Deprecated: Use JmsConnectionProviderURL instead
	JmsConnectionProviderURLDeprecated string `toml:"jmsConnectionProviderUrl"`
	Publisher                          binaryPublisher
}

type binaryPublisher struct {
	Username string
	Password string
	URLGroup []urlGroup
	// Deprecated: Use URLGroup instead
	URLGroupDeprecated []urlGroup `toml:"urlGroup"`
	Pool               publisherPool
	Agent              binaryAgent
}

type urlGroup struct {
	ReceiverURLs []string
	AuthURLs     []string
	Type         string
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
	// Deprecated: Use Enabled instead
	Enable                           bool
	Enabled                          bool
	Encoding                         string
	ClaimDialect                     string
	ConvertDialect                   bool
	Header                           string
	SigningAlgorithm                 string
	EnableUserClaims                 bool
	GatewayGeneratorImpl             string
	ClaimsExtractorImpl              string
	TokenTTL                         int32
	PublicCertificatePath            string
	PrivateKeyPath                   string
	Keypair                          []KeyPair
	JwksRatelimitQuota               uint32
	JwksRatelimitTimeWindowInSeconds uint32
}

// KeyPair represents hthe rsa keypair used for signing JWTs
type KeyPair struct {
	PrivateKeyPath        string
	PublicCertificatePath string
	UseForSigning         bool
}

type claimMapping struct {
	RemoteClaim string
	LocalClaim  string
}

type cache struct {
	Enabled     bool
	MaximumSize int32
	ExpiryTime  int32
}

type analytics struct {
	Enabled  bool
	Adapter  analyticsAdapter
	Enforcer analyticsEnforcer
}

type tracing struct {
	Enabled          bool
	Type             string
	ConfigProperties map[string]string
}

type metrics struct {
	Enabled bool
	Type    string
}

type analyticsAdapter struct {
	BufferFlushInterval time.Duration
	BufferSizeBytes     uint32
	GRPCRequestTimeout  time.Duration
}

type analyticsEnforcer struct {
	// TODO: (VirajSalaka) convert it to map[string]{}interface
	ConfigProperties map[string]string
	LogReceiver      authService
}

type authHeader struct {
	EnableOutboundAuthHeader bool
	AuthorizationHeader      string
	TestConsoleHeaderName    string
}

type jwtIssuer struct {
	Enabled               bool
	Issuer                string
	Encoding              string
	ClaimDialect          string
	SigningAlgorithm      string
	PublicCertificatePath string
	PrivateKeyPath        string
	ValidityPeriod        int32
	JwtUser               []JwtUser
}

// JwtUser represents allowed users to generate JWT tokens
type JwtUser struct {
	Username string
	Password string
}

// APICtlUser represents registered APICtl Users
type APICtlUser struct {
	Username string
	Password string
}

// ControlPlane struct contains configurations related to the API Manager
type controlPlane struct {
	Enabled    bool
	ServiceURL string
	// Deprecated: Use ServiceURL instead.
	ServiceURLDeprecated       string `toml:"serviceUrl"`
	Username                   string
	Password                   string
	OrganizationID             string
	SyncApisOnStartUp          bool
	SendRevisionUpdate         bool
	EnvironmentLabels          []string
	DynamicEnvironments        dynamicEnvironments
	RetryInterval              time.Duration
	SkipSSLVerification        bool
	BrokerConnectionParameters brokerConnectionParameters
	HTTPClient                 httpClient
	RequestWorkerPool          requestWorkerPool
	InitialFetch               initialFetch
}

type dynamicEnvironments struct {
	Enabled                  bool
	DataPlaneID              string
	GatewayAccessibilityType string
}

type requestWorkerPool struct {
	PoolSize              int
	QueueSizePerPool      int
	PauseTimeAfterFailure time.Duration
}

type initialFetch struct {
	ChunkSize int
}

type globalAdapter struct {
	Enabled    bool
	ServiceURL string
	// Deprecated: Use ServiceURL instead.
	ServiceURLDeprecated string `toml:"serviceUrl"`
	LocalLabel           string
	// Deprecated: Use OverrideHostName instead.
	OverwriteHostName string
	OverrideHostName  string
	RetryInterval     time.Duration
}

type brokerConnectionParameters struct {
	EventListeningEndpoints []string
	ReconnectInterval       time.Duration
	ReconnectRetryCount     int
}

// Configuration for Enforcer admin rest api
type restServer struct {
	// Deprecated: Use Enabled Instead
	Enable  bool
	Enabled bool
}

// Enforcer admin credentials
type management struct {
	Username string
	Password string
}

type filter struct {
	ClassName        string
	Position         int32
	ConfigProperties map[string]string
}

type httpClient struct {
	RequestTimeOut time.Duration
}
