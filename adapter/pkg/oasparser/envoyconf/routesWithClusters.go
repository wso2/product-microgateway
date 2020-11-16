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

package envoyconf

import (
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	tlsv3 "github.com/envoyproxy/go-control-plane/envoy/extensions/transport_sockets/tls/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/any"

	"github.com/wso2/micro-gw/config"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/model"

	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
)

// CreateRoutesWithClusters creates envoy routes along with clusters and endpoint instances.
// This creates routes for all the swagger resources and link to clusters.
// Create clusters for api level production and sandbox endpoints.
// If a resource has resource level endpoint, it create another cluster and
// link it. If resources doesn't has resource level endpoints, those clusters are linked
// to the api level clusters.
//
// First set of routes, clusters, addresses represents the production endpoints related
// configurations. Next set represents the sandbox endpoints related configurations.
func CreateRoutesWithClusters(mgwSwagger model.MgwSwagger) (routesP []*routev3.Route,
	clustersP []*clusterv3.Cluster, addressesP []*corev3.Address,
	routesS []*routev3.Route, clustersS []*clusterv3.Cluster, addressesS []*corev3.Address) {
	var (
		routesProd              []*routev3.Route
		clustersProd            []*clusterv3.Cluster
		endpointProd            []model.Endpoint
		apiLevelEndpointProd    []model.Endpoint
		apilevelClusterProd     *clusterv3.Cluster
		apiLevelClusterNameProd string
		endpointsProd           []*corev3.Address

		routesSand              []*routev3.Route
		clustersSand            []*clusterv3.Cluster
		endpointSand            []model.Endpoint
		apiLevelEndpointSand    []model.Endpoint
		apilevelClusterSand     *clusterv3.Cluster
		apiLevelClusterNameSand string
		endpointsSand           []*corev3.Address
	)
	//check API level sandbox endpoints availble
	if len(mgwSwagger.GetSandEndpoints()) > 0 {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		apilevelAddressSand := createAddress(apiLevelEndpointSand[0].Host, apiLevelEndpointSand[0].Port)
		apiLevelClusterNameSand = strings.TrimSpace("clusterSand_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) +
			mgwSwagger.GetVersion())
		apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameSand, apiLevelEndpointSand[0].URLType)
		clustersSand = append(clustersSand, apilevelClusterSand)
		endpointsSand = append(endpointsSand, apilevelAddressSand)
	}

	//check API level production endpoints available
	if len(mgwSwagger.GetProdEndpoints()) > 0 {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].Host, apiLevelEndpointProd[0].Port)
		apiLevelClusterNameProd = strings.TrimSpace("clusterProd_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) +
			mgwSwagger.GetVersion())
		apilevelClusterProd = createCluster(apilevelAddressP, apiLevelClusterNameProd, apiLevelEndpointProd[0].URLType)
		clustersProd = append(clustersProd, apilevelClusterProd)
		endpointsProd = append(endpointsProd, apilevelAddressP)

	} else {
		logger.LoggerOasparser.Warn("API level Producton endpoints are not defined")
	}
	for _, resource := range mgwSwagger.GetResources() {
		apiTitle := mgwSwagger.GetTitle()
		apiVersion := mgwSwagger.GetVersion()
		apiBasePath := mgwSwagger.GetXWso2Basepath()

		//resource level check sandbox endpoints
		if len(resource.GetSandEndpoints()) > 0 {
			endpointSand = resource.GetSandEndpoints()
			addressSand := createAddress(endpointSand[0].Host, endpointSand[0].Port)
			//TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameSand := strings.TrimSpace(apiLevelClusterNameSand + "_" + strings.Replace(resource.GetID(), " ", "", -1) +
				"0")
			clusterSand := createCluster(addressSand, clusterNameSand, endpointSand[0].URLType)
			clustersSand = append(clustersSand, clusterSand)
			clusterRefSand := clusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(apiTitle, apiBasePath, apiVersion, endpointSand[0], resource, clusterRefSand)
			routesSand = append(routesSand, routeS)
			endpointsSand = append(endpointsSand, addressSand)

			//API level check
		} else if len(mgwSwagger.GetSandEndpoints()) > 0 {
			endpointSand = apiLevelEndpointSand
			clusterRefSand := apilevelClusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(apiTitle, apiBasePath, apiVersion, endpointSand[0], resource, clusterRefSand)
			routesSand = append(routesSand, routeS)

		}

		//resource level check production endpoints
		if len(resource.GetProdEndpoints()) > 0 {
			endpointProd = resource.GetProdEndpoints()
			addressProd := createAddress(endpointProd[0].Host, endpointProd[0].Port)
			//TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameProd := strings.TrimSpace(apiLevelClusterNameProd + "_" + strings.Replace(resource.GetID(), " ", "", -1) +
				"0")
			clusterProd := createCluster(addressProd, clusterNameProd, endpointProd[0].URLType)
			clustersProd = append(clustersProd, clusterProd)
			clusterRefProd := clusterProd.GetName()

			//production endpoints
			routeP := createRoute(apiTitle, apiBasePath, apiVersion, endpointProd[0], resource, clusterRefProd)
			routesProd = append(routesProd, routeP)
			endpointsProd = append(endpointsProd, addressProd)

			//API level check
		} else if len(mgwSwagger.GetProdEndpoints()) > 0 {
			endpointProd = apiLevelEndpointProd
			clusterRefProd := apilevelClusterProd.GetName()

			//production endpoints
			routeP := createRoute(apiTitle, apiBasePath, apiVersion, endpointProd[0], resource, clusterRefProd)
			routesProd = append(routesProd, routeP)

		} else {
			logger.LoggerOasparser.Fatalf("Producton endpoints are not defined")
		}
	}
	return routesProd, clustersProd, endpointsProd, routesSand, clustersSand, endpointsSand
}

// createCluster creates cluster configuration. AddressConfiguration, cluster name and
// urlType (http or https) is required to be provided.
func createCluster(address *corev3.Address, clusterName string, urlType string) *clusterv3.Cluster {
	logger.LoggerOasparser.Debug("creating a cluster....")
	conf, errReadConfig := config.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}

	cluster := clusterv3.Cluster{
		Name:                 clusterName,
		ConnectTimeout:       ptypes.DurationProto(conf.Envoy.ClusterTimeoutInSeconds * time.Second),
		ClusterDiscoveryType: &clusterv3.Cluster_Type{Type: clusterv3.Cluster_STRICT_DNS},
		DnsLookupFamily:      clusterv3.Cluster_V4_ONLY,
		LbPolicy:             clusterv3.Cluster_ROUND_ROBIN,
		LoadAssignment: &endpointv3.ClusterLoadAssignment{
			ClusterName: clusterName,
			Endpoints: []*endpointv3.LocalityLbEndpoints{
				{
					LbEndpoints: []*endpointv3.LbEndpoint{
						{
							HostIdentifier: &endpointv3.LbEndpoint_Endpoint{
								Endpoint: &endpointv3.Endpoint{
									Address: address,
								},
							},
						},
					},
				},
			},
		},
	}
	if strings.HasPrefix(urlType, "https") {
		upstreamtlsContext := &tlsv3.UpstreamTlsContext{
			CommonTlsContext: &tlsv3.CommonTlsContext{
				ValidationContextType: &tlsv3.CommonTlsContext_ValidationContext{
					ValidationContext: &tlsv3.CertificateValidationContext{
						TrustedCa: &corev3.DataSource{
							Specifier: &corev3.DataSource_Filename{
								Filename: "/etc/ssl/certs/ca-certificates.crt",
							},
						},
					},
				},
			},
		}
		marshalledTLSContext, err := ptypes.MarshalAny(upstreamtlsContext)
		if err != nil {
			logger.LoggerOasparser.Error("Internal Error while marshalling the upstream TLS Context.")
		} else {
			upstreamTransportSocket := &corev3.TransportSocket{
				Name: "envoy.transport_sockets.tls",
				ConfigType: &corev3.TransportSocket_TypedConfig{
					TypedConfig: marshalledTLSContext,
				},
			}
			cluster.TransportSocket = upstreamTransportSocket
		}
	}
	return &cluster
}

func createRoute(title string, xWso2Basepath string, version string, endpoint model.Endpoint,
	resource model.Resource, clusterName string) *routev3.Route {
	logger.LoggerOasparser.Debug("creating a route....")
	var (
		router       routev3.Route
		action       *routev3.Route_Route
		match        *routev3.RouteMatch
		decorator    *routev3.Decorator
		resourcePath string
	)
	headerMatcherArray := routev3.HeaderMatcher{
		Name: ":method",
		HeaderMatchSpecifier: &routev3.HeaderMatcher_SafeRegexMatch{
			SafeRegexMatch: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: "^(" + strings.Join(resource.GetMethod(), "|") + ")$",
			},
		},
	}
	resourcePath = resource.GetPath()
	routePath := generateRoutePaths(xWso2Basepath, endpoint.Basepath, resourcePath)

	match = &routev3.RouteMatch{
		PathSpecifier: &routev3.RouteMatch_SafeRegex{
			SafeRegex: &envoy_type_matcherv3.RegexMatcher{
				EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
					GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
						MaxProgramSize: nil,
					},
				},
				Regex: routePath,
			},
		},
		Headers: []*routev3.HeaderMatcher{&headerMatcherArray},
	}

	hostRewriteSpecifier := &routev3.RouteAction_HostRewriteLiteral{
		HostRewriteLiteral: endpoint.Host,
	}

	clusterSpecifier := &routev3.RouteAction_Cluster{
		Cluster: clusterName,
	}
	decorator = &routev3.Decorator{
		Operation: resourcePath,
	}
	var contextExtensions = make(map[string]string)
	contextExtensions["path"] = resourcePath
	if xWso2Basepath != "" {
		contextExtensions["basePath"] = xWso2Basepath
	} else {
		contextExtensions["basePath"] = endpoint.Basepath
	}
	contextExtensions["method"] = strings.Join(resource.GetMethod(), " ")
	contextExtensions["version"] = version
	contextExtensions["name"] = title

	perFilterConfig := extAuthService.ExtAuthzPerRoute{
		Override: &extAuthService.ExtAuthzPerRoute_CheckSettings{
			CheckSettings: &extAuthService.CheckSettings{
				ContextExtensions: contextExtensions,
			},
		},
	}

	b := proto.NewBuffer(nil)
	b.SetDeterministic(true)
	_ = b.Marshal(&perFilterConfig)
	filter := &any.Any{
		TypeUrl: "type.googleapis.com/envoy.extensions.filters.http.ext_authz.v3.ExtAuthzPerRoute",
		Value:   b.Bytes(),
	}

	if xWso2Basepath != "" {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
				//TODO: (VirajSalaka) Provide prefix rewrite since it is simple
				RegexRewrite: &envoy_type_matcherv3.RegexMatchAndSubstitute{
					Pattern: &envoy_type_matcherv3.RegexMatcher{
						EngineType: &envoy_type_matcherv3.RegexMatcher_GoogleRe2{
							GoogleRe2: &envoy_type_matcherv3.RegexMatcher_GoogleRE2{
								MaxProgramSize: nil,
							},
						},
						Regex: xWso2Basepath,
					},
					Substitution: endpoint.Basepath,
				},
				ClusterSpecifier: clusterSpecifier,
			},
		}
	} else {
		action = &routev3.Route_Route{
			Route: &routev3.RouteAction{
				HostRewriteSpecifier: hostRewriteSpecifier,
				ClusterSpecifier:     clusterSpecifier,
			},
		}
	}
	logger.LoggerOasparser.Debug("adding route ", resourcePath)

	router = routev3.Route{
		Name:      xWso2Basepath, //Categorize routes with same base path
		Match:     match,
		Action:    action,
		Metadata:  nil,
		Decorator: decorator,
		TypedPerFilterConfig: map[string]*any.Any{
			wellknown.HTTPExternalAuthorization: filter,
		},
	}
	return &router
}

/**
 * Generates route paths for the api resources.
 *
 * @param xWso2Basepath   Xwso2 basepath
 * @param basePath  Default basepath
 * @param resourcePath  Resource path
 * @return string  new route path
 */
func generateRoutePaths(xWso2Basepath, basePath, resourcePath string) string {
	prefix := ""
	newPath := ""
	if strings.TrimSpace(xWso2Basepath) != "" {
		prefix = basepathConsistent(xWso2Basepath)

	} else {
		prefix = basepathConsistent(basePath)
		//TODO: (VirajSalaka) Decide if it is possible to proceed without both basepath options
	}
	fullpath := prefix + resourcePath
	newPath = generateRegex(fullpath)
	return newPath
}

func basepathConsistent(basePath string) string {
	modifiedBasePath := basePath
	if !strings.HasPrefix(basePath, "/") {
		modifiedBasePath = "/" + modifiedBasePath
	}
	modifiedBasePath = strings.TrimSuffix(modifiedBasePath, "/")
	return modifiedBasePath
}

//  generateRegex generates regex for the resources which have path paramaters
// such that the envoy configuration can use it as a route.
// If path has path parameters ({id}), append a regex pattern (pathParaRegex).
// To avoid query parameter issues, add a regex pattern ( endRegex) for end of all routes.
// It takes the path value as an input and then returns the regex value.
//TODO: (VirajSalaka) Improve regex specifically for strings, integers etc.
func generateRegex(fullpath string) string {
	pathParaRegex := "([^/]+)"
	endRegex := "(\\?([^/]+))?"
	newPath := ""

	if strings.Contains(fullpath, "{") || strings.Contains(fullpath, "}") {
		res1 := strings.Split(fullpath, "/")

		for i, p := range res1 {
			if strings.Contains(p, "{") || strings.Contains(p, "}") {
				res1[i] = pathParaRegex
			}
		}
		newPath = "^" + strings.Join(res1[:], "/") + endRegex + "$"

	} else {
		newPath = "^" + fullpath + endRegex + "$"
	}
	return newPath
}
