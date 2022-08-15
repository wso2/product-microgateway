/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package model

import (
	"errors"

	"github.com/google/uuid"
	"github.com/wso2/product-microgateway/adapter/internal/oasparser/constants"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/api"
)

// GraphQLComplexityYaml contains complexity values relevant to the fields included in the GraphQL schema.
type GraphQLComplexityYaml struct {
	Data struct {
		List []*api.GraphqlComplexity
	}
}

// SetInfoGraphQLAPI populates the MgwSwagger object with information in api.yaml.
func (swagger *MgwSwagger) SetInfoGraphQLAPI(apiYaml APIYaml) error {

	var securitySchemes []SecurityScheme
	var isAPIKeyEnabled bool

	// assigns mgw security schemes
	for _, securitySchemeValue := range apiYaml.Data.SecurityScheme {
		if securitySchemeValue == constants.APIMOauth2Type {
			securitySchemes = append(securitySchemes, SecurityScheme{DefinitionName: "default", Type: securitySchemeValue})
		} else if securitySchemeValue == constants.APIMAPIKeyType {
			isAPIKeyEnabled = true
			securitySchemes = append(securitySchemes, SecurityScheme{DefinitionName: constants.APIMAPIKeyInHeader,
				Type: constants.APIKeyTypeInOAS, Name: constants.APIKeyNameWithApim, In: constants.APIKeyInHeaderOAS})
			securitySchemes = append(securitySchemes, SecurityScheme{DefinitionName: constants.APIMAPIKeyInQuery,
				Type: constants.APIKeyTypeInOAS, Name: constants.APIKeyNameWithApim, In: constants.APIKeyInQueryOAS})
		}
	}
	swagger.securityScheme = securitySchemes

	// sets resources relevant to the GraphQL API considering api.yaml
	var resources []*Resource
	if len(apiYaml.Data.Operations) < 0 {
		return errors.New("cannot process api.yaml since operations not defined in the api.yaml")
	}

	for _, operation := range apiYaml.Data.Operations {
		var resource Resource
		var methods []*Operation
		var resourceMethod Operation
		var scopes []string
		var security []map[string][]string

		// assigns resource level attribute values
		resource.iD = uuid.New().String()
		resource.path = operation.Target

		// assigns operation level attribute values
		resourceMethod.method = operation.Verb
		resourceMethod.tier = operation.ThrottlingPolicy
		resourceMethod.iD = operation.ID

		for _, scope := range operation.Scopes {
			scopes = append(scopes, scope)
		}
		if operation.AuthType == "None" {
			resourceMethod.disableSecurity = true
		} else {
			security = append(security, map[string][]string{constants.APIMDefaultOauth2Security: scopes})
			if isAPIKeyEnabled {
				security = append(security, map[string][]string{constants.APIMAPIKeyInHeader: {}})
				security = append(security, map[string][]string{constants.APIMAPIKeyInQuery: {}})
			}
		}
		resourceMethod.security = security

		methods = append(methods, &resourceMethod)
		resource.methods = methods
		resources = append(resources, &resource)
	}
	swagger.resources = resources

	var corsConfig *CorsConfig = generateGlobalCors()
	if apiYaml.Data.CorsConfiguration.CorsConfigurationEnabled == true {
		corsConfig.AccessControlAllowOrigins = apiYaml.Data.CorsConfiguration.AccessControlAllowOrigins
		corsConfig.AccessControlAllowCredentials = apiYaml.Data.CorsConfiguration.AccessControlAllowCredentials
		corsConfig.AccessControlAllowHeaders = apiYaml.Data.CorsConfiguration.AccessControlAllowHeaders
		corsConfig.AccessControlAllowMethods = apiYaml.Data.CorsConfiguration.AccessControlAllowMethods
	}
	swagger.xWso2Cors = corsConfig

	// enables request body passing feature for GraphQL APIs
	swagger.xWso2RequestBodyPass = true

	return nil
}
