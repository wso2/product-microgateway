package envoyCodegen

import (
	"errors"
	"fmt"
	v2 "github.com/envoyproxy/go-control-plane/envoy/api/v2"
	core "github.com/envoyproxy/go-control-plane/envoy/api/v2/core"
	envoy_api_v2_endpoint "github.com/envoyproxy/go-control-plane/envoy/api/v2/endpoint"
	v2route "github.com/envoyproxy/go-control-plane/envoy/api/v2/route"
	envoy_type_matcher "github.com/envoyproxy/go-control-plane/envoy/type/matcher"
	"github.com/golang/protobuf/ptypes"
	"github.com/wso2/envoy-control-plane/internal/pkg/oasparser/config"
	c "github.com/wso2/envoy-control-plane/internal/pkg/oasparser/constants"
	"github.com/wso2/envoy-control-plane/internal/pkg/oasparser/models/apiDefinition"
	s "github.com/wso2/envoy-control-plane/internal/pkg/oasparser/swaggerOperator"
	"strings"
	"time"
)

func CreateRoutesWithClusters(mgwSwagger apiDefinition.MgwSwagger) ([]*v2route.Route, []*v2.Cluster, []*core.Address, []*v2route.Route, []*v2.Cluster, []*core.Address) {
	var (
		routesP           []*v2route.Route
		clustersP         []*v2.Cluster
		endpointP         apiDefinition.Endpoint
		apiLevelEndpointP apiDefinition.Endpoint
		clusterP          v2.Cluster
		apilevelClusterP  v2.Cluster
		clusterNameP      string
		addressP          core.Address
		cluster_refP      string
		endpointsP        []*core.Address

		routesS           []*v2route.Route
		clustersS         []*v2.Cluster
		endpointS         apiDefinition.Endpoint
		apiLevelEndpointS apiDefinition.Endpoint
		clusterS          v2.Cluster
		apilevelClusterS  v2.Cluster
		clusterNameS      string
		addressS          core.Address
		cluster_refS      string
		endpointsS        []*core.Address
	)

	if s.IsSandboxEndpointsAvailable(mgwSwagger.VendorExtensible) {
		apiLevelEndpointS = s.GetEndpoints(mgwSwagger.VendorExtensible, c.SANDBOX_ENDPOINTS)
		apilevelAddressS := createAddress(apiLevelEndpointS.Url[0], config.API_PORT)
		apiLevelClusterNameS := "clusterSand_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version
		apilevelClusterS = createCluster(apilevelAddressS, apiLevelClusterNameS)
		clustersS = append(clustersS, &apilevelClusterS)

		endpointsS = append(endpointsS, &apilevelAddressS)
	}

	if s.IsProductionEndpointsAvailable(mgwSwagger.VendorExtensible) {
		apiLevelEndpointP = s.GetEndpoints(mgwSwagger.VendorExtensible, c.PRODUCTION_ENDPOINTS)
		apilevelAddressP := createAddress(apiLevelEndpointP.Url[0], config.API_PORT)
		apiLevelClusterNameP := "clusterProd_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version
		apilevelClusterP = createCluster(apilevelAddressP, apiLevelClusterNameP)
		clustersP = append(clustersP, &apilevelClusterP)

		endpointsP = append(endpointsP, &apilevelAddressP)

	} else {
		errors.New("Producton endpoints are not defined")
	}

	for ind, resource := range mgwSwagger.Resources {

		//resource level check sandbox endpoints
		if s.IsSandboxEndpointsAvailable(resource.VendorExtensible) {
			endpointS = s.GetEndpoints(resource.VendorExtensible, c.SANDBOX_ENDPOINTS)
			addressS = createAddress(endpointS.Url[0], config.API_PORT)
			clusterNameS = "clusterSand_" + strings.Replace(resource.ID, " ", "", -1) + string(ind)
			clusterS = createCluster(addressS, clusterNameS)
			clustersS = append(clustersS, &clusterS)

			cluster_refS = clusterS.GetName()

			//sandbox endpoints
			routeS := createRoute(endpointS.Url[0], resource.Context, cluster_refS)
			routesS = append(routesS, &routeS)

			endpointsS = append(endpointsS, &addressS)

			//API level check
		} else if s.IsSandboxEndpointsAvailable(mgwSwagger.VendorExtensible) {
			endpointS = apiLevelEndpointS
			cluster_refS = apilevelClusterS.GetName()

			//sandbox endpoints
			routeS := createRoute(endpointS.Url[0], resource.Context, cluster_refS)
			routesS = append(routesS, &routeS)

		}

		//resource level check
		if s.IsProductionEndpointsAvailable(resource.VendorExtensible) {
			endpointP = s.GetEndpoints(resource.VendorExtensible, c.PRODUCTION_ENDPOINTS)
			addressP = createAddress(endpointP.Url[0], config.API_PORT)
			clusterNameP = "clusterProd_" + strings.Replace(resource.ID, " ", "", -1) + string(ind)
			clusterP = createCluster(addressP, clusterNameP)
			clustersP = append(clustersP, &clusterP)

			cluster_refP = clusterP.GetName()

			//production endpoints
			routeP := createRoute(endpointP.Url[0], resource.Context, cluster_refP)
			routesP = append(routesP, &routeP)

			endpointsP = append(endpointsP, &addressP)

			//API level check
		} else if s.IsProductionEndpointsAvailable(mgwSwagger.VendorExtensible) {
			endpointP = apiLevelEndpointP
			cluster_refP = apilevelClusterP.GetName()

			//production endpoints
			routeP := createRoute(endpointP.Url[0], resource.Context, cluster_refP)
			routesP = append(routesP, &routeP)

		} else {
			errors.New("Producton endpoints are not defined")
		}

	}

	fmt.Println(len(routesP), "routes are generated successfully")
	fmt.Println(len(clustersP), "clusters are generated successfully")
	fmt.Println(len(endpointsP), "endpoints are generated successfully")
	return routesP, clustersP, endpointsP, routesS, clustersS, endpointsP

}

func createCluster(address core.Address, clusterName string) v2.Cluster {

	h := &address
	cluster := v2.Cluster{
		Name:                 clusterName,
		ConnectTimeout:       ptypes.DurationProto(2 * time.Second),
		ClusterDiscoveryType: &v2.Cluster_Type{Type: v2.Cluster_STRICT_DNS},
		DnsLookupFamily:      v2.Cluster_V4_ONLY,
		LbPolicy:             v2.Cluster_ROUND_ROBIN,
		LoadAssignment: &v2.ClusterLoadAssignment{
			ClusterName: clusterName,
			Endpoints: []*envoy_api_v2_endpoint.LocalityLbEndpoints{
				{
					LbEndpoints: []*envoy_api_v2_endpoint.LbEndpoint{
						{
							HostIdentifier: &envoy_api_v2_endpoint.LbEndpoint_Endpoint{
								Endpoint: &envoy_api_v2_endpoint.Endpoint{
									Address: h,
								},
							},
						},
					},
				},
			},
		},
		//Hosts:                []*core.Address{h},
	}

	return cluster
}

func createRoute(HostUrl string, resourcePath string, clusterName string) v2route.Route {
	//var targetRegex = "/"
	route := v2route.Route{
		Match: &v2route.RouteMatch{
			PathSpecifier: &v2route.RouteMatch_SafeRegex{
				SafeRegex: &envoy_type_matcher.RegexMatcher{
					EngineType: &envoy_type_matcher.RegexMatcher_GoogleRe2{
						GoogleRe2: &envoy_type_matcher.RegexMatcher_GoogleRE2{
							MaxProgramSize: nil,
						},
					},
					Regex: `\/pet\/[0-9]+`,
				},
			},
			//Headers: []*v2route.HeaderMatcher {
			//	{
			//		Name: "path",
			//		HeaderMatchSpecifier: &v2route.HeaderMatcher_PrefixMatch{
			//			PrefixMatch: resourcePath,
			//		},
			//	},
			//},
		},
		Action: &v2route.Route_Route{
			Route: &v2route.RouteAction{
				HostRewriteSpecifier: &v2route.RouteAction_HostRewrite{
					HostRewrite: HostUrl + "/v2",
				},
				ClusterSpecifier: &v2route.RouteAction_Cluster{
					Cluster: clusterName,
				},
			},
		},
		Metadata: nil,
	}
	fmt.Println(HostUrl, resourcePath)
	return route
}
