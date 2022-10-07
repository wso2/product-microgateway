/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.graphql;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.common.gateway.constants.GraphQLConstants;
import org.wso2.carbon.apimgt.common.gateway.graphql.GraphQLProcessorUtil;
import org.wso2.carbon.apimgt.common.gateway.graphql.QueryValidator;
import org.wso2.choreo.connect.discovery.api.GraphqlComplexity;
import org.wso2.choreo.connect.enforcer.api.API;
import org.wso2.choreo.connect.enforcer.api.APIFactory;
import org.wso2.choreo.connect.enforcer.commons.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.GraphQLCustomComplexityInfoDTO;
import org.wso2.choreo.connect.enforcer.commons.model.GraphQLSchemaDTO;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * utils for GraphQL Request payload processing.
 */
public class GraphQLPayloadUtils {
    private static final Logger logger = LogManager.getLogger(GraphQLPayloadUtils.class);

    /**
     * This method will decode the qraphQL query body.
     *
     * @param api       matched api
     * @param queryBody graphQL query
     * @return matching resource configs for the request
     * @throws EnforcerException use for error response handling
     */
    public static ArrayList<ResourceConfig> buildGQLRequestContext(API api, String queryBody) throws EnforcerException {
        GraphQLSchemaDTO graphQLSchemaDTO = api.getAPIConfig().getGraphQLSchemaDTO();
        try {
            // Validate payload with graphQLSchema
            Document document = new Parser().parseDocument(queryBody);
            String validationErrors = validatePayloadWithSchema(graphQLSchemaDTO.getGraphQLSchema(), document);
            if (validationErrors == null) {
                ArrayList<String> operationList = new ArrayList<>();
                String method = "";
                // Extract the operation type and operations from the payload
                for (Definition definition : document.getDefinitions()) {
                    // we only allow one operation type per request
                    if (definition instanceof OperationDefinition) {
                        OperationDefinition operation = (OperationDefinition) definition;
                        if (operation.getOperation() != null) {
                            method = operation.getOperation().toString();
                            operationList = GraphQLProcessorUtil.getOperationList(operation,
                                    graphQLSchemaDTO.getTypeDefinitionRegistry());
                            logger.debug("Found operation list : " + operationList.toString());
                            break;
                        }
                    } else {
                        throw new EnforcerException("Operation definition cannot be empty");
                    }
                }
                ArrayList<ResourceConfig> resourceConfigs = new ArrayList<>();
                for (String op : operationList) {
                    ResourceConfig resourceConfig = APIFactory.getInstance().getMatchedResource(api, op, method);
                    if (resourceConfig != null) {
                        resourceConfigs.add(resourceConfig);
                    } else {
                        logger.error("No matching operations found for {} in APIUUID : {} API : {}, version : {}", op,
                                api.getAPIConfig().getName(), api.getAPIConfig().getUuid(),
                                api.getAPIConfig().getVersion(),
                                ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6705));
                        return new ArrayList<>();
                    }
                }
                return resourceConfigs;
            } else {
                throw new EnforcerException("Payload is invalid", new Exception(validationErrors));
            }
        } catch (InvalidSyntaxException exception) {
            throw new EnforcerException("Invalid syntax", exception);
        }
    }

    /**
     * @param requestPayload request payload
     * @param requestHeaders request headers
     * @return
     * @throws EnforcerException invalid payloads
     */
    public static String getGQLRequestPayload(String requestPayload, Map<String, String> requestHeaders)
            throws EnforcerException {
        String queryBody = "";
        if (!requestHeaders.containsKey(APIConstants.CONTENT_TYPE_HEADER) ||
                (requestHeaders.containsKey(APIConstants.CONTENT_TYPE_HEADER) && APIConstants.APPLICATION_JSON
                        .equalsIgnoreCase(requestHeaders.get(APIConstants.CONTENT_TYPE_HEADER)))) {
            try {
                JSONObject jsonObject = new JSONObject(requestPayload);
                queryBody = jsonObject.getString(GraphQLConstants.GRAPHQL_QUERY.toLowerCase(Locale.ROOT));
            } catch (JSONException e) {
                throw new EnforcerException("Invalid GraphQL query body structure");
            }
        } else if (requestHeaders.containsKey(APIConstants.CONTENT_TYPE_HEADER) && APIConstants.APPLICATION_GRAPHQL
                .equalsIgnoreCase(requestHeaders.get(APIConstants.CONTENT_TYPE_HEADER))) {
            queryBody = requestPayload;
        } else {
            throw new EnforcerException("Invalid content type. Make sure the content type is " +
                    APIConstants.APPLICATION_JSON + " or " + APIConstants.APPLICATION_GRAPHQL);
        }
        if (StringUtils.isNotBlank(queryBody)) {
            return queryBody;
        }
        throw new EnforcerException("Query cannot be empty");
    }

    /**
     * This method validate the payload.
     *
     * @param graphQLSchema graphql sdl
     * @param document      graphQL schema of the request
     * @return true or false
     */
    private static String validatePayloadWithSchema(GraphQLSchema graphQLSchema, Document document) {
        QueryValidator queryValidator = new QueryValidator(new Validator());
        return queryValidator.validatePayload(graphQLSchema, document);
    }

    public static List<GraphQLCustomComplexityInfoDTO> parseComplexityDTO(List<GraphqlComplexity>
                                                                                  customComplexityDetailsList) {
        List<GraphQLCustomComplexityInfoDTO> graphQLCustomComplexityInfoDTOList = new ArrayList<>();
        for (GraphqlComplexity complexity : customComplexityDetailsList) {
            GraphQLCustomComplexityInfoDTO graphQLCustomComplexityInfoDTO = new GraphQLCustomComplexityInfoDTO(
                    complexity.getType(), complexity.getField(), complexity.getComplexityValue());
            graphQLCustomComplexityInfoDTOList.add(graphQLCustomComplexityInfoDTO);
        }
        return graphQLCustomComplexityInfoDTOList;
    }
}


