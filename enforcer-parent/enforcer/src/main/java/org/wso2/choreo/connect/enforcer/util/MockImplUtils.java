/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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
package org.wso2.choreo.connect.enforcer.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.api.ResponseObject;
import org.wso2.choreo.connect.enforcer.commons.model.MockedApiConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedContentExamples;
import org.wso2.choreo.connect.enforcer.commons.model.MockedHeaderConfig;
import org.wso2.choreo.connect.enforcer.commons.model.MockedResponseConfig;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * MockImplUtils contains mock response generation related methods.
 */
public class MockImplUtils {

    private static final Logger log = LogManager.getLogger(MockImplUtils.class);

    /**
     * Handles mock API call and prepares response object considering provided values in the request.
     *
     * @param requestContext request context
     * @param responseObject response object for the mock API call
     */
    public static void processMockedApiCall(RequestContext requestContext, ResponseObject responseObject) {
        responseObject.setDirectResponse(true);
        Map<String, String> preferences = new HashMap<>();
        // only getting first operation is enough as only one resource config will be present for mock APIs.
        MockedApiConfig mockedApiConfig = requestContext.getMatchedResourcePaths().get(0).getMockedApiConfig();
        Map<String, String> headersMap = requestContext.getHeaders();
        String[] acceptType = new String[]{};
        if (headersMap.containsKey(APIConstants.ACCEPT_HEADER)) {
            acceptType = headersMap.get(APIConstants.ACCEPT_HEADER).split(",");
        }
        // check prefer header for selected example
        if (headersMap.containsKey(APIConstants.PREFER_HEADER)) {
            preferences = processPreferHeader(headersMap.get(APIConstants.PREFER_HEADER));
        }
        setMockApiResponse(responseObject, preferences, mockedApiConfig, acceptType);
    }

    public static Map<String, String> processPreferHeader(String headerValue) {
        Map<String, String> preferences = new HashMap<>();
        String[] paramMap = headerValue.split(",");
        for (String param : paramMap) {
            if (StringUtils.countMatches(param, "=") == 1) {
                String[] keyValue = param.split("=");
                String key = keyValue[0].strip().toLowerCase();
                if (APIConstants.PREFER_KEYS.contains(key)) {
                    preferences.put(key, keyValue[1].strip().toLowerCase());
                }
            }
        }
        return preferences;
    }

    /**
     * Prepares mock API response considering the properties map and mockedApiConfig.
     *
     * @param responseObject  Response object (represents response for the API call)
     * @param preferences     A map which includes values specified in headers or query parameters
     * @param mockedApiConfig Holds the JSON values specified in the mock API implementation
     * @param acceptTypes     Denotes accepted contend type as the response
     */
    private static void setMockApiResponse(ResponseObject responseObject, Map<String, String> preferences,
                                           MockedApiConfig mockedApiConfig, String[] acceptTypes) {
        String preferCode = "";
        String preferExample = "";
        int statusCode = 200;
        String mediaType = "application/json";
        if (preferences.containsKey(APIConstants.PREFER_CODE)) {
            preferCode = preferences.get(APIConstants.PREFER_CODE);
            boolean isInvalidCode = false;
            try {
                if (preferCode.length() == 3) {
                    statusCode = Integer.parseInt(preferCode);
                } else {
                    isInvalidCode = true;
                }
            } catch (NumberFormatException e) {
                isInvalidCode = true;
            }
            if (isInvalidCode) {
                log.error("Mock API request contains invalid value for code preference.");
                responseObject.setErrorCode(GeneralErrorCodeConstants.MockImpl.BAD_REQUEST_CODE);
                responseObject.setStatusCode(APIConstants.StatusCodes.BAD_REQUEST_ERROR.getCode());
                responseObject.setErrorMessage(APIConstants.BAD_REQUEST_MESSAGE);
                responseObject.setErrorDescription("Invalid format for code preference");
                return;
            }
        }
        if (preferences.containsKey(APIConstants.PREFER_EXAMPLE)) {
            preferExample = preferences.get(APIConstants.PREFER_EXAMPLE);
        }

        Map<String, MockedResponseConfig> responseConfigList = mockedApiConfig.getResponses();
        MockedResponseConfig responseConfig;
        if (responseConfigList.isEmpty()) {
            log.debug("API operation does not have mock examples configured.");
            setMockApiErrorResponse(responseObject, "");
            return;
        } else if (!preferCode.isEmpty()) {
            if (responseConfigList.containsKey(preferCode)) {
                responseConfig = responseConfigList.get(preferCode);
            } else if (responseConfigList.containsKey(preferCode.substring(0, 2) + "x")) {
                responseConfig = responseConfigList.get(preferCode.substring(0, 2) + "x");
            } else if (responseConfigList.containsKey(preferCode.charAt(0) + "xx")) {
                responseConfig = responseConfigList.get(preferCode.charAt(0) + "xx");
            } else {
                setMockApiErrorResponse(responseObject, "Preferred code " + preferCode +
                        " is not supported for this resource.");
                return;
            }
        } else if (responseConfigList.containsKey(APIConstants.DEFAULT)) {
            responseConfig = responseConfigList.get(APIConstants.DEFAULT);
        } else {
            Map.Entry<String, MockedResponseConfig> firstEntry =
                    responseConfigList.entrySet().stream().findFirst().get();
            responseConfig = firstEntry.getValue();
            // adapter has validated this already to contain only x and integers
            String mockedStatusCode = firstEntry.getKey().replace("x", "0");
            statusCode = Integer.parseInt(mockedStatusCode);
        }

        MockedContentExamples contentExamples = null;
        if (responseConfig.getContentMap() != null && responseConfig.getContentMap().size() > 0) {
            if (acceptTypes.length < 1) {
                if (responseConfig.getContentMap().containsKey(APIConstants.APPLICATION_JSON)) {
                    mediaType = APIConstants.APPLICATION_JSON;
                    contentExamples = responseConfig.getContentMap().get(APIConstants.APPLICATION_JSON);
                } else {
                    Map.Entry<String, MockedContentExamples> firstExample =
                            responseConfig.getContentMap().entrySet().stream().findFirst().get();
                    contentExamples = firstExample.getValue();
                    mediaType = firstExample.getKey();
                }
            } else {
                for (String acceptType : acceptTypes) {
                    String[] acceptTypeValues = acceptType.split(";");
                    String acceptMediaType = acceptTypeValues[0].strip();
                    //todo(amali) handle q priorities
                    for (String mt : responseConfig.getContentMap().keySet()) {
                        String regex = ("\\Q" + acceptMediaType + "\\E").replace("*", "\\E.*\\Q");
                        if (mt.matches(regex)) {
                            contentExamples = responseConfig.getContentMap().get(mt);
                            mediaType = mt;
                            break;
                        }
                    }
                }
                if (contentExamples == null) {
                    setMockApiErrorResponse(responseObject, "Accept type " + Arrays.toString(acceptTypes) +
                            " is not supported for this resource");
                    return;
                }
            }
        }

        // even if content is empty for the response, example headers with empty body will be sent
        String content = "";
        if (!preferExample.isEmpty()) {
            if (contentExamples == null || !contentExamples.getExampleMap().containsKey(preferExample)) {
                setMockApiErrorResponse(responseObject, "Example preference " + preferExample +
                        " is not supported for this resource");
                return;
            }
            content = contentExamples.getExampleMap().get(preferExample);

        } else if (contentExamples != null && contentExamples.getExampleMap().size() > 0) {
            content = contentExamples.getExampleMap().entrySet().stream().findFirst().get().getValue();
        }

        responseObject.setStatusCode(statusCode);
        Map<String, String> headerMap = responseObject.getHeaderMap();
        if (responseConfig.getHeaders() != null && !responseConfig.getHeaders().isEmpty()) {
            // iterates over the headers list in the mock API JSON.
            for (MockedHeaderConfig header : responseConfig.getHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }
        }
        headerMap.put(APIConstants.CONTENT_TYPE_HEADER, mediaType);
        responseObject.setHeaderMap(headerMap);
        responseObject.setResponseContent(content);
    }

    private static void setMockApiErrorResponse(ResponseObject responseObject, String message) {
        log.error("Cannot process the mock API request. " + message);
        responseObject.setStatusCode(APIConstants.StatusCodes.NOT_IMPLEMENTED_ERROR.getCode());
        responseObject.setErrorCode(GeneralErrorCodeConstants.MockImpl.NOT_IMPLEMENTED_CODE);
        responseObject.setErrorMessage(APIConstants.NOT_IMPLEMENTED_MESSAGE);
        responseObject.setErrorDescription(message);
    }
}
