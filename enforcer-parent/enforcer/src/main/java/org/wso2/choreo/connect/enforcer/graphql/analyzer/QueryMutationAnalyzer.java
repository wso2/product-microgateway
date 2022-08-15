/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.choreo.connect.enforcer.graphql.analyzer;

import graphql.analysis.FieldComplexityCalculator;
import graphql.schema.GraphQLSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.common.gateway.dto.QueryAnalyzerResponseDTO;
import org.wso2.carbon.apimgt.common.gateway.graphql.FieldComplexityCalculatorImpl;
import org.wso2.carbon.apimgt.common.gateway.graphql.QueryAnalyzer;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.graphql.utils.GraphQLSchemaDefinitionUtils;

/**
 * QueryAnalyzer class extension for GraphQL query and mutation operations.
 */
public class QueryMutationAnalyzer extends QueryAnalyzer {

    private static final Logger logger = LogManager.getLogger(QueryMutationAnalyzer.class);

    public QueryMutationAnalyzer(GraphQLSchema schema) {
        super(schema);
    }

    /**
     * This method analyses the query depth.
     *
     * @param requestContext message context of the request
     * @param payload        payload of the request
     * @return true, if the query depth does not exceed the maximum value or false, if query depth exceeds the maximum
     */
    public boolean analyseQueryMutationDepth(RequestContext requestContext, String payload) {

        int maxQueryDepth = getMaxQueryDepth(requestContext);
        QueryAnalyzerResponseDTO responseDTO = analyseQueryDepth(maxQueryDepth, payload);
        if (!responseDTO.isSuccess() && !responseDTO.getErrorList().isEmpty()) {
            handleFailure(requestContext, APIConstants.GraphQL.GRAPHQL_QUERY_TOO_DEEP,
                    APIConstants.GraphQL.GRAPHQL_QUERY_TOO_DEEP_MESSAGE, responseDTO.getErrorList().toString());
            logger.error("Requested query's depth has exceeded. API : {}, version : {}, Error : {}",
                    requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                    responseDTO.getErrorList().toString(), ErrorDetails.errorLog(LoggingConstants.Severity.MINOR,
                            7301));
            return false;
        }
        return true;
    }

    /**
     * This method returns the maximum query complexity value.
     *
     * @param requestContext message context of the request
     * @return maximum query depth value if exists, or -1 to denote no complexity limitation
     */
    private int getMaxQueryDepth(RequestContext requestContext) {
        if (requestContext.getProperties().containsKey(APIConstants.GraphQL.MAXIMUM_QUERY_DEPTH)) {
            int maxDepth = (Integer) requestContext.getProperties().get(APIConstants.GraphQL.MAXIMUM_QUERY_DEPTH);
            if (maxDepth > 0) {
                return maxDepth;
            } else {
                logger.debug("Maximum query depth value is 0");
                return -1;
            }
        } else {
            logger.debug("Maximum query depth not applicable");
            return -1;
        }
    }

    /**
     * This method analyses the query complexity.
     *
     * @param requestContext message context of the request
     * @param payload        payload of the request
     * @return true, if query complexity does not exceed the maximum or false, if query complexity exceeds the maximum
     */
    public boolean analyseQueryMutationComplexity(RequestContext requestContext, String payload) {

        FieldComplexityCalculator fieldComplexityCalculator;
        try {
            fieldComplexityCalculator = new FieldComplexityCalculatorImpl(
                    GraphQLSchemaDefinitionUtils.policyDefinitionToJson(
                            requestContext.getMatchedAPI().getGraphQLSchemaDTO()
                                    .getGraphQLCustomComplexityInfoDTO()).toJSONString());
        } catch (ParseException e) {
            String errorMessage = "Policy definition parsing failed. ";
            handleFailure(requestContext, APIConstants.GraphQL.GRAPHQL_INVALID_QUERY,
                    APIConstants.GraphQL.GRAPHQL_INVALID_QUERY_MESSAGE, errorMessage);
            return false;
        }
        int maxQueryComplexity = getMaxQueryComplexity(requestContext);
        QueryAnalyzerResponseDTO responseDTO = analyseQueryComplexity(maxQueryComplexity, payload,
                fieldComplexityCalculator);
        if (!responseDTO.isSuccess() && !responseDTO.getErrorList().isEmpty()) {
            handleFailure(requestContext, APIConstants.GraphQL.GRAPHQL_QUERY_TOO_COMPLEX,
                    APIConstants.GraphQL.GRAPHQL_QUERY_TOO_COMPLEX_MESSAGE, responseDTO.getErrorList().toString());
            logger.error("Requested query's complexity has exceeded. API : {}, version : {}, Error: {}",
                    requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                    responseDTO.getErrorList().toString(), ErrorDetails.errorLog(LoggingConstants.Severity.MINOR,
                            7303));
            return false;
        }
        return true;
    }

    /**
     * This method returns the maximum query complexity value.
     *
     * @param requestContext message context of the request
     * @return maximum query complexity value if exists, or -1 to denote no complexity limitation
     */
    private int getMaxQueryComplexity(RequestContext requestContext) {
        if (requestContext.getProperties().containsKey(APIConstants.GraphQL.MAXIMUM_QUERY_COMPLEXITY)) {
            int maxComplexity = (Integer) requestContext.getProperties()
                    .get(APIConstants.GraphQL.MAXIMUM_QUERY_COMPLEXITY);
            if (maxComplexity > 0) {
                return maxComplexity;
            } else {
                logger.debug("Maximum query complexity value is 0");
                return -1;
            }
        } else {
            logger.debug("Maximum query complexity not applicable");
            return -1;
        }
    }

    /**
     * This method handle the query mutation analysis failures.
     *
     * @param errorCodeValue   error code of the failure
     * @param requestContext   message context of the request
     * @param errorMessage     error message of the failure
     * @param errorDescription error description of the failure
     */
    private void handleFailure(RequestContext requestContext, int errorCodeValue, String errorMessage,
                               String errorDescription) {
        requestContext.getProperties().put(APIConstants.MessageFormat.STATUS_CODE,
                APIConstants.StatusCodes.BAD_REQUEST_ERROR.getCode());
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_CODE,
                errorCodeValue);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE,
                errorMessage);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                errorDescription);
    }
}
