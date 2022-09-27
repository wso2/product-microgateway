/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.choreo.connect.enforcer.graphql.utils;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.json.simple.JSONObject;
import org.wso2.choreo.connect.enforcer.commons.model.GraphQLCustomComplexityInfoDTO;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * utils to process GraphQLSchemaDefinition.
 */
public class GraphQLSchemaDefinitionUtils {
    /**
     * Extract GraphQL Operations from given schema.
     *
     * @param typeRegistry graphQL Schema Type Registry
     * @param type         operation type string
     * @return the arrayList of APIOperationsDTO
     */
    public static List<ResourceConfig> extractGraphQLOperationList(TypeDefinitionRegistry typeRegistry, String type) {
        List<ResourceConfig> operationArray = new ArrayList<>();
        Map<String, TypeDefinition> operationList = typeRegistry.types();
        for (Map.Entry<String, TypeDefinition> entry : operationList.entrySet()) {
            Optional<SchemaDefinition> schemaDefinition = typeRegistry.schemaDefinition();
            if (schemaDefinition.isPresent()) {
                List<OperationTypeDefinition> operationTypeList = schemaDefinition.get().getOperationTypeDefinitions();
                for (OperationTypeDefinition operationTypeDefinition : operationTypeList) {
                    boolean canAddOperation = (entry.getValue().getName().equalsIgnoreCase(operationTypeDefinition
                            .getTypeName().getName())) && (type == null ||
                            type.equals(operationTypeDefinition.getName().toUpperCase()));
                    if (canAddOperation) {
                        addOperations(entry, operationTypeDefinition.getName().toUpperCase(), operationArray);
                    }
                }
            } else {
                boolean canAddOperation = (APIConstants.GraphQL.GRAPHQL_QUERY.equalsIgnoreCase(entry.getValue()
                        .getName()) || APIConstants.GraphQL.GRAPHQL_MUTATION.equalsIgnoreCase(entry.getValue()
                        .getName()) || APIConstants.GraphQL.GRAPHQL_SUBSCRIPTION.equalsIgnoreCase(entry.getValue()
                        .getName())) && (type == null || type.equals(entry.getValue().getName().toUpperCase()));
                if (canAddOperation) {
                    addOperations(entry, entry.getKey(), operationArray);
                }
            }
        }
        return operationArray;
    }

    private static void addOperations(Map.Entry<String, TypeDefinition> entry, String graphQLType, List<ResourceConfig>
            operationArray) {
        for (FieldDefinition fieldDef : ((ObjectTypeDefinition) entry.getValue()).getFieldDefinitions()) {
            ResourceConfig operation = new ResourceConfig();
            operation.setMethod(ResourceConfig.HttpMethods.valueOf(graphQLType.toUpperCase()));
            operation.setPath(fieldDef.getName());
            operationArray.add(operation);
        }
    }

    /**
     * Method to convert GraphqlComplexityInfo object to a JSONObject.
     *
     * @param graphqlComplexityInfoList GraphqlComplexityInfo object
     * @return json object which contains the policy definition
     */
    public static JSONObject policyDefinitionToJson(List<GraphQLCustomComplexityInfoDTO> graphqlComplexityInfoList) {
        JSONObject policyDefinition = new JSONObject();
        HashMap<String, HashMap<String, Integer>> customComplexityMap = new HashMap<>();
        for (GraphQLCustomComplexityInfoDTO graphqlComplexityInfo : graphqlComplexityInfoList) {
            String type = graphqlComplexityInfo.getType();
            String field = graphqlComplexityInfo.getField();
            int complexityValue = graphqlComplexityInfo.getComplexityValue();
            if (customComplexityMap.containsKey(type)) {
                customComplexityMap.get(type).put(field, complexityValue);
            } else {
                HashMap<String, Integer> complexityValueMap = new HashMap<>();
                complexityValueMap.put(field, complexityValue);
                customComplexityMap.put(type, complexityValueMap);
            }
        }

        Map<String, Map<String, Object>> customComplexityObject = new LinkedHashMap<>(customComplexityMap.size());
        for (HashMap.Entry<String, HashMap<String, Integer>> entry : customComplexityMap.entrySet()) {
            HashMap<String, Integer> fieldValueMap = entry.getValue();
            String type = entry.getKey();
            Map<String, Object> fieldValueObject = new LinkedHashMap<>(fieldValueMap.size());
            for (HashMap.Entry<String, Integer> subEntry : fieldValueMap.entrySet()) {
                String field = subEntry.getKey();
                int complexityValue = subEntry.getValue();
                fieldValueObject.put(field, complexityValue);
            }
            customComplexityObject.put(type, fieldValueObject);
        }

        policyDefinition.put(APIConstants.GraphQL.QUERY_ANALYSIS_COMPLEXITY, customComplexityObject);
        return policyDefinition;
    }
}

