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

package config

// Configuration object which is populated with default values.
var defaultConfig = &Config{
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
			Enabled:            false,
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
			KeyPath:  "/home/wso2/security/keystore/mg.key",
			CertPath: "/home/wso2/security/keystore/mg.pem",
		},
		Truststore: truststore{
			Location: "/home/wso2/security/truststore",
		},
		ArtifactsDirectory:    "/home/wso2/artifacts",
		SoapErrorInXMLEnabled: false,
		SourceControl: sourceControl{
			Enabled:            false,
			PollInterval:       30,
			RetryInterval:      5,
			MaxRetryCount:      20,
			ArtifactsDirectory: "/home/wso2/git-artifacts",
		},
	},
	Envoy: envoy{
		ListenerHost:                     "0.0.0.0",
		ListenerPort:                     9090,
		SecuredListenerHost:              "0.0.0.0",
		SecuredListenerPort:              9095,
		ListenerCodecType:                "AUTO",
		ClusterTimeoutInSeconds:          20,
		EnforcerResponseTimeoutInSeconds: 20,
		UseRemoteAddress:                 false,
		KeyStore: keystore{
			KeyPath:  "/home/wso2/security/keystore/mg.key",
			CertPath: "/home/wso2/security/keystore/mg.pem",
		},
		SystemHost: "localhost",
		Cors: globalCors{
			Enabled:      true,
			AllowOrigins: []string{"*"},
			AllowMethods: []string{"GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS"},
			AllowHeaders: []string{"authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction", "apikey",
				"testKey", "Internal-Key"},
			AllowCredentials: false,
			ExposeHeaders:    []string{},
		},
		Upstream: envoyUpstream{
			TLS: upstreamTLS{
				MinimumProtocolVersion: "TLS1_1",
				MaximumProtocolVersion: "TLS1_2",
				Ciphers: "ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-RSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES128-SHA, ECDHE-RSA-AES128-SHA, " +
					"AES128-GCM-SHA256, AES128-SHA, ECDHE-ECDSA-AES256-GCM-SHA384, ECDHE-RSA-AES256-GCM-SHA384, " +
					"ECDHE-ECDSA-AES256-SHA, ECDHE-RSA-AES256-SHA, AES256-GCM-SHA384, AES256-SHA",
				TrustedCertPath:        "/etc/ssl/certs/ca-certificates.crt",
				VerifyHostName:         true,
				DisableSslVerification: false,
			},
			Timeouts: upstreamTimeout{
				MaxRouteTimeoutInSeconds:  60,
				RouteTimeoutInSeconds:     60,
				RouteIdleTimeoutInSeconds: 300,
			},
			Health: upstreamHealth{
				Timeout:            1,
				Interval:           10,
				UnhealthyThreshold: 2,
				HealthyThreshold:   2,
			},
			Retry: upstreamRetry{
				MaxRetryCount:        5,
				BaseIntervalInMillis: 25,
				StatusCodes:          []uint32{504},
			},
			DNS: upstreamDNS{
				DNSRefreshRate: 5000,
				RespectDNSTtl:  false,
			},
			HTTP2: upstreamHTTP2Options{
				HpackTableSize:       4096,
				MaxConcurrentStreams: 2147483647,
			},
		},
		Downstream: envoyDownstream{
			TLS: downstreamTLS{
				TrustedCertPath: "/etc/ssl/certs/ca-certificates.crt",
				MTLSAPIsEnabled: false,
			},
		},
		Connection: connection{
			Timeouts: connectionTimeouts{
				RequestTimeoutInSeconds:        0,
				RequestHeadersTimeoutInSeconds: 0,
				StreamIdleTimeoutInSeconds:     300,
				IdleTimeoutInSeconds:           3600,
			},
		},
		PayloadPassingToEnforcer: payloadPassingToEnforcer{
			PassRequestPayload:  false,
			MaxRequestBytes:     102400,
			AllowPartialMessage: false,
			PackAsBytes:         false,
		},
		Filters: filters{
			Compression: compression{
				Enabled: true,
				Library: "gzip",
				RequestDirection: requestDirection{
					Enabled:              false,
					MinimumContentLength: 30,
					ContentType:          []string{"application/javascript", "application/json", "application/xhtml+xml", "image/svg+xml", "text/css", "text/html", "text/plain", "text/xml"},
				},
				ResponseDirection: responseDirection{
					Enabled:              true,
					MinimumContentLength: 30,
					ContentType:          []string{"application/javascript", "application/json", "application/xhtml+xml", "image/svg+xml", "text/css", "text/html", "text/plain", "text/xml"},
					EnableForEtagHeader:  true,
				},
				LibraryProperties: map[string]interface{}{
					"memoryLevel":         3,
					"windowBits":          12,
					"compressionLevel":    9,
					"compressionStrategy": "defaultStrategy",
					"chunkSize":           4096,
				},
			},
		},
	},
	Enforcer: enforcer{
		Management: management{
			Username: "admin",
			Password: "admin",
		},
		RestServer: restServer{
			Enabled: true,
			Enable:  true,
		},
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
					Name:                 "APIM Publisher",
					Issuer:               "https://localhost:9443/publisher",
					ValidateSubscription: true,
					CertificateAlias:     "publisher_certificate_alias",
					CertificateFilePath:  "/home/wso2/security/truststore/wso2carbon.pem",
				},
				{
					Name:                 "APIM APIkey",
					Issuer:               "",
					ValidateSubscription: true,
					CertificateAlias:     "apikey_certificate_alias",
					CertificateFilePath:  "/home/wso2/security/truststore/wso2carbon.pem",
				},
			},
			AuthHeader: authHeader{
				EnableOutboundAuthHeader: false,
				AuthorizationHeader:      "authorization",
				TestConsoleHeaderName:    "Internal-Key",
			},
			MutualSSL: mutualSSL{
				CertificateHeader:               "X-WSO2-CLIENT-CERTIFICATE",
				EnableClientValidation:          true,
				ClientCertificateEncode:         false,
				EnableOutboundCertificateHeader: false,
			},
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
			Enabled:               false,
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
			TokenTTL:              3600,
		},
		Cache: cache{
			Enabled:     true,
			MaximumSize: 10000,
			ExpiryTime:  15,
		},
		Metrics: metrics{
			Enabled: false,
			Type:    "azure",
		},
		Throttling: throttlingConfig{
			EnableGlobalEventPublishing:        false,
			EnableHeaderConditions:             false,
			EnableQueryParamConditions:         false,
			EnableJwtClaimConditions:           false,
			JmsConnectionInitialContextFactory: "org.wso2.andes.jndi.PropertiesFileInitialContextFactory",
			JmsConnectionProviderURL:           "amqp://admin:$env{tm_admin_pwd}@carbon/carbon?brokerlist='tcp://apim:5672'",
			JmsConnectionProviderURLDeprecated: UnassignedAsDeprecated,
			Publisher: binaryPublisher{
				Username: "admin",
				Password: "$env{tm_admin_pwd}",
				URLGroup: []urlGroup{
					{
						ReceiverURLs: []string{"tcp://apim:9611"},
						AuthURLs:     []string{"ssl://apim:9711"},
					},
				},
				URLGroupDeprecated: []urlGroup{},
				Pool: publisherPool{
					MaxIdleDataPublishingAgents:        1000,
					InitIdleObjectDataPublishingAgents: 200,
					PublisherThreadPoolCoreSize:        200,
					PublisherThreadPoolMaximumSize:     1000,
					PublisherThreadPoolKeepAliveTime:   200,
				},
				Agent: binaryAgent{
					SslEnabledProtocols: "TLSv1.2",
					Ciphers: "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
						"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
						"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256," +
						"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA," +
						"TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
						"TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
						"TLS_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
						"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256," +
						"TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA," +
						"TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA," +
						"SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
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
			JwtUser: []JwtUser{
				{
					Username: "admin",
					Password: "$env{enforcer_admin_pwd}",
				},
			},
		},
	},
	ControlPlane: controlPlane{
		Enabled:              false,
		ServiceURL:           "https://apim:9443/",
		ServiceURLDeprecated: UnassignedAsDeprecated,
		Username:             "admin",
		Password:             "$env{cp_admin_pwd}",
		EnvironmentLabels:    []string{"Default"},
		RetryInterval:        5,
		SkipSSLVerification:  false,
		BrokerConnectionParameters: brokerConnectionParameters{
			EventListeningEndpoints: []string{"amqp://admin:$env{cp_admin_pwd}@apim:5672?retries='10'&connectdelay='30'"},
			ReconnectInterval:       5000, //in milli seconds
			ReconnectRetryCount:     60,
		},
		SendRevisionUpdate: false,
		HTTPClient: httpClient{
			RequestTimeOut: 30,
		},
		RequestWorkerPool: requestWorkerPool{
			PoolSize:              4,
			QueueSizePerPool:      1000,
			PauseTimeAfterFailure: 5,
		},
	},
	GlobalAdapter: globalAdapter{
		Enabled:              false,
		ServiceURL:           "global-adapter:18000",
		ServiceURLDeprecated: UnassignedAsDeprecated,
		OverwriteHostName:    UnassignedAsDeprecated,
		OverrideHostName:     "",
		LocalLabel:           "default",
		RetryInterval:        5,
	},
	Analytics: analytics{
		Enabled: false,
		Type:    "Default",
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
			LogReceiver: authService{
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
	Tracing: tracing{
		Enabled: false,
		Type:    "zipkin",
		ConfigProperties: map[string]string{
			"libraryName":            "CHOREO-CONNECT",
			"maximumTracesPerSecond": "2",
			"maxPathLength":          "256",
			"host":                   "jaeger",
			"port":                   "9411",
			"endpoint":               "/api/v2/spans",
		},
	},
}
