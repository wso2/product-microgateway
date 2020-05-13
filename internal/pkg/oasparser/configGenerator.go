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
package oasparser

//package envoy_config_generator

import (
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	e "github.com/wso2/micro-gw/internal/pkg/oasparser/envoyCodegen"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/envoy"
	swgger "github.com/wso2/micro-gw/internal/pkg/oasparser/swaggerOperator"
	"strings"
)

func GetProductionSources(location string) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	mgwSwagger := swgger.GenerateMgwSwagger(location)
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

func GetSandboxSources(location string) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	mgwSwagger := swgger.GenerateMgwSwagger(location)
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
