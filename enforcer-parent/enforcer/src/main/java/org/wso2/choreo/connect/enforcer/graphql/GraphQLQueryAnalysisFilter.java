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
import org.wso2.choreo.connect.enforcer.commons.Filter;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.APIConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.graphql.analyzer.QueryMutationAnalyzer;

import java.util.Map;

/**
 * This Handler can be used to analyse GraphQL Query. This implementation uses previously set
 * complexity and depth limitation to block the complex queries before it reaches the backend.
 */
public class GraphQLQueryAnalysisFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(GraphQLQueryAnalysisFilter.class);
    private QueryMutationAnalyzer queryMutationAnalyzer;

    @Override
    public void init(APIConfig apiConfig, Map<String, String> configProperties) {
        GraphQLSchema schema = apiConfig.getGraphQLSchemaDTO().getGraphQLSchema();
        queryMutationAnalyzer = new QueryMutationAnalyzer(schema);
    }

    @Override
    public boolean handleRequest(RequestContext requestContext) {
        String payload = requestContext.getRequestPayload();
        if (!isSuccessQueryAnalysis(requestContext, payload)) {
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
    private boolean isSuccessQueryAnalysis(RequestContext requestContext, String payload) {
        try {
            return queryMutationAnalyzer.analyseQueryMutationDepth(requestContext, payload) &&
                    queryMutationAnalyzer.analyseQueryMutationComplexity(requestContext, payload);
        } catch (Exception e) {
            logger.error("Policy definition parsing failed for API : {} version : {}",
                    requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion(),
                    ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 7300), e);
            handleFailure(requestContext);
            return false;
        }
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
}
