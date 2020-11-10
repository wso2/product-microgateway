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
package envoyCodegen

import (
	clusterv3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	endpointv3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	extAuthService "github.com/envoyproxy/go-control-plane/envoy/config/filter/http/ext_authz/v2"
	routev3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	envoy_type_matcherv3 "github.com/envoyproxy/go-control-plane/envoy/type/matcher/v3"
	"github.com/envoyproxy/go-control-plane/pkg/wellknown"
	"github.com/golang/protobuf/ptypes/any"

	"github.com/wso2/micro-gw/configs"
	logger "github.com/wso2/micro-gw/loggers"
	"github.com/wso2/micro-gw/pkg/oasparser/models/apiDefinition"
	swag_operator "github.com/wso2/micro-gw/pkg/oasparser/swaggerOperator"

	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
)

/**
 * Create envoy routes along with clusters and endpoint instances.
 * This create routes for all the swagger resources and link to clusters.
 * Create clusters for api level production and sandbox endpoints.
 * If a resource has resource level endpoint, it create another cluster and
 * link it. If resources doesn't has resource level endpoints, those clusters are linked
 * to the api level clusters.
 *
 * @param mgwSwagger  mgwSwagger instance
 * @return []*v3route.Route  Production routes
 * @return []*v3.Cluster  Production clusters
 * @return []*core.Address  Production endpoints
 * @return []*v3route.Route  Sandbox routes
 * @return []*v3.Cluster  Sandbox clusters
 * @return []*core.Address  Sandbox endpoints
 */
func CreateRoutesWithClusters(mgwSwagger apiDefinition.MgwSwagger) ([]*routev3.Route, []*clusterv3.Cluster, []*corev3.Address, []*routev3.Route, []*clusterv3.Cluster, []*corev3.Address) {
	var (
		routesProd              []*routev3.Route
		clustersProd            []*clusterv3.Cluster
		endpointProd            []apiDefinition.Endpoint
		apiLevelEndpointProd    []apiDefinition.Endpoint
		apilevelClusterProd     clusterv3.Cluster
		apiLevelClusterNameProd string
		endpointsProd           []*corev3.Address

		routesSand              []*routev3.Route
		clustersSand            []*clusterv3.Cluster
		endpointSand            []apiDefinition.Endpoint
		apiLevelEndpointSand    []apiDefinition.Endpoint
		apilevelClusterSand     clusterv3.Cluster
		apiLevelClusterNameSand string
		endpointsSand           []*corev3.Address
	)
	//check API level sandbox endpoints availble
	if swag_operator.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
		apiLevelEndpointSand = mgwSwagger.GetSandEndpoints()
		apilevelAddressSand := createAddress(apiLevelEndpointSand[0].GetHost(), apiLevelEndpointSand[0].GetPort())
		apiLevelClusterNameSand = strings.TrimSpace("clusterSand_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) +
			mgwSwagger.GetVersion())
		apilevelClusterSand = createCluster(apilevelAddressSand, apiLevelClusterNameSand)
		clustersSand = append(clustersSand, &apilevelClusterSand)
		endpointsSand = append(endpointsSand, &apilevelAddressSand)
	}

	//check API level production endpoints available
	if swag_operator.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
		apiLevelEndpointProd = mgwSwagger.GetProdEndpoints()
		apilevelAddressP := createAddress(apiLevelEndpointProd[0].GetHost(), apiLevelEndpointProd[0].GetPort())
		apiLevelClusterNameProd = strings.TrimSpace("clusterProd_" + strings.Replace(mgwSwagger.GetTitle(), " ", "", -1) +
			mgwSwagger.GetVersion())
		apilevelClusterProd = createCluster(apilevelAddressP, apiLevelClusterNameProd)
		clustersProd = append(clustersProd, &apilevelClusterProd)
		endpointsProd = append(endpointsProd, &apilevelAddressP)

	} else {
		logger.LoggerOasparser.Warn("API level Producton endpoints are not defined")
	}
	for _, resource := range mgwSwagger.GetResources() {

		//resource level check sandbox endpoints
		if swag_operator.IsEndpointsAvailable(resource.GetSandEndpoints()) {
			endpointSand = resource.GetSandEndpoints()
			addressSand := createAddress(endpointSand[0].GetHost(), endpointSand[0].GetPort())
			//TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameSand := strings.TrimSpace(apiLevelClusterNameSand + "_" + strings.Replace(resource.GetId(), " ", "", -1) +
				"0")
			clusterSand := createCluster(addressSand, clusterNameSand)
			clustersSand = append(clustersSand, &clusterSand)
			clusterRefSand := clusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(mgwSwagger.GetXWso2Basepath(), mgwSwagger.GetVersion(), endpointSand[0], resource, clusterRefSand)
			routesSand = append(routesSand, &routeS)
			endpointsSand = append(endpointsSand, &addressSand)

			//API level check
		} else if swag_operator.IsEndpointsAvailable(mgwSwagger.GetSandEndpoints()) {
			endpointSand = apiLevelEndpointSand
			clusterRefSand := apilevelClusterSand.GetName()

			//sandbox endpoints
			routeS := createRoute(mgwSwagger.GetXWso2Basepath(), mgwSwagger.GetVersion(), endpointSand[0], resource, clusterRefSand)
			routesSand = append(routesSand, &routeS)

		}

		//resource level check production endpoints
		if swag_operator.IsEndpointsAvailable(resource.GetProdEndpoints()) {
			endpointProd = resource.GetProdEndpoints()
			addressProd := createAddress(endpointProd[0].GetHost(), endpointProd[0].GetPort())
			//TODO: (VirajSalaka) 0 is hardcoded as only one endpoint is supported at the moment
			clusterNameProd := strings.TrimSpace(apiLevelClusterNameProd + "_" + strings.Replace(resource.GetId(), " ", "", -1) +
				"0")
			clusterProd := createCluster(addressProd, clusterNameProd)
			clustersProd = append(clustersProd, &clusterProd)
			clusterRefProd := clusterProd.GetName()

			//production endpoints
			routeP := createRoute(mgwSwagger.GetXWso2Basepath(), mgwSwagger.GetVersion(), endpointProd[0], resource, clusterRefProd)
			routesProd = append(routesProd, &routeP)
			endpointsProd = append(endpointsProd, &addressProd)

			//API level check
		} else if swag_operator.IsEndpointsAvailable(mgwSwagger.GetProdEndpoints()) {
			endpointProd = apiLevelEndpointProd
			clusterRefProd := apilevelClusterProd.GetName()

			//production endpoints

			routeP := createRoute(mgwSwagger.GetXWso2Basepath(), mgwSwagger.GetVersion(), endpointProd[0], resource, clusterRefProd)
			routesProd = append(routesProd, &routeP)

		} else {
			logger.LoggerOasparser.Fatalf("Producton endpoints are not defined")
		}
	}

	return routesProd, clustersProd, endpointsProd, routesSand, clustersSand, endpointsSand
}

/**
 * Create a cluster.
 *
 * @param address   Address which has host and port
 * @return v2.Cluster  Cluster instance
 */
func createCluster(address corev3.Address, clusterName string) clusterv3.Cluster {
	logger.LoggerOasparser.Debug("creating a cluster....")
	conf, errReadConfig := configs.ReadConfigs()
	if errReadConfig != nil {
		logger.LoggerOasparser.Fatal("Error loading configuration. ", errReadConfig)
	}

	h := &address
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
									Address: h,
								},
							},
						},
					},
				},
			},
		},
	}
	return cluster
}

/**
 * Create a route.
 *
 * @param xWso2Basepath   Xwso2 basepath
 * @param endpoint  Endpoint
 * @param resource  Microgateway API Resource
 * @param clusterName  Name of the cluster
 * @return v2route.Route  Route instance
 */
func createRoute(xWso2Basepath string, version string, endpoint apiDefinition.Endpoint, resource apiDefinition.Resource, clusterName string) routev3.Route {
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
		//TODO: (VirajSalaka) Decide if contains match or regex match is more appropriate
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
	routePath := generateRoutePaths(xWso2Basepath, endpoint.GetBasepath(), resourcePath)

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
		HostRewriteLiteral: endpoint.GetHost(),
	}

	clusterSpecifier := &routev3.RouteAction_Cluster{
		Cluster: clusterName,
	}
	decorator = &routev3.Decorator{
		Operation: resourcePath,
	}
	var contextExtensions = make(map[string]string)
	contextExtensions["path"] = resourcePath
	//TODO: (VirajSalaka) This will be only assigned when
	contextExtensions["basePath"] = xWso2Basepath
	contextExtensions["method"] = strings.Join(resource.GetMethod(), " ")
	contextExtensions["version"] = version

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
					Substitution: endpoint.GetBasepath(),
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

	return router
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
	newPath = GenerateRegex(fullpath)
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

//TODO: (VirajSalaka) Improve regex specifically for strings, integers etc.
/**
 * Generates regex for the resources which have path paramaters.
 * If path has path parameters ({id}), append a regex pattern (pathParaRegex).
 * To avoid query parameter issues, add a regex pattern ( endRegex) for end of all routes.
 *
 * @param fullpath   resource full path
 * @return string  new route path
 */
func GenerateRegex(fullpath string) string {
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
