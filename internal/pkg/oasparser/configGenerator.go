package oasparser

//package envoy_config_generator

import (
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	e "github.com/wso2/envoy-control-plane/internal/pkg/oasparser/envoyCodegen"
	"github.com/wso2/envoy-control-plane/internal/pkg/oasparser/models/envoy"
	swgger "github.com/wso2/envoy-control-plane/internal/pkg/oasparser/swaggerOperator"
	"strings"
)

func main() {

	GetProductionSources()

}

func GetProductionSources() ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	mgwSwagger := swgger.GenerateMgwSwagger()
	//fmt.Println(mgwSwagger)

	routesP, clustersP, endpointsP, _, _, _ := e.CreateRoutesWithClusters(mgwSwagger)

	vHost_NameP := "serviceProd_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version

	vHostP, _ := e.CreateVirtualHost(vHost_NameP, routesP)

	listenerNameP := "listenerProd_1"
	routeConfigNameP := "routeProd_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version

	listnerProd := e.CreateListener(listenerNameP, routeConfigNameP, vHostP)

	envoyNodeProd := new(envoy.EnvoyNode)
	envoyNodeProd.SetListener(&listnerProd)
	envoyNodeProd.SetClusters(clustersP)
	envoyNodeProd.SetRoutes(routesP)
	envoyNodeProd.SetEndpoints(endpointsP)
	//fmt.Println(endpointsP)

	return envoyNodeProd.GetSources()
}

func GetSandboxSources() ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	mgwSwagger := swgger.GenerateMgwSwagger()
	//fmt.Println(mgwSwagger)

	_, _, _, routesS, clustersS, endpointsS := e.CreateRoutesWithClusters(mgwSwagger)
	if routesS == nil {
		return nil, nil, nil, nil
	}

	vHost_NameS := "serviceSand_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version

	vHostS, _ := e.CreateVirtualHost(vHost_NameS, routesS)

	//fmt.Println(err)

	listenerNameS := "listenerSand_1"
	routeConfigNameS := "routeSand_" + strings.Replace(mgwSwagger.Title, " ", "", -1) + mgwSwagger.Version

	listnerSand := e.CreateListener(listenerNameS, routeConfigNameS, vHostS)

	envoyNodeSand := new(envoy.EnvoyNode)
	envoyNodeSand.SetListener(&listnerSand)
	envoyNodeSand.SetClusters(clustersS)
	envoyNodeSand.SetRoutes(routesS)
	envoyNodeSand.SetEndpoints(endpointsS)
	//fmt.Println(endpointsS)
	return envoyNodeSand.GetSources()
}
