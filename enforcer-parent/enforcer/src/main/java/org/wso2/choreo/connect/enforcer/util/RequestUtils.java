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

package org.wso2.choreo.connect.enforcer.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for processing request properties.
 */
public class RequestUtils {

    public static String constructQueryParamString(boolean removeAllQueryParams, String requestPath,
            Map<String, String> currentQueryParamMap, List<String> queryParamsToRemove,
            Map<String, String> queryParamsToAdd) {
        // If no query parameters needs to be removed/added, then the request path can
        // be applied as it is.
        if (!removeAllQueryParams && queryParamsToRemove.size() == 0 && queryParamsToAdd.size() == 0) {
            return requestPath;
        }

        Map<String, String> queryParamMap = new HashMap<>();
        if (currentQueryParamMap != null) {
            queryParamMap.putAll(currentQueryParamMap);
        }

        if (queryParamsToAdd != null) {
            queryParamMap.putAll(queryParamsToAdd);
        }

        String pathWithoutQueryParams = requestPath.split("\\?")[0];
        StringBuilder requestPathBuilder = new StringBuilder(pathWithoutQueryParams);
        int count = 0;
        if (!removeAllQueryParams && queryParamMap.size() > 0) {
            for (String queryParam : queryParamMap.keySet()) {
                if (queryParamsToRemove.contains(queryParam)) {
                    continue;
                }
                if (count == 0) {
                    requestPathBuilder.append("?");
                } else {
                    requestPathBuilder.append("&");
                }
                requestPathBuilder.append(queryParam);
                if (queryParamMap.get(queryParam) != null) {
                    requestPathBuilder.append("=").append(queryParamMap.get(queryParam));
                }
                count++;
            }
        }
        return requestPathBuilder.toString();
    }

}
