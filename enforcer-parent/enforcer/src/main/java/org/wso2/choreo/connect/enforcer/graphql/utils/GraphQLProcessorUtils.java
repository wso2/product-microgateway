/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.graphql.utils;

import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Util class to handle graphQL specific API request processing.
 */
public class GraphQLProcessorUtils {
    private static final Logger logger = LogManager.getLogger(GraphQLProcessorUtils.class);

    /**
     * This method used to extract operation List.
     *
     * @param operation              operation
     * @param typeDefinitionRegistry TypeDefinitionRegistry
     * @return operationList
     */
    public static ArrayList<String> getOperationList(OperationDefinition operation,
                                                     TypeDefinitionRegistry typeDefinitionRegistry) {
        ArrayList<String> operationArray = new ArrayList<>();

        List<ResourceConfig> list = GraphQLSchemaDefinitionUtils.extractGraphQLOperationList(typeDefinitionRegistry,
                operation.getOperation().toString());
        ArrayList<String> supportedFields = getSupportedFields(list);

        getNestedLevelOperations(operation.getSelectionSet().getSelections(), supportedFields, operationArray);
        return operationArray;
    }

    /**
     * This method support to extracted nested level operations.
     *
     * @param selectionList   selection List
     * @param supportedFields supportedFields
     * @param operationArray  operationArray
     */
    public static void getNestedLevelOperations(List<Selection> selectionList, ArrayList<String> supportedFields,
                                                ArrayList<String> operationArray) {
        for (Selection selection : selectionList) {
            if (!(selection instanceof Field)) {
                continue;
            }
            Field levelField = (Field) selection;
            if (!operationArray.contains(levelField.getName()) &&
                    supportedFields.contains(levelField.getName())) {
                operationArray.add(levelField.getName());
                logger.debug("Extracted operation: " + levelField.getName());
            }
            if (levelField.getSelectionSet() != null) {
                getNestedLevelOperations(levelField.getSelectionSet().getSelections(), supportedFields, operationArray);
            }
        }
    }

    /**
     * This method helps to extract only supported operation names.
     *
     * @param list URITemplates
     * @return supported Fields
     */
    private static ArrayList<String> getSupportedFields(List<ResourceConfig> list) {
        ArrayList<String> supportedFields = new ArrayList<>();
        for (ResourceConfig template : list) {
            supportedFields.add(template.getPath());
        }
        return supportedFields;
    }
}
