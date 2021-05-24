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

// Package config contains the implementation and data structures related to configurations and
// configuration (log and adapter config) parsing. If a new configuration is introduced to the adapter
// configuration file, the corresponding change needs to be added to the relevant data stucture as well.
package config

import (
	"fmt"
	"io/ioutil"
	"os"
	"reflect"
	"regexp"
	"strings"
	"sync"

	toml "github.com/pelletier/go-toml"
	logger "github.com/sirupsen/logrus"
)

var (
	onceConfigRead      sync.Once
	onceGetDefaultVhost sync.Once
	onceLogConfigRead   sync.Once
	onceGetMgwHome      sync.Once
	adapterConfig       *Config
	defaultVhost        map[string]string
	adapterLogConfig    *LogConfig
	mgwHome             string
	e                   error
)

// DefaultGatewayName represents the name of the default gateway
const DefaultGatewayName = "Default"

// DefaultGatewayVHost represents the default vhost of default gateway environment if it is not configured
const DefaultGatewayVHost = "localhost" // TODO (renuka): check this with pubuduG and raji: do we want this?
// for /testtoken and /health check, if user not configured default env, we have no vhost

const (
	// The environtmental variable which represents the path of the distribution in host machine.
	mgwHomeEnvVariable = "MGW_HOME"
	// RelativeConfigPath is the relative file path where the configuration file is.
	relativeConfigPath = "/conf/config.toml"
	// RelativeLogConfigPath is the relative file path where the log configuration file is.
	relativeLogConfigPath = "/conf/log_config.toml"
	// The prefix used when configs should be read from environment variables.
	envConfigPrefix = "$env"
)

//constants related to utility functions
const (
	tenantDomainSeparator = "@"
	superTenantDomain     = "carbon.super"
)

// ReadConfigs implements adapter configuration read operation. The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the configuration file location would be picked relative to the
// variable's value ("/conf/config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the populated configuration object.
func ReadConfigs() (*Config, error) {
	onceConfigRead.Do(func() {
		adapterConfig = GetDefaultAdapterConfig()
		_, err := os.Stat(GetMgwHome() + relativeConfigPath)
		if err != nil {
			logger.Fatal("Configuration file not found.", err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + relativeConfigPath)
		if readErr != nil {
			logger.Fatal("Error reading configurations. ", readErr)
			return
		}
		parseErr := toml.Unmarshal(content, adapterConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the configuration ", parseErr)
			return
		}
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Adapter)).Elem())
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.ControlPlane)).Elem())
		resolveConfigEnvValues(reflect.ValueOf(&(adapterConfig.Envoy)).Elem())
	})
	return adapterConfig, e
}

// GetDefaultAdapterConfig returns the adapter configuration which is popluated with default values.
func GetDefaultAdapterConfig() *Config {
	adapterConfig := Config{
		Adapter: adapter{
			Server: server{
				Enabled: true,
				Host:    "0.0.0.0",
				Port:    "9843",
				Users: []APICtlUser{
					{
						Username: "admin",
						Password: "$env{adapter_admin_pwd}",
					},
				},
				TokenTTL:            "1h",
				TokenPrivateKeyPath: "/home/wso2/security/keystore/mg.key",
			},
			VhostMapping: []vhostMapping{
				{
					Environment: "Default",
					Vhost:       "localhost",
				},
			},
			Consul: consul{
				Enable:             false,
				URL:                "https://169.254.1.1:8501",
				PollInterval:       5,
				ACLToken:           "d3a2a719-4221-8c65-5212-58d4727427ac",
				MgwServiceName:     "wso2",
				ServiceMeshEnabled: false,
				CaCertFile:         "/home/wso2/security/truststore/consul/consul-agent-ca.pem",
				CertFile:           "/home/wso2/security/truststore/consul/local-dc-client-consul-0.pem",
				KeyFile:            "/home/wso2/security/truststore/consul/local-dc-client-consul-0-key.pem",
			},
			Keystore: keystore{
				PrivateKeyLocation: "/home/wso2/security/keystore/mg.key",
				PublicKeyLocation:  "/home/wso2/security/keystore/mg.pem",
			},
			Truststore: truststore{
				Location: "/home/wso2/security/truststore",
			},
		},
		Envoy: envoy{
			ListenerHost:                     "0.0.0.0",
			ListenerPort:                     9090,
			SecuredListenerHost:              "0.0.0.0",
			SecuredListenerPort:              9095,
			ClusterTimeoutInSeconds:          20,
			EnforcerResponseTimeoutInSeconds: 20,
			KeyStore: keystore{
				PrivateKeyLocation: "/home/wso2/security/keystore/mg.key",
				PublicKeyLocation:  "/home/wso2/security/keystore/mg.pem",
			},
			SystemHost: "localhost",
			Cors: globalCors{
				Enabled:          true,
				AllowOrigins:     []string{"*"},
				AllowMethods:     []string{"GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"},
				AllowHeaders:     []string{"authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction", "apikey", "testKey", "Internal-Key"},
				AllowCredentials: false,
				ExposeHeaders:    []string{},
			},
			Upstream: envoyUpstream{
				TLS: upstreamTLS{
					MinVersion:             "TLS1_1",
					MaxVersion:             "TLS1_2",
					Ciphers:                "ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-RSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES128-SHA, ECDHE-RSA-AES128-SHA, AES128-GCM-SHA256, AES128-SHA, ECDHE-ECDSA-AES256-GCM-SHA384, ECDHE-RSA-AES256-GCM-SHA384, ECDHE-ECDSA-AES256-SHA, ECDHE-RSA-AES256-SHA, AES256-GCM-SHA384, AES256-SHA",
					CACrtPath:              "/etc/ssl/certs/ca-certificates.crt",
					VerifyHostName:         true,
					DisableSSLVerification: false,
				},
			},
		},
		Enforcer: enforcer{
			Security: security{
				TokenService: []tokenService{
					{
						Name:                 "Resident Key Manager",
						Issuer:               "https://localhost:9443/oauth2/token",
						CertificateAlias:     "wso2carbon",
						JwksURL:              "",
						ValidateSubscription: false,
						ConsumerKeyClaim:     "azp",
						CertificateFilePath:  "/home/wso2/security/truststore/wso2carbon.pem",
					},
					{
						Name:                 "MGW",
						Issuer:               "https://localhost:9095/testkey",
						CertificateAlias:     "mgw",
						JwksURL:              "",
						ValidateSubscription: false,
						ConsumerKeyClaim:     "",
						CertificateFilePath:  "/home/wso2/security/truststore/mg.pem",
					},
					{
						Name:                "APIM Publisher",
						Issuer:              "https://localhost:9443/publisher",
						CertificateAlias:    "",
						CertificateFilePath: "/home/wso2/security/truststore/wso2carbon.pem",
					},
				},
				AuthHeader: authHeader{
					EnableOutboundAuthHeader: false,
					AuthorizationHeader:      "authorization",
					TestConsoleHeaderName:    "internal-key",
				},
			},
			ApimCredentials: apimCredentials{
				Username: "admin",
				Password: "$env{apim_admin_pwd}",
			},
			AuthService: authService{
				Port:           8081,
				MaxMessageSize: 1000000000,
				MaxHeaderLimit: 8192,
				KeepAliveTime:  600,
				ThreadPool: threadPool{
					CoreSize:      400,
					MaxSize:       500,
					KeepAliveTime: 600,
					QueueSize:     1000,
				},
			},
			JwtGenerator: jwtGenerator{
				Enable:                false,
				Encoding:              "base64",
				ClaimDialect:          "http://wso2.org/claims",
				ConvertDialect:        false,
				Header:                "X-JWT-Assertion",
				SigningAlgorithm:      "SHA256withRSA",
				EnableUserClaims:      false,
				GatewayGeneratorImpl:  "org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl",
				ClaimsExtractorImpl:   "org.wso2.carbon.apimgt.impl.token.ExtendedDefaultClaimsRetriever",
				PublicCertificatePath: "/home/wso2/security/truststore/mg.pem",
				PrivateKeyPath:        "/home/wso2/security/keystore/mg.key",
			},
			Cache: cache{
				Enabled:     true,
				MaximumSize: 10000,
				ExpiryTime:  15,
			},
			Throttling: throttlingConfig{
				EnableGlobalEventPublishing:        false,
				EnableHeaderConditions:             false,
				EnableQueryParamConditions:         false,
				EnableJwtClaimConditions:           false,
				JmsConnectionInitialContextFactory: "org.wso2.andes.jndi.PropertiesFileInitialContextFactory",
				JmsConnectionProviderURL:           "amqp://admin:$env{tm_admin_pwd}@carbon/carbon?brokerlist='tcp://apim:5672'",
				Publisher: binaryPublisher{
					Username: "admin",
					Password: "$env{tm_admin_pwd}",
					URLGroup: []urlGroup{
						{
							ReceiverURLs: []string{"tcp://apim:9611"},
							AuthURLs:     []string{"ssl://apim:9711"},
						},
					},
					Pool: publisherPool{
						MaxIdleDataPublishingAgents:        1000,
						InitIdleObjectDataPublishingAgents: 200,
						PublisherThreadPoolCoreSize:        200,
						PublisherThreadPoolMaximumSize:     1000,
						PublisherThreadPoolKeepAliveTime:   200,
					},
					Agent: binaryAgent{
						SslEnabledProtocols:        "TLSv1.2",
						Ciphers:                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256, TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA, TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA, TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256  ,TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256  ,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA, TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA, SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
						QueueSize:                  32768,
						BatchSize:                  200,
						CorePoolSize:               1,
						SocketTimeoutMS:            30000,
						MaxPoolSize:                1,
						KeepAliveTimeInPool:        20,
						ReconnectionInterval:       30,
						MaxTransportPoolSize:       250,
						MaxIdleConnections:         250,
						EvictionTimePeriod:         5500,
						MinIdleTimeInPool:          5000,
						SecureMaxTransportPoolSize: 250,
						SecureMaxIdleConnections:   250,
						SecureEvictionTimePeriod:   5500,
						SecureMinIdleTimeInPool:    5000,
					},
				},
			},
			JwtIssuer: jwtIssuer{
				Enabled:               true,
				Issuer:                "https://localhost:9095/testkey",
				Encoding:              "base64",
				ClaimDialect:          "",
				SigningAlgorithm:      "SHA256withRSA",
				PublicCertificatePath: "/home/wso2/security/truststore/mg.pem",
				PrivateKeyPath:        "/home/wso2/security/keystore/mg.key",
				ValidityPeriod:        3600,
				JwtUsers: []JwtUser{
					{
						Username: "admin",
						Password: "$env{enforcer_admin_pwd}",
					},
				},
			},
		},
		ControlPlane: controlPlane{
			Enabled:             false,
			ServiceURL:          "https://apim:9443/",
			Username:            "admin",
			Password:            "$env{cp_admin_pwd}",
			EnvironmentLabels:   []string{"Default"},
			RetryInterval:       5,
			SkipSSLVerification: true,
			JmsConnectionParameters: jmsConnectionParameters{
				EventListeningEndpoints: []string{"amqp://admin:$env{cp_admin_pwd}@apim:5672?retries='10'&connectdelay='30'"},
			},
		},
		Analytics: analytics{
			Enabled: false,
			Adapter: analyticsAdapter{
				BufferFlushInterval: 1000000000,
				BufferSizeBytes:     16384,
				GRPCRequestTimeout:  20000000000,
			},
			Enforcer: analyticsEnforcer{
				ConfigProperties: map[string]string{
					"authURL":   "$env{analytics_authURL}",
					"authToken": "$env{analytics_authToken}",
				},
				EnforcerLogReceiver: authService{
					Port:           18090,
					MaxMessageSize: 1000000000,
					MaxHeaderLimit: 8192,
					KeepAliveTime:  600,
					ThreadPool: threadPool{
						CoreSize:      10,
						MaxSize:       100,
						KeepAliveTime: 600,
						QueueSize:     1000,
					},
				},
			},
		},
	}
	return &adapterConfig
}

// GetDefaultVhost returns the default vhost of given environment read from Adapter
// configurations. Store the configuration in a map, so do not want to loop through
// the config value Config.Adapter.VhostMapping
func GetDefaultVhost(environment string) (string, bool, error) {
	var err error
	onceGetDefaultVhost.Do(func() {
		defaultVhost = make(map[string]string)
		configs, errConf := ReadConfigs()
		if errConf != nil {
			err = errConf
			return
		}
		for _, gateway := range configs.Adapter.VhostMapping {
			defaultVhost[gateway.Environment] = gateway.Vhost
		}
	})
	vhost, ok := defaultVhost[environment]
	if !ok && environment == DefaultGatewayName {
		return DefaultGatewayVHost, true, nil
	}
	return vhost, ok, err
}

// resolveConfigEnvValues looks for the string type config values which should be read from environment variables
// and replace the respective config values from environment variable.
func resolveConfigEnvValues(v reflect.Value) {
	s := v
	for fieldNum := 0; fieldNum < s.NumField(); fieldNum++ {
		field := s.Field(fieldNum)
		if field.Kind() == reflect.String && strings.Contains(fmt.Sprint(field.Interface()), envConfigPrefix) {
			field.SetString(resolveEnvValue(fmt.Sprint(field.Interface())))
		}
		if reflect.TypeOf(field.Interface()).Kind() == reflect.Slice {
			for index := 0; index < field.Len(); index++ {
				if field.Index(index).Kind() == reflect.Struct {
					resolveConfigEnvValues(field.Index(index).Addr().Elem())
				} else if field.Index(index).Kind() == reflect.String && strings.Contains(field.Index(index).String(), envConfigPrefix) {
					field.Index(index).SetString(resolveEnvValue(field.Index(index).String()))
				}
			}
		}
		if field.Kind() == reflect.Struct {
			resolveConfigEnvValues(field.Addr().Elem())
		}
	}
}

func resolveEnvValue(value string) string {
	re := regexp.MustCompile(`(?s)\{(.*)}`) // regex to get everything in between curly brackets
	m := re.FindStringSubmatch(value)
	if len(m) > 1 {
		envValue, exists := os.LookupEnv(m[1])
		if exists {
			return strings.ReplaceAll(re.ReplaceAllString(value, envValue), envConfigPrefix, "")
		}
	}
	return value
}

// ReadLogConfigs implements adapter/proxy log-configuration read operation.The read operation will happen only once, hence
// the consistancy is ensured.
//
// If the "MGW_HOME" variable is set, the log configuration file location would be picked relative to the
// variable's value ("/conf/log_config.toml"). otherwise, the "MGW_HOME" variable would be set to the directory
// from where the executable is called from.
//
// Returns the log configuration object mapped from the configuration file during the startup.
func ReadLogConfigs() (*LogConfig, error) {
	onceLogConfigRead.Do(func() {
		adapterLogConfig = new(LogConfig)
		_, err := os.Stat(GetMgwHome() + relativeLogConfigPath)
		if err != nil {
			logger.Fatal("Log configuration file not found.", err)
			panic(err)
		}
		content, readErr := ioutil.ReadFile(mgwHome + relativeLogConfigPath)
		if readErr != nil {
			logger.Fatal("Error reading log configurations. ", readErr)
			panic(err)
		}
		parseErr := toml.Unmarshal(content, adapterLogConfig)
		if parseErr != nil {
			logger.Fatal("Error parsing the log configuration ", parseErr)
			panic(parseErr)
		}

	})
	return adapterLogConfig, e
}

// ClearLogConfigInstance removes the existing configuration.
// Then the log configuration can be re-initialized.
func ClearLogConfigInstance() {
	onceLogConfigRead = sync.Once{}
}

// GetMgwHome reads the MGW_HOME environmental variable and returns the value.
// This represent the directory where the distribution is located.
// If the env variable is not present, the directory from which the executable is triggered will be assigned.
func GetMgwHome() string {
	onceGetMgwHome.Do(func() {
		mgwHome = os.Getenv(mgwHomeEnvVariable)
		if len(strings.TrimSpace(mgwHome)) == 0 {
			mgwHome, _ = os.Getwd()
		}
	})
	return mgwHome
}

// GetControlPlaneConnectedTenantDomain returns the tenant domain of the user used to authenticate with event hub.
func GetControlPlaneConnectedTenantDomain() string {
	// Read configurations to get the control plane authenticated user
	conf, _ := ReadConfigs()

	// Populate data from the config
	cpTenantAdminUser := conf.ControlPlane.Username
	tenantDomain := strings.Split(cpTenantAdminUser, tenantDomainSeparator)
	if len(tenantDomain) > 1 {
		return tenantDomain[len(tenantDomain)-1]
	}
	return superTenantDomain
}
