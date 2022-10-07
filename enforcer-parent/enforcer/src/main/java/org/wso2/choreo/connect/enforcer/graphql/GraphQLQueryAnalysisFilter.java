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
package org.wso2.choreo.connect.enforcer.graphql;

import graphql.schema.GraphQLSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.common.gateway.constants.GraphQLConstants;
import org.wso2.carbon.apimgt.common.gateway.dto.QueryAnalyzerResponseDTO;
import org.wso2.carbon.apimgt.common.gateway.graphql.QueryAnalyzer;
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.GraphQLCustomComplexityInfoDTO;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This Handler can be used to analyse GraphQL Query. This implementation uses previously set
 * complexity and depth limitation to block the complex queries before it reaches the backend.
 */
public class GraphQLQueryAnalysisFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(GraphQLQueryAnalysisFilter.class);
    private QueryAnalyzer queryAnalyzer;

    @Override
    public void init(APIConfig apiConfig, Map<String, String> configProperties) {
        GraphQLSchema schema = apiConfig.getGraphQLSchemaDTO().getGraphQLSchema();
        queryAnalyzer = new QueryAnalyzer(schema);
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        String payload = requestContext.getRequestPayload();
        if (!isDepthAndComplexityValid(requestContext, payload)) {
            logger.debug("Query was blocked by the static query analyser");
            return false;
        }
        return true;
    }

    /**
     * This method analyses the query.
     *
     * @param requestContext message context of the request
     * @param payload        payload of the request
     * @return true, if the query is not blocked or false, if the query is blocked
     */
    private boolean isDepthAndComplexityValid(RequestContext requestContext, String payload) {
        try {
            return isDepthValid(requestContext, payload) && isComplexityValid(requestContext, payload);
        } catch (Exception e) {
            logger.error("Policy definition parsing failed for API UUID : {} API : {} version : {}",
                    requestContext.getMatchedAPI().getUuid(), requestContext.getMatchedAPI().getName(),
                    requestContext.getMatchedAPI().getVersion(),
                    ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 7300), e);
            handleFailure(requestContext);
            return false;
        }
    }

    private boolean isDepthValid(RequestContext requestContext, String payload) {
        int maxQueryDepth = -1;
        if (requestContext.getProperties().containsKey(GraphQLConstants.MAXIMUM_QUERY_DEPTH)) {
            maxQueryDepth = (Integer) requestContext.getProperties().get(GraphQLConstants.MAXIMUM_QUERY_DEPTH);
        }
        QueryAnalyzerResponseDTO responseDTO = queryAnalyzer.analyseQueryDepth(maxQueryDepth, payload);
        if (!responseDTO.isSuccess() && !responseDTO.getErrorList().isEmpty()) {
            handleFailure(requestContext, GraphQLConstants.GRAPHQL_QUERY_TOO_DEEP,
                    GraphQLConstants.GRAPHQL_QUERY_TOO_DEEP_MESSAGE, responseDTO.getErrorList().toString());
            logger.error("Requested query's depth has exceeded. API : {}, version : {}, Error : {}",
                    requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                    responseDTO.getErrorList().toString(), ErrorDetails.errorLog(LoggingConstants.Severity.MINOR,
                            7301));
            return false;
        }
        return true;
    }

    private boolean isComplexityValid(RequestContext requestContext, String payload) {
        int queryComplexity = -1;
        if (requestContext.getProperties().containsKey(GraphQLConstants.MAXIMUM_QUERY_COMPLEXITY)) {
            queryComplexity = (Integer) requestContext.getProperties()
                    .get(GraphQLConstants.MAXIMUM_QUERY_COMPLEXITY);
        }
        QueryAnalyzerResponseDTO responseDTO = null;
        try {
            responseDTO = queryAnalyzer.analyseQueryMutationComplexity(payload, queryComplexity,
                    policyDefinitionToJson(requestContext.getMatchedAPI().getGraphQLSchemaDTO()
                            .getGraphQLCustomComplexityInfoDTO()).toJSONString());
        } catch (ParseException e) {
            String errorMessage = "Policy definition parsing failed. ";
            handleFailure(requestContext, GraphQLConstants.GRAPHQL_INVALID_QUERY, errorMessage, errorMessage);
        }
        if (responseDTO != null && !responseDTO.isSuccess() && !responseDTO.getErrorList().isEmpty()) {
            handleFailure(requestContext, GraphQLConstants.GRAPHQL_QUERY_TOO_COMPLEX,
                    GraphQLConstants.GRAPHQL_QUERY_TOO_COMPLEX_MESSAGE, responseDTO.getErrorList().toString());
            logger.error("Requested query's complexity has exceeded. API : {}, version : {}, Error: {}",
                    requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                    responseDTO.getErrorList().toString(), ErrorDetails.errorLog(LoggingConstants.Severity.MINOR,
                            7303));
            return false;
        }
        return true;
    }

    /**
     * This method handle the failure.
     *
     * @param requestContext message context of the request
     */
    private void handleFailure(RequestContext requestContext) {
        requestContext.getProperties().put(APIConstants.MessageFormat.STATUS_CODE,
                APIConstants.StatusCodes.INTERNAL_SERVER_ERROR);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_CODE,
                APISecurityConstants.API_AUTH_GENERAL_ERROR);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_MESSAGE,
                APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
        requestContext.getProperties().put(APIConstants.MessageFormat.ERROR_DESCRIPTION,
                APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
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


    /**
     * Method to convert GraphqlComplexityInfo object to a JSONObject.
     *
     * @param graphqlComplexityInfoList GraphqlComplexityInfo object
     * @return json object which contains the policy definition
     */
    private JSONObject policyDefinitionToJson(List<GraphQLCustomComplexityInfoDTO> graphqlComplexityInfoList) {
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

        policyDefinition.put(GraphQLConstants.QUERY_ANALYSIS_COMPLEXITY, customComplexityObject);
        return policyDefinition;
    }
}
