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

	core "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	cluster "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	route "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"

	swgger "github.com/wso2/micro-gw/internal/pkg/oasparser/swaggerOperator"
	enovoy "github.com/wso2/micro-gw/internal/pkg/oasparser/envoyCodegen"
	logger "github.com/wso2/micro-gw/internal/loggers"
	"github.com/wso2/micro-gw/internal/pkg/oasparser/models/envoy"

	"strings"
)

/**
 * Get all production resources for envoy.
 *
 * @param location  Location of swagger files
 * @return []types.Resource Production listeners
 * @return []types.Resource Production clusters
 * @return []types.Resource Production routes
 * @return []types.Resource Production endpoints
 */
func GetProductionSources(location string) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	logger.LoggerOasparser.Debug("debug check....................")
	mgwSwaggers, err := swgger.GenerateMgwSwagger(location)
	if err != nil {
		logger.LoggerOasparser.Fatal("Error Generating mgwSwagger struct:", err)

	}

	var (
		routesP    []*route.Route
		clustersP  []*cluster.Cluster
		endpointsP []*core.Address
	)

	for _, swagger := range mgwSwaggers {
		routes, clusters, endpoints, _, _, _ := enovoy.CreateRoutesWithClusters(swagger)
		routesP = append(routesP, routes...)
		clustersP = append(clustersP, clusters...)
		endpointsP = append(endpointsP, endpoints...)
	}

	envoyNodeProd := new(envoy.EnvoyNode)

	if len(mgwSwaggers) > 0 {
		vHost_NameP := "serviceProd_" + strings.Replace(mgwSwaggers[0].GetTitle(), " ", "", -1) + mgwSwaggers[0].GetVersion()
		vHostP, _ := enovoy.CreateVirtualHost(vHost_NameP, routesP)
		listenerNameP := "listenerProd_1"
		routeConfigNameP := "routeProd_" + strings.Replace(mgwSwaggers[0].GetTitle(), " ", "", -1) + mgwSwaggers[0].GetVersion()
		listnerProd := enovoy.CreateListener(listenerNameP, routeConfigNameP, vHostP)

		envoyNodeProd.SetListener(&listnerProd)
		envoyNodeProd.SetClusters(clustersP)
		envoyNodeProd.SetRoutes(routesP)
		envoyNodeProd.SetEndpoints(endpointsP)

	} else {
		logger.LoggerOasparser.Error("No Api definitions found")
	}

	logger.LoggerOasparser.Info(len(routesP), " routes are generated successfully")
	logger.LoggerOasparser.Info(len(clustersP), " clusters are generated successfully")
	logger.LoggerOasparser.Info(len(endpointsP), " endpoints are generated successfully")
	return envoyNodeProd.GetSources()
}

/**
 * Get all sandbox resources for envoy.
 *
 * @param location  Location of swagger files
 * @return []types.Resource sandbox listeners
 * @return []types.Resource sandbox clusters
 * @return []types.Resource sandbox routes
 * @return []types.Resource sandbox endpoints
 */
func GetSandboxSources(location string) ([]types.Resource, []types.Resource, []types.Resource, []types.Resource) {
	mgwSwaggers, err := swgger.GenerateMgwSwagger(location)
	if err != nil {
		logger.LoggerOasparser.Fatal("Error Generating mgwSwagger struct:", err)
	}
	//fmt.Println(mgwSwagger)
	var (
		routesS    []*route.Route
		clustersS  []*cluster.Cluster
		endpointsS []*core.Address
	)

	for _, swagger := range mgwSwaggers {
		_, _, _, routes, clusters, endpoints := enovoy.CreateRoutesWithClusters(swagger)
		routesS = append(routes)
		clustersS = append(clusters)
		endpointsS = append(endpoints)
	}

	if routesS == nil {
		return nil, nil, nil, nil
	}
	envoyNodeSand := new(envoy.EnvoyNode)

	if len(mgwSwaggers) > 0 {
		vHost_NameS := "serviceSand_" + strings.Replace(mgwSwaggers[0].GetTitle(), " ", "", -1) + mgwSwaggers[0].GetVersion()
		vHostS, _ := enovoy.CreateVirtualHost(vHost_NameS, routesS)
		listenerNameS := "listenerSand_1"
		routeConfigNameS := "routeSand_" + strings.Replace(mgwSwaggers[0].GetTitle(), " ", "", -1) + mgwSwaggers[0].GetVersion()
		listnerSand := enovoy.CreateListener(listenerNameS, routeConfigNameS, vHostS)

		envoyNodeSand.SetListener(&listnerSand)
		envoyNodeSand.SetClusters(clustersS)
		envoyNodeSand.SetRoutes(routesS)
		envoyNodeSand.SetEndpoints(endpointsS)
	} else {
		logger.LoggerOasparser.Error("No Api definitions found")
	}

	return envoyNodeSand.GetSources()
}
