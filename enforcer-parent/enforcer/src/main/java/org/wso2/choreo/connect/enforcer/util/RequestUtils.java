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

import java.util.List;
import java.util.Map;

/**
 * Utility class for processing request properties.
 */
public class RequestUtils {

    public static String constructQueryParamString(String requestPath, Map<String, String> queryParamMap,
            List<String> queryParamsToRemove) {
        // If no query parameters needs to be removed, then the request path can be
        // applied as it is.
        if (queryParamsToRemove.size() == 0) {
            return requestPath;
        }

        String pathWithoutQueryParams = requestPath.split("\\?")[0];
        StringBuilder requestPathBuilder = new StringBuilder(pathWithoutQueryParams);
        int count = 0;
        if (queryParamMap.size() > 0) {
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
